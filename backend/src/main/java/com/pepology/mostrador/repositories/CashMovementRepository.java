package com.pepology.mostrador.repositories;

import com.pepology.mostrador.models.entities.CashMovementEntity;
import com.pepology.mostrador.models.entities.CashSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Acceso a cash_movement. El historial de entradas y salidas de una sesión.
 */
public interface CashMovementRepository extends JpaRepository<CashMovementEntity, Long> {

	/** Movimientos de una planilla, del más reciente al más viejo. */
	List<CashMovementEntity> findBySessionOrderByMovementAtDesc(CashSessionEntity session);
}
