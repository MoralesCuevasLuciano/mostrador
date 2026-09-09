package com.pepology.mostrador.services;

import com.pepology.mostrador.dto.cash.CashMovementResponse;
import com.pepology.mostrador.dto.cash.CashSessionResponse;
import com.pepology.mostrador.exceptions.BusinessRuleException;
import com.pepology.mostrador.exceptions.NotFoundException;
import com.pepology.mostrador.mappers.CashMapper;
import com.pepology.mostrador.models.entities.BranchEntity;
import com.pepology.mostrador.models.entities.CashMovementEntity;
import com.pepology.mostrador.models.entities.CashSessionEntity;
import com.pepology.mostrador.models.enums.CashMovementType;
import com.pepology.mostrador.repositories.CashMovementRepository;
import com.pepology.mostrador.repositories.CashSessionRepository;
import com.pepology.mostrador.repositories.SalePaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Caja por sucursal y fecha. closing_amount vacío = abierta.
 * El monto esperado y la diferencia no se guardan: se derivan al armar la respuesta.
 * Esperado = apertura + ventas en efectivo + salidas + ingresos.
 */
@Service
@RequiredArgsConstructor
public class CashService {

	private static final ZoneId ZONE = ZoneId.of("America/Argentina/Buenos_Aires");

	private final CashSessionRepository cashSessionRepository;
	private final CashMovementRepository cashMovementRepository;
	private final SalePaymentRepository salePaymentRepository;
	private final BranchService branchService;
	private final CashMapper cashMapper;

	/** Planilla de hoy: la abre si no existe, heredando el cierre anterior. */
	@Transactional
	public CashSessionResponse current(Long branchId) {
		return getOrOpen(branchId, today());
	}

	/** Planilla de ese día. 404 si todavía no se abrió. */
	@Transactional(readOnly = true)
	public CashSessionResponse get(Long branchId, LocalDate businessDate) {
		return toResponse(requireSession(branchId, businessDate));
	}

	/**
	 * Planillas de un local entre from y to (inclusive), de la más reciente a la más vieja.
	 * El rango no puede superar 62 días para no devolver el historial entero.
	 */
	@Transactional(readOnly = true)
	public List<CashSessionResponse> listByBranch(Long branchId, LocalDate from, LocalDate to) {
		if (from.isAfter(to)) {
			throw new BusinessRuleException("La fecha desde no puede ser posterior a hasta");
		}
		long days = ChronoUnit.DAYS.between(from, to) + 1;
		if (days > 62) {
			throw new BusinessRuleException("El rango no puede superar los 62 días");
		}
		BranchEntity branch = branchService.requireActive(branchId);
		return cashSessionRepository
				.findByBranchAndBusinessDateBetweenOrderByBusinessDateDesc(branch, from, to)
				.stream()
				.map(this::toResponse)
				.toList();
	}

	/** Planillas abiertas de un local, para detectar una caja anterior sin cerrar. */
	@Transactional(readOnly = true)
	public List<CashSessionResponse> listOpen(Long branchId) {
		BranchEntity branch = branchService.requireActive(branchId);
		return cashSessionRepository.findByBranchAndClosingAmountIsNullOrderByBusinessDateDesc(branch).stream()
				.map(this::toResponse)
				.toList();
	}

	/** La última planilla anterior a esa fecha. 404 si el local nunca abrió caja. */
	@Transactional(readOnly = true)
	public CashSessionResponse previous(Long branchId, LocalDate before) {
		BranchEntity branch = branchService.requireActive(branchId);
		CashSessionEntity session = cashSessionRepository
				.findFirstByBranchAndBusinessDateLessThanOrderByBusinessDateDesc(branch, before)
				.orElseThrow(() -> new NotFoundException("No hay una caja anterior al " + before));
		return toResponse(session);
	}

	/**
	 * Devuelve la planilla de ese día. Si no hay, la abre con el cierre anterior
	 * (o cero si es la primera). No abre si hay una caja previa sin cerrar.
	 */
	@Transactional
	public CashSessionResponse getOrOpen(Long branchId, LocalDate businessDate) {
		return toResponse(getOrOpenSession(branchId, businessDate));
	}

	/** El usuario cuenta el cajón al abrir. Pisa el monto heredado y marca opening_counted_at. */
	@Transactional
	public CashSessionResponse countOpening(Long branchId, LocalDate businessDate, BigDecimal countedAmount) {
		BigDecimal amount = requireNonNegative(countedAmount, "El recuento de apertura no puede ser negativo");
		CashSessionEntity session = requireOpen(getOrOpenSession(branchId, businessDate));
		session.setOpeningAmount(amount);
		session.setOpeningCountedAt(LocalDateTime.now());
		return toResponse(session);
	}

