package com.pepology.mostrador.controllers;

import com.pepology.mostrador.dto.branch.BranchRequest;
import com.pepology.mostrador.dto.branch.BranchResponse;
import com.pepology.mostrador.services.BranchService;
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
 * HTTP de sucursales: /api/branches. Delega las reglas al BranchService.
 */
@RestController
@RequestMapping("/api/branches")
@RequiredArgsConstructor
public class BranchController {

	private final BranchService branchService;

	/** POST /api/branches — alta. */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public BranchResponse create(@Valid @RequestBody BranchRequest request) {
		return branchService.create(request);
	}

	/** GET /api/branches — listado completo. */
	@GetMapping
	public List<BranchResponse> findAll() {
		return branchService.findAll();
	}

	/** GET /api/branches/{id} — una sucursal. */
	@GetMapping("/{id}")
	public BranchResponse findById(@PathVariable Long id) {
		return branchService.findById(id);
	}

	/** PUT /api/branches/{id} — edita la ficha. */
	@PutMapping("/{id}")
	public BranchResponse update(@PathVariable Long id, @Valid @RequestBody BranchRequest request) {
		return branchService.update(id, request);
	}

	/** DELETE /api/branches/{id} — baja lógica. */
	@DeleteMapping("/{id}")
	public BranchResponse deactivate(@PathVariable Long id) {
		return branchService.deactivate(id);
	}

	/** POST /api/branches/{id}/activate — reactivar. */
	@PostMapping("/{id}/activate")
	public BranchResponse reactivate(@PathVariable Long id) {
		return branchService.reactivate(id);
	}
}
