package com.pepology.mostrador.models.entities;

import com.pepology.mostrador.models.enums.CashMovementType;
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
 * Entrada o salida de efectivo que no es venta. amount con signo: negativo sale, positivo entra.
 */
@Entity
@Table(name = "cash_movement")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CashMovementEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Setter(AccessLevel.NONE)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "cash_session_id", nullable = false)
	private CashSessionEntity session;

	@Enumerated(EnumType.STRING)
	@Column(name = "movement_type", nullable = false, length = 30)
	private CashMovementType movementType;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal amount;

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
	 * Fábrica de un movimiento. amount es con signo: positivo entra, negativo sale.
	 */
	public static CashMovementEntity of(
			CashSessionEntity session,
			CashMovementType movementType,
			BigDecimal amount,
			String description,
			LocalDateTime movementAt) {
		CashMovementEntity movement = new CashMovementEntity();
		movement.setSession(session);
		movement.setMovementType(movementType);
		movement.setAmount(amount);
		movement.setDescription(description);
		movement.setMovementAt(movementAt == null ? LocalDateTime.now() : movementAt);
		return movement;
	}
}
