package com.pepology.mostrador.repositories;

import com.pepology.mostrador.models.entities.CategoryEntity;
import com.pepology.mostrador.models.entities.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<ProductEntity, Long> {

	List<ProductEntity> findByCategory(CategoryEntity category);
}
