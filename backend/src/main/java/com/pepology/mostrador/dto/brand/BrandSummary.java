package com.pepology.mostrador.dto.brand;

/** Marca reducida (id + nombre) para embeberla en una variante. */
public record BrandSummary(
		Long id,
		String name
) {
}
