package com.pepology.mostrador.dto.cash;

import com.pepology.mostrador.models.enums.CashMovementType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Body para corregir un movimiento de una caja abierta.
 * El monto va positivo; el signo lo pone el tipo.
 */
public record CashMovementUpdateRequest(
		@NotNull CashMovementType movementType,
		@NotNull
		@DecimalMin("0.01")
		@Digits(integer = 10, fraction = 2)
		BigDecimal amount,
		@Size(max = 255) String description
) {
}
