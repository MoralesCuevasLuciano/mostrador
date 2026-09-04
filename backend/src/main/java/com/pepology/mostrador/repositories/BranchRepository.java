package com.pepology.mostrador.repositories;

import com.pepology.mostrador.models.entities.BranchEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a branch. El nombre y el punto de venta ARCA no se pueden repetir.
 */
public interface BranchRepository extends JpaRepository<BranchEntity, Long> {

	boolean existsByNameIgnoreCase(String name);

	boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

	boolean existsByPointOfSale(Integer pointOfSale);

	boolean existsByPointOfSaleAndIdNot(Integer pointOfSale, Long id);
}
