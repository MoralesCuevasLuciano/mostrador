package com.pepology.mostrador.dto.stock;

import com.pepology.mostrador.models.enums.StockMovementType;

import java.time.LocalDateTime;

/** Movimiento de stock listo para el historial. */
public record StockMovementResponse(
		Long id,
		Long variantId,
		String sku,
		Long branchId,
		String branchName,
		StockMovementType movementType,
		int quantity,
		Long relatedMovementId,
		String description,
		LocalDateTime movementAt
) {
}
