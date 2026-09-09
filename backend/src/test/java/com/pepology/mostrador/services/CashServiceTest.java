package com.pepology.mostrador.services;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests de CashService: apertura heredada, recuento, movimientos y cierre.
 */
@ExtendWith(MockitoExtension.class)
class CashServiceTest {

	private static final LocalDate DAY = LocalDate.of(2026, 9, 8);

	@Mock
	private CashSessionRepository cashSessionRepository;
	@Mock
	private CashMovementRepository cashMovementRepository;
	@Mock
	private BranchService branchService;
	@Mock
	private SalePaymentRepository salePaymentRepository;

	private CashService cashService;
	private BranchEntity branch;

	@BeforeEach
	void setUp() {
		cashService = new CashService(
				cashSessionRepository,
				cashMovementRepository,
				salePaymentRepository,
				branchService,
				new CashMapper());
		branch = BranchEntity.of("Sucursal 1", "Calle falsa 123", null, null);
		ReflectionTestUtils.setField(branch, "id", 1L);
		lenient().when(branchService.requireActive(1L)).thenReturn(branch);
		lenient().when(cashMovementRepository.findBySessionOrderByMovementAtDesc(any()))
				.thenReturn(List.of());
		lenient().when(salePaymentRepository.sumCashBySession(any())).thenReturn(BigDecimal.ZERO);
		stubSaves();
	}

	@Test
	void firstOpenStartsAtZero() {
		when(cashSessionRepository.findByBranchAndBusinessDate(branch, DAY)).thenReturn(Optional.empty());
		when(cashSessionRepository.findFirstByBranchAndBusinessDateLessThanOrderByBusinessDateDesc(branch, DAY))
				.thenReturn(Optional.empty());

		var session = cashService.getOrOpen(1L, DAY);

		assertTrue(session.open());
		assertEquals(0, session.openingAmount().compareTo(new BigDecimal("0.00")));
		assertFalse(session.openingCounted());
		assertEquals(0, session.expectedAmount().compareTo(new BigDecimal("0.00")));
		assertEquals(0, session.totalCashOut().compareTo(new BigDecimal("0.00")));
		assertEquals(0, session.totalCashIn().compareTo(new BigDecimal("0.00")));
		assertNull(session.difference());
	}

	@Test
	void openInheritsPreviousClosing() {
		CashSessionEntity previous = CashSessionEntity.of(branch, DAY.minusDays(1), new BigDecimal("100.00"));
		previous.setClosingAmount(new BigDecimal("430.50"));
		when(cashSessionRepository.findByBranchAndBusinessDate(branch, DAY)).thenReturn(Optional.empty());
		when(cashSessionRepository.findFirstByBranchAndBusinessDateLessThanOrderByBusinessDateDesc(branch, DAY))
				.thenReturn(Optional.of(previous));

		var session = cashService.getOrOpen(1L, DAY);

		assertEquals(0, session.openingAmount().compareTo(new BigDecimal("430.50")));
		assertFalse(session.openingCounted());
	}

	@Test
	void openRejectsIfPreviousStillOpen() {
		CashSessionEntity previous = CashSessionEntity.of(branch, DAY.minusDays(1), new BigDecimal("100.00"));
		when(cashSessionRepository.findByBranchAndBusinessDate(branch, DAY)).thenReturn(Optional.empty());
		when(cashSessionRepository.findFirstByBranchAndBusinessDateLessThanOrderByBusinessDateDesc(branch, DAY))
				.thenReturn(Optional.of(previous));

		assertThrows(BusinessRuleException.class, () -> cashService.getOrOpen(1L, DAY));
	}

	@Test
	void countOpeningMarksCountedAt() {
		CashSessionEntity existing = CashSessionEntity.of(branch, DAY, new BigDecimal("100.00"));
		ReflectionTestUtils.setField(existing, "id", 10L);
		when(cashSessionRepository.findByBranchAndBusinessDate(branch, DAY)).thenReturn(Optional.of(existing));

		var session = cashService.countOpening(1L, DAY, new BigDecimal("95.00"));

		assertEquals(0, session.openingAmount().compareTo(new BigDecimal("95.00")));
		assertTrue(session.openingCounted());
		assertNotNull(existing.getOpeningCountedAt());
	}

