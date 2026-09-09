package com.pepology.mostrador.controllers;

import com.pepology.mostrador.dto.sale.SaleRequest;
import com.pepology.mostrador.dto.sale.SaleResponse;
import com.pepology.mostrador.services.SaleService;
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
 * HTTP de ventas: /api/sales. Delega las reglas al SaleService.
 */
@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
public class SaleController {

	private final SaleService saleService;

	/** POST /api/sales/{branchId} — cobra un ticket en la caja de hoy. */
	@PostMapping("/{branchId}")
	@ResponseStatus(HttpStatus.CREATED)
	public SaleResponse create(@PathVariable Long branchId, @Valid @RequestBody SaleRequest request) {
		return saleService.create(branchId, request);
	}

	/** GET /api/sales/{branchId}/today — tickets de la planilla de hoy. */
	@GetMapping("/{branchId}/today")
	public List<SaleResponse> listToday(@PathVariable Long branchId) {
		return saleService.listToday(branchId);
	}
}
