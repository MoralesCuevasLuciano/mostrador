package com.pepology.mostrador.repositories;

import com.pepology.mostrador.models.entities.BranchEntity;
import com.pepology.mostrador.models.entities.ProductVariantEntity;
import com.pepology.mostrador.models.entities.StockMovementEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Acceso a stock_movement. El historial que explica cada cambio de saldo.
 */
public interface StockMovementRepository extends JpaRepository<StockMovementEntity, Long> {

	/** Movimientos de una variante en un local, del más reciente al más viejo. */
	List<StockMovementEntity> findByVariantAndBranchOrderByMovementAtDesc(
			ProductVariantEntity variant,
			BranchEntity branch);

	List<StockMovementEntity> findByBranchOrderByMovementAtDesc(BranchEntity branch);
}
