package com.pepology.mostrador.models.enums;

/**
 * Medio de pago de una venta. En español porque así se guarda en MySQL.
 */
public enum SalePaymentMethod {
	EFECTIVO,
	DEBITO,
	CREDITO,
	TRANSFERENCIA,
	QR
}
