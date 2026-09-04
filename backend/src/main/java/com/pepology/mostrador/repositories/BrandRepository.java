package com.pepology.mostrador.repositories;

import com.pepology.mostrador.models.entities.BrandEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Acceso a la tabla brand. Los existsByName* evitan duplicados ignorando mayúsculas.
 */
public interface BrandRepository extends JpaRepository<BrandEntity, Long> {

	Optional<BrandEntity> findByName(String name);

	/** True si ya hay una marca con ese nombre, sin importar mayúsculas. */
	boolean existsByNameIgnoreCase(String name);

	/** Igual que existsByNameIgnoreCase, pero ignora la fila que se está editando. */
	boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
