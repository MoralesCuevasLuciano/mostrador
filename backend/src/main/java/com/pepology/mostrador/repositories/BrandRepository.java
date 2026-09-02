package com.pepology.mostrador.repositories;

import com.pepology.mostrador.models.entities.BrandEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BrandRepository extends JpaRepository<BrandEntity, Long> {

	Optional<BrandEntity> findByName(String name);
}
