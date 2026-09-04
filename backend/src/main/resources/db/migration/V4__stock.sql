-- Inventario: el saldo por variante y sucursal, más el historial que lo explica.

CREATE TABLE stock (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_variant_id BIGINT NOT NULL,
    branch_id          BIGINT NOT NULL,
    -- Admite valores negativos a propósito: no se bloquean ventas por falta de
    -- stock, y un saldo en rojo es la señal de que esa variante hay que contarla.
    quantity           INT NOT NULL,
    min_quantity       INT,
    created_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_stock_variant FOREIGN KEY (product_variant_id) REFERENCES product_variant (id),
    CONSTRAINT fk_stock_branch FOREIGN KEY (branch_id) REFERENCES branch (id),
    -- Una sola fila por variante y sucursal. Es la restricción que sostiene la
    -- integridad del inventario: si se pierde, ningún saldo es confiable.
    CONSTRAINT uq_stock_variant_branch UNIQUE (product_variant_id, branch_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE stock_movement (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_variant_id  BIGINT NOT NULL,
    branch_id           BIGINT NOT NULL,
    movement_type       VARCHAR(30) NOT NULL,
    -- Con signo: negativo cuando sale mercadería, positivo cuando entra. Así el
    -- saldo de una variante es la suma de sus movimientos, sin condicionales.
    quantity            INT NOT NULL,
    -- Vincula las dos patas de un traslado: la salida de una sucursal con la
    -- entrada en la otra.
    related_movement_id BIGINT,
    description         VARCHAR(255),
    movement_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_movement_variant FOREIGN KEY (product_variant_id) REFERENCES product_variant (id),
    CONSTRAINT fk_movement_branch FOREIGN KEY (branch_id) REFERENCES branch (id),
    CONSTRAINT fk_movement_related FOREIGN KEY (related_movement_id) REFERENCES stock_movement (id),
    INDEX idx_movement_variant_branch (product_variant_id, branch_id),
    INDEX idx_movement_at (movement_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
