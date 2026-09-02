-- Catálogo: sucursales, categorías, marcas, productos y variantes.

CREATE TABLE branch (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    address       VARCHAR(200),
    phone         VARCHAR(50),
    point_of_sale INT,
    is_active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE category (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    parent_id  BIGINT,
    is_active  BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) REFERENCES category (id),
    -- MySQL no aplica UNIQUE cuando parent_id es NULL, así que los nombres
    -- duplicados entre rubros de primer nivel se validan en la aplicación.
    CONSTRAINT uq_category_parent_name UNIQUE (parent_id, name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE brand (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    is_active  BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_brand_name UNIQUE (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE product (
    id                       BIGINT AUTO_INCREMENT PRIMARY KEY,
    name                     VARCHAR(200) NOT NULL,
    description              TEXT,
    category_id              BIGINT,
    vat_rate                 DECIMAL(5, 2) NOT NULL DEFAULT 21.00,
    allows_employee_discount BOOLEAN NOT NULL DEFAULT TRUE,
    is_active                BOOLEAN NOT NULL DEFAULT TRUE,
    created_at               TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES category (id),
    INDEX idx_product_name (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE product_variant (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id     BIGINT NOT NULL,
    brand_id       BIGINT,
    sku            VARCHAR(50) NOT NULL,
    label          VARCHAR(100) NOT NULL,
    barcode        VARCHAR(50),
    price          DECIMAL(12, 2) NOT NULL,
    item_condition VARCHAR(20) NOT NULL DEFAULT 'NUEVA',
    image_url      VARCHAR(500),
    is_active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_variant_product FOREIGN KEY (product_id) REFERENCES product (id),
    CONSTRAINT fk_variant_brand FOREIGN KEY (brand_id) REFERENCES brand (id),
    CONSTRAINT uq_variant_sku UNIQUE (sku),
    -- El código de barras lleva índice para que el escaneo sea rápido, pero no
    -- unicidad: entra mercadería con códigos repetidos o apócrifos y un índice
    -- único bloquearía la carga.
    INDEX idx_variant_barcode (barcode)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
