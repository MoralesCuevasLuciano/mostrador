package com.pepology.mostrador.mappers;

import com.pepology.mostrador.dto.brand.BrandRequest;
import com.pepology.mostrador.dto.brand.BrandResponse;
import com.pepology.mostrador.models.entities.BrandEntity;
import org.springframework.stereotype.Component;

@Component
public class BrandMapper {

	public BrandEntity toEntity(BrandRequest request) {
		return BrandEntity.of(request.name());
	}

	public BrandResponse toResponse(BrandEntity entity) {
		return new BrandResponse(entity.getId(), entity.getName(), entity.isActive());
	}
}
