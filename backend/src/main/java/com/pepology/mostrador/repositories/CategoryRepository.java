package com.pepology.mostrador.repositories;

import com.pepology.mostrador.models.entities.CategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<CategoryEntity, Long> {

	List<CategoryEntity> findByParentIsNull();

	List<CategoryEntity> findByParent(CategoryEntity parent);
}
