package com.pepology.mostrador.services;

import com.pepology.mostrador.dto.product.ProductRequest;
import com.pepology.mostrador.dto.product.ProductUpdateRequest;
import com.pepology.mostrador.dto.product.VariantRequest;
import com.pepology.mostrador.exceptions.BusinessRuleException;
import com.pepology.mostrador.exceptions.NotFoundException;
import com.pepology.mostrador.mappers.ProductMapper;
import com.pepology.mostrador.models.entities.CategoryEntity;
import com.pepology.mostrador.models.entities.ProductEntity;
import com.pepology.mostrador.models.entities.ProductVariantEntity;
import com.pepology.mostrador.models.enums.ItemCondition;
import com.pepology.mostrador.repositories.ProductRepository;
import com.pepology.mostrador.repositories.ProductVariantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

	@Mock
	private ProductRepository productRepository;
	@Mock
	private ProductVariantRepository productVariantRepository;
	@Mock
	private CategoryService categoryService;
	@Mock
	private BrandService brandService;

	private ProductService productService;

	@BeforeEach
	void setUp() {
		productService = new ProductService(
				productRepository,
				productVariantRepository,
				categoryService,
				brandService,
				new ProductMapper());
	}

	@Test
	void createSavesProductAndGeneratesSku() {
		when(productRepository.save(any(ProductEntity.class))).thenAnswer(invocation -> {
			ProductEntity product = invocation.getArgument(0);
			ReflectionTestUtils.setField(product, "id", 10L);
			return product;
		});
		when(productVariantRepository.existsBySku("MF-10-01")).thenReturn(false);
		when(productVariantRepository.save(any(ProductVariantEntity.class))).thenAnswer(invocation -> {
			ProductVariantEntity variant = invocation.getArgument(0);
			ReflectionTestUtils.setField(variant, "id", 1L);
			return variant;
		});

		var response = productService.create(new ProductRequest(
				"  Cuaderno A4  ",
				null,
				null,
				null,
				null,
				List.of(new VariantRequest(
						null,
						"Única",
						null,
						new BigDecimal("1500.00"),
						null,
						null))));

		assertEquals("Cuaderno A4", response.name());
		assertEquals(new BigDecimal("21.00"), response.vatRate());
		assertEquals(true, response.allowsEmployeeDiscount());
		assertEquals(1, response.variants().size());
		assertEquals("MF-10-01", response.variants().getFirst().sku());
		assertEquals(ItemCondition.NUEVA, response.variants().getFirst().itemCondition());
	}

	@Test
	void createRejectsMissingCategory() {
		when(categoryService.requireActive(99L)).thenThrow(new NotFoundException("No existe la categoría 99"));

		assertThrows(NotFoundException.class, () -> productService.create(new ProductRequest(
				"Cuaderno",
				null,
				99L,
				null,
				null,
				List.of(new VariantRequest(null, "Única", null, new BigDecimal("1.00"), null, null)))));
	}

	@Test
	void createRejectsInactiveBrand() {
		when(productRepository.save(any(ProductEntity.class))).thenAnswer(invocation -> {
			ProductEntity product = invocation.getArgument(0);
			ReflectionTestUtils.setField(product, "id", 10L);
			return product;
		});
		when(brandService.requireActive(5L)).thenThrow(new BusinessRuleException("No se puede usar una marca dada de baja"));

		assertThrows(BusinessRuleException.class, () -> productService.create(new ProductRequest(
				"Lapicera",
				null,
				null,
				null,
				null,
				List.of(new VariantRequest(5L, "Azul", null, new BigDecimal("200.00"), null, null)))));
	}

	@Test
	void findByIdThrowsWhenMissing() {
		when(productRepository.findById(99L)).thenReturn(Optional.empty());

		assertThrows(NotFoundException.class, () -> productService.findById(99L));
	}

	@Test
	void updateChangesName() {
		ProductEntity product = ProductEntity.of("Viejo", null, null, new BigDecimal("21.00"), true);
		ReflectionTestUtils.setField(product, "id", 1L);
		when(productRepository.findById(1L)).thenReturn(Optional.of(product));
		when(productVariantRepository.findByProduct(product)).thenReturn(List.of());

		var response = productService.update(1L, new ProductUpdateRequest("Nuevo", null, null, null, null));

		assertEquals("Nuevo", product.getName());
		assertEquals("Nuevo", response.name());
	}

	@Test
	void deactivateRejectsIfAlreadyInactive() {
		ProductEntity product = ProductEntity.of("Cuaderno", null, null, new BigDecimal("21.00"), true);
		product.setActive(false);
		when(productRepository.findById(1L)).thenReturn(Optional.of(product));

		assertThrows(BusinessRuleException.class, () -> productService.deactivate(1L));
	}

	@Test
	void deactivateRejectsIfHasActiveVariants() {
		ProductEntity product = ProductEntity.of("Cuaderno", null, null, new BigDecimal("21.00"), true);
		when(productRepository.findById(1L)).thenReturn(Optional.of(product));
		when(productVariantRepository.existsByProductAndActiveTrue(product)).thenReturn(true);

		assertThrows(BusinessRuleException.class, () -> productService.deactivate(1L));
	}

	@Test
	void deactivateAllVariantsSetsThemInactive() {
		ProductEntity product = ProductEntity.of("Cuaderno", null, null, new BigDecimal("21.00"), true);
		ProductVariantEntity active = ProductVariantEntity.of(
				product, null, "MF-1-01", "Única", null, new BigDecimal("1.00"), ItemCondition.NUEVA, null);
		ProductVariantEntity alreadyOff = ProductVariantEntity.of(
				product, null, "MF-1-02", "Defectuosa", null, new BigDecimal("1.00"), ItemCondition.DEFECTUOSA, null);
		alreadyOff.setActive(false);
		when(productRepository.findById(1L)).thenReturn(Optional.of(product));
		when(productVariantRepository.findByProduct(product)).thenReturn(List.of(active, alreadyOff));

		productService.deactivateAllVariants(1L);

		assertEquals(false, active.isActive());
		assertEquals(false, alreadyOff.isActive());
	}

	@Test
	void updateVariantSetsImageUrl() {
		ProductEntity product = ProductEntity.of("Resma", null, null, new BigDecimal("21.00"), true);
		ReflectionTestUtils.setField(product, "id", 1L);
		ProductVariantEntity variant = ProductVariantEntity.of(
				product, null, "MF-1-01", "Única", null, new BigDecimal("8500.00"), ItemCondition.NUEVA, null);
		ReflectionTestUtils.setField(variant, "id", 1L);
		when(productRepository.findById(1L)).thenReturn(Optional.of(product));
		when(productVariantRepository.findById(1L)).thenReturn(Optional.of(variant));
		when(productVariantRepository.findByProduct(product)).thenReturn(List.of(variant));

		var response = productService.updateVariant(
				1L, 1L, new VariantRequest(
						null, "Chamex", null, new BigDecimal("8400.00"), null, "https://ejemplo.com/chamex.jpg"));

		assertEquals("https://ejemplo.com/chamex.jpg", variant.getImageUrl());
		assertEquals("https://ejemplo.com/chamex.jpg", response.variants().getFirst().imageUrl());
	}

	@Test
	void updateVariantKeepsImageWhenOmitted() {
		ProductEntity product = ProductEntity.of("Resma", null, null, new BigDecimal("21.00"), true);
		ReflectionTestUtils.setField(product, "id", 1L);
		ProductVariantEntity variant = ProductVariantEntity.of(
				product, null, "MF-1-01", "Única", null, new BigDecimal("8500.00"), ItemCondition.NUEVA,
				"https://ejemplo.com/vieja.jpg");
		ReflectionTestUtils.setField(variant, "id", 1L);
		when(productRepository.findById(1L)).thenReturn(Optional.of(product));
		when(productVariantRepository.findById(1L)).thenReturn(Optional.of(variant));
		when(productVariantRepository.findByProduct(product)).thenReturn(List.of(variant));

		productService.updateVariant(
				1L, 1L, new VariantRequest(null, "Chamex", null, new BigDecimal("8400.00"), null, null));

		assertEquals("https://ejemplo.com/vieja.jpg", variant.getImageUrl());
	}

	@Test
	void reactivateVariantSetsActive() {
		ProductEntity product = ProductEntity.of("Cuaderno", null, null, new BigDecimal("21.00"), true);
		ReflectionTestUtils.setField(product, "id", 1L);
		ProductVariantEntity variant = ProductVariantEntity.of(
				product, null, "MF-1-01", "Única", null, new BigDecimal("1.00"), ItemCondition.NUEVA, null);
		variant.setActive(false);
		ReflectionTestUtils.setField(variant, "id", 2L);
		when(productRepository.findById(1L)).thenReturn(Optional.of(product));
		when(productVariantRepository.findById(2L)).thenReturn(Optional.of(variant));
		when(productVariantRepository.findByProduct(product)).thenReturn(List.of(variant));

		var response = productService.reactivateVariant(1L, 2L);

		assertEquals(true, variant.isActive());
		assertEquals(true, response.variants().getFirst().active());
	}

	@Test
	void reactivateVariantRejectsIfProductInactive() {
		ProductEntity product = ProductEntity.of("Cuaderno", null, null, new BigDecimal("21.00"), true);
		product.setActive(false);
		ReflectionTestUtils.setField(product, "id", 1L);
		ProductVariantEntity variant = ProductVariantEntity.of(
				product, null, "MF-1-01", "Única", null, new BigDecimal("1.00"), ItemCondition.NUEVA, null);
		variant.setActive(false);
		when(productRepository.findById(1L)).thenReturn(Optional.of(product));
		when(productVariantRepository.findById(2L)).thenReturn(Optional.of(variant));

		assertThrows(BusinessRuleException.class, () -> productService.reactivateVariant(1L, 2L));
	}

	@Test
	void updateVariantRejectsIfBelongsToAnotherProduct() {
		ProductEntity owner = ProductEntity.of("A", null, null, new BigDecimal("21.00"), true);
		ProductEntity other = ProductEntity.of("B", null, null, new BigDecimal("21.00"), true);
		ReflectionTestUtils.setField(owner, "id", 1L);
		ReflectionTestUtils.setField(other, "id", 2L);
		ProductVariantEntity variant = ProductVariantEntity.of(
				other, null, "MF-2-01", "Única", null, new BigDecimal("1.00"), ItemCondition.NUEVA, null);
		when(productRepository.findById(1L)).thenReturn(Optional.of(owner));
		when(productVariantRepository.findById(8L)).thenReturn(Optional.of(variant));

		assertThrows(BusinessRuleException.class, () -> productService.updateVariant(
				1L, 8L, new VariantRequest(null, "Rojo", null, new BigDecimal("2.00"), null, null)));
	}

	@Test
	void createAllowsSameNameUnderCategory() {
		CategoryEntity escolar = CategoryEntity.of("Escolar", null);
		ReflectionTestUtils.setField(escolar, "id", 3L);
		when(categoryService.requireActive(3L)).thenReturn(escolar);
		when(productRepository.save(any(ProductEntity.class))).thenAnswer(invocation -> {
			ProductEntity product = invocation.getArgument(0);
			ReflectionTestUtils.setField(product, "id", 4L);
			return product;
		});
		when(productVariantRepository.existsBySku(any())).thenReturn(false);
		when(productVariantRepository.save(any(ProductVariantEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		var response = productService.create(new ProductRequest(
				"Cuaderno A4 rayado",
				null,
				3L,
				null,
				null,
				List.of(new VariantRequest(null, "Rivadavia", "779", new BigDecimal("2500.00"), ItemCondition.NUEVA, null))));

		assertEquals("Escolar", response.category().name());
		assertEquals("Rivadavia", response.variants().getFirst().label());
	}
}
