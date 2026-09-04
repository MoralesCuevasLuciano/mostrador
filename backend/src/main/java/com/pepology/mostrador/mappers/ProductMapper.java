package com.pepology.mostrador.mappers;

import com.pepology.mostrador.dto.brand.BrandSummary;
import com.pepology.mostrador.dto.category.CategorySummary;
import com.pepology.mostrador.dto.product.ProductRequest;
import com.pepology.mostrador.dto.product.ProductResponse;
import com.pepology.mostrador.dto.product.VariantRequest;
import com.pepology.mostrador.dto.product.VariantResponse;
import com.pepology.mostrador.models.entities.BrandEntity;
import com.pepology.mostrador.models.entities.CategoryEntity;
import com.pepology.mostrador.models.entities.ProductEntity;
import com.pepology.mostrador.models.entities.ProductVariantEntity;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Convierte producto/variante entre request, entidad y response.
 */
@Component
public class ProductMapper {

	/** Ficha de producto nueva. description vacío se guarda como null. */
	public ProductEntity toEntity(
			ProductRequest request,
			CategoryEntity category,
			boolean allowsEmployeeDiscount,
			boolean tracksStock) {
		return ProductEntity.of(
				request.name(),
				blankToNull(request.description()),
				category,
				request.vatRate(),
				allowsEmployeeDiscount,
				tracksStock);
	}

	/** Variante nueva. El SKU lo arma el servicio, no el cliente. */
	public ProductVariantEntity toVariantEntity(
			VariantRequest request,
			ProductEntity product,
			BrandEntity brand,
			String sku) {
		return ProductVariantEntity.of(
				product,
				brand,
				sku,
				request.label().trim().replaceAll("\\s+", " "),
				blankToNull(request.barcode()),
				request.price(),
				request.itemCondition(),
				blankToNull(request.imageUrl()));
	}

	/** Producto + lista de variantes → JSON de catálogo. */
	public ProductResponse toResponse(ProductEntity product, List<ProductVariantEntity> variants) {
		return new ProductResponse(
				product.getId(),
				product.getName(),
				product.getDescription(),
				toCategorySummary(product.getCategory()),
				product.getVatRate(),
				product.isAllowsEmployeeDiscount(),
				product.isTracksStock(),
				product.isActive(),
				variants.stream().map(this::toVariantResponse).toList());
	}

	/** Una variante → JSON (incluye resumen de marca). */
	public VariantResponse toVariantResponse(ProductVariantEntity variant) {
		return new VariantResponse(
				variant.getId(),
				variant.getSku(),
				variant.getLabel(),
				variant.getBarcode(),
				variant.getPrice(),
				variant.getItemCondition(),
				variant.getImageUrl(),
				variant.isActive(),
				toBrandSummary(variant.getBrand()));
	}

	/** Rubro reducido a id + nombre, o null. */
	private static CategorySummary toCategorySummary(CategoryEntity category) {
		if (category == null) {
			return null;
		}
		return new CategorySummary(category.getId(), category.getName());
	}

	/** Marca reducida a id + nombre, o null. */
	private static BrandSummary toBrandSummary(BrandEntity brand) {
		if (brand == null) {
			return null;
		}
		return new BrandSummary(brand.getId(), brand.getName());
	}

	/** Cadena vacía o solo espacios → null. */
	private static String blankToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
