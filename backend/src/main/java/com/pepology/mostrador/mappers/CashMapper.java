package com.pepology.mostrador.mappers;

import com.pepology.mostrador.dto.cash.CashMovementResponse;
import com.pepology.mostrador.dto.cash.CashSessionResponse;
import com.pepology.mostrador.models.entities.CashMovementEntity;
import com.pepology.mostrador.models.entities.CashSessionEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Convierte sesión y movimiento de caja a DTO.
 */
@Component
public class CashMapper {

	/**
	 * Planilla persistida → respuesta.
	 * liveOut y liveIn se usan solo si la caja está abierta; también van en el DTO.
	 */
	public CashSessionResponse toSession(CashSessionEntity session, BigDecimal liveOut, BigDecimal liveIn) {
		boolean open = session.getClosingAmount() == null;
		BigDecimal sales = open ? BigDecimal.ZERO : zeroIfNull(session.getTotalCashSales());
		BigDecimal out = open ? zeroIfNull(liveOut) : zeroIfNull(session.getTotalCashOut());
		BigDecimal in = open ? zeroIfNull(liveIn) : zeroIfNull(session.getTotalCashIn());
		BigDecimal expected = session.getOpeningAmount().add(sales).add(out).add(in);
		BigDecimal difference = open ? null : session.getClosingAmount().subtract(expected);
		return new CashSessionResponse(
				session.getId(),
				session.getBranch().getId(),
				session.getBranch().getName(),
				session.getBusinessDate(),
				session.getOpeningAmount(),
				session.getOpeningCountedAt(),
				session.getOpeningCountedAt() != null,
				open ? sales : session.getTotalCashSales(),
				out,
				in,
				session.getClosingAmount(),
				session.getClosedAt(),
				open,
				expected,
				difference,
				session.getNote());
	}

	/** Movimiento persistido → historial. */
	public CashMovementResponse toMovement(CashMovementEntity movement) {
		return new CashMovementResponse(
				movement.getId(),
				movement.getSession().getId(),
				movement.getMovementType(),
				movement.getAmount(),
				movement.getDescription(),
				movement.getMovementAt());
	}

	private static BigDecimal zeroIfNull(BigDecimal value) {
		return value == null ? BigDecimal.ZERO : value;
	}
}
