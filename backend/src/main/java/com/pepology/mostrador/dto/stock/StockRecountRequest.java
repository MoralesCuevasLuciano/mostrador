package com.pepology.mostrador.dto.stock;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Body de un recuento: cuántas unidades hay ahora en ese local. */
public record StockRecountRequest(
		@NotNull @Min(0) Integer countedQuantity,
		@Size(max = 255) String description
) {
}
