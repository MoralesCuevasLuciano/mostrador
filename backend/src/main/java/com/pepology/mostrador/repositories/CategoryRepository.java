package com.pepology.mostrador.repositories;

import com.pepology.mostrador.models.entities.CategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<CategoryEntity, Long> {

	List<CategoryEntity> findByParentIsNull();

	List<CategoryEntity> findByParent(CategoryEntity parent);

	boolean existsByParentIsNullAndNameIgnoreCase(String name);

	boolean existsByParentAndNameIgnoreCase(CategoryEntity parent, String name);

	boolean existsByParentIsNullAndNameIgnoreCaseAndIdNot(String name, Long id);

	boolean existsByParentAndNameIgnoreCaseAndIdNot(CategoryEntity parent, String name, Long id);

	boolean existsByParentAndActiveTrue(CategoryEntity parent);
}
