package com.pepology.mostrador.mappers;

import com.pepology.mostrador.dto.brand.BrandRequest;
import com.pepology.mostrador.dto.brand.BrandResponse;
import com.pepology.mostrador.models.entities.BrandEntity;
import org.springframework.stereotype.Component;

/**
 * Convierte entre DTO de marca y la entidad JPA.
 */
@Component
public class BrandMapper {

	/** Request de alta/edición → entidad nueva. */
	public BrandEntity toEntity(BrandRequest request) {
		return BrandEntity.of(request.name());
	}

	/** Entidad persistida → JSON de salida. */
	public BrandResponse toResponse(BrandEntity entity) {
		return new BrandResponse(entity.getId(), entity.getName(), entity.isActive());
	}
}
