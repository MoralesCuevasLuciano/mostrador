package com.pepology.mostrador.services;

import com.pepology.mostrador.exceptions.BusinessRuleException;
import com.pepology.mostrador.mappers.StockMapper;
import com.pepology.mostrador.models.entities.BranchEntity;
import com.pepology.mostrador.models.entities.ProductEntity;
import com.pepology.mostrador.models.entities.ProductVariantEntity;
import com.pepology.mostrador.models.entities.StockEntity;
import com.pepology.mostrador.models.entities.StockMovementEntity;
import com.pepology.mostrador.models.enums.ItemCondition;
import com.pepology.mostrador.models.enums.StockMovementType;
import com.pepology.mostrador.repositories.StockMovementRepository;
import com.pepology.mostrador.repositories.StockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests de StockService: recuento, producto sin inventario y traslado.
 */
@ExtendWith(MockitoExtension.class)
class StockServiceTest {

	@Mock
	private StockRepository stockRepository;
	@Mock
	private StockMovementRepository stockMovementRepository;
	@Mock
	private BranchService branchService;
	@Mock
	private ProductService productService;

	private StockService stockService;
	private ProductVariantEntity variant;
	private BranchEntity branch;
	private BranchEntity otherBranch;

	@BeforeEach
	void setUp() {
		stockService = new StockService(
				stockRepository,
				stockMovementRepository,
				branchService,
				productService,
				new StockMapper());
		ProductEntity product = ProductEntity.of("Cuaderno", null, null, new BigDecimal("21.00"), true, true);
		ReflectionTestUtils.setField(product, "id", 1L);
		variant = ProductVariantEntity.of(
				product, null, "MF-1-01", "A4", null, new BigDecimal("1500.00"), ItemCondition.NUEVA, null);
		ReflectionTestUtils.setField(variant, "id", 10L);
		branch = BranchEntity.of("Sucursal 1", "Calle falsa 123", null, null);
		ReflectionTestUtils.setField(branch, "id", 1L);
		otherBranch = BranchEntity.of("Sucursal 2", "Siempreviva 348", null, null);
		ReflectionTestUtils.setField(otherBranch, "id", 2L);
	}

	@Test
	void firstRecountCreatesStockRow() {
		when(productService.requireActiveVariant(10L)).thenReturn(variant);
		when(branchService.requireActive(1L)).thenReturn(branch);
		when(stockRepository.findByVariantAndBranch(variant, branch)).thenReturn(Optional.empty());
		stubSaves();

		var balance = stockService.recount(10L, 1L, 12, "primer conteo");

		ArgumentCaptor<StockMovementEntity> movementCaptor = ArgumentCaptor.forClass(StockMovementEntity.class);
		verify(stockMovementRepository).save(movementCaptor.capture());
		assertEquals(StockMovementType.AJUSTE_INICIAL, movementCaptor.getValue().getMovementType());
		assertEquals(12, movementCaptor.getValue().getQuantity());
		assertTrue(balance.inventoried());
		assertEquals(12, balance.quantity());
	}

	@Test
	void laterRecountStoresDifference() {
		when(productService.requireActiveVariant(10L)).thenReturn(variant);
		when(branchService.requireActive(1L)).thenReturn(branch);
		StockEntity existing = StockEntity.of(variant, branch, 12, null);
		ReflectionTestUtils.setField(existing, "id", 50L);
		when(stockRepository.findByVariantAndBranch(variant, branch)).thenReturn(Optional.of(existing));
		stubSaves();

		var balance = stockService.recount(10L, 1L, 10, null);

		ArgumentCaptor<StockMovementEntity> movementCaptor = ArgumentCaptor.forClass(StockMovementEntity.class);
		verify(stockMovementRepository).save(movementCaptor.capture());
		assertEquals(StockMovementType.AJUSTE_RECUENTO, movementCaptor.getValue().getMovementType());
		assertEquals(-2, movementCaptor.getValue().getQuantity());
		assertEquals(10, balance.quantity());
	}

