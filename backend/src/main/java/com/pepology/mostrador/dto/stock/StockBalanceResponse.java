package com.pepology.mostrador.dto.stock;

/** Saldo de una variante en un local. inventoried=false si nunca se contó. */
public record StockBalanceResponse(
		Long id,
		Long variantId,
		String sku,
		String variantLabel,
		Long branchId,
		String branchName,
		Integer quantity,
		Integer minQuantity,
		boolean inventoried
) {
}