	@Test
	void withdrawalStoresNegativeAmount() {
		CashSessionEntity existing = CashSessionEntity.of(branch, DAY, new BigDecimal("200.00"));
		ReflectionTestUtils.setField(existing, "id", 10L);
		when(cashSessionRepository.findByBranchAndBusinessDate(branch, DAY)).thenReturn(Optional.of(existing));

		var movement = cashService.registerWithdrawal(1L, DAY, new BigDecimal("50.00"), "al banco");

		ArgumentCaptor<CashMovementEntity> captor = ArgumentCaptor.forClass(CashMovementEntity.class);
		verify(cashMovementRepository).save(captor.capture());
		assertEquals(CashMovementType.RETIRO_RESGUARDO, captor.getValue().getMovementType());
		assertEquals(0, captor.getValue().getAmount().compareTo(new BigDecimal("-50.00")));
		assertEquals(0, movement.amount().compareTo(new BigDecimal("-50.00")));
	}

	@Test
	void cashInStoresPositiveAmount() {
		CashSessionEntity existing = CashSessionEntity.of(branch, DAY, new BigDecimal("200.00"));
		ReflectionTestUtils.setField(existing, "id", 10L);
		when(cashSessionRepository.findByBranchAndBusinessDate(branch, DAY)).thenReturn(Optional.of(existing));

		cashService.registerCashIn(1L, DAY, new BigDecimal("80"), "cambio");

		ArgumentCaptor<CashMovementEntity> captor = ArgumentCaptor.forClass(CashMovementEntity.class);
		verify(cashMovementRepository).save(captor.capture());
		assertEquals(CashMovementType.INGRESO_EFECTIVO, captor.getValue().getMovementType());
		assertEquals(0, captor.getValue().getAmount().compareTo(new BigDecimal("80.00")));
	}

	@Test
	void closeFreezesMovementTotalAndDifference() {
		CashSessionEntity existing = CashSessionEntity.of(branch, DAY, new BigDecimal("200.00"));
		ReflectionTestUtils.setField(existing, "id", 10L);
		when(cashSessionRepository.findByBranchAndBusinessDate(branch, DAY)).thenReturn(Optional.of(existing));
		CashMovementEntity withdrawal = CashMovementEntity.of(
				existing, CashMovementType.RETIRO_RESGUARDO, new BigDecimal("-30.00"), null, null);
		when(cashMovementRepository.findBySessionOrderByMovementAtDesc(existing)).thenReturn(List.of(withdrawal));

		var closed = cashService.close(1L, DAY, new BigDecimal("165.00"), "faltó un poco");

		assertFalse(closed.open());
		assertEquals(0, closed.totalCashSales().compareTo(new BigDecimal("0.00")));
		assertEquals(0, closed.totalCashOut().compareTo(new BigDecimal("-30.00")));
		assertEquals(0, closed.totalCashIn().compareTo(new BigDecimal("0.00")));
		assertEquals(0, closed.expectedAmount().compareTo(new BigDecimal("170.00")));
		assertEquals(0, closed.difference().compareTo(new BigDecimal("-5.00")));
		assertEquals("faltó un poco", closed.note());
	}

	@Test
	void closeSplitsOutflowsAndInflows() {
		CashSessionEntity existing = CashSessionEntity.of(branch, DAY, new BigDecimal("200.00"));
		ReflectionTestUtils.setField(existing, "id", 10L);
		when(cashSessionRepository.findByBranchAndBusinessDate(branch, DAY)).thenReturn(Optional.of(existing));
		when(cashMovementRepository.findBySessionOrderByMovementAtDesc(existing)).thenReturn(List.of(
				CashMovementEntity.of(existing, CashMovementType.RETIRO_RESGUARDO, new BigDecimal("-80.00"), null, null),
				CashMovementEntity.of(existing, CashMovementType.GASTO, new BigDecimal("-120.00"), null, null),
				CashMovementEntity.of(existing, CashMovementType.VALE, new BigDecimal("-5.00"), null, null),
				CashMovementEntity.of(existing, CashMovementType.INGRESO_EFECTIVO, new BigDecimal("200.00"), null, null)));

		var closed = cashService.close(1L, DAY, new BigDecimal("195.00"), null);

		assertEquals(0, closed.totalCashOut().compareTo(new BigDecimal("-205.00")));
		assertEquals(0, closed.totalCashIn().compareTo(new BigDecimal("200.00")));
		assertEquals(0, closed.expectedAmount().compareTo(new BigDecimal("195.00")));
		assertEquals(0, closed.difference().compareTo(new BigDecimal("0.00")));
	}

