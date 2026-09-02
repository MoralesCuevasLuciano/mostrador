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

@RestController
@RequestMapping("/api/brands")
@RequiredArgsConstructor
public class BrandController {

	private final BrandService brandService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public BrandResponse create(@Valid @RequestBody BrandRequest request) {
		return brandService.create(request);
	}

	@GetMapping
	public List<BrandResponse> findAll() {
		return brandService.findAll();
	}

	@GetMapping("/{id}")
	public BrandResponse findById(@PathVariable Long id) {
		return brandService.findById(id);
	}

	@PutMapping("/{id}")
	public BrandResponse update(@PathVariable Long id, @Valid @RequestBody BrandRequest request) {
		return brandService.update(id, request);
	}

	@DeleteMapping("/{id}")
	public BrandResponse deactivate(@PathVariable Long id) {
		return brandService.deactivate(id);
	}

	@PostMapping("/{id}/activate")
	public BrandResponse reactivate(@PathVariable Long id) {
		return brandService.reactivate(id);
	}
}
