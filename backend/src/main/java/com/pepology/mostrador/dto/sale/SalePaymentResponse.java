package com.pepology.mostrador.dto.sale;

import com.pepology.mostrador.models.enums.SalePaymentMethod;

import java.math.BigDecimal;

/** Cobro registrado en el ticket. */
public record SalePaymentResponse(
		Long id,
		SalePaymentMethod method,
		BigDecimal amount,
		BigDecimal surchargeAmount
) {
}
