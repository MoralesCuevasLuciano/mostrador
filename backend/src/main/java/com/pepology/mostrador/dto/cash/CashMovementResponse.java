package com.pepology.mostrador.dto.cash;

import com.pepology.mostrador.models.enums.CashMovementType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Movimiento de caja listo para el historial. amount con signo. */
public record CashMovementResponse(
		Long id,
		Long sessionId,
		CashMovementType movementType,
		BigDecimal amount,
		String description,
		LocalDateTime movementAt
) {
}
