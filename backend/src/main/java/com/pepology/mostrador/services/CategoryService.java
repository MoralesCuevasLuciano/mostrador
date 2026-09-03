package com.pepology.mostrador.services;

import com.pepology.mostrador.dto.category.CategoryRequest;
import com.pepology.mostrador.dto.category.CategoryResponse;
import com.pepology.mostrador.exceptions.BusinessRuleException;
import com.pepology.mostrador.exceptions.NotFoundException;
import com.pepology.mostrador.mappers.CategoryMapper;
import com.pepology.mostrador.models.entities.CategoryEntity;
import com.pepology.mostrador.repositories.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

	private final CategoryRepository categoryRepository;
	private final CategoryMapper categoryMapper;

	@Transactional
	public CategoryResponse create(CategoryRequest request) {
		String name = normalize(request.name());
		CategoryEntity parent = resolveParent(request.parentId());
		assertNameAvailable(name, parent, null);
		CategoryEntity saved = categoryRepository.save(
				categoryMapper.toEntity(new CategoryRequest(name, request.parentId()), parent));
		return categoryMapper.toResponse(saved);
	}

	@Transactional(readOnly = true)
	public List<CategoryResponse> findAll() {
		return categoryRepository.findAll(Sort.by("name")).stream()
				.map(categoryMapper::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public CategoryResponse findById(Long id) {
		return categoryMapper.toResponse(requireById(id));
	}

	@Transactional
	public CategoryResponse update(Long id, CategoryRequest request) {
		CategoryEntity category = requireById(id);
		if (request.parentId() != null && request.parentId().equals(id)) {
			throw new BusinessRuleException("Una categoría no puede ser padre de sí misma");
		}
		String name = normalize(request.name());
		CategoryEntity parent = resolveParent(request.parentId());
		if (parent != null && hasChildren(category)) {
			throw new BusinessRuleException("Un rubro con subcategorías no puede pasar a ser subcategoría");
		}
		assertNameAvailable(name, parent, id);
		category.setName(name);
		category.setParent(parent);
		return categoryMapper.toResponse(category);
	}

	@Transactional
	public CategoryResponse deactivate(Long id) {
		CategoryEntity category = requireById(id);
		if (!category.isActive()) {
			throw new BusinessRuleException("La categoría ya está dada de baja");
		}
		if (categoryRepository.existsByParentAndActiveTrue(category)) {
			throw new BusinessRuleException("No se puede dar de baja un rubro que tiene subcategorías activas");
		}
		category.setActive(false);
		return categoryMapper.toResponse(category);
	}

	@Transactional
	public CategoryResponse reactivate(Long id) {
		CategoryEntity category = requireById(id);
		if (category.isActive()) {
			throw new BusinessRuleException("La categoría ya está activa");
		}
		CategoryEntity parent = category.getParent();
		if (parent != null && !parent.isActive()) {
			throw new BusinessRuleException("No se puede reactivar una subcategoría cuyo rubro está dado de baja");
		}
		category.setActive(true);
		return categoryMapper.toResponse(category);
	}

	private CategoryEntity resolveParent(Long parentId) {
		if (parentId == null) {
			return null;
		}
		CategoryEntity parent = requireActive(parentId);
		if (parent.getParent() != null) {
			throw new BusinessRuleException("Solo hay dos niveles: no se puede colgar de una subcategoría");
		}
		return parent;
	}

	private void assertNameAvailable(String name, CategoryEntity parent, Long excludeId) {
		boolean exists = parent == null
				? existsAsRoot(name, excludeId)
				: existsAsChild(name, parent, excludeId);
		if (exists) {
			String scope = parent == null ? "un rubro" : "esta categoría";
			throw new BusinessRuleException("Ya existe " + scope + " con el nombre \"" + name + "\"");
		}
	}

	private boolean existsAsRoot(String name, Long excludeId) {
		return excludeId == null
				? categoryRepository.existsByParentIsNullAndNameIgnoreCase(name)
				: categoryRepository.existsByParentIsNullAndNameIgnoreCaseAndIdNot(name, excludeId);
	}

	private boolean existsAsChild(String name, CategoryEntity parent, Long excludeId) {
		return excludeId == null
				? categoryRepository.existsByParentAndNameIgnoreCase(parent, name)
				: categoryRepository.existsByParentAndNameIgnoreCaseAndIdNot(parent, name, excludeId);
	}

	private boolean hasChildren(CategoryEntity category) {
		return !categoryRepository.findByParent(category).isEmpty();
	}

	private CategoryEntity requireById(Long id) {
		return categoryRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("No existe la categoría " + id));
	}

	@Transactional(readOnly = true)
	public CategoryEntity requireActive(Long id) {
		CategoryEntity category = requireById(id);
		if (!category.isActive()) {
			throw new BusinessRuleException("No se puede usar una categoría dada de baja");
		}
		return category;
	}

	private static String normalize(String name) {
		return name.trim().replaceAll("\\s+", " ");
	}
}
