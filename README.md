# Sistema de Gestión Comercial

Sistema de gestión para un comercio polirrubro con dos sucursales: catálogo con variantes, control de stock por sucursal, ventas con promociones, facturación electrónica ante ARCA, liquidación de personal y arqueo de caja.

## El problema

El comercio opera hoy enteramente en papel. No hay control de stock: las cantidades se estiman a ojo y no existe registro de cuánto hay de cada artículo. Las ventas se anotan en una planilla diaria, igual que los horarios del personal y los retiros de efectivo. La facturación se arma cada varios días de forma manual, agrupando montos. Cuando la caja no cuadra, nadie se entera.

El sistema apunta a reemplazar ese circuito sin pedirle a las personas más trabajo del que ya hacen.

## Contexto que condiciona el diseño

Estas particularidades del negocio explican buena parte de las decisiones del modelo:

- **Polirrubro real.** Conviven artículos escolares, juguetería, bazar, santería y kiosco. La mercadería genérica sin marca ni código de barras es habitual.
- **Variantes con reglas propias.** Muchos artículos son el mismo producto en distintos colores o diseños, a veces con el mismo código de barras y a veces no.
- **Promociones por cantidad que cruzan productos.** El "2 por $X" aplica a un grupo de artículos distintos que comparten precio, no a un solo producto.
- **Dos sucursales con stock independiente** y traslados frecuentes entre ellas.
- **Rotación constante de personal en el mostrador.** Varias personas venden alternadamente durante el mismo turno, por lo que identificarse en cada venta no es viable.
- **Retiros de efectivo varias veces al día** por seguridad, mezclados con vales de empleados.
- **Responsable inscripto** ante ARCA, con un punto de venta por sucursal.

## Alcance

| Módulo | Estado |
|---|---|
| Catálogo y variantes | Modelado; falta Flyway V1 |
| Inventario por sucursal | Modelado; falta Flyway V2 |
| Ventas, pagos y promociones | Modelado |
| Facturación electrónica | Modelado |
| Empleados y liquidaciones | Modelado |
| Caja y arqueo | Modelado |
| Usuarios y permisos | Pendiente |
| Compras y proveedores | Pendiente |
| Devoluciones y saldo a favor | Pendiente |

## Estado

Esqueleto de aplicación en marcha. El modelo de datos está definido en 22 tablas. El backend Spring Boot conecta a MySQL; las migraciones Flyway todavía no crearon el catálogo.

## Estructura

- `docs/` — modelo de datos y decisiones de diseño
- `backend/` — API Java + Spring Boot + MySQL + Flyway
- `frontend/` — interfaz (todavía vacío)

Para levantar el backend:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

## Stack

Java 21, Spring Boot 4.1, MySQL 8 y Flyway. La facturación electrónica se integra por los web services de ARCA (`wsfe` para comprobantes y `ws_sr_constancia_inscripcion` para datos de contribuyentes), con entorno de homologación para desarrollo.

## Documentación

- [Modelo de datos](docs/modelo-de-datos.md) — las 22 tablas con sus campos y relaciones.
- [Decisiones de diseño](docs/decisiones-de-diseno.md) — qué se decidió, qué alternativas se evaluaron y por qué.
- [Pendientes](docs/pendientes.md) — lo que falta modelar, lo que espera confirmación y lo que se postergó a propósito.

## Plan de implementación

Por fases, cada una utilizable por sí sola:

1. **Catálogo e inventario.** Permite cargar productos y empezar a contar stock. Es la fase que más depende de trabajo humano, porque el catálogo se puebla a mano.
2. **Ventas y caja.** Reemplaza la planilla diaria y habilita el arqueo, que hoy no existe.
3. **Empleados y liquidaciones.** Bastante independiente del resto.
4. **Facturación electrónica.** Se deja para cuando el resto ya esté rodando, porque depende de terceros.
5. **Compras, devoluciones y permisos.**

La carga inicial del catálogo se piensa de forma incremental: los productos se dan de alta al momento de venderlos si no existen, y el inventario se cuenta por sector con el celular en vez de hacer un recuento masivo previo.
