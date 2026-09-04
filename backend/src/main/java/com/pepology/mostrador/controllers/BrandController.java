package com.pepology.mostrador.controllers;

import com.pepology.mostrador.dto.brand.BrandRequest;
import com.pepology.mostrador.dto.brand.BrandResponse;
import com.pepology.mostrador.services.BrandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

import java.util.List;

/**
 * HTTP de marcas: /api/brands. Delega las reglas al BrandService.
 */
@RestController
@RequestMapping("/api/brands")
@RequiredArgsConstructor
public class BrandController {

	private final BrandService brandService;

	/** POST /api/brands — alta. */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public BrandResponse create(@Valid @RequestBody BrandRequest request) {
		return brandService.create(request);
	}

	/** GET /api/brands — listado completo. */
	@GetMapping
	public List<BrandResponse> findAll() {
		return brandService.findAll();
	}

	/** GET /api/brands/{id} — una marca. */
	@GetMapping("/{id}")
	public BrandResponse findById(@PathVariable Long id) {
		return brandService.findById(id);
	}

	/** PUT /api/brands/{id} — cambia el nombre. */
	@PutMapping("/{id}")
	public BrandResponse update(@PathVariable Long id, @Valid @RequestBody BrandRequest request) {
		return brandService.update(id, request);
	}

	/** DELETE /api/brands/{id} — baja lógica. */
	@DeleteMapping("/{id}")
	public BrandResponse deactivate(@PathVariable Long id) {
		return brandService.deactivate(id);
	}

	/** POST /api/brands/{id}/activate — reactivar. */
	@PostMapping("/{id}/activate")
	public BrandResponse reactivate(@PathVariable Long id) {
		return brandService.reactivate(id);
	}
}
