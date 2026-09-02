package com.pepology.mostrador.dto.category;

public record CategoryResponse(
		Long id,
		String name,
		Long parentId,
		boolean active
) {
}
