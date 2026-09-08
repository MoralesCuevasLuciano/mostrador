-- Caja: la planilla del día por sucursal y las salidas/entradas que no son ventas.

CREATE TABLE cash_session (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    branch_id          BIGINT NOT NULL,
    business_date      DATE NOT NULL,
    -- Se completa sola con el cierre del día anterior cuando la sesión se abre
    -- automáticamente. El cajón no se vacía nunca.
    opening_amount     DECIMAL(12, 2) NOT NULL,
    -- Vacío = el monto de apertura es heredado y nadie lo verificó contando.
    opening_counted_at TIMESTAMP NULL,
    -- Congelados al cerrar. Son los únicos que vienen de otras tablas.
    total_cash_sales   DECIMAL(12, 2),
    total_cash_out     DECIMAL(12, 2),
    -- Vacío mientras la caja está abierta. Reemplaza a un campo de estado.
    closing_amount     DECIMAL(12, 2),
    closed_at          TIMESTAMP NULL,
    note               TEXT,
    created_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_session_branch FOREIGN KEY (branch_id) REFERENCES branch (id),
    -- Una sola planilla por sucursal y día.
    CONSTRAINT uq_session_branch_date UNIQUE (branch_id, business_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE cash_movement (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    cash_session_id BIGINT NOT NULL,
    movement_type   VARCHAR(30) NOT NULL,
    -- Con signo: negativo cuando sale plata, positivo cuando entra.
    -- Las ventas no se registran acá: el efectivo se va a sumar desde
    -- sale_payment cuando exista ese módulo.
    amount          DECIMAL(12, 2) NOT NULL,
    description     VARCHAR(255),
    movement_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_cash_movement_session FOREIGN KEY (cash_session_id) REFERENCES cash_session (id),
    INDEX idx_cash_movement_session (cash_session_id),
    INDEX idx_cash_movement_at (movement_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
