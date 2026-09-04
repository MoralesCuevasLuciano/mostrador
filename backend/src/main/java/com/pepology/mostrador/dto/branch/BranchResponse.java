package com.pepology.mostrador.dto.branch;

/** Sucursal tal como sale en la API. */
public record BranchResponse(
		Long id,
		String name,
		String address,
		String phone,
		Integer pointOfSale,
		boolean active
) {
}
