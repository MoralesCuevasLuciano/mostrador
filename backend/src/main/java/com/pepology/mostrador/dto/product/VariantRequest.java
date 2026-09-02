package com.pepology.mostrador.dto.product;

import com.pepology.mostrador.models.enums.ItemCondition;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record VariantRequest(
		Long brandId,
		@NotBlank @Size(max = 100) String label,
		@Size(max = 50) String barcode,
		@NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal price,
		ItemCondition itemCondition,
		@Size(max = 500) String imageUrl
) {
}
