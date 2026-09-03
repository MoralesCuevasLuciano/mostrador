package com.pepology.mostrador.repositories;

import com.pepology.mostrador.models.entities.ProductEntity;
import com.pepology.mostrador.models.entities.ProductVariantEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductVariantRepository extends JpaRepository<ProductVariantEntity, Long> {

	Optional<ProductVariantEntity> findBySku(String sku);

	List<ProductVariantEntity> findByBarcode(String barcode);

	List<ProductVariantEntity> findByProduct(ProductEntity product);

	boolean existsBySku(String sku);

	boolean existsByProductAndActiveTrue(ProductEntity product);
}
