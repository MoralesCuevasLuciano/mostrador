package com.pepology.mostrador.services;

import com.pepology.mostrador.dto.branch.BranchRequest;
import com.pepology.mostrador.dto.branch.BranchResponse;
import com.pepology.mostrador.exceptions.BusinessRuleException;
import com.pepology.mostrador.exceptions.NotFoundException;
import com.pepology.mostrador.mappers.BranchMapper;
import com.pepology.mostrador.models.entities.BranchEntity;
import com.pepology.mostrador.repositories.BranchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Sucursales: nombre único, punto de venta ARCA único si está cargado, baja lógica.
 */
@Service
@RequiredArgsConstructor
public class BranchService {

	private final BranchRepository branchRepository;
	private final BranchMapper branchMapper;

	/** Alta. Normaliza el nombre y rechaza duplicados de nombre o de punto de venta. */
	@Transactional
	public BranchResponse create(BranchRequest request) {
		BranchRequest normalized = normalize(request);
		assertNameAvailable(normalized.name(), null);
		assertPointOfSaleAvailable(normalized.pointOfSale(), null);
		BranchEntity saved = branchRepository.save(branchMapper.toEntity(normalized));
		return branchMapper.toResponse(saved);
	}

	/** Listado ordenado por nombre, incluyendo las dadas de baja. */
	@Transactional(readOnly = true)
	public List<BranchResponse> findAll() {
		return branchRepository.findAll(Sort.by("name")).stream()
				.map(branchMapper::toResponse)
				.toList();
	}

	/** Una sucursal por id. 404 si no existe. */
	@Transactional(readOnly = true)
	public BranchResponse findById(Long id) {
		return branchMapper.toResponse(requireById(id));
	}

	/** Edita nombre, dirección, teléfono y punto de venta. */
	@Transactional
	public BranchResponse update(Long id, BranchRequest request) {
		BranchEntity branch = requireById(id);
		BranchRequest normalized = normalize(request);
		assertNameAvailable(normalized.name(), id);
		assertPointOfSaleAvailable(normalized.pointOfSale(), id);
		branch.setName(normalized.name());
		branch.setAddress(normalized.address());
		branch.setPhone(normalized.phone());
		branch.setPointOfSale(normalized.pointOfSale());
		return branchMapper.toResponse(branch);
	}

	/** Baja lógica (is_active = false). No borra la fila. */
	@Transactional
	public BranchResponse deactivate(Long id) {
		BranchEntity branch = requireById(id);
		if (!branch.isActive()) {
			throw new BusinessRuleException("La sucursal ya está dada de baja");
		}
		branch.setActive(false);
		return branchMapper.toResponse(branch);
	}

	/** Vuelve a activar una sucursal dada de baja. */
	@Transactional
	public BranchResponse reactivate(Long id) {
		BranchEntity branch = requireById(id);
		if (branch.isActive()) {
			throw new BusinessRuleException("La sucursal ya está activa");
		}
		branch.setActive(true);
		return branchMapper.toResponse(branch);
	}

	/** Busca por id o lanza 404. */
	private BranchEntity requireById(Long id) {
		return branchRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("No existe la sucursal " + id));
	}

	/** Para stock y caja: la sucursal tiene que existir y estar activa. */
	@Transactional(readOnly = true)
	public BranchEntity requireActive(Long id) {
		BranchEntity branch = requireById(id);
		if (!branch.isActive()) {
			throw new BusinessRuleException("No se puede usar una sucursal dada de baja");
		}
		return branch;
	}

	/** El nombre no puede repetirse, sin importar mayúsculas. excludeId se usa al editar. */
	private void assertNameAvailable(String name, Long excludeId) {
		boolean taken = excludeId == null
				? branchRepository.existsByNameIgnoreCase(name)
				: branchRepository.existsByNameIgnoreCaseAndIdNot(name, excludeId);
		if (taken) {
			throw new BusinessRuleException("Ya existe una sucursal con el nombre \"" + name + "\"");
		}
	}

	/** Dos locales no pueden compartir el mismo punto de venta ARCA. */
	private void assertPointOfSaleAvailable(Integer pointOfSale, Long excludeId) {
		if (pointOfSale == null) {
			return;
		}
		boolean taken = excludeId == null
				? branchRepository.existsByPointOfSale(pointOfSale)
				: branchRepository.existsByPointOfSaleAndIdNot(pointOfSale, excludeId);
		if (taken) {
			throw new BusinessRuleException("Ya hay una sucursal con el punto de venta ARCA " + pointOfSale);
		}
	}

	/** Recorta nombre; dirección y teléfono vacíos pasan a null. */
	private static BranchRequest normalize(BranchRequest request) {
		return new BranchRequest(
				request.name().trim().replaceAll("\\s+", " "),
				blankToNull(request.address()),
				blankToNull(request.phone()),
				request.pointOfSale());
	}

	/** Cadena vacía o solo espacios → null. */
	private static String blankToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
