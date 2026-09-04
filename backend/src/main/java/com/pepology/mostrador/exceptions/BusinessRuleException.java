package com.pepology.mostrador.exceptions;

/**
 * Regla de negocio incumplida (nombre duplicado, baja inválida, etc.).
 * El handler global la traduce a HTTP 409.
 */
public class BusinessRuleException extends RuntimeException {

	public BusinessRuleException(String message) {
		super(message);
	}
}
