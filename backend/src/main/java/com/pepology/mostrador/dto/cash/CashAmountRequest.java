package com.pepology.mostrador.dto.cash;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Body de retiro, vale, gasto o ingreso: monto positivo; el signo lo pone el servicio. */
public record CashAmountRequest(
		@NotNull
		@DecimalMin("0.01")
		@Digits(integer = 10, fraction = 2)
		BigDecimal amount,
		@Size(max = 255) String description
) {
}
