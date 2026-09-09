package com.pepology.mostrador.models.entities;

import com.pepology.mostrador.models.enums.SalePaymentMethod;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Un medio con el que se cobró la venta. amount incluye recargo.
 */
@Entity
@Table(name = "sale_payment")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SalePaymentEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Setter(AccessLevel.NONE)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "sale_id", nullable = false)
	private SaleEntity sale;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private SalePaymentMethod method;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal amount;

	@Column(name = "surcharge_amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal surchargeAmount;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	@Setter(AccessLevel.NONE)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
	@Setter(AccessLevel.NONE)
	private LocalDateTime updatedAt;

	/** Fábrica de un cobro. surchargeAmount es cero si no hay cuotas. */
	public static SalePaymentEntity of(
			SaleEntity sale,
			SalePaymentMethod method,
			BigDecimal amount,
			BigDecimal surchargeAmount) {
		SalePaymentEntity payment = new SalePaymentEntity();
		payment.setSale(sale);
		payment.setMethod(method);
		payment.setAmount(amount);
		payment.setSurchargeAmount(surchargeAmount);
		return payment;
	}
}
