# Mostrador

Sistema de gestión para un comercio polirrubro con dos sucursales: catálogo con variantes, control de stock por sucursal, ventas con promociones, facturación electrónica ante ARCA, liquidación de personal y arqueo de caja.

Hoy está en **fase 1**: el catálogo ya se puede cargar y consultar. El resto del modelo (22 tablas) está diseñado; inventario, ventas, caja y facturación todavía no tienen API ni pantalla.

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

## Alcance (4 de septiembre de 2026)

| Módulo | Estado |
|---|---|
| Catálogo y variantes | **Implementado.** Flyway V1, API REST y UI de listado + alta |
| Inventario por sucursal | Modelado; falta Flyway V2 y API |
| Ventas, pagos y promociones | Modelado |
| Facturación electrónica | Modelado (ARCA `wsfe` + constancia de inscripción) |
| Empleados y liquidaciones | Modelado |
| Caja y arqueo | Modelado |
| Usuarios y permisos | Pendiente de diseñar |
| Compras y proveedores | Pendiente de diseñar |
| Devoluciones y saldo a favor | Pendiente de diseñar |

La tabla `branch` ya existe en V1 (sucursal + punto de venta ARCA), pero todavía no tiene endpoints ni pantalla.

## Qué hay hoy del catálogo

**Backend**

- Tablas: `branch`, `category`, `brand`, `product`, `product_variant`
- CRUD de marcas, categorías (rubro / subcategoría, máximo dos niveles) y productos con variantes
- Baja y reactivación lógica (`is_active`)
- SKU único generado (`MF-{productId}-{nn}`); código de barras **no único** (mercadería apócrifa / mismo EAN en variantes distintas)
- Precio, marca y barcode viven en la **variante**; el producto es el tipo de artículo
- IVA por defecto 21 %; flag de descuento de empleado; condición `NUEVA` / `DEFECTUOSA`
- Errores como `ProblemDetail` (`404` / `409`)
- Tests unitarios de `BrandService`, `CategoryService` y `ProductService`
- Colecciones HTTP en `backend/http/` (`brands.http`, `categories.http`, `products.http`)

**Frontend**

- Listado de productos (categoría, IVA, variantes, SKU, barcode, precio, baja lógica)
- Alta de producto con N variantes
- Alta inline de marca y categoría
- Lectura de código de barras por teclado (pistola / buffer)

La API de catálogo ya permite editar, dar de baja y reactivar; **la UI todavía no** — solo lista y crea.

## Stack

| Capa | Tecnología |
|---|---|
| Backend | Java 21, Spring Boot 4.1.1, Spring Web MVC, Validation, Data JPA |
| Base de datos | MySQL 8, Flyway (`ddl-auto: validate` — el esquema lo crean las migraciones) |
| Frontend | React 19, TypeScript, Vite 8, CSS propio (sin router ni librería de UI) |
| Zona horaria | `America/Argentina/Buenos_Aires` |

No hay Spring Security ni OpenAPI todavía. El proxy de Vite reenvía `/api` a `http://127.0.0.1:8080`.

La facturación electrónica se integrará por los web services de ARCA (`wsfe` para comprobantes y `ws_sr_constancia_inscripcion` para datos de contribuyentes), con entorno de homologación para desarrollo. Eso es diseño, no código.

## Estructura

```
mostrador/
├── docs/          modelo de datos, decisiones y pendientes
├── backend/       API Spring Boot + Flyway
│   ├── http/      requests de prueba del catálogo
│   └── src/main/resources/db/migration/
│       └── V1__catalogo.sql
└── frontend/      React + Vite (listado y alta de productos)
```

## Cómo levantarlo

### Prerrequisitos

- JDK 21
- MySQL 8
- Node.js 18+ (para el frontend)
- Maven (o el wrapper `mvnw`)

### 1. Base de datos

```sql
CREATE DATABASE mostrador CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'mostrador'@'localhost' IDENTIFIED BY 'TU_PASSWORD';
GRANT ALL ON mostrador.* TO 'mostrador'@'localhost';
```

En `backend/src/main/resources/application.yaml` el usuario es `mostrador` y **no hay password commiteada**. Hay que definirla, por ejemplo:

```powershell
$env:SPRING_DATASOURCE_PASSWORD = "TU_PASSWORD"
```

o agregarla en el YAML local (sin subirla al repo).

Flyway corre `V1__catalogo.sql` al arrancar.

### 2. Backend

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

API en [http://localhost:8080](http://localhost:8080). Prefijo: `/api`.

### 3. Frontend

En otra terminal:

```powershell
cd frontend
npm install
npm run dev
```

La UI queda en el puerto por defecto de Vite ([http://localhost:5173](http://localhost:5173)) y habla con el backend vía `/api`.

## API de catálogo

| Recurso | Base |
|---|---|
| Marcas | `/api/brands` |
| Categorías | `/api/categories` |
| Productos | `/api/products` |
| Variantes | `/api/products/{id}/variants` |

Operaciones típicas: `POST` alta, `GET` listado/detalle, `PUT` edición, `DELETE` baja lógica, `POST …/activate` reactivación. Las variantes se agregan, editan, dan de baja y reactivan bajo el producto.

El frontend hoy usa solo `GET/POST` de productos, marcas y categorías.

## Decisiones que ya se ven en el código

- Todo producto tiene **al menos una variante** (si no varía, el label es `Única` y la UI lo oculta).
- El barcode **no es único**; el SKU sí.
- Baja lógica, no borrado físico.
- Esquema con **Flyway**, no con Hibernate DDL.
- Stock (cuando exista) será por **variante × sucursal**, no por producto.

El resto de las decisiones —venta distinta del comprobante fiscal, caja por sesión y no por vendedor, stock que puede ser negativo, promociones por lista explícita— está en [decisiones de diseño](docs/decisiones-de-diseno.md) y todavía no tiene implementación.

## Plan de implementación

Por fases, cada una utilizable por sí sola:

1. **Catálogo e inventario.** En curso: el catálogo ya se carga a mano desde la UI. Siguiente paso: Flyway V2 (`stock`, `stock_movement`) y su API. Es la fase que más depende de trabajo humano.
2. **Ventas y caja.** Reemplaza la planilla diaria y habilita el arqueo, que hoy no existe.
3. **Empleados y liquidaciones.** Bastante independiente del resto.
4. **Facturación electrónica.** Se deja para cuando el resto ya esté rodando, porque depende de terceros (puntos de venta RECE y certificado ARCA).
5. **Compras, devoluciones y permisos.**

La carga inicial del catálogo se piensa de forma incremental: los productos se dan de alta al momento de venderlos si no existen, y el inventario se cuenta por sector con el celular en vez de hacer un recuento masivo previo.

## Documentación

- [Modelo de datos](docs/modelo-de-datos.md) — las 22 tablas con campos y relaciones. En la base, hoy existen las 5 de V1.
- [Decisiones de diseño](docs/decisiones-de-diseno.md) — qué se decidió, qué alternativas se evaluaron y por qué.
- [Pendientes](docs/pendientes.md) — lo que falta modelar, lo que espera confirmación y lo que se postergó a propósito.
