package com.pepology.mostrador.repositories;

import com.pepology.mostrador.models.entities.SaleEntity;
import com.pepology.mostrador.models.entities.SaleLineEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Acceso a sale_line.
 */
public interface SaleLineRepository extends JpaRepository<SaleLineEntity, Long> {

	List<SaleLineEntity> findBySale(SaleEntity sale);
}