	@Test
	void rejectsMovementOnClosedSession() {
		CashSessionEntity closed = CashSessionEntity.of(branch, DAY, new BigDecimal("200.00"));
		closed.setClosingAmount(new BigDecimal("200.00"));
		ReflectionTestUtils.setField(closed, "id", 10L);
		when(cashSessionRepository.findByBranchAndBusinessDate(branch, DAY)).thenReturn(Optional.of(closed));

		assertThrows(BusinessRuleException.class,
				() -> cashService.registerVale(1L, DAY, new BigDecimal("10.00"), null));
	}

	@Test
	void getMissingSessionIsNotFound() {
		when(cashSessionRepository.findByBranchAndBusinessDate(branch, DAY)).thenReturn(Optional.empty());

		assertThrows(NotFoundException.class, () -> cashService.get(1L, DAY));
	}

	@Test
	void openSessionExposesLiveTotalsWithoutPersisting() {
		CashSessionEntity existing = CashSessionEntity.of(branch, DAY, new BigDecimal("200.00"));
		ReflectionTestUtils.setField(existing, "id", 10L);
		when(cashSessionRepository.findByBranchAndBusinessDate(branch, DAY)).thenReturn(Optional.of(existing));
		when(cashMovementRepository.findBySessionOrderByMovementAtDesc(existing)).thenReturn(List.of(
				CashMovementEntity.of(existing, CashMovementType.VALE, new BigDecimal("-50.00"), null, null),
				CashMovementEntity.of(existing, CashMovementType.INGRESO_EFECTIVO, new BigDecimal("20.00"), null, null)));

		var session = cashService.get(1L, DAY);

		assertTrue(session.open());
		assertNull(existing.getTotalCashOut());
		assertNull(existing.getTotalCashIn());
		assertEquals(0, session.totalCashOut().compareTo(new BigDecimal("-50.00")));
		assertEquals(0, session.totalCashIn().compareTo(new BigDecimal("20.00")));
		assertEquals(0, session.expectedAmount().compareTo(new BigDecimal("170.00")));
	}

	@Test
	void listByBranchReturnsOnlyTheRange() {
		CashSessionEntity inRange = CashSessionEntity.of(branch, DAY, new BigDecimal("100.00"));
		ReflectionTestUtils.setField(inRange, "id", 10L);
		when(cashSessionRepository.findByBranchAndBusinessDateBetweenOrderByBusinessDateDesc(
				branch, DAY.minusDays(1), DAY.plusDays(1)))
				.thenReturn(List.of(inRange));

		var listed = cashService.listByBranch(1L, DAY.minusDays(1), DAY.plusDays(1));

		assertEquals(1, listed.size());
		assertEquals(DAY, listed.getFirst().businessDate());
	}

	@Test
	void listByBranchRejectsInvertedRange() {
		assertThrows(BusinessRuleException.class, () -> cashService.listByBranch(1L, DAY, DAY.minusDays(1)));
	}

	@Test
	void listByBranchRejectsRangeLongerThan62Days() {
		assertThrows(BusinessRuleException.class,
				() -> cashService.listByBranch(1L, DAY, DAY.plusDays(62)));
	}

	@Test
	void previousReturnsLastSessionBeforeDate() {
		CashSessionEntity previous = CashSessionEntity.of(branch, DAY.minusDays(1), new BigDecimal("80.00"));
		previous.setClosingAmount(new BigDecimal("80.00"));
		ReflectionTestUtils.setField(previous, "id", 9L);
		when(cashSessionRepository.findFirstByBranchAndBusinessDateLessThanOrderByBusinessDateDesc(branch, DAY))
				.thenReturn(Optional.of(previous));

		var response = cashService.previous(1L, DAY);

		assertEquals(DAY.minusDays(1), response.businessDate());
		assertEquals(0, response.closingAmount().compareTo(new BigDecimal("80.00")));
	}

