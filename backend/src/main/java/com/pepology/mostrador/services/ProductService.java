package com.pepology.mostrador.services;

import com.pepology.mostrador.dto.product.ProductRequest;
import com.pepology.mostrador.dto.product.ProductResponse;
import com.pepology.mostrador.dto.product.ProductUpdateRequest;
import com.pepology.mostrador.dto.product.VariantRequest;
import com.pepology.mostrador.exceptions.BusinessRuleException;
import com.pepology.mostrador.exceptions.NotFoundException;
import com.pepology.mostrador.mappers.ProductMapper;
import com.pepology.mostrador.models.entities.BrandEntity;
import com.pepology.mostrador.models.entities.CategoryEntity;
import com.pepology.mostrador.models.entities.ProductEntity;
import com.pepology.mostrador.models.entities.ProductVariantEntity;
import com.pepology.mostrador.repositories.ProductRepository;
import com.pepology.mostrador.repositories.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

	private static final BigDecimal DEFAULT_VAT = new BigDecimal("21.00");

	private final ProductRepository productRepository;
	private final ProductVariantRepository productVariantRepository;
	private final CategoryService categoryService;
	private final BrandService brandService;
	private final ProductMapper productMapper;

	@Transactional
	public ProductResponse create(ProductRequest request) {
		String name = normalize(request.name());
		CategoryEntity category = resolveCategory(request.categoryId());
		boolean allowsEmployeeDiscount = request.allowsEmployeeDiscount() == null || request.allowsEmployeeDiscount();
		ProductRequest normalized = new ProductRequest(
				name,
				request.description(),
				request.categoryId(),
				request.vatRate() == null ? DEFAULT_VAT : request.vatRate(),
				allowsEmployeeDiscount,
				request.variants());
		ProductEntity saved = productRepository.save(productMapper.toEntity(normalized, category, allowsEmployeeDiscount));
		List<ProductVariantEntity> variants = new ArrayList<>();
		int index = 1;
		for (VariantRequest variantRequest : request.variants()) {
			variants.add(saveVariant(saved, variantRequest, index++));
		}
		return productMapper.toResponse(saved, variants);
	}

	@Transactional(readOnly = true)
	public List<ProductResponse> findAll() {
		return productRepository.findAll(Sort.by("name")).stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public ProductResponse findById(Long id) {
		return toResponse(requireProduct(id));
	}

	@Transactional
	public ProductResponse update(Long id, ProductUpdateRequest request) {
		ProductEntity product = requireProduct(id);
		product.setName(normalize(request.name()));
		product.setDescription(blankToNull(request.description()));
		product.setCategory(resolveCategory(request.categoryId()));
		if (request.vatRate() != null) {
			product.setVatRate(request.vatRate());
		}
		if (request.allowsEmployeeDiscount() != null) {
			product.setAllowsEmployeeDiscount(request.allowsEmployeeDiscount());
		}
		return toResponse(product);
	}

	@Transactional
	public ProductResponse deactivate(Long id) {
		ProductEntity product = requireProduct(id);
		if (!product.isActive()) {
			throw new BusinessRuleException("El producto ya está dado de baja");
		}
		if (productVariantRepository.existsByProductAndActiveTrue(product)) {
			throw new BusinessRuleException("No se puede dar de baja un producto que tiene variantes activas");
		}
		product.setActive(false);
		return toResponse(product);
	}

	@Transactional
	public ProductResponse reactivate(Long id) {
		ProductEntity product = requireProduct(id);
		if (product.isActive()) {
			throw new BusinessRuleException("El producto ya está activo");
		}
		product.setActive(true);
		return toResponse(product);
	}

	@Transactional
	public ProductResponse deactivateAllVariants(Long productId) {
		ProductEntity product = requireProduct(productId);
		productVariantRepository.findByProduct(product).stream()
				.filter(ProductVariantEntity::isActive)
				.forEach(variant -> variant.setActive(false));
		return toResponse(product);
	}

	@Transactional
	public ProductResponse addVariant(Long productId, VariantRequest request) {
		ProductEntity product = requireProduct(productId);
		int nextIndex = productVariantRepository.findByProduct(product).size() + 1;
		saveVariant(product, request, nextIndex);
		return toResponse(product);
	}

	@Transactional
	public ProductResponse updateVariant(Long productId, Long variantId, VariantRequest request) {
		ProductVariantEntity variant = requireVariantOfProduct(productId, variantId);
		variant.setBrand(resolveBrand(request.brandId()));
		variant.setLabel(normalize(request.label()));
		variant.setBarcode(blankToNull(request.barcode()));
		variant.setPrice(request.price());
		if (request.itemCondition() != null) {
			variant.setItemCondition(request.itemCondition());
		}
		if (request.imageUrl() != null) {
			variant.setImageUrl(blankToNull(request.imageUrl()));
		}
		return toResponse(variant.getProduct());
	}

	@Transactional
	public ProductResponse deactivateVariant(Long productId, Long variantId) {
		ProductVariantEntity variant = requireVariantOfProduct(productId, variantId);
		if (!variant.isActive()) {
			throw new BusinessRuleException("La variante ya está dada de baja");
		}
		variant.setActive(false);
		return toResponse(variant.getProduct());
	}

	@Transactional
	public ProductResponse reactivateVariant(Long productId, Long variantId) {
		ProductVariantEntity variant = requireVariantOfProduct(productId, variantId);
		if (variant.isActive()) {
			throw new BusinessRuleException("La variante ya está activa");
		}
		if (!variant.getProduct().isActive()) {
			throw new BusinessRuleException("No se puede reactivar una variante de un producto dado de baja");
		}
		variant.setActive(true);
		return toResponse(variant.getProduct());
	}

	private ProductVariantEntity saveVariant(ProductEntity product, VariantRequest request, int index) {
		BrandEntity brand = resolveBrand(request.brandId());
		String sku = nextSku(product.getId(), index);
		return productVariantRepository.save(productMapper.toVariantEntity(request, product, brand, sku));
	}

	private String nextSku(Long productId, int startIndex) {
		int index = startIndex;
		String sku;
		do {
			sku = "MF-%d-%02d".formatted(productId, index++);
		} while (productVariantRepository.existsBySku(sku));
		return sku;
	}

	private ProductResponse toResponse(ProductEntity product) {
		return productMapper.toResponse(product, productVariantRepository.findByProduct(product));
	}

	private CategoryEntity resolveCategory(Long categoryId) {
		return categoryId == null ? null : categoryService.requireActive(categoryId);
	}

	private BrandEntity resolveBrand(Long brandId) {
		return brandId == null ? null : brandService.requireActive(brandId);
	}

	private ProductEntity requireProduct(Long id) {
		return productRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("No existe el producto " + id));
	}

	private ProductVariantEntity requireVariantOfProduct(Long productId, Long variantId) {
		requireProduct(productId);
		ProductVariantEntity variant = productVariantRepository.findById(variantId)
				.orElseThrow(() -> new NotFoundException("No existe la variante " + variantId));
		if (!variant.getProduct().getId().equals(productId)) {
			throw new BusinessRuleException("La variante no pertenece a ese producto");
		}
		return variant;
	}

	private static String normalize(String value) {
		return value.trim().replaceAll("\\s+", " ");
	}

	private static String blankToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
