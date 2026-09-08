package com.pepology.mostrador.dto.cash;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Body del recuento de apertura: cuánta plata hay en el cajón. */
public record CashCountRequest(
		@NotNull
		@DecimalMin("0.00")
		@Digits(integer = 10, fraction = 2)
		BigDecimal openingAmount
) {
}
