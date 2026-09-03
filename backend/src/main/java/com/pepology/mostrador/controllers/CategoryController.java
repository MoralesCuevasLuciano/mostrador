package com.pepology.mostrador.controllers;

import com.pepology.mostrador.dto.category.CategoryRequest;
import com.pepology.mostrador.dto.category.CategoryResponse;
import com.pepology.mostrador.services.CategoryService;
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
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

	private final CategoryService categoryService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public CategoryResponse create(@Valid @RequestBody CategoryRequest request) {
		return categoryService.create(request);
	}

	@GetMapping
	public List<CategoryResponse> findAll() {
		return categoryService.findAll();
	}

	@GetMapping("/{id}")
	public CategoryResponse findById(@PathVariable Long id) {
		return categoryService.findById(id);
	}

	@PutMapping("/{id}")
	public CategoryResponse update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
		return categoryService.update(id, request);
	}

	@DeleteMapping("/{id}")
	public CategoryResponse deactivate(@PathVariable Long id) {
		return categoryService.deactivate(id);
	}

	@PostMapping("/{id}/activate")
	public CategoryResponse reactivate(@PathVariable Long id) {
		return categoryService.reactivate(id);
	}
}
