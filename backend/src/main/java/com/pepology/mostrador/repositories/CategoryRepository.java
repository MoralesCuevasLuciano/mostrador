package com.pepology.mostrador.repositories;

import com.pepology.mostrador.models.entities.CategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Acceso a category. Consultas de unicidad por padre y de hijas activas (para la baja).
 */
public interface CategoryRepository extends JpaRepository<CategoryEntity, Long> {

	List<CategoryEntity> findByParentIsNull();

	List<CategoryEntity> findByParent(CategoryEntity parent);

	/** ¿Ya hay un rubro raíz con ese nombre? */
	boolean existsByParentIsNullAndNameIgnoreCase(String name);

	/** ¿Ya hay una hija de ese padre con ese nombre? */
	boolean existsByParentAndNameIgnoreCase(CategoryEntity parent, String name);

	/** Unicidad de rubro raíz al editar (excluye el id actual). */
	boolean existsByParentIsNullAndNameIgnoreCaseAndIdNot(String name, Long id);

	/** Unicidad de subcategoría al editar. */
	boolean existsByParentAndNameIgnoreCaseAndIdNot(CategoryEntity parent, String name, Long id);

	/** True si el rubro todavía tiene subcategorías activas. */
	boolean existsByParentAndActiveTrue(CategoryEntity parent);
}
