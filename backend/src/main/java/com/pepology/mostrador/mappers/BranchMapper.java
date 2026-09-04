package com.pepology.mostrador.mappers;

import com.pepology.mostrador.dto.branch.BranchRequest;
import com.pepology.mostrador.dto.branch.BranchResponse;
import com.pepology.mostrador.models.entities.BranchEntity;
import org.springframework.stereotype.Component;

/**
 * Convierte entre DTO de sucursal y la entidad JPA.
 */
@Component
public class BranchMapper {

	/** Request de alta → entidad nueva. */
	public BranchEntity toEntity(BranchRequest request) {
		return BranchEntity.of(request.name(), request.address(), request.phone(), request.pointOfSale());
	}

	/** Entidad persistida → JSON de salida. */
	public BranchResponse toResponse(BranchEntity entity) {
		return new BranchResponse(
				entity.getId(),
				entity.getName(),
				entity.getAddress(),
				entity.getPhone(),
				entity.getPointOfSale(),
				entity.isActive());
	}
}