	/** Plata que sale del cajón por seguridad y sigue siendo del negocio. */
	@Transactional
	public CashMovementResponse registerWithdrawal(
			Long branchId,
			LocalDate businessDate,
			BigDecimal amount,
			String description) {
		return registerSigned(branchId, businessDate, CashMovementType.RETIRO_RESGUARDO, amount, description);
	}

	/** Vale: sale plata y genera deuda del empleado (la cuenta del empleado llega después). */
	@Transactional
	public CashMovementResponse registerVale(
			Long branchId,
			LocalDate businessDate,
			BigDecimal amount,
			String description) {
		return registerSigned(branchId, businessDate, CashMovementType.VALE, amount, description);
	}

	/** Gasto o pago a proveedor. */
	@Transactional
	public CashMovementResponse registerExpense(
			Long branchId,
			LocalDate businessDate,
			BigDecimal amount,
			String description) {
		return registerSigned(branchId, businessDate, CashMovementType.GASTO, amount, description);
	}

	/** Ingreso de efectivo (reponer cambio). */
	@Transactional
	public CashMovementResponse registerCashIn(
			Long branchId,
			LocalDate businessDate,
			BigDecimal amount,
			String description) {
		return registerSigned(branchId, businessDate, CashMovementType.INGRESO_EFECTIVO, amount, description);
	}

	/** Historial de una planilla, del más reciente al más viejo. */
	@Transactional(readOnly = true)
	public List<CashMovementResponse> listMovements(Long branchId, LocalDate businessDate) {
		CashSessionEntity session = requireSession(branchId, businessDate);
		return cashMovementRepository.findBySessionOrderByMovementAtDesc(session).stream()
				.map(cashMapper::toMovement)
				.toList();
	}

	/** Corrige tipo, monto o nota de un movimiento. Solo si la caja sigue abierta. */
	@Transactional
	public CashMovementResponse updateMovement(
			Long branchId,
			Long movementId,
			CashMovementType type,
			BigDecimal amount,
			String description) {
		CashMovementEntity movement = requireOpenMovement(branchId, movementId);
		BigDecimal positive = requirePositive(amount);
		movement.setMovementType(type);
		movement.setAmount(positive.multiply(BigDecimal.valueOf(type.sign())));
		movement.setDescription(blankToNull(description));
		return cashMapper.toMovement(movement);
	}

	/** Borra un movimiento. Solo si la caja sigue abierta. */
	@Transactional
	public void deleteMovement(Long branchId, Long movementId) {
		CashMovementEntity movement = requireOpenMovement(branchId, movementId);
		cashMovementRepository.delete(movement);
	}

	/**
	 * Cierra la caja: congela ventas en efectivo, salidas e ingresos.
	 * El monto esperado y la diferencia se calculan en la respuesta, no se persisten.
	 */
	@Transactional
	public CashSessionResponse close(
			Long branchId,
			LocalDate businessDate,
			BigDecimal closingAmount,
			String note) {
		BigDecimal counted = requireNonNegative(closingAmount, "El cierre no puede ser negativo");
		CashSessionEntity session = requireOpen(getOrOpenSession(branchId, businessDate));
		List<CashMovementEntity> movements = cashMovementRepository.findBySessionOrderByMovementAtDesc(session);
		session.setTotalCashSales(cashSales(session));
		session.setTotalCashOut(sumBySign(movements, -1));
		session.setTotalCashIn(sumBySign(movements, 1));
		session.setClosingAmount(counted);
		session.setClosedAt(LocalDateTime.now());
		session.setNote(blankToNull(note));
		return toResponse(session);
	}

	/** Caja de hoy, abierta. Para cobrar una venta. */
	@Transactional
	public CashSessionEntity requireOpenToday(Long branchId) {
		return requireOpen(getOrOpenSession(branchId, today()));
	}

	/** Planilla de hoy: la abre si no existe, aunque ya esté cerrada. */
	@Transactional
	public CashSessionEntity todaySession(Long branchId) {
		return getOrOpenSession(branchId, today());
	}

	/** Busca la planilla o la crea heredando el último cierre. */
	private CashSessionEntity getOrOpenSession(Long branchId, LocalDate businessDate) {
		BranchEntity branch = branchService.requireActive(branchId);
		return cashSessionRepository.findByBranchAndBusinessDate(branch, businessDate)
				.orElseGet(() -> open(branch, businessDate));
	}

