package com.pepology.mostrador.dto.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body de alta o edición. parentId null = rubro raíz. */
public record CategoryRequest(
		@NotBlank @Size(max = 100) String name,
		Long parentId
) {
}
