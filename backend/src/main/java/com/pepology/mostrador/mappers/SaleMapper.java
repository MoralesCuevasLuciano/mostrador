package com.pepology.mostrador.mappers;

import com.pepology.mostrador.dto.sale.SaleLineResponse;
import com.pepology.mostrador.dto.sale.SalePaymentResponse;
import com.pepology.mostrador.dto.sale.SaleResponse;
import com.pepology.mostrador.models.entities.SaleEntity;
import com.pepology.mostrador.models.entities.SaleLineEntity;
import com.pepology.mostrador.models.entities.SalePaymentEntity;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Convierte venta, líneas y pagos a DTO.
 */
@Component
public class SaleMapper {

	/** Venta persistida → ticket de respuesta. */
	public SaleResponse toResponse(SaleEntity sale, List<SaleLineEntity> lines, List<SalePaymentEntity> payments) {
		return new SaleResponse(
				sale.getId(),
				sale.getBranch().getId(),
				sale.getSession().getId(),
				sale.getSaleType(),
				sale.getStatus(),
				sale.getSoldAt(),
				sale.getSubtotal(),
				sale.getDiscountAmount(),
				sale.getTotal(),
				lines.stream().map(this::toLine).toList(),
				payments.stream().map(this::toPayment).toList());
	}

	/** Línea persistida → renglón del ticket. */
	public SaleLineResponse toLine(SaleLineEntity line) {
		var variant = line.getVariant();
		String label = variant.getLabel();
		String productName = variant.getProduct().getName();
		return new SaleLineResponse(
				line.getId(),
				variant.getId(),
				variant.getSku(),
				productName,
				label,
				line.getQuantity(),
				line.getListUnitPrice(),
				line.getLineTotal());
	}

	/** Pago persistido → medio cobrado. */
	public SalePaymentResponse toPayment(SalePaymentEntity payment) {
		return new SalePaymentResponse(
				payment.getId(),
				payment.getMethod(),
				payment.getAmount(),
				payment.getSurchargeAmount());
	}
}
