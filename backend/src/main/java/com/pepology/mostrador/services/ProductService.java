package com.pepology.mostrador.services;

import com.pepology.mostrador.dto.product.BarcodeMatchResponse;
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

/**
 * Catálogo: ficha de producto más variantes (SKU, precio, foto, código de barras).
 * No toca repositorios de marca ni categoría: pide las entidades activas a sus servicios.
 */
@Service
@RequiredArgsConstructor
public class ProductService {

	private static final BigDecimal DEFAULT_VAT = new BigDecimal("21.00");

	private final ProductRepository productRepository;
	private final ProductVariantRepository productVariantRepository;
	private final CategoryService categoryService;
	private final BrandService brandService;
	private final ProductMapper productMapper;

	/** Alta de producto con al menos una variante. IVA por defecto 21 %. */
	@Transactional
	public ProductResponse create(ProductRequest request) {
		String name = normalize(request.name());
		CategoryEntity category = resolveCategory(request.categoryId());
		boolean allowsEmployeeDiscount = request.allowsEmployeeDiscount() == null || request.allowsEmployeeDiscount();
		boolean tracksStock = request.tracksStock() == null || request.tracksStock();
		ProductRequest normalized = new ProductRequest(
				name,
				request.description(),
				request.categoryId(),
				request.vatRate() == null ? DEFAULT_VAT : request.vatRate(),
				allowsEmployeeDiscount,
				tracksStock,
				request.variants());
		ProductEntity saved = productRepository.save(
				productMapper.toEntity(normalized, category, allowsEmployeeDiscount, tracksStock));
		List<ProductVariantEntity> variants = new ArrayList<>();
		int index = 1;
		for (VariantRequest variantRequest : request.variants()) {
			variants.add(saveVariant(saved, variantRequest, index++));
		}
		return productMapper.toResponse(saved, variants);
	}

	/** Listado ordenado por nombre, con todas las variantes de cada producto. */
	@Transactional(readOnly = true)
	public List<ProductResponse> findAll() {
		return productRepository.findAll(Sort.by("name")).stream()
				.map(this::toResponse)
				.toList();
	}

	/** Un producto por id, con sus variantes. 404 si no existe. */
	@Transactional(readOnly = true)
	public ProductResponse findById(Long id) {
		return toResponse(requireProduct(id));
	}

	/** Variantes que ya usan ese código de barras (el barcode no es único). */
	@Transactional(readOnly = true)
	public List<BarcodeMatchResponse> findBarcodeMatches(String barcode) {
		String code = blankToNull(barcode);
		if (code == null) {
			return List.of();
		}
		return productVariantRepository.findByBarcode(code).stream()
				.map(variant -> new BarcodeMatchResponse(
						variant.getProduct().getId(),
						variant.getProduct().getName(),
						variant.getId(),
						variant.getLabel(),
						variant.getSku(),
						variant.isActive()))
				.toList();
	}

	/** Edita la ficha (nombre, descripción, rubro, IVA, descuento, si lleva inventario). No toca variantes. */
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
		if (request.tracksStock() != null) {
			product.setTracksStock(request.tracksStock());
		}
		return toResponse(product);
	}

	/** Baja lógica del producto. Primero hay que dar de baja todas las variantes. */
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

	/** Reactiva el producto. Las variantes siguen como estaban. */
	@Transactional
	public ProductResponse reactivate(Long id) {
		ProductEntity product = requireProduct(id);
		if (product.isActive()) {
			throw new BusinessRuleException("El producto ya está activo");
		}
		product.setActive(true);
		return toResponse(product);
	}

	/** Da de baja todas las variantes activas de un producto (paso previo a bajar el producto). */
	@Transactional
	public ProductResponse deactivateAllVariants(Long productId) {
		ProductEntity product = requireProduct(productId);
		productVariantRepository.findByProduct(product).stream()
				.filter(ProductVariantEntity::isActive)
				.forEach(variant -> variant.setActive(false));
		return toResponse(product);
	}

	/** Agrega una variante y le asigna el próximo SKU libre (MF-{id}-NN). */
	@Transactional
	public ProductResponse addVariant(Long productId, VariantRequest request) {
		ProductEntity product = requireProduct(productId);
		int nextIndex = productVariantRepository.findByProduct(product).size() + 1;
		saveVariant(product, request, nextIndex);
		return toResponse(product);
	}

	/** Edita una variante. imageUrl null significa “no tocar la foto actual”. */
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

	/** Baja lógica de una variante. */
	@Transactional
	public ProductResponse deactivateVariant(Long productId, Long variantId) {
		ProductVariantEntity variant = requireVariantOfProduct(productId, variantId);
		if (!variant.isActive()) {
			throw new BusinessRuleException("La variante ya está dada de baja");
		}
		variant.setActive(false);
		return toResponse(variant.getProduct());
	}

	/** Reactiva una variante. El producto tiene que estar activo. */
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

	/** Para inventario: la variante tiene que existir y estar activa. */
	@Transactional(readOnly = true)
	public ProductVariantEntity requireActiveVariant(Long id) {
		ProductVariantEntity variant = productVariantRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("No existe la variante " + id));
		if (!variant.isActive()) {
			throw new BusinessRuleException("No se puede usar una variante dada de baja");
		}
		return variant;
	}

	/** Persiste una variante nueva con marca activa y SKU generado. */
	private ProductVariantEntity saveVariant(ProductEntity product, VariantRequest request, int index) {
		BrandEntity brand = resolveBrand(request.brandId());
		String sku = nextSku(product.getId(), index);
		return productVariantRepository.save(productMapper.toVariantEntity(request, product, brand, sku));
	}

	/** Primer SKU MF-{productId}-NN que no exista (por si ya hay uno ocupado). */
	private String nextSku(Long productId, int startIndex) {
		int index = startIndex;
		String sku;
		do {
			sku = "MF-%d-%02d".formatted(productId, index++);
		} while (productVariantRepository.existsBySku(sku));
		return sku;
	}

	/** Arma el DTO incluyendo las variantes del producto. */
	private ProductResponse toResponse(ProductEntity product) {
		return productMapper.toResponse(product, productVariantRepository.findByProduct(product));
	}

	/** Categoría activa o null si no se indicó. */
	private CategoryEntity resolveCategory(Long categoryId) {
		return categoryId == null ? null : categoryService.requireActive(categoryId);
	}

	/** Marca activa o null si no se indicó. */
	private BrandEntity resolveBrand(Long brandId) {
		return brandId == null ? null : brandService.requireActive(brandId);
	}

	/** Busca el producto o lanza 404. */
	private ProductEntity requireProduct(Long id) {
		return productRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("No existe el producto " + id));
	}

	/** La variante tiene que existir y pertenecer a ese producto. */
	private ProductVariantEntity requireVariantOfProduct(Long productId, Long variantId) {
		requireProduct(productId);
		ProductVariantEntity variant = productVariantRepository.findById(variantId)
				.orElseThrow(() -> new NotFoundException("No existe la variante " + variantId));
		if (!variant.getProduct().getId().equals(productId)) {
			throw new BusinessRuleException("La variante no pertenece a ese producto");
		}
		return variant;
	}

	/** Recorta extremos y colapsa espacios internos. */
	private static String normalize(String value) {
		return value.trim().replaceAll("\\s+", " ");
	}

	/** Cadena vacía o solo espacios → null (campos opcionales). */
	private static String blankToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
