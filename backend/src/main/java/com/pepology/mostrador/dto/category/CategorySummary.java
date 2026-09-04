package com.pepology.mostrador.dto.category;

/** Rubro reducido (id + nombre) para embeberlo en un producto. */
public record CategorySummary(
		Long id,
		String name
) {
}
