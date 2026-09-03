package com.pepology.mostrador.controllers;

import com.pepology.mostrador.dto.product.ProductRequest;
import com.pepology.mostrador.dto.product.ProductResponse;
import com.pepology.mostrador.dto.product.ProductUpdateRequest;
import com.pepology.mostrador.dto.product.VariantRequest;
import com.pepology.mostrador.services.ProductService;
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
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

	private final ProductService productService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ProductResponse create(@Valid @RequestBody ProductRequest request) {
		return productService.create(request);
	}

	@GetMapping
	public List<ProductResponse> findAll() {
		return productService.findAll();
	}

	@GetMapping("/{id}")
	public ProductResponse findById(@PathVariable Long id) {
		return productService.findById(id);
	}

	@PutMapping("/{id}")
	public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductUpdateRequest request) {
		return productService.update(id, request);
	}

	@DeleteMapping("/{id}")
	public ProductResponse deactivate(@PathVariable Long id) {
		return productService.deactivate(id);
	}

	@PostMapping("/{id}/activate")
	public ProductResponse reactivate(@PathVariable Long id) {
		return productService.reactivate(id);
	}

	@PostMapping("/{id}/variants/deactivate")
	public ProductResponse deactivateAllVariants(@PathVariable Long id) {
		return productService.deactivateAllVariants(id);
	}

	@PostMapping("/{id}/variants")
	@ResponseStatus(HttpStatus.CREATED)
	public ProductResponse addVariant(@PathVariable Long id, @Valid @RequestBody VariantRequest request) {
		return productService.addVariant(id, request);
	}

	@PutMapping("/{id}/variants/{variantId}")
	public ProductResponse updateVariant(
			@PathVariable Long id,
			@PathVariable Long variantId,
			@Valid @RequestBody VariantRequest request) {
		return productService.updateVariant(id, variantId, request);
	}

	@DeleteMapping("/{id}/variants/{variantId}")
	public ProductResponse deactivateVariant(@PathVariable Long id, @PathVariable Long variantId) {
		return productService.deactivateVariant(id, variantId);
	}

	@PostMapping("/{id}/variants/{variantId}/activate")
	public ProductResponse reactivateVariant(@PathVariable Long id, @PathVariable Long variantId) {
		return productService.reactivateVariant(id, variantId);
	}
}
