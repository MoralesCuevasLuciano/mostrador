package com.pepology.mostrador.repositories;

import com.pepology.mostrador.models.entities.BranchEntity;
import com.pepology.mostrador.models.entities.ProductVariantEntity;
import com.pepology.mostrador.models.entities.StockEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Acceso a stock. Una fila por variante y sucursal; si no hay fila, nunca se inventarió.
 */
public interface StockRepository extends JpaRepository<StockEntity, Long> {

	/** El saldo de esa variante en ese local, o vacío si todavía no se contó. */
	Optional<StockEntity> findByVariantAndBranch(ProductVariantEntity variant, BranchEntity branch);

	List<StockEntity> findByBranch(BranchEntity branch);

	List<StockEntity> findByVariant(ProductVariantEntity variant);
}
