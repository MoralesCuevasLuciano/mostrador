package com.pepology.mostrador.services;

import com.pepology.mostrador.dto.stock.StockBalanceResponse;
import com.pepology.mostrador.dto.stock.StockMovementResponse;
import com.pepology.mostrador.dto.stock.StockTransferResponse;
import com.pepology.mostrador.exceptions.BusinessRuleException;
import com.pepology.mostrador.mappers.StockMapper;
import com.pepology.mostrador.models.entities.BranchEntity;
import com.pepology.mostrador.models.entities.ProductVariantEntity;
import com.pepology.mostrador.models.entities.StockEntity;
import com.pepology.mostrador.models.entities.StockMovementEntity;
import com.pepology.mostrador.models.enums.StockMovementType;
import com.pepology.mostrador.repositories.StockMovementRepository;
import com.pepology.mostrador.repositories.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Inventario por variante y sucursal. El saldo solo cambia si se persiste un movimiento.
 */
@Service
@RequiredArgsConstructor
public class StockService {

	private final StockRepository stockRepository;
	private final StockMovementRepository stockMovementRepository;
	private final BranchService branchService;
	private final ProductService productService;
	private final StockMapper stockMapper;

	/** Saldo de una variante en un local. Sin fila no es cero: nunca se inventarió. */
	@Transactional(readOnly = true)
	public StockBalanceResponse getBalance(Long variantId, Long branchId) {
		ProductVariantEntity variant = productService.requireActiveVariant(variantId);
		BranchEntity branch = branchService.requireActive(branchId);
		return stockRepository.findByVariantAndBranch(variant, branch)
				.map(stockMapper::toBalance)
				.orElseGet(() -> stockMapper.toUninventoriedBalance(variant, branch));
	}

	/** Variantes ya contadas en esa sucursal. */
	@Transactional(readOnly = true)
	public List<StockBalanceResponse> listByBranch(Long branchId) {
		BranchEntity branch = branchService.requireActive(branchId);
		return stockRepository.findByBranch(branch).stream()
				.map(stockMapper::toBalance)
				.toList();
	}

	/** Historial de una variante en un local, del más reciente al más viejo. */
	@Transactional(readOnly = true)
	public List<StockMovementResponse> listMovements(Long variantId, Long branchId) {
		ProductVariantEntity variant = productService.requireActiveVariant(variantId);
		BranchEntity branch = branchService.requireActive(branchId);
		return stockMovementRepository.findByVariantAndBranchOrderByMovementAtDesc(variant, branch).stream()
				.map(stockMapper::toMovement)
				.toList();
	}

	/**
	 * Recuento: el usuario dice cuántas hay. El movimiento guarda la diferencia.
	 * Sin fila previa es AJUSTE_INICIAL; si ya había saldo, AJUSTE_RECUENTO.
	 */
	@Transactional
	public StockBalanceResponse recount(Long variantId, Long branchId, int countedQuantity, String description) {
		if (countedQuantity < 0) {
			throw new BusinessRuleException("El recuento no puede ser negativo");
		}
		Context context = requireStockable(variantId, branchId);
		var existing = stockRepository.findByVariantAndBranch(context.variant(), context.branch());
		int current = existing.map(StockEntity::getQuantity).orElse(0);
		StockMovementType type = existing.isPresent()
				? StockMovementType.AJUSTE_RECUENTO
				: StockMovementType.AJUSTE_INICIAL;
		return apply(context, type, countedQuantity - current, null, description).stock();
	}

	/** Mercadería que entra al local. quantity es cuántas unidades llegan. */
	@Transactional
	public StockBalanceResponse registerEntry(Long variantId, Long branchId, int quantity, String description) {
		return applySignedUnits(variantId, branchId, quantity, StockMovementType.ENTRADA, +1, description);
	}

