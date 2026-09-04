package com.pepology.mostrador.dto.brand;

/** Marca tal como sale en la API. */
public record BrandResponse(
		Long id,
		String name,
		boolean active
) {
}
