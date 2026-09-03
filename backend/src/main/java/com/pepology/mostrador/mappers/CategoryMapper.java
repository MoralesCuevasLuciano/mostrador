package com.pepology.mostrador.mappers;

import com.pepology.mostrador.dto.category.CategoryRequest;
import com.pepology.mostrador.dto.category.CategoryResponse;
import com.pepology.mostrador.models.entities.CategoryEntity;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

	public CategoryEntity toEntity(CategoryRequest request, CategoryEntity parent) {
		return CategoryEntity.of(request.name(), parent);
	}

	public CategoryResponse toResponse(CategoryEntity entity) {
		Long parentId = entity.getParent() == null ? null : entity.getParent().getId();
		return new CategoryResponse(entity.getId(), entity.getName(), parentId, entity.isActive());
	}
}
