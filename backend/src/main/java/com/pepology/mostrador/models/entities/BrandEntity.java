package com.pepology.mostrador.models.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Fila de la tabla brand. El nombre es único; la baja es lógica (is_active).
 */
@Entity
@Table(name = "brand")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BrandEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Setter(AccessLevel.NONE)
	private Long id;

	@Column(nullable = false, unique = true, length = 100)
	private String name;

	@Column(name = "is_active", nullable = false)
	private boolean active = true;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	@Setter(AccessLevel.NONE)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
	@Setter(AccessLevel.NONE)
	private LocalDateTime updatedAt;

	/** Fábrica para un alta: solo el nombre; el resto lo completa MySQL. */
	public static BrandEntity of(String name) {
		BrandEntity brand = new BrandEntity();
		brand.setName(name);
		return brand;
	}
}
