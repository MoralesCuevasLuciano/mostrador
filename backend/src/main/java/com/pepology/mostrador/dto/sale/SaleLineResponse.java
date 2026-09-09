package com.pepology.mostrador.dto.sale;

import java.math.BigDecimal;

/** Línea del ticket tal como se cobró. */
public record SaleLineResponse(
		Long id,
		Long variantId,
		String sku,
		String productName,
		String variantLabel,
		int quantity,
		BigDecimal listUnitPrice,
		BigDecimal lineTotal
) {
}
