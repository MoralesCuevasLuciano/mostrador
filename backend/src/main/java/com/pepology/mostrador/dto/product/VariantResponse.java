package com.pepology.mostrador.dto.product;

import com.pepology.mostrador.dto.brand.BrandSummary;
import com.pepology.mostrador.models.enums.ItemCondition;

import java.math.BigDecimal;

public record VariantResponse(
		Long id,
		String sku,
		String label,
		String barcode,
		BigDecimal price,
		ItemCondition itemCondition,
		String imageUrl,
		boolean active,
		BrandSummary brand
) {
}
