package com.pepology.mostrador.controllers;

import com.pepology.mostrador.dto.stock.StockBalanceResponse;
import com.pepology.mostrador.dto.stock.StockMovementResponse;
import com.pepology.mostrador.dto.stock.StockQuantityRequest;
import com.pepology.mostrador.dto.stock.StockRecountRequest;
import com.pepology.mostrador.dto.stock.StockTransferRequest;
import com.pepology.mostrador.dto.stock.StockTransferResponse;
import com.pepology.mostrador.services.StockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * HTTP de inventario: /api/stock. Delega las reglas al StockService.
 */
@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
public class StockController {

	private final StockService stockService;

	/** POST /api/stock/transfers — mueve unidades de un local a otro. */
	@PostMapping("/transfers")
	@ResponseStatus(HttpStatus.CREATED)
	public StockTransferResponse transfer(@Valid @RequestBody StockTransferRequest request) {
		return stockService.transfer(
				request.variantId(),
				request.fromBranchId(),
				request.toBranchId(),
				request.quantity(),
				request.description());
	}

	/** GET /api/stock/{branchId} — variantes ya contadas en esa sucursal. */
	@GetMapping("/{branchId}")
	public List<StockBalanceResponse> listByBranch(@PathVariable Long branchId) {
		return stockService.listByBranch(branchId);
	}

	/** GET /api/stock/{branchId}/{variantId} — saldo; sin fila no es cero. */
	@GetMapping("/{branchId}/{variantId}")
	public StockBalanceResponse getBalance(@PathVariable Long branchId, @PathVariable Long variantId) {
		return stockService.getBalance(variantId, branchId);
	}

	/** GET /api/stock/{branchId}/{variantId}/movements — historial de esa variante. */
	@GetMapping("/{branchId}/{variantId}/movements")
	public List<StockMovementResponse> listMovements(
			@PathVariable Long branchId,
			@PathVariable Long variantId) {
		return stockService.listMovements(variantId, branchId);
	}

	/** POST /api/stock/{branchId}/{variantId}/recount — recuento (inicial o posterior). */
	@PostMapping("/{branchId}/{variantId}/recount")
	@ResponseStatus(HttpStatus.CREATED)
	public StockBalanceResponse recount(
			@PathVariable Long branchId,
			@PathVariable Long variantId,
			@Valid @RequestBody StockRecountRequest request) {
		return stockService.recount(variantId, branchId, request.countedQuantity(), request.description());
	}

	/** POST /api/stock/{branchId}/{variantId}/entries — mercadería que entra. */
	@PostMapping("/{branchId}/{variantId}/entries")
	@ResponseStatus(HttpStatus.CREATED)
	public StockBalanceResponse registerEntry(
			@PathVariable Long branchId,
			@PathVariable Long variantId,
			@Valid @RequestBody StockQuantityRequest request) {
		return stockService.registerEntry(variantId, branchId, request.quantity(), request.description());
	}

	/** POST /api/stock/{branchId}/{variantId}/internal-consumption — uso interno. */
	@PostMapping("/{branchId}/{variantId}/internal-consumption")
	@ResponseStatus(HttpStatus.CREATED)
	public StockBalanceResponse registerInternalConsumption(
			@PathVariable Long branchId,
			@PathVariable Long variantId,
			@Valid @RequestBody StockQuantityRequest request) {
		return stockService.registerInternalConsumption(
				variantId, branchId, request.quantity(), request.description());
	}

	/** POST /api/stock/{branchId}/{variantId}/losses — extravío. */
	@PostMapping("/{branchId}/{variantId}/losses")
	@ResponseStatus(HttpStatus.CREATED)
	public StockBalanceResponse registerLoss(
			@PathVariable Long branchId,
			@PathVariable Long variantId,
			@Valid @RequestBody StockQuantityRequest request) {
		return stockService.registerLoss(variantId, branchId, request.quantity(), request.description());
	}
}
