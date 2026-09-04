package com.pepology.mostrador.dto.category;

/** Categoría tal como sale en la API. */
public record CategoryResponse(
		Long id,
		String name,
		Long parentId,
		boolean active
) {
}
