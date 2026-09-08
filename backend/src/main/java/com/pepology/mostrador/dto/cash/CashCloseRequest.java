package com.pepology.mostrador.dto.cash;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Body del cierre: cuánta plata hay al terminar el día. */
public record CashCloseRequest(
		@NotNull
		@DecimalMin("0.00")
		@Digits(integer = 10, fraction = 2)
		BigDecimal closingAmount,
		@Size(max = 2000) String note
) {
}
