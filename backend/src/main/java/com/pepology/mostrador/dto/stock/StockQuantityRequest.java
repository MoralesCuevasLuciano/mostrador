package com.pepology.mostrador.dto.stock;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Body de entrada, consumo interno o extravío: unidades positivas. */
public record StockQuantityRequest(
		@NotNull @Min(1) Integer quantity,
		@Size(max = 255) String description
) {
}
