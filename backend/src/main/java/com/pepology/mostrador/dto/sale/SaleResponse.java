package com.pepology.mostrador.dto.sale;

import com.pepology.mostrador.models.enums.SaleStatus;
import com.pepology.mostrador.models.enums.SaleType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Ticket cobrado, con líneas y pagos. */
public record SaleResponse(
		Long id,
		Long branchId,
		Long cashSessionId,
		SaleType saleType,
		SaleStatus status,
		LocalDateTime soldAt,
		BigDecimal subtotal,
		BigDecimal discountAmount,
		BigDecimal total,
		List<SaleLineResponse> lines,
		List<SalePaymentResponse> payments
) {
}
