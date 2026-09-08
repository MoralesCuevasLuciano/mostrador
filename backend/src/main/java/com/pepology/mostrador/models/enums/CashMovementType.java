package com.pepology.mostrador.models.enums;

/**
 * Tipo de movimiento de caja. En español porque así se guarda en MySQL.
 * Las ventas no entran: el efectivo se suma desde los pagos cuando exista ese módulo.
 */
public enum CashMovementType {
	RETIRO_RESGUARDO,
	VALE,
	GASTO,
	INGRESO_EFECTIVO;

	/** +1 si entra plata, -1 si sale. */
	public int sign() {
		return this == INGRESO_EFECTIVO ? 1 : -1;
	}
}
