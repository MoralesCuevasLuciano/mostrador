package com.pepology.mostrador.dto.product;

import com.pepology.mostrador.dto.category.CategorySummary;

import java.math.BigDecimal;
import java.util.List;

/** Producto con sus variantes, listo para el listado o el detalle. */
public record ProductResponse(
		Long id,
		String name,
		String description,
		CategorySummary category,
		BigDecimal vatRate,
		boolean allowsEmployeeDiscount,
		boolean active,
		List<VariantResponse> variants
) {
}