	@Test
	void rejectsProductThatDoesNotTrackStock() {
		ProductEntity candy = ProductEntity.of("Caramelos", null, null, new BigDecimal("21.00"), true, false);
		ProductVariantEntity candyVariant = ProductVariantEntity.of(
				candy, null, "MF-2-01", "Suelto", null, new BigDecimal("10.00"), ItemCondition.NUEVA, null);
		ReflectionTestUtils.setField(candyVariant, "id", 20L);
		when(productService.requireActiveVariant(20L)).thenReturn(candyVariant);
		when(branchService.requireActive(1L)).thenReturn(branch);

		assertThrows(BusinessRuleException.class, () -> stockService.recount(20L, 1L, 100, null));
	}

	@Test
	void transferCreatesLinkedPair() {
		when(productService.requireActiveVariant(10L)).thenReturn(variant);
		when(branchService.requireActive(1L)).thenReturn(branch);
		when(branchService.requireActive(2L)).thenReturn(otherBranch);
		StockEntity origin = StockEntity.of(variant, branch, 8, null);
		ReflectionTestUtils.setField(origin, "id", 51L);
		when(stockRepository.findByVariantAndBranch(variant, branch)).thenReturn(Optional.of(origin));
		when(stockRepository.findByVariantAndBranch(variant, otherBranch)).thenReturn(Optional.empty());
		stubSaves();

		var result = stockService.transfer(10L, 1L, 2L, 5, "a sucursal 2");

		ArgumentCaptor<StockMovementEntity> movementCaptor = ArgumentCaptor.forClass(StockMovementEntity.class);
		verify(stockMovementRepository, times(3)).save(movementCaptor.capture());
		var saved = movementCaptor.getAllValues();
		StockMovementEntity outbound = saved.get(0);
		StockMovementEntity inbound = saved.get(1);
		assertEquals(StockMovementType.TRASLADO, outbound.getMovementType());
		assertEquals(-5, outbound.getQuantity());
		assertEquals(branch, outbound.getBranch());
		assertEquals(StockMovementType.TRASLADO, inbound.getMovementType());
		assertEquals(5, inbound.getQuantity());
		assertEquals(otherBranch, inbound.getBranch());
		assertEquals(outbound, inbound.getRelatedMovement());
		assertEquals(inbound, outbound.getRelatedMovement());
		assertEquals(3, result.from().quantity());
		assertEquals(5, result.to().quantity());
		assertEquals(result.inbound().id(), result.outbound().relatedMovementId());
		assertEquals(result.outbound().id(), result.inbound().relatedMovementId());
	}

	@Test
	void getBalanceMarksNeverInventoried() {
		when(productService.requireActiveVariant(10L)).thenReturn(variant);
		when(branchService.requireActive(1L)).thenReturn(branch);
		when(stockRepository.findByVariantAndBranch(variant, branch)).thenReturn(Optional.empty());

		var balance = stockService.getBalance(10L, 1L);

		assertFalse(balance.inventoried());
		assertNull(balance.quantity());
		assertEquals("MF-1-01", balance.sku());
	}

	@Test
	void transferRejectsSameBranch() {
		assertThrows(BusinessRuleException.class, () -> stockService.transfer(10L, 1L, 1L, 3, null));
	}

	/** Save de movimiento y saldo: les pone id y devuelve el mismo objeto. */
	private void stubSaves() {
		AtomicLong movementIds = new AtomicLong(100);
		AtomicLong stockIds = new AtomicLong(200);
		when(stockMovementRepository.save(any(StockMovementEntity.class))).thenAnswer(invocation -> {
			StockMovementEntity movement = invocation.getArgument(0);
			if (movement.getId() == null) {
				ReflectionTestUtils.setField(movement, "id", movementIds.getAndIncrement());
			}
			return movement;
		});
		when(stockRepository.save(any(StockEntity.class))).thenAnswer(invocation -> {
			StockEntity stock = invocation.getArgument(0);
			if (stock.getId() == null) {
				ReflectionTestUtils.setField(stock, "id", stockIds.getAndIncrement());
			}
			return stock;
		});
	}
}
