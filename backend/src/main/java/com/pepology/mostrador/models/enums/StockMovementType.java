package com.pepology.mostrador.models.enums;

/**
 * Tipo de movimiento de stock. En español porque así se guarda en MySQL.
 */
public enum StockMovementType {
	VENTA,
	ANULACION_VENTA,
	AJUSTE_INICIAL,
	AJUSTE_RECUENTO,
	CONSUMO_INTERNO,
	TRASLADO,
	ENTRADA,
	EXTRAVÍO
}
