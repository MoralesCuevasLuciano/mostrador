package com.pepology.mostrador.dto.product;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record ProductRequest(
		@NotBlank @Size(max = 200) String name,
		String description,
		Long categoryId,
		@DecimalMin("0.00") @Digits(integer = 3, fraction = 2) BigDecimal vatRate,
		Boolean allowsEmployeeDiscount,
		@NotEmpty @Valid List<VariantRequest> variants
) {
}
