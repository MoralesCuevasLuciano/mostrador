-- Las dos sucursales del comercio. En cada PC nueva Flyway las crea al arrancar.

INSERT INTO branch (name, address, phone, point_of_sale) VALUES
    ('Sucursal 1', 'Calle falsa 123', NULL, NULL),
    ('Sucursal 2', 'Siempreviva 348', NULL, NULL);
