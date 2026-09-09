package com.pepology.mostrador.services;

import com.pepology.mostrador.dto.sale.SaleLineRequest;
import com.pepology.mostrador.dto.sale.SalePaymentRequest;
import com.pepology.mostrador.dto.sale.SaleRequest;
import com.pepology.mostrador.exceptions.BusinessRuleException;
import com.pepology.mostrador.mappers.SaleMapper;
import com.pepology.mostrador.models.entities.BranchEntity;
import com.pepology.mostrador.models.entities.CashSessionEntity;
import com.pepology.mostrador.models.entities.ProductEntity;
import com.pepology.mostrador.models.entities.ProductVariantEntity;
import com.pepology.mostrador.models.entities.SaleEntity;
import com.pepology.mostrador.models.entities.SaleLineEntity;
import com.pepology.mostrador.models.entities.SalePaymentEntity;
import com.pepology.mostrador.models.enums.ItemCondition;
import com.pepology.mostrador.models.enums.SalePaymentMethod;
import com.pepology.mostrador.repositories.SaleLineRepository;
import com.pepology.mostrador.repositories.SalePaymentRepository;
import com.pepology.mostrador.repositories.SaleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests de SaleService: cobra, descuenta stock y exige que los pagos cierren.
 */
@ExtendWith(MockitoExtension.class)
class SaleServiceTest {

	@Mock
	private SaleRepository saleRepository;
	@Mock
	private SaleLineRepository saleLineRepository;
	@Mock
	private SalePaymentRepository salePaymentRepository;
	@Mock
	private CashService cashService;
	@Mock
	private ProductService productService;
	@Mock
	private StockService stockService;

	private SaleService saleService;
	private BranchEntity branch;
	private CashSessionEntity session;
	private ProductVariantEntity variant;

	@BeforeEach
	void setUp() {
		saleService = new SaleService(
				saleRepository,
				saleLineRepository,
				salePaymentRepository,
				cashService,
				productService,
				stockService,
				new SaleMapper());
		branch = BranchEntity.of("Sucursal 1", null, null, null);
		ReflectionTestUtils.setField(branch, "id", 3L);
		session = CashSessionEntity.of(branch, LocalDate.of(2026, 9, 9), new BigDecimal("100.00"));
		ReflectionTestUtils.setField(session, "id", 10L);
		ProductEntity product = ProductEntity.of("Resma", null, null, new BigDecimal("21.00"), true, true);
		ReflectionTestUtils.setField(product, "id", 1L);
		variant = ProductVariantEntity.of(
				product, null, "MF-1-01", "Chamex", null, new BigDecimal("8400.00"), ItemCondition.NUEVA, null);
		ReflectionTestUtils.setField(variant, "id", 1L);
		stubSaves();
	}

	@Test
	void createChargesAndMovesStock() {
		when(cashService.requireOpenToday(3L)).thenReturn(session);
		when(productService.requireActiveVariant(1L)).thenReturn(variant);

		var response = saleService.create(3L, new SaleRequest(
				List.of(new SaleLineRequest(1L, 2)),
				List.of(new SalePaymentRequest(SalePaymentMethod.EFECTIVO, new BigDecimal("16800.00")))));

		assertEquals(0, response.total().compareTo(new BigDecimal("16800.00")));
		assertEquals(1, response.lines().size());
		assertEquals(2, response.lines().getFirst().quantity());
		verify(stockService).registerSale(eq(1L), eq(3L), eq(2), any());
	}

	@Test
	void skipsStockWhenProductDoesNotTrack() {
		ProductEntity candy = ProductEntity.of("Caramelos", null, null, new BigDecimal("21.00"), true, false);
		ProductVariantEntity candyVariant = ProductVariantEntity.of(
				candy, null, "MF-2-01", "Suelto", null, new BigDecimal("10.00"), ItemCondition.NUEVA, null);
		ReflectionTestUtils.setField(candyVariant, "id", 20L);
		when(cashService.requireOpenToday(3L)).thenReturn(session);
		when(productService.requireActiveVariant(20L)).thenReturn(candyVariant);

		saleService.create(3L, new SaleRequest(
				List.of(new SaleLineRequest(20L, 3)),
				List.of(new SalePaymentRequest(SalePaymentMethod.EFECTIVO, new BigDecimal("30.00")))));

		verify(stockService, never()).registerSale(any(), any(), any(Integer.class), any());
	}

	@Test
	void rejectsWhenPaymentsDoNotMatchTotal() {
		when(cashService.requireOpenToday(3L)).thenReturn(session);
		when(productService.requireActiveVariant(1L)).thenReturn(variant);

		assertThrows(BusinessRuleException.class, () -> saleService.create(3L, new SaleRequest(
				List.of(new SaleLineRequest(1L, 1)),
				List.of(new SalePaymentRequest(SalePaymentMethod.EFECTIVO, new BigDecimal("100.00"))))));
	}

	@Test
	void rejectsWhenCashIsClosed() {
		when(cashService.requireOpenToday(3L)).thenThrow(new BusinessRuleException("La caja de ese día ya está cerrada"));

		assertThrows(BusinessRuleException.class, () -> saleService.create(3L, new SaleRequest(
				List.of(new SaleLineRequest(1L, 1)),
				List.of(new SalePaymentRequest(SalePaymentMethod.EFECTIVO, new BigDecimal("8400.00"))))));
	}

	/** Save de venta, línea y pago: les pone id y devuelve el mismo objeto. */
	private void stubSaves() {
		AtomicLong saleIds = new AtomicLong(500);
		lenient().when(saleRepository.save(any(SaleEntity.class))).thenAnswer(invocation -> {
			SaleEntity sale = invocation.getArgument(0);
			if (sale.getId() == null) {
				ReflectionTestUtils.setField(sale, "id", saleIds.getAndIncrement());
			}
			return sale;
		});
		lenient().when(saleLineRepository.save(any(SaleLineEntity.class))).thenAnswer(invocation -> {
			SaleLineEntity line = invocation.getArgument(0);
			if (line.getId() == null) {
				ReflectionTestUtils.setField(line, "id", 1L);
			}
			return line;
		});
		lenient().when(salePaymentRepository.save(any(SalePaymentEntity.class))).thenAnswer(invocation -> {
			SalePaymentEntity payment = invocation.getArgument(0);
			if (payment.getId() == null) {
				ReflectionTestUtils.setField(payment, "id", 1L);
			}
			return payment;
		});
	}
}
