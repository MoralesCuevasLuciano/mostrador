package com.pepology.mostrador.models.entities;

import com.pepology.mostrador.models.enums.StockMovementType;
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

import java.time.LocalDateTime;

/**
 * Movimiento que explica un cambio de saldo. El stock no se escribe sin pasar por acá.
 */
@Entity
@Table(name = "stock_movement")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockMovementEntity {

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

	@Enumerated(EnumType.STRING)
	@Column(name = "movement_type", nullable = false, length = 30)
	private StockMovementType movementType;

	@Column(nullable = false)
	private int quantity;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "related_movement_id")
	private StockMovementEntity relatedMovement;

	@Column(length = 255)
	private String description;

	@Column(name = "movement_at", nullable = false)
	private LocalDateTime movementAt;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	@Setter(AccessLevel.NONE)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
	@Setter(AccessLevel.NONE)
	private LocalDateTime updatedAt;

	/**
	 * Fábrica de un movimiento. quantity es con signo: positivo entra, negativo sale.
	 * relatedMovement une las dos patas de un traslado.
	 */
	public static StockMovementEntity of(
			ProductVariantEntity variant,
			BranchEntity branch,
			StockMovementType movementType,
			int quantity,
			StockMovementEntity relatedMovement,
			String description,
			LocalDateTime movementAt) {
		StockMovementEntity movement = new StockMovementEntity();
		movement.setVariant(variant);
		movement.setBranch(branch);
		movement.setMovementType(movementType);
		movement.setQuantity(quantity);
		movement.setRelatedMovement(relatedMovement);
		movement.setDescription(description);
		movement.setMovementAt(movementAt == null ? LocalDateTime.now() : movementAt);
		return movement;
	}
}
