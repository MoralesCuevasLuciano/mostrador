-- Primera versión de ventas: ticket, líneas y pagos. Sin factura, empleado ni promoción.

CREATE TABLE sale (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    branch_id        BIGINT NOT NULL,
    cash_session_id  BIGINT NOT NULL,
    sale_type        VARCHAR(20) NOT NULL,
    sold_at          TIMESTAMP NOT NULL,
    subtotal         DECIMAL(12, 2) NOT NULL,
    discount_amount  DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    total            DECIMAL(12, 2) NOT NULL,
    status           VARCHAR(20) NOT NULL,
    note             TEXT,
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_sale_branch FOREIGN KEY (branch_id) REFERENCES branch (id),
    CONSTRAINT fk_sale_session FOREIGN KEY (cash_session_id) REFERENCES cash_session (id),
    INDEX idx_sale_session (cash_session_id),
    INDEX idx_sale_sold_at (sold_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE sale_line (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    sale_id            BIGINT NOT NULL,
    product_variant_id BIGINT NOT NULL,
    quantity           INT NOT NULL,
    list_unit_price    DECIMAL(12, 2) NOT NULL,
    line_total         DECIMAL(12, 2) NOT NULL,
    vat_rate           DECIMAL(5, 2) NOT NULL,
    created_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_sale_line_sale FOREIGN KEY (sale_id) REFERENCES sale (id),
    CONSTRAINT fk_sale_line_variant FOREIGN KEY (product_variant_id) REFERENCES product_variant (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE sale_payment (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    sale_id          BIGINT NOT NULL,
    method           VARCHAR(30) NOT NULL,
    amount           DECIMAL(12, 2) NOT NULL,
    surcharge_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_sale_payment_sale FOREIGN KEY (sale_id) REFERENCES sale (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
