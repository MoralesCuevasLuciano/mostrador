package com.pepology.mostrador.services;

import com.pepology.mostrador.dto.category.CategoryRequest;
import com.pepology.mostrador.exceptions.BusinessRuleException;
import com.pepology.mostrador.exceptions.NotFoundException;
import com.pepology.mostrador.mappers.CategoryMapper;
import com.pepology.mostrador.models.entities.CategoryEntity;
import com.pepology.mostrador.repositories.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

	@Mock
	private CategoryRepository categoryRepository;

	private CategoryService categoryService;

	@BeforeEach
	void setUp() {
		categoryService = new CategoryService(categoryRepository, new CategoryMapper());
	}

	@Test
	void createCollapsesSpacesAndKeepsOriginalCasing() {
		when(categoryRepository.existsByParentIsNullAndNameIgnoreCase("Escolar")).thenReturn(false);
		when(categoryRepository.save(any(CategoryEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		categoryService.create(new CategoryRequest("  Escolar  ", null));

		ArgumentCaptor<CategoryEntity> captor = ArgumentCaptor.forClass(CategoryEntity.class);
		verify(categoryRepository).save(captor.capture());
		assertEquals("Escolar", captor.getValue().getName());
		assertNull(captor.getValue().getParent());
	}

	@Test
	void createRejectsDuplicateRootIgnoringCase() {
		when(categoryRepository.existsByParentIsNullAndNameIgnoreCase("escolar")).thenReturn(true);

		assertThrows(BusinessRuleException.class,
				() -> categoryService.create(new CategoryRequest("escolar", null)));
	}

	@Test
	void createAllowsSameNameUnderDifferentParent() {
		CategoryEntity escolar = CategoryEntity.of("Escolar", null);
		when(categoryRepository.findById(1L)).thenReturn(Optional.of(escolar));
		when(categoryRepository.existsByParentAndNameIgnoreCase(escolar, "Varios")).thenReturn(false);
		when(categoryRepository.save(any(CategoryEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		categoryService.create(new CategoryRequest("Varios", 1L));

		ArgumentCaptor<CategoryEntity> captor = ArgumentCaptor.forClass(CategoryEntity.class);
		verify(categoryRepository).save(captor.capture());
		assertEquals("Varios", captor.getValue().getName());
		assertEquals(escolar, captor.getValue().getParent());
	}

	@Test
	void createRejectsThirdLevel() {
		CategoryEntity escolar = CategoryEntity.of("Escolar", null);
		CategoryEntity cuadernos = CategoryEntity.of("Cuadernos", escolar);
		when(categoryRepository.findById(2L)).thenReturn(Optional.of(cuadernos));

		assertThrows(BusinessRuleException.class,
				() -> categoryService.create(new CategoryRequest("A4", 2L)));
	}

	@Test
	void findByIdThrowsWhenMissing() {
		when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

		assertThrows(NotFoundException.class, () -> categoryService.findById(99L));
	}

	@Test
	void updateRejectsSelfAsParent() {
		when(categoryRepository.findById(1L)).thenReturn(Optional.of(CategoryEntity.of("Escolar", null)));

		assertThrows(BusinessRuleException.class,
				() -> categoryService.update(1L, new CategoryRequest("Escolar", 1L)));
	}

	@Test
	void deactivateSetsInactive() {
		CategoryEntity category = CategoryEntity.of("Escolar", null);
		when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
		when(categoryRepository.existsByParentAndActiveTrue(category)).thenReturn(false);

		var response = categoryService.deactivate(1L);

		assertEquals(false, category.isActive());
		assertEquals(false, response.active());
	}

	@Test
	void deactivateRejectsIfHasActiveChildren() {
		CategoryEntity category = CategoryEntity.of("Escolar", null);
		when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
		when(categoryRepository.existsByParentAndActiveTrue(category)).thenReturn(true);

		assertThrows(BusinessRuleException.class, () -> categoryService.deactivate(1L));
	}

	@Test
	void reactivateSetsActive() {
		CategoryEntity category = CategoryEntity.of("Escolar", null);
		category.setActive(false);
		when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

		var response = categoryService.reactivate(1L);

		assertEquals(true, category.isActive());
		assertEquals(true, response.active());
	}

	@Test
	void updateCannotNestAParentWithChildren() {
		CategoryEntity escolar = CategoryEntity.of("Escolar", null);
		CategoryEntity bazar = CategoryEntity.of("Bazar", null);
		when(categoryRepository.findById(1L)).thenReturn(Optional.of(escolar));
		when(categoryRepository.findById(2L)).thenReturn(Optional.of(bazar));
		when(categoryRepository.findByParent(escolar)).thenReturn(List.of(CategoryEntity.of("Cuadernos", escolar)));

		assertThrows(BusinessRuleException.class,
				() -> categoryService.update(1L, new CategoryRequest("Escolar", 2L)));
	}

	@Test
	void requireActiveRejectsInactive() {
		CategoryEntity category = CategoryEntity.of("Escolar", null);
		category.setActive(false);
		when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

		assertThrows(BusinessRuleException.class, () -> categoryService.requireActive(1L));
	}
}
