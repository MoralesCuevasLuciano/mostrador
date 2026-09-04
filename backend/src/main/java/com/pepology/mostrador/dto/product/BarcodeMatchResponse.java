package com.pepology.mostrador.dto.product;

/**
 * Una variante que ya usa un código de barras, para avisar al cargar otro igual.
 */
public record BarcodeMatchResponse(
		Long productId,
		String productName,
		Long variantId,
		String variantLabel,
		String sku,
		boolean active
) {
}
