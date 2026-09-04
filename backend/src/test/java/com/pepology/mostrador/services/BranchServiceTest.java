package com.pepology.mostrador.services;

import com.pepology.mostrador.dto.branch.BranchRequest;
import com.pepology.mostrador.exceptions.BusinessRuleException;
import com.pepology.mostrador.exceptions.NotFoundException;
import com.pepology.mostrador.mappers.BranchMapper;
import com.pepology.mostrador.models.entities.BranchEntity;
import com.pepology.mostrador.repositories.BranchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests de BranchService: unicidad de nombre y punto de venta, baja lógica.
 */
@ExtendWith(MockitoExtension.class)
class BranchServiceTest {

	@Mock
	private BranchRepository branchRepository;

	private BranchService branchService;

	@BeforeEach
	void setUp() {
		branchService = new BranchService(branchRepository, new BranchMapper());
	}

	@Test
	void createCollapsesSpacesAndClearsBlankAddress() {
		when(branchRepository.existsByNameIgnoreCase("Local centro")).thenReturn(false);
		when(branchRepository.save(any(BranchEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		branchService.create(new BranchRequest("  Local   centro  ", "   ", null, 1));

		ArgumentCaptor<BranchEntity> captor = ArgumentCaptor.forClass(BranchEntity.class);
		verify(branchRepository).save(captor.capture());
		assertEquals("Local centro", captor.getValue().getName());
		assertNull(captor.getValue().getAddress());
		assertEquals(1, captor.getValue().getPointOfSale());
	}

	@Test
	void createRejectsDuplicateNameIgnoringCase() {
		when(branchRepository.existsByNameIgnoreCase("centro")).thenReturn(true);

		assertThrows(BusinessRuleException.class,
				() -> branchService.create(new BranchRequest("centro", null, null, null)));
	}

	@Test
	void createRejectsDuplicatePointOfSale() {
		when(branchRepository.existsByNameIgnoreCase("Sucursal 2")).thenReturn(false);
		when(branchRepository.existsByPointOfSale(3)).thenReturn(true);

		assertThrows(BusinessRuleException.class,
				() -> branchService.create(new BranchRequest("Sucursal 2", null, null, 3)));
	}

	@Test
	void findByIdThrowsWhenMissing() {
		when(branchRepository.findById(99L)).thenReturn(Optional.empty());

		assertThrows(NotFoundException.class, () -> branchService.findById(99L));
	}

	@Test
	void updateRenames() {
		BranchEntity branch = BranchEntity.of("Viejo", null, null, 1);
		when(branchRepository.findById(1L)).thenReturn(Optional.of(branch));
		when(branchRepository.existsByNameIgnoreCaseAndIdNot("Nuevo", 1L)).thenReturn(false);

		var response = branchService.update(1L, new BranchRequest("  Nuevo  ", null, null, 1));

		assertEquals("Nuevo", branch.getName());
		assertEquals("Nuevo", response.name());
	}

	@Test
	void deactivateSetsInactive() {
		BranchEntity branch = BranchEntity.of("Centro", null, null, null);
		when(branchRepository.findById(1L)).thenReturn(Optional.of(branch));

		var response = branchService.deactivate(1L);

		assertEquals(false, branch.isActive());
		assertEquals(false, response.active());
	}

	@Test
	void deactivateRejectsIfAlreadyInactive() {
		BranchEntity branch = BranchEntity.of("Centro", null, null, null);
		branch.setActive(false);
		when(branchRepository.findById(1L)).thenReturn(Optional.of(branch));

		assertThrows(BusinessRuleException.class, () -> branchService.deactivate(1L));
	}

	@Test
	void requireActiveRejectsInactive() {
		BranchEntity branch = BranchEntity.of("Centro", null, null, null);
		branch.setActive(false);
		when(branchRepository.findById(1L)).thenReturn(Optional.of(branch));

		assertThrows(BusinessRuleException.class, () -> branchService.requireActive(1L));
	}
}
