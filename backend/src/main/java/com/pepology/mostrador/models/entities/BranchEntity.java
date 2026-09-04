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
 * Sucursal (local físico). Todavía sin API; entra cuando haya stock y caja.
 */
@Entity
@Table(name = "branch")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BranchEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Setter(AccessLevel.NONE)
	private Long id;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(length = 200)
	private String address;

	@Column(length = 50)
	private String phone;

	@Column(name = "point_of_sale")
	private Integer pointOfSale;

	@Column(name = "is_active", nullable = false)
	private boolean active = true;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	@Setter(AccessLevel.NONE)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
	@Setter(AccessLevel.NONE)
	private LocalDateTime updatedAt;
}
