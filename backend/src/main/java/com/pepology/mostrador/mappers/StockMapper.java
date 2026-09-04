package com.pepology.mostrador.mappers;

import com.pepology.mostrador.dto.stock.StockBalanceResponse;
import com.pepology.mostrador.dto.stock.StockMovementResponse;
import com.pepology.mostrador.models.entities.BranchEntity;
import com.pepology.mostrador.models.entities.ProductVariantEntity;
import com.pepology.mostrador.models.entities.StockEntity;
import com.pepology.mostrador.models.entities.StockMovementEntity;
import org.springframework.stereotype.Component;

/**
 * Convierte saldo y movimiento de stock a DTO.
 */
@Component
public class StockMapper {

	/** Fila persistida → saldo inventariado. */
	public StockBalanceResponse toBalance(StockEntity stock) {
		ProductVariantEntity variant = stock.getVariant();
		BranchEntity branch = stock.getBranch();
		return new StockBalanceResponse(
				stock.getId(),
				variant.getId(),
				variant.getSku(),
				variant.getLabel(),
				branch.getId(),
				branch.getName(),
				stock.getQuantity(),
				stock.getMinQuantity(),
				true);
	}

	/** Variante y sucursal sin fila de stock: nunca se inventarió. */
	public StockBalanceResponse toUninventoriedBalance(ProductVariantEntity variant, BranchEntity branch) {
		return new StockBalanceResponse(
				null,
				variant.getId(),
				variant.getSku(),
				variant.getLabel(),
				branch.getId(),
				branch.getName(),
				null,
				null,
				false);
	}

	/** Movimiento persistido → historial. */
	public StockMovementResponse toMovement(StockMovementEntity movement) {
		ProductVariantEntity variant = movement.getVariant();
		BranchEntity branch = movement.getBranch();
		Long relatedId = movement.getRelatedMovement() == null ? null : movement.getRelatedMovement().getId();
		return new StockMovementResponse(
				movement.getId(),
				variant.getId(),
				variant.getSku(),
				branch.getId(),
				branch.getName(),
				movement.getMovementType(),
				movement.getQuantity(),
				relatedId,
				movement.getDescription(),
				movement.getMovementAt());
	}
}
