package com.pepology.mostrador.dto.sale;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** Body de una venta: líneas a cobrar y cómo se paga. */
public record SaleRequest(
		@NotEmpty @Valid List<SaleLineRequest> lines,
		@NotEmpty @Valid List<SalePaymentRequest> payments
) {
}
