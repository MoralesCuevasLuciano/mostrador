package com.pepology.mostrador.repositories;

import com.pepology.mostrador.models.entities.CashSessionEntity;
import com.pepology.mostrador.models.entities.SaleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Acceso a sale. El historial del día cuelga de la sesión de caja.
 */
public interface SaleRepository extends JpaRepository<SaleEntity, Long> {

	/** Tickets de esa planilla, del más reciente al más viejo. */
	List<SaleEntity> findBySessionOrderBySoldAtDesc(CashSessionEntity session);
}