	/** Abre una planilla nueva. El cajón no se vacía: arrastra el último cierre. */
	private CashSessionEntity open(BranchEntity branch, LocalDate businessDate) {
		var previous = cashSessionRepository
				.findFirstByBranchAndBusinessDateLessThanOrderByBusinessDateDesc(branch, businessDate);
		if (previous.isPresent() && previous.get().getClosingAmount() == null) {
			throw new BusinessRuleException("Hay una caja anterior sin cerrar");
		}
		BigDecimal opening = previous
				.map(CashSessionEntity::getClosingAmount)
				.orElse(BigDecimal.ZERO)
				.setScale(2, RoundingMode.UNNECESSARY);
		return cashSessionRepository.save(CashSessionEntity.of(branch, businessDate, opening));
	}

	/** El usuario manda un monto positivo; el tipo decide si entra o sale. */
	private CashMovementResponse registerSigned(
			Long branchId,
			LocalDate businessDate,
			CashMovementType type,
			BigDecimal amount,
			String description) {
		BigDecimal positive = requirePositive(amount);
		CashSessionEntity session = requireOpen(getOrOpenSession(branchId, businessDate));
		CashMovementEntity saved = cashMovementRepository.save(CashMovementEntity.of(
				session,
				type,
				positive.multiply(BigDecimal.valueOf(type.sign())),
				blankToNull(description),
				null));
		return cashMapper.toMovement(saved);
	}

	private CashSessionEntity requireSession(Long branchId, LocalDate businessDate) {
		BranchEntity branch = branchService.requireActive(branchId);
		return cashSessionRepository.findByBranchAndBusinessDate(branch, businessDate)
				.orElseThrow(() -> new NotFoundException(
						"No hay caja para esa sucursal el " + businessDate));
	}

	/** Movimiento de esa sucursal, con la planilla todavía abierta. */
	private CashMovementEntity requireOpenMovement(Long branchId, Long movementId) {
		branchService.requireActive(branchId);
		CashMovementEntity movement = cashMovementRepository.findById(movementId)
				.orElseThrow(() -> new NotFoundException("No existe ese movimiento de caja"));
		if (!movement.getSession().getBranch().getId().equals(branchId)) {
			throw new NotFoundException("No existe ese movimiento de caja");
		}
		requireOpen(movement.getSession());
		return movement;
	}

	private static CashSessionEntity requireOpen(CashSessionEntity session) {
		if (session.getClosingAmount() != null) {
			throw new BusinessRuleException("La caja de ese día ya está cerrada");
		}
		return session;
	}

	private CashSessionResponse toResponse(CashSessionEntity session) {
		BigDecimal liveOut = BigDecimal.ZERO;
		BigDecimal liveIn = BigDecimal.ZERO;
		BigDecimal liveSales = BigDecimal.ZERO;
		if (session.getClosingAmount() == null) {
			List<CashMovementEntity> movements = cashMovementRepository.findBySessionOrderByMovementAtDesc(session);
			liveOut = sumBySign(movements, -1);
			liveIn = sumBySign(movements, 1);
			liveSales = cashSales(session);
		}
		return cashMapper.toSession(session, liveSales, liveOut, liveIn);
	}

	/** Efectivo cobrado en ventas cerradas de esa planilla. */
	private BigDecimal cashSales(CashSessionEntity session) {
		BigDecimal sum = salePaymentRepository.sumCashBySession(session);
		return (sum == null ? BigDecimal.ZERO : sum).setScale(2, RoundingMode.HALF_UP);
	}

	/** Suma los montos del signo pedido: -1 salidas, +1 ingresos. */
	private static BigDecimal sumBySign(List<CashMovementEntity> movements, int signum) {
		return movements.stream()
				.map(CashMovementEntity::getAmount)
				.filter(amount -> amount.signum() == signum)
				.reduce(BigDecimal.ZERO, BigDecimal::add)
				.setScale(2, RoundingMode.UNNECESSARY);
	}

	private static BigDecimal requirePositive(BigDecimal amount) {
		BigDecimal scaled = requireAmount(amount);
		if (scaled.compareTo(BigDecimal.ZERO) <= 0) {
			throw new BusinessRuleException("El monto tiene que ser mayor a cero");
		}
		return scaled;
	}

	private static BigDecimal requireNonNegative(BigDecimal amount, String message) {
		BigDecimal scaled = requireAmount(amount);
		if (scaled.compareTo(BigDecimal.ZERO) < 0) {
			throw new BusinessRuleException(message);
		}
		return scaled;
	}

	private static BigDecimal requireAmount(BigDecimal amount) {
		if (amount == null) {
			throw new BusinessRuleException("El monto es obligatorio");
		}
		return amount.setScale(2, RoundingMode.HALF_UP);
	}

	private static LocalDate today() {
		return LocalDate.now(ZONE);
	}

	private static String blankToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
