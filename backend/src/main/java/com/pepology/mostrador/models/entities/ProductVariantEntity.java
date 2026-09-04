package com.pepology.mostrador.models.entities;

import com.pepology.mostrador.models.enums.ItemCondition;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Unidad vendible: SKU, precio, foto, código de barras y estado (nueva/defectuosa).
 */
@Entity
@Table(name = "product_variant")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductVariantEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Setter(AccessLevel.NONE)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id", nullable = false)
	private ProductEntity product;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "brand_id")
	private BrandEntity brand;

	@Column(nullable = false, unique = true, length = 50)
	private String sku;

	@Column(nullable = false, length = 100)
	private String label;

	@Column(length = 50)
	private String barcode;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal price;

	@Enumerated(EnumType.STRING)
	@Column(name = "item_condition", nullable = false, length = 20)
	private ItemCondition itemCondition = ItemCondition.NUEVA;

	@Column(name = "image_url", length = 500)
	private String imageUrl;

	@Column(name = "is_active", nullable = false)
	private boolean active = true;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	@Setter(AccessLevel.NONE)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
	@Setter(AccessLevel.NONE)
	private LocalDateTime updatedAt;

	/** Fábrica de alta. El SKU lo genera el servicio, no el cliente. */
	public static ProductVariantEntity of(
			ProductEntity product,
			BrandEntity brand,
			String sku,
			String label,
			String barcode,
			BigDecimal price,
			ItemCondition itemCondition,
			String imageUrl) {
		ProductVariantEntity variant = new ProductVariantEntity();
		variant.setProduct(product);
		variant.setBrand(brand);
		variant.setSku(sku);
		variant.setLabel(label);
		variant.setBarcode(barcode);
		variant.setPrice(price);
		if (itemCondition != null) {
			variant.setItemCondition(itemCondition);
		}
		variant.setImageUrl(imageUrl);
		return variant;
	}
}
