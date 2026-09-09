package com.pepology.mostrador.services;

import com.pepology.mostrador.dto.sale.SaleLineRequest;
import com.pepology.mostrador.dto.sale.SalePaymentRequest;
import com.pepology.mostrador.dto.sale.SaleRequest;
import com.pepology.mostrador.dto.sale.SaleResponse;
import com.pepology.mostrador.exceptions.BusinessRuleException;
import com.pepology.mostrador.mappers.SaleMapper;
import com.pepology.mostrador.models.entities.CashSessionEntity;
import com.pepology.mostrador.models.entities.ProductVariantEntity;
import com.pepology.mostrador.models.entities.SaleEntity;
import com.pepology.mostrador.models.entities.SaleLineEntity;
import com.pepology.mostrador.models.entities.SalePaymentEntity;
import com.pepology.mostrador.models.enums.SaleStatus;
import com.pepology.mostrador.models.enums.SaleType;
import com.pepology.mostrador.repositories.SaleLineRepository;
import com.pepology.mostrador.repositories.SalePaymentRepository;
import com.pepology.mostrador.repositories.SaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ventas del mostrador. Precio e IVA se congelan de la ficha; el stock sale por movimiento.
 */
@Service
@RequiredArgsConstructor
public class SaleService {

	private final SaleRepository saleRepository;
	private final SaleLineRepository saleLineRepository;
	private final SalePaymentRepository salePaymentRepository;
	private final CashService cashService;
	private final ProductService productService;
	private final StockService stockService;
	private final SaleMapper saleMapper;

	/** Cobra una venta en la caja abierta de hoy. */
	@Transactional
	public SaleResponse create(Long branchId, SaleRequest request) {
		CashSessionEntity session = cashService.requireOpenToday(branchId);
		Map<Long, Integer> quantities = mergeLines(request.lines());
		List<PreparedLine> prepared = new ArrayList<>();
		BigDecimal subtotal = BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY);
		for (var entry : quantities.entrySet()) {
			ProductVariantEntity variant = productService.requireActiveVariant(entry.getKey());
			if (!variant.getProduct().isActive()) {
				throw new BusinessRuleException("No se puede vender un producto dado de baja");
			}
			BigDecimal unit = variant.getPrice().setScale(2, RoundingMode.HALF_UP);
			BigDecimal lineTotal = unit.multiply(BigDecimal.valueOf(entry.getValue()))
					.setScale(2, RoundingMode.HALF_UP);
			subtotal = subtotal.add(lineTotal);
			prepared.add(new PreparedLine(variant, entry.getValue(), unit, lineTotal));
		}
		BigDecimal total = subtotal;
		assertPaymentsMatch(request.payments(), total);
		SaleEntity sale = saleRepository.save(SaleEntity.of(
				session.getBranch(),
				session,
				SaleType.NORMAL,
				subtotal,
				BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY),
				total,
				SaleStatus.CERRADA));
		List<SaleLineEntity> lines = new ArrayList<>();
		for (PreparedLine preparedLine : prepared) {
			SaleLineEntity line = saleLineRepository.save(SaleLineEntity.of(
					sale,
					preparedLine.variant(),
					preparedLine.quantity(),
					preparedLine.unit(),
					preparedLine.lineTotal(),
					preparedLine.variant().getProduct().getVatRate()));
			lines.add(line);
			if (preparedLine.variant().getProduct().isTracksStock()) {
				stockService.registerSale(
						preparedLine.variant().getId(),
						branchId,
						preparedLine.quantity(),
						"venta " + sale.getId());
			}
		}
		List<SalePaymentEntity> payments = new ArrayList<>();
		for (SalePaymentRequest paymentRequest : request.payments()) {
			payments.add(salePaymentRepository.save(SalePaymentEntity.of(
					sale,
					paymentRequest.method(),
					paymentRequest.amount().setScale(2, RoundingMode.HALF_UP),
					BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY))));
		}
		return saleMapper.toResponse(sale, lines, payments);
	}

	/** Tickets de la planilla de hoy de esa sucursal. */
	@Transactional(readOnly = true)
	public List<SaleResponse> listToday(Long branchId) {
		CashSessionEntity session = cashService.todaySession(branchId);
		return saleRepository.findBySessionOrderBySoldAtDesc(session).stream()
				.map(sale -> saleMapper.toResponse(
						sale,
						saleLineRepository.findBySale(sale),
						salePaymentRepository.findBySale(sale)))
				.toList();
	}

	/** Junta cantidades si la misma variante viene dos veces. */
	private static Map<Long, Integer> mergeLines(List<SaleLineRequest> lines) {
		Map<Long, Integer> quantities = new LinkedHashMap<>();
		for (SaleLineRequest line : lines) {
			quantities.merge(line.variantId(), line.quantity(), Integer::sum);
		}
		return quantities;
	}

	/** Los pagos tienen que cubrir el total exacto, ni más ni menos. */
	private static void assertPaymentsMatch(List<SalePaymentRequest> payments, BigDecimal total) {
		BigDecimal paid = payments.stream()
				.map(payment -> payment.amount().setScale(2, RoundingMode.HALF_UP))
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		if (paid.compareTo(total) != 0) {
			throw new BusinessRuleException("Los pagos tienen que sumar el total de la venta");
		}
	}

	/** Línea ya valorada con el precio de la ficha. */
	private record PreparedLine(
			ProductVariantEntity variant,
			int quantity,
			BigDecimal unit,
			BigDecimal lineTotal) {
	}
}
