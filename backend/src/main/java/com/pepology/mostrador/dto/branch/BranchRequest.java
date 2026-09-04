package com.pepology.mostrador.dto.branch;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body de alta o edición de sucursal. */
public record BranchRequest(
		@NotBlank @Size(max = 100) String name,
		@Size(max = 200) String address,
		@Size(max = 50) String phone,
		@Min(1) Integer pointOfSale
) {
}