	@Test
	void updateMovementChangesTypeAndAmount() {
		CashSessionEntity existing = openSession();
		CashMovementEntity movement = CashMovementEntity.of(
				existing, CashMovementType.VALE, new BigDecimal("-10.00"), "mal cargado", null);
		ReflectionTestUtils.setField(movement, "id", 50L);
		when(cashMovementRepository.findById(50L)).thenReturn(Optional.of(movement));

		var updated = cashService.updateMovement(
				1L, 50L, CashMovementType.GASTO, new BigDecimal("40.00"), "proveedor");

		assertEquals(CashMovementType.GASTO, updated.movementType());
		assertEquals(0, updated.amount().compareTo(new BigDecimal("-40.00")));
		assertEquals("proveedor", updated.description());
	}

	@Test
	void deleteMovementOnOpenSession() {
		CashSessionEntity existing = openSession();
		CashMovementEntity movement = CashMovementEntity.of(
				existing, CashMovementType.VALE, new BigDecimal("-10.00"), null, null);
		ReflectionTestUtils.setField(movement, "id", 50L);
		when(cashMovementRepository.findById(50L)).thenReturn(Optional.of(movement));

		cashService.deleteMovement(1L, 50L);

		verify(cashMovementRepository).delete(movement);
	}

	@Test
	void rejectsUpdateOnClosedSession() {
		CashSessionEntity closed = CashSessionEntity.of(branch, DAY, new BigDecimal("200.00"));
		closed.setClosingAmount(new BigDecimal("200.00"));
		ReflectionTestUtils.setField(closed, "id", 10L);
		CashMovementEntity movement = CashMovementEntity.of(
				closed, CashMovementType.VALE, new BigDecimal("-10.00"), null, null);
		ReflectionTestUtils.setField(movement, "id", 50L);
		when(cashMovementRepository.findById(50L)).thenReturn(Optional.of(movement));

		assertThrows(BusinessRuleException.class,
				() -> cashService.updateMovement(1L, 50L, CashMovementType.VALE, new BigDecimal("10.00"), null));
	}

	@Test
	void rejectsDeleteOnClosedSession() {
		CashSessionEntity closed = CashSessionEntity.of(branch, DAY, new BigDecimal("200.00"));
		closed.setClosingAmount(new BigDecimal("200.00"));
		ReflectionTestUtils.setField(closed, "id", 10L);
		CashMovementEntity movement = CashMovementEntity.of(
				closed, CashMovementType.VALE, new BigDecimal("-10.00"), null, null);
		ReflectionTestUtils.setField(movement, "id", 50L);
		when(cashMovementRepository.findById(50L)).thenReturn(Optional.of(movement));

		assertThrows(BusinessRuleException.class, () -> cashService.deleteMovement(1L, 50L));
	}

	/** Planilla abierta de la sucursal 1. */
	private CashSessionEntity openSession() {
		CashSessionEntity existing = CashSessionEntity.of(branch, DAY, new BigDecimal("200.00"));
		ReflectionTestUtils.setField(existing, "id", 10L);
		return existing;
	}

	/** Save de sesión y movimiento: les pone id y devuelve el mismo objeto. */
	private void stubSaves() {
		AtomicLong sessionIds = new AtomicLong(10);
		AtomicLong movementIds = new AtomicLong(100);
		lenient().when(cashSessionRepository.save(any(CashSessionEntity.class))).thenAnswer(invocation -> {
			CashSessionEntity session = invocation.getArgument(0);
			if (session.getId() == null) {
				ReflectionTestUtils.setField(session, "id", sessionIds.getAndIncrement());
			}
			return session;
		});
		lenient().when(cashMovementRepository.save(any(CashMovementEntity.class))).thenAnswer(invocation -> {
			CashMovementEntity movement = invocation.getArgument(0);
			if (movement.getId() == null) {
				ReflectionTestUtils.setField(movement, "id", movementIds.getAndIncrement());
			}
			return movement;
		});
	}
}
