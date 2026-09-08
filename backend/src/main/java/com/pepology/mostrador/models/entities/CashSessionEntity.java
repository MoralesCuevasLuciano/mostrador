package com.pepology.mostrador.models.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Planilla del día por sucursal. closing_amount vacío significa que la caja está abierta.
 */
@Entity
@Table(
		name = "cash_session",
		uniqueConstraints = @UniqueConstraint(
				name = "uq_session_branch_date",
				columnNames = { "branch_id", "business_date" }))
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CashSessionEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Setter(AccessLevel.NONE)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "branch_id", nullable = false)
	private BranchEntity branch;

	@Column(name = "business_date", nullable = false)
	private LocalDate businessDate;

	@Column(name = "opening_amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal openingAmount;

	/** Vacío = el monto de apertura es heredado y nadie lo verificó contando. */
	@Column(name = "opening_counted_at")
	private LocalDateTime openingCountedAt;

	@Column(name = "total_cash_sales", precision = 12, scale = 2)
	private BigDecimal totalCashSales;

	/** Suma congelada de movimientos negativos (vale, gasto, retiro). */
	@Column(name = "total_cash_out", precision = 12, scale = 2)
	private BigDecimal totalCashOut;

	/** Suma congelada de ingresos de efectivo. */
	@Column(name = "total_cash_in", precision = 12, scale = 2)
	private BigDecimal totalCashIn;

	@Column(name = "closing_amount", precision = 12, scale = 2)
	private BigDecimal closingAmount;

	@Column(name = "closed_at")
	private LocalDateTime closedAt;

	@Column(columnDefinition = "TEXT")
	private String note;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	@Setter(AccessLevel.NONE)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
	@Setter(AccessLevel.NONE)
	private LocalDateTime updatedAt;

	/** Fábrica de apertura. openingCountedAt queda vacío: nadie contó todavía. */
	public static CashSessionEntity of(
			BranchEntity branch,
			LocalDate businessDate,
			BigDecimal openingAmount) {
		CashSessionEntity session = new CashSessionEntity();
		session.setBranch(branch);
		session.setBusinessDate(businessDate);
		session.setOpeningAmount(openingAmount);
		return session;
	}
}
