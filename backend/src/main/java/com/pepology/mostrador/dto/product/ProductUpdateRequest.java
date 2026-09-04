package com.pepology.mostrador.dto.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Body de edición de la ficha. No incluye variantes. */
public record ProductUpdateRequest(
		@NotBlank @Size(max = 200) String name,
		String description,
		Long categoryId,
		@DecimalMin("0.00") @Digits(integer = 3, fraction = 2) BigDecimal vatRate,
		Boolean allowsEmployeeDiscount
) {
}
