package com.pepology.mostrador.dto.cash;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Planilla de caja. expectedAmount y difference no se guardan: se derivan.
 * open=true si closingAmount está vacío. difference es null mientras está abierta.
 * totalCashOut son solo salidas; totalCashIn, solo ingresos.
 * Con la caja abierta, ventas en efectivo, salidas e ingresos van en vivo, sin persistir.
 */
public record CashSessionResponse(
		Long id,
		Long branchId,
		String branchName,
		LocalDate businessDate,
		BigDecimal openingAmount,
		LocalDateTime openingCountedAt,
		boolean openingCounted,
		BigDecimal totalCashSales,
		BigDecimal totalCashOut,
		BigDecimal totalCashIn,
		BigDecimal closingAmount,
		LocalDateTime closedAt,
		boolean open,
		BigDecimal expectedAmount,
		BigDecimal difference,
		String note
) {
}
