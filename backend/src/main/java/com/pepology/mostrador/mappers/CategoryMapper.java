package com.pepology.mostrador.mappers;

import com.pepology.mostrador.dto.category.CategoryRequest;
import com.pepology.mostrador.dto.category.CategoryResponse;
import com.pepology.mostrador.models.entities.CategoryEntity;
import org.springframework.stereotype.Component;

/**
 * Convierte entre DTO de categoría y la entidad JPA.
 */
@Component
public class CategoryMapper {

	/** Request + padre ya resuelto → entidad nueva. */
	public CategoryEntity toEntity(CategoryRequest request, CategoryEntity parent) {
		return CategoryEntity.of(request.name(), parent);
	}

	/** Entidad persistida → JSON (el padre se expone solo como id). */
	public CategoryResponse toResponse(CategoryEntity entity) {
		Long parentId = entity.getParent() == null ? null : entity.getParent().getId();
		return new CategoryResponse(entity.getId(), entity.getName(), parentId, entity.isActive());
	}
}
