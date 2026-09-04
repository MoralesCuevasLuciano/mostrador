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

import java.time.LocalDateTime;

/**
 * Saldo actual de una variante en una sucursal. Sin fila = nunca se inventarió.
 */
@Entity
@Table(
		name = "stock",
		uniqueConstraints = @UniqueConstraint(
				name = "uq_stock_variant_branch",
				columnNames = { "product_variant_id", "branch_id" }))
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Setter(AccessLevel.NONE)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_variant_id", nullable = false)
	private ProductVariantEntity variant;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "branch_id", nullable = false)
	private BranchEntity branch;

	@Column(nullable = false)
	private int quantity;

	@Column(name = "min_quantity")
	private Integer minQuantity;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	@Setter(AccessLevel.NONE)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
	@Setter(AccessLevel.NONE)
	private LocalDateTime updatedAt;

	/** Fábrica de un saldo. quantity puede ser negativo. */
	public static StockEntity of(
			ProductVariantEntity variant,
			BranchEntity branch,
			int quantity,
			Integer minQuantity) {
		StockEntity stock = new StockEntity();
		stock.setVariant(variant);
		stock.setBranch(branch);
		stock.setQuantity(quantity);
		stock.setMinQuantity(minQuantity);
		return stock;
	}
}