	/** Unidades que se usan en el local y no se venden. */
	@Transactional
	public StockBalanceResponse registerInternalConsumption(
			Long variantId,
			Long branchId,
			int quantity,
			String description) {
		return applySignedUnits(variantId, branchId, quantity, StockMovementType.CONSUMO_INTERNO, -1, description);
	}

	/** Unidades perdidas o que no aparecen. */
	@Transactional
	public StockBalanceResponse registerLoss(Long variantId, Long branchId, int quantity, String description) {
		return applySignedUnits(variantId, branchId, quantity, StockMovementType.EXTRAVÍO, -1, description);
	}

	/** Mueve unidades de un local a otro. Origina dos movimientos TRASLADO vinculados. */
	@Transactional
	public StockTransferResponse transfer(
			Long variantId,
			Long fromBranchId,
			Long toBranchId,
			int quantity,
			String description) {
		if (quantity <= 0) {
			throw new BusinessRuleException("La cantidad tiene que ser mayor a cero");
		}
		if (fromBranchId.equals(toBranchId)) {
			throw new BusinessRuleException("El origen y el destino no pueden ser la misma sucursal");
		}
		Context from = requireStockable(variantId, fromBranchId);
		Context to = requireStockable(variantId, toBranchId);
		Applied outbound = apply(from, StockMovementType.TRASLADO, -quantity, null, description);
		Applied inbound = apply(to, StockMovementType.TRASLADO, quantity, outbound.movement(), description);
		outbound.movement().setRelatedMovement(inbound.movement());
		stockMovementRepository.save(outbound.movement());
		return new StockTransferResponse(
				stockMapper.toMovement(outbound.movement()),
				stockMapper.toMovement(inbound.movement()),
				outbound.stock(),
				inbound.stock());
	}

	/** Entrada o salida por unidades positivas del usuario; sign decide el signo del movimiento. */
	private StockBalanceResponse applySignedUnits(
			Long variantId,
			Long branchId,
			int quantity,
			StockMovementType type,
			int sign,
			String description) {
		if (quantity <= 0) {
			throw new BusinessRuleException("La cantidad tiene que ser mayor a cero");
		}
		return apply(requireStockable(variantId, branchId), type, sign * quantity, null, description).stock();
	}

	/**
	 * Persiste el movimiento y después crea o actualiza el saldo.
	 * No bloquea si el resultado queda negativo.
	 */
	private Applied apply(
			Context context,
			StockMovementType type,
			int signedQuantity,
			StockMovementEntity relatedMovement,
			String description) {
		StockMovementEntity movement = stockMovementRepository.save(StockMovementEntity.of(
				context.variant(),
				context.branch(),
				type,
				signedQuantity,
				relatedMovement,
				blankToNull(description),
				null));
		StockEntity stock = stockRepository.findByVariantAndBranch(context.variant(), context.branch())
				.orElseGet(() -> StockEntity.of(context.variant(), context.branch(), 0, null));
		stock.setQuantity(stock.getQuantity() + signedQuantity);
		StockEntity saved = stockRepository.save(stock);
		return new Applied(movement, stockMapper.toBalance(saved));
	}

	/** Variante activa, sucursal activa y producto que lleva inventario. */
	private Context requireStockable(Long variantId, Long branchId) {
		ProductVariantEntity variant = productService.requireActiveVariant(variantId);
		BranchEntity branch = branchService.requireActive(branchId);
		if (!variant.getProduct().isTracksStock()) {
			throw new BusinessRuleException("Ese producto no lleva inventario");
		}
		return new Context(variant, branch);
	}

	/** Cadena vacía o solo espacios → null. */
	private static String blankToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	/** Variante y sucursal ya validadas para un movimiento. */
	private record Context(ProductVariantEntity variant, BranchEntity branch) {
	}

	/** Movimiento persistido y saldo resultante de aplicarlo. */
	private record Applied(StockMovementEntity movement, StockBalanceResponse stock) {
	}
}
