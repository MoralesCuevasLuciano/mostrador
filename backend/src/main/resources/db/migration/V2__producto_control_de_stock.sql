-- Distingue los productos que llevan inventario de los que no.
--
-- Sin esta columna, "no tiene fila en stock" significaría dos cosas opuestas:
-- que todavía no se inventarió, donde la venta debe descontar y dejar el saldo
-- en negativo como señal de recuento, o que no se lleva stock a propósito
-- —caramelos sueltos, fotocopias—, donde no hay que generar ningún movimiento.

ALTER TABLE product
    ADD COLUMN tracks_stock BOOLEAN NOT NULL DEFAULT TRUE AFTER allows_employee_discount;
