package com.pepology.mostrador.dto.stock;

/** Resultado de un traslado: las dos patas y los saldos de origen y destino. */
public record StockTransferResponse(
		StockMovementResponse outbound,
		StockMovementResponse inbound,
		StockBalanceResponse from,
		StockBalanceResponse to
) {
}
