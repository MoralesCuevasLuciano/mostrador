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
 * Ficha del producto (nombre, rubro, IVA). Lo vendible es cada variante, no esta fila.
 */
@Entity
@Table(name = "product")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Setter(AccessLevel.NONE)
	private Long id;

	@Column(nullable = false, length = 200)
	private String name;

	@Column(columnDefinition = "TEXT")
	private String description;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "category_id")
	private CategoryEntity category;

	@Column(name = "vat_rate", nullable = false, precision = 5, scale = 2)
	private BigDecimal vatRate = new BigDecimal("21.00");

	@Column(name = "allows_employee_discount", nullable = false)
	private boolean allowsEmployeeDiscount = true;

	@Column(name = "is_active", nullable = false)
	private boolean active = true;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	@Setter(AccessLevel.NONE)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
	@Setter(AccessLevel.NONE)
	private LocalDateTime updatedAt;

	/** Fábrica de alta. Si vatRate es null queda el 21 % de la columna. */
	public static ProductEntity of(
			String name,
			String description,
			CategoryEntity category,
			BigDecimal vatRate,
			boolean allowsEmployeeDiscount) {
		ProductEntity product = new ProductEntity();
		product.setName(name);
		product.setDescription(description);
		product.setCategory(category);
		if (vatRate != null) {
			product.setVatRate(vatRate);
		}
		product.setAllowsEmployeeDiscount(allowsEmployeeDiscount);
		return product;
	}
}
