package com.pepology.mostrador.repositories;

import com.pepology.mostrador.models.entities.ProductEntity;
import com.pepology.mostrador.models.entities.ProductVariantEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Acceso a product_variant. El barcode no es único: varios pueden compartir código.
 */
public interface ProductVariantRepository extends JpaRepository<ProductVariantEntity, Long> {

	Optional<ProductVariantEntity> findBySku(String sku);

	List<ProductVariantEntity> findByBarcode(String barcode);

	List<ProductVariantEntity> findByProduct(ProductEntity product);

	boolean existsBySku(String sku);

	/** True si el producto todavía tiene variantes activas (bloquea la baja). */
	boolean existsByProductAndActiveTrue(ProductEntity product);
}
