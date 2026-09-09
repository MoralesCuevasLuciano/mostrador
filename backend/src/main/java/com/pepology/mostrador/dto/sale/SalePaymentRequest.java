package com.pepology.mostrador.dto.sale;

import com.pepology.mostrador.models.enums.SalePaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Un cobro. amount es lo que entra por ese medio, recargo incluido. */
public record SalePaymentRequest(
		@NotNull SalePaymentMethod method,
		@NotNull @DecimalMin("0.01") BigDecimal amount
) {
}
