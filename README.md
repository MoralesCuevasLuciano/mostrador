# Mostrador

Sistema de gestión para un comercio polirrubro con dos sucursales: catálogo con variantes, control de stock por sucursal, ventas con promociones, facturación electrónica ante ARCA, liquidación de personal y arqueo de caja.

Hoy están en marcha **el catálogo y el inventario**. El resto del modelo (ventas, caja, empleados, facturación) está diseñado; todavía no tiene API ni pantalla.

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
| Catálogo y variantes | **Implementado.** Flyway V1–V2, API REST y UI (listado, alta, edición, baja) |
| Sucursales | **Implementado.** API `/api/branches`; V3 carga las dos sucursales. Switch en la barra de la UI |
| Inventario por sucursal | **Implementado.** Flyway V4, API `/api/stock` y pantalla Inventario |
| Ventas, pagos y promociones | Modelado |
| Facturación electrónica | Modelado (ARCA `wsfe` + constancia de inscripción) |
| Empleados y liquidaciones | Modelado |
| Caja y arqueo | Modelado |
| Usuarios y permisos | Pendiente de diseñar |
| Compras y proveedores | Pendiente de diseñar |
| Devoluciones y saldo a favor | Pendiente de diseñar |

## Qué hay hoy

**Backend**

- Tablas en MySQL: `branch`, `category`, `brand`, `product`, `product_variant`, `stock`, `stock_movement`
- Flyway: `V1` catálogo, `V2` `tracks_stock`, `V3` carga de sucursales, `V4` inventario
- CRUD de marcas, categorías (rubro / subcategoría, máximo dos niveles), productos con variantes y sucursales
- Inventario: recuento, entrada, consumo interno, extravío, traslado entre locales, saldo e historial
- Baja y reactivación lógica (`is_active`)
- SKU único generado (`MF-{productId}-{nn}`); código de barras **no único** (aviso al escanear o al salir del campo, no bloquea)
- Precio, marca y barcode viven en la **variante**; el producto es el tipo de artículo
- `tracks_stock` en el producto (falso en caramelos sueltos, fotocopias, etc.)
- IVA por defecto 21 %; flag de descuento de empleado; condición `NUEVA` / `DEFECTUOSA`
- Subida de fotos (`/api/uploads`, máx. 5 MB)
- Errores como `ProblemDetail` (`404` / `409`)
- Tests unitarios de `BrandService`, `CategoryService`, `ProductService`, `BranchService`, `StockService` y `UploadService`
- Colecciones HTTP en `backend/http/`

**Frontend**

- Nav: Productos | Cargar producto | Inventario | sucursal de trabajo (arriba a la derecha, se recuerda al recargar)
- Listado de productos activos; **Ver dados de baja** para los inactivos. Las variantes dadas de baja no se listan en la tarjeta
- Alta y edición de producto (N variantes, foto, “Lleva inventario”)
- En edición: botón **Variantes dadas de baja** (popup para reactivarlas)
- Gestión de marcas y categorías desde el listado
- Lectura de código de barras por teclado (pistola); aviso si el código ya existe
- Inventario filtrado por la sucursal elegida: **Sin contar** y **En este local** (recuento, entrada, consumo, extravío, traslado, historial). No muestra variantes ni productos dados de baja
- El catálogo no se filtra por sucursal: es el mismo en los dos locales

## Stack

| Capa | Tecnología |
|---|---|
| Backend | Java 21, Spring Boot 4.1.1, Spring Web MVC, Validation, Data JPA |
| Base de datos | MySQL 8, Flyway (`ddl-auto: validate` — el esquema lo crean las migraciones) |
| Frontend | React 19, TypeScript, Vite 8, CSS propio (sin router ni librería de UI) |
| Zona horaria | `America/Argentina/Buenos_Aires` |

No hay Spring Security ni OpenAPI todavía. El proxy de Vite reenvía `/api` y `/uploads` a `http://127.0.0.1:8080`.

La facturación electrónica se integrará por los web services de ARCA (`wsfe` para comprobantes y `ws_sr_constancia_inscripcion` para datos de contribuyentes), con entorno de homologación para desarrollo. Eso es diseño, no código.

## Estructura

