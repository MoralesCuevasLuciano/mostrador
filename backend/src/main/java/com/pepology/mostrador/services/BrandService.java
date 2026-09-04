package com.pepology.mostrador.services;

import com.pepology.mostrador.dto.brand.BrandRequest;
import com.pepology.mostrador.dto.brand.BrandResponse;
import com.pepology.mostrador.exceptions.BusinessRuleException;
import com.pepology.mostrador.exceptions.NotFoundException;
import com.pepology.mostrador.mappers.BrandMapper;
import com.pepology.mostrador.models.entities.BrandEntity;
import com.pepology.mostrador.repositories.BrandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Reglas de negocio de marcas: unicidad del nombre, baja lógica y reactivar.
 */
@Service
@RequiredArgsConstructor
public class BrandService {

	private final BrandRepository brandRepository;
	private final BrandMapper brandMapper;

	/** Alta. Normaliza espacios y rechaza si el nombre ya existe (sin importar mayúsculas). */
	@Transactional
	public BrandResponse create(BrandRequest request) {
		String name = normalize(request.name());
		if (brandRepository.existsByNameIgnoreCase(name)) {
			throw new BusinessRuleException("Ya existe una marca con el nombre \"" + name + "\"");
		}
		BrandEntity saved = brandRepository.save(brandMapper.toEntity(new BrandRequest(name)));
		return brandMapper.toResponse(saved);
	}

	/** Listado ordenado por nombre, incluyendo las dadas de baja. */
	@Transactional(readOnly = true)
	public List<BrandResponse> findAll() {
		return brandRepository.findAll(Sort.by("name")).stream()
				.map(brandMapper::toResponse)
				.toList();
	}

	/** Una marca por id. 404 si no existe. */
	@Transactional(readOnly = true)
	public BrandResponse findById(Long id) {
		return brandMapper.toResponse(requireById(id));
	}

	/** Cambia el nombre. No puede pisar el de otra marca. */
	@Transactional
	public BrandResponse update(Long id, BrandRequest request) {
		BrandEntity brand = requireById(id);
		String name = normalize(request.name());
		if (brandRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
			throw new BusinessRuleException("Ya existe una marca con el nombre \"" + name + "\"");
		}
		brand.setName(name);
		return brandMapper.toResponse(brand);
	}

	/** Baja lógica (is_active = false). No borra la fila. */
	@Transactional
	public BrandResponse deactivate(Long id) {
		BrandEntity brand = requireById(id);
		if (!brand.isActive()) {
			throw new BusinessRuleException("La marca ya está dada de baja");
		}
		brand.setActive(false);
		return brandMapper.toResponse(brand);
	}

	/** Vuelve a activar una marca dada de baja. */
	@Transactional
	public BrandResponse reactivate(Long id) {
		BrandEntity brand = requireById(id);
		if (brand.isActive()) {
			throw new BusinessRuleException("La marca ya está activa");
		}
		brand.setActive(true);
		return brandMapper.toResponse(brand);
	}

	/** Busca por id o lanza 404. */
	private BrandEntity requireById(Long id) {
		return brandRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("No existe la marca " + id));
	}

	/** Para colgar una variante: la marca tiene que existir y estar activa. */
	@Transactional(readOnly = true)
	public BrandEntity requireActive(Long id) {
		BrandEntity brand = requireById(id);
		if (!brand.isActive()) {
			throw new BusinessRuleException("No se puede usar una marca dada de baja");
		}
		return brand;
	}

	/** Recorta extremos y colapsa espacios internos. No cambia mayúsculas. */
	private static String normalize(String name) {
		return name.trim().replaceAll("\\s+", " ");
	}
}
