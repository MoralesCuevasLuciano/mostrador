package com.pepology.mostrador.models.entities;

import com.pepology.mostrador.models.enums.SaleStatus;
import com.pepology.mostrador.models.enums.SaleType;
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
 * Venta cobrada en el mostrador. El id es el número de ticket interno.
 */
@Entity
@Table(name = "sale")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SaleEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Setter(AccessLevel.NONE)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "branch_id", nullable = false)
	private BranchEntity branch;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "cash_session_id", nullable = false)
	private CashSessionEntity session;

	@Enumerated(EnumType.STRING)
	@Column(name = "sale_type", nullable = false, length = 20)
	private SaleType saleType;

	@Column(name = "sold_at", nullable = false)
	private LocalDateTime soldAt;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal subtotal;

	@Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal discountAmount;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal total;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private SaleStatus status;

	@Column(columnDefinition = "TEXT")
	private String note;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	@Setter(AccessLevel.NONE)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
	@Setter(AccessLevel.NONE)
	private LocalDateTime updatedAt;

	/** Fábrica de una venta nueva, ya cobrada. */
	public static SaleEntity of(
			BranchEntity branch,
			CashSessionEntity session,
			SaleType saleType,
			BigDecimal subtotal,
			BigDecimal discountAmount,
			BigDecimal total,
			SaleStatus status) {
		SaleEntity sale = new SaleEntity();
		sale.setBranch(branch);
		sale.setSession(session);
		sale.setSaleType(saleType);
		sale.setSoldAt(LocalDateTime.now());
		sale.setSubtotal(subtotal);
		sale.setDiscountAmount(discountAmount);
		sale.setTotal(total);
		sale.setStatus(status);
		return sale;
	}
}
