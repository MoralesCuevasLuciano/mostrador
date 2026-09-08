package com.pepology.mostrador.controllers;

import com.pepology.mostrador.dto.cash.CashAmountRequest;
import com.pepology.mostrador.dto.cash.CashCloseRequest;
import com.pepology.mostrador.dto.cash.CashCountRequest;
import com.pepology.mostrador.dto.cash.CashMovementResponse;
import com.pepology.mostrador.dto.cash.CashMovementUpdateRequest;
import com.pepology.mostrador.dto.cash.CashSessionResponse;
import com.pepology.mostrador.services.CashService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * HTTP de caja: /api/cash. Delega las reglas al CashService.
 */
@RestController
@RequestMapping("/api/cash")
@RequiredArgsConstructor
public class CashController {

	private final CashService cashService;

	/** GET /api/cash/{branchId}/today — planilla de hoy; la abre si no existe. */
	@GetMapping("/{branchId}/today")
	public CashSessionResponse current(@PathVariable Long branchId) {
		return cashService.current(branchId);
	}

	/** GET /api/cash/{branchId}/sessions — historial de planillas de ese local. */
	@GetMapping("/{branchId}/sessions")
	public List<CashSessionResponse> listByBranch(@PathVariable Long branchId) {
		return cashService.listByBranch(branchId);
	}

	/** GET /api/cash/{branchId}/sessions/{date} — planilla de ese día. 404 si no se abrió. */
	@GetMapping("/{branchId}/sessions/{date}")
	public CashSessionResponse get(
			@PathVariable Long branchId,
			@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
		return cashService.get(branchId, date);
	}

	/** GET /api/cash/{branchId}/sessions/{date}/movements — historial de esa planilla. */
	@GetMapping("/{branchId}/sessions/{date}/movements")
	public List<CashMovementResponse> listMovements(
			@PathVariable Long branchId,
			@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
		return cashService.listMovements(branchId, date);
	}

	/** PUT /api/cash/{branchId}/movements/{movementId} — corrige un movimiento de una caja abierta. */
	@PutMapping("/{branchId}/movements/{movementId}")
	public CashMovementResponse updateMovement(
			@PathVariable Long branchId,
			@PathVariable Long movementId,
			@Valid @RequestBody CashMovementUpdateRequest request) {
		return cashService.updateMovement(
				branchId,
				movementId,
				request.movementType(),
				request.amount(),
				request.description());
	}

	/** DELETE /api/cash/{branchId}/movements/{movementId} — borra un movimiento de una caja abierta. */
	@DeleteMapping("/{branchId}/movements/{movementId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteMovement(@PathVariable Long branchId, @PathVariable Long movementId) {
		cashService.deleteMovement(branchId, movementId);
	}

	/** POST /api/cash/{branchId}/sessions/{date}/opening-count — recuento de apertura. */
	@PostMapping("/{branchId}/sessions/{date}/opening-count")
	public CashSessionResponse countOpening(
			@PathVariable Long branchId,
			@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			@Valid @RequestBody CashCountRequest request) {
		return cashService.countOpening(branchId, date, request.openingAmount());
	}

	/** POST /api/cash/{branchId}/sessions/{date}/withdrawals — retiro por resguardo. */
	@PostMapping("/{branchId}/sessions/{date}/withdrawals")
	@ResponseStatus(HttpStatus.CREATED)
	public CashMovementResponse registerWithdrawal(
			@PathVariable Long branchId,
			@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			@Valid @RequestBody CashAmountRequest request) {
		return cashService.registerWithdrawal(branchId, date, request.amount(), request.description());
	}

	/** POST /api/cash/{branchId}/sessions/{date}/vales — vale a empleado. */
	@PostMapping("/{branchId}/sessions/{date}/vales")
	@ResponseStatus(HttpStatus.CREATED)
	public CashMovementResponse registerVale(
			@PathVariable Long branchId,
			@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			@Valid @RequestBody CashAmountRequest request) {
		return cashService.registerVale(branchId, date, request.amount(), request.description());
	}

	/** POST /api/cash/{branchId}/sessions/{date}/expenses — gasto o pago a proveedor. */
	@PostMapping("/{branchId}/sessions/{date}/expenses")
	@ResponseStatus(HttpStatus.CREATED)
	public CashMovementResponse registerExpense(
			@PathVariable Long branchId,
			@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			@Valid @RequestBody CashAmountRequest request) {
		return cashService.registerExpense(branchId, date, request.amount(), request.description());
	}

	/** POST /api/cash/{branchId}/sessions/{date}/cash-ins — ingreso de efectivo. */
	@PostMapping("/{branchId}/sessions/{date}/cash-ins")
	@ResponseStatus(HttpStatus.CREATED)
	public CashMovementResponse registerCashIn(
			@PathVariable Long branchId,
			@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			@Valid @RequestBody CashAmountRequest request) {
		return cashService.registerCashIn(branchId, date, request.amount(), request.description());
	}

	/** POST /api/cash/{branchId}/sessions/{date}/close — cierra la caja del día. */
	@PostMapping("/{branchId}/sessions/{date}/close")
	public CashSessionResponse close(
			@PathVariable Long branchId,
			@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			@Valid @RequestBody CashCloseRequest request) {
		return cashService.close(branchId, date, request.closingAmount(), request.note());
	}
}
