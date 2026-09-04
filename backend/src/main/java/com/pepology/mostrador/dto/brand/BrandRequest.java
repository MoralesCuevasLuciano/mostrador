package com.pepology.mostrador.dto.brand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body de alta o edición de marca. */
public record BrandRequest(
		@NotBlank @Size(max = 100) String name
) {
}
