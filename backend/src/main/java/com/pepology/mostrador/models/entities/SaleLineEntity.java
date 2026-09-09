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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Línea de un ticket: variante, cantidad y precios congelados al vender.
 */
@Entity
@Table(name = "sale_line")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SaleLineEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Setter(AccessLevel.NONE)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "sale_id", nullable = false)
	private SaleEntity sale;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_variant_id", nullable = false)
	private ProductVariantEntity variant;

	@Column(nullable = false)
	private int quantity;

	@Column(name = "list_unit_price", nullable = false, precision = 12, scale = 2)
	private BigDecimal listUnitPrice;

	@Column(name = "line_total", nullable = false, precision = 12, scale = 2)
	private BigDecimal lineTotal;

	@Column(name = "vat_rate", nullable = false, precision = 5, scale = 2)
	private BigDecimal vatRate;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	@Setter(AccessLevel.NONE)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
	@Setter(AccessLevel.NONE)
	private LocalDateTime updatedAt;

	/** Fábrica de una línea. listUnitPrice y vatRate salen de la ficha, no del cliente. */
	public static SaleLineEntity of(
			SaleEntity sale,
			ProductVariantEntity variant,
			int quantity,
			BigDecimal listUnitPrice,
			BigDecimal lineTotal,
			BigDecimal vatRate) {
		SaleLineEntity line = new SaleLineEntity();
		line.setSale(sale);
		line.setVariant(variant);
		line.setQuantity(quantity);
		line.setListUnitPrice(listUnitPrice);
		line.setLineTotal(lineTotal);
		line.setVatRate(vatRate);
		return line;
	}
}
