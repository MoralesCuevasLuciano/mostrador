package com.pepology.mostrador.dto.stock;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Body de un traslado entre sucursales. */
public record StockTransferRequest(
		@NotNull Long variantId,
		@NotNull Long fromBranchId,
		@NotNull Long toBranchId,
		@NotNull @Min(1) Integer quantity,
		@Size(max = 255) String description
) {
}
