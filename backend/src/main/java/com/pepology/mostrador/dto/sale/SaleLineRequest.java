package com.pepology.mostrador.dto.sale;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Una variante y cuántas unidades van en el ticket. El precio lo pone el servidor. */
public record SaleLineRequest(
		@NotNull Long variantId,
		@NotNull @Min(1) Integer quantity
) {
}
