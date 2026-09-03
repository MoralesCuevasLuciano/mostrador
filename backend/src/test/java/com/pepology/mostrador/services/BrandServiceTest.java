package com.pepology.mostrador.services;

import com.pepology.mostrador.dto.brand.BrandRequest;
import com.pepology.mostrador.exceptions.BusinessRuleException;
import com.pepology.mostrador.exceptions.NotFoundException;
import com.pepology.mostrador.mappers.BrandMapper;
import com.pepology.mostrador.models.entities.BrandEntity;
import com.pepology.mostrador.repositories.BrandRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BrandServiceTest {

	@Mock
	private BrandRepository brandRepository;

	private BrandService brandService;

	@BeforeEach
	void setUp() {
		brandService = new BrandService(brandRepository, new BrandMapper());
	}

	@Test
	void createCollapsesSpacesAndKeepsOriginalCasing() {
		when(brandRepository.existsByNameIgnoreCase("BIC")).thenReturn(false);
		when(brandRepository.save(any(BrandEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		brandService.create(new BrandRequest("  BIC  "));

		ArgumentCaptor<BrandEntity> captor = ArgumentCaptor.forClass(BrandEntity.class);
		verify(brandRepository).save(captor.capture());
		assertEquals("BIC", captor.getValue().getName());
	}

	@Test
	void createRejectsDuplicateIgnoringCase() {
		when(brandRepository.existsByNameIgnoreCase("faber")).thenReturn(true);

		assertThrows(BusinessRuleException.class,
				() -> brandService.create(new BrandRequest("faber")));
	}

	@Test
	void findByIdThrowsWhenMissing() {
		when(brandRepository.findById(99L)).thenReturn(Optional.empty());

		assertThrows(NotFoundException.class, () -> brandService.findById(99L));
	}

	@Test
	void updateRenamesAndCollapsesSpaces() {
		BrandEntity brand = BrandEntity.of("Gloria");
		when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));
		when(brandRepository.existsByNameIgnoreCaseAndIdNot("Rivadavia", 1L)).thenReturn(false);

		var response = brandService.update(1L, new BrandRequest("  Rivadavia  "));

		assertEquals("Rivadavia", brand.getName());
		assertEquals("Rivadavia", response.name());
	}

	@Test
	void updateRejectsNameTakenByAnother() {
		when(brandRepository.findById(1L)).thenReturn(Optional.of(BrandEntity.of("Gloria")));
		when(brandRepository.existsByNameIgnoreCaseAndIdNot("BIC", 1L)).thenReturn(true);

		assertThrows(BusinessRuleException.class,
				() -> brandService.update(1L, new BrandRequest("BIC")));
	}

	@Test
	void deactivateSetsInactive() {
		BrandEntity brand = BrandEntity.of("Gloria");
		when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));

		var response = brandService.deactivate(1L);

		assertEquals(false, brand.isActive());
		assertEquals(false, response.active());
	}

	@Test
	void deactivateRejectsIfAlreadyInactive() {
		BrandEntity brand = BrandEntity.of("Gloria");
		brand.setActive(false);
		when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));

		assertThrows(BusinessRuleException.class, () -> brandService.deactivate(1L));
	}

	@Test
	void reactivateSetsActive() {
		BrandEntity brand = BrandEntity.of("Gloria");
		brand.setActive(false);
		when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));

		var response = brandService.reactivate(1L);

		assertEquals(true, brand.isActive());
		assertEquals(true, response.active());
	}

	@Test
	void reactivateRejectsIfAlreadyActive() {
		when(brandRepository.findById(1L)).thenReturn(Optional.of(BrandEntity.of("Gloria")));

		assertThrows(BusinessRuleException.class, () -> brandService.reactivate(1L));
	}

	@Test
	void requireActiveRejectsInactive() {
		BrandEntity brand = BrandEntity.of("BIC");
		brand.setActive(false);
		when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));

		assertThrows(BusinessRuleException.class, () -> brandService.requireActive(1L));
	}
}