```
mostrador/
├── docs/          modelo de datos, decisiones y pendientes
├── backend/       API Spring Boot + Flyway
│   ├── http/      requests de prueba
│   └── src/main/resources/db/migration/
│       ├── V1__catalogo.sql
│       ├── V2__producto_control_de_stock.sql
│       ├── V3__carga_sucursales.sql
│       └── V4__stock.sql
└── frontend/      React + Vite
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

El password **no se commitea**. Va en `backend/src/main/resources/application-local.yaml` (está en `.gitignore`):

```yaml
spring:
  datasource:
    password: TU_PASSWORD
```

Flyway corre las migraciones al arrancar (hoy hasta V4).

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

## API

| Recurso | Base |
|---|---|
| Marcas | `/api/brands` |
| Categorías | `/api/categories` |
| Productos | `/api/products` |
| Variantes | `/api/products/{id}/variants` |
| Sucursales | `/api/branches` |
| Inventario | `/api/stock` |
| Fotos | `/api/uploads` |

Operaciones típicas de catálogo y sucursales: `POST` alta, `GET` listado/detalle, `PUT` edición, `DELETE` baja lógica, `POST …/activate` reactivación.

Inventario (la sucursal va en la URL; el recuento y los deltas van en el body):

| Método | Ruta | Qué hace |
|---|---|---|
| `GET` | `/api/stock/{branchId}` | Variantes ya contadas en ese local |
| `GET` | `/api/stock/{branchId}/{variantId}` | Saldo; sin fila no es cero (`inventoried: false`) |
| `GET` | `/api/stock/{branchId}/{variantId}/movements` | Historial |
| `POST` | `/api/stock/{branchId}/{variantId}/recount` | Recuento (inicial o posterior) |
| `POST` | `/api/stock/{branchId}/{variantId}/entries` | Entrada |
| `POST` | `/api/stock/{branchId}/{variantId}/internal-consumption` | Consumo interno |
| `POST` | `/api/stock/{branchId}/{variantId}/losses` | Extravío |
| `POST` | `/api/stock/transfers` | Traslado entre sucursales |

También: `GET /api/products/barcode-matches?barcode=` — quién ya usa ese código (no bloquea el alta).

Los ids de sucursal no son fijos: `GET /api/branches` devuelve los reales (en una base que ya tenía filas, V3 puede haber insertado como 3 y 4).

## Decisiones que ya se ven en el código

- Todo producto tiene **al menos una variante** (si no varía, el label es `Única` y la UI lo oculta).
- El barcode **no es único**; el SKU sí.
- Baja lógica, no borrado físico. En el listado y en inventario no se muestran las variantes dadas de baja; se reactivan desde Editar.
- Esquema con **Flyway**, no con Hibernate DDL (`validate`).
- Stock por **variante × sucursal**. Sin fila = nunca se inventarió. El saldo solo cambia si se persiste un movimiento. Puede quedar negativo.
- Productos con `tracks_stock = false` no entran a inventario.
- La sucursal de la barra es **dónde se trabaja**, no un filtro del catálogo.

El resto de las decisiones —venta distinta del comprobante fiscal, caja por sesión y no por vendedor, promociones por lista explícita— está en [decisiones de diseño](docs/decisiones-de-diseno.md) y todavía no tiene implementación.

## Plan de implementación

Por fases, cada una utilizable por sí sola:

1. **Catálogo e inventario.** Hecho: se carga el catálogo, se elige sucursal y se cuenta / mueve stock. Las ventas todavía no descuentan (tipos `VENTA` / `ANULACION_VENTA` existen en el enum, sin endpoint).
2. **Ventas y caja.** Reemplaza la planilla diaria y habilita el arqueo, que hoy no existe.
3. **Empleados y liquidaciones.** Bastante independiente del resto.
4. **Facturación electrónica.** Se deja para cuando el resto ya esté rodando, porque depende de terceros (puntos de venta RECE y certificado ARCA).
5. **Compras, devoluciones y permisos.**

La carga inicial del catálogo se piensa de forma incremental: los productos se dan de alta al momento de venderlos si no existen, y el inventario se cuenta por sector (pantalla **Sin contar**) en vez de un recuento masivo previo.

## Documentación

- [Modelo de datos](docs/modelo-de-datos.md) — las 22 tablas del diseño. En MySQL hoy existen las 7 de V1–V4.
- [Decisiones de diseño](docs/decisiones-de-diseno.md) — qué se decidió, qué alternativas se evaluaron y por qué.
- [Pendientes](docs/pendientes.md) — lo que falta modelar, lo que espera confirmación y lo que se postergó a propósito.
