# Modelo de datos

22 tablas agrupadas en siete bloques. Este documento es el contrato del esquema. Las tablas se crean con migraciones Flyway en `mostrador/backend/src/main/resources/db/migration/` (`V1__…sql`, `V2__…sql`). Hibernate no genera el esquema (`ddl-auto: none`).

Convenciones: nombres en `snake_case` y singular, claves foráneas como `tabla_id`, baja lógica con `is_active`, y `created_at` / `updated_at` en todas las tablas (no se repiten en los listados de abajo).

Los importes van en `decimal`, nunca en punto flotante. Los identificadores que no se usan para hacer aritmética —SKU, código de barras, número de documento— van como texto, para no perder ceros a la izquierda.

---

## Estructura base

### branch

Las sucursales.

| Campo | Tipo | Obligatorio |
|---|---|---|
| id | PK | sí |
| name | varchar | sí |
| address | varchar | no |
| phone | varchar | no |
| point_of_sale | integer | no |
| is_active | boolean | sí |

`point_of_sale` es el punto de venta de ARCA asignado a esa sucursal, lo que hace que la numeración de comprobantes de cada local sea independiente.

Una sucursal nunca se elimina: si cierra, se marca inactiva y su historial queda intacto.

---

## Catálogo

### category

Categorías con jerarquía de dos niveles: rubro y subcategoría.

| Campo | Tipo | Obligatorio |
|---|---|---|
| id | PK | sí |
| name | varchar | sí |
| parent_id | FK → category | no |
| is_active | boolean | sí |

Autorreferencia: las que no tienen padre son los rubros. Único sobre `(parent_id, name)`, para permitir un "Varios" dentro de cada rubro sin duplicados dentro del mismo.

Un producto puede apuntar a cualquier nivel: si un rubro no tiene hijos, funciona como categoría.

### brand

| Campo | Tipo | Obligatorio |
|---|---|---|
| id | PK | sí |
| name | varchar, único | sí |
| is_active | boolean | sí |

El nombre se normaliza al guardar (espacios y mayúsculas) para evitar duplicados por tipeo.

### product

La ficha comercial del artículo. No tiene precio ni stock ni código de barras: todo eso vive en la variante.

| Campo | Tipo | Obligatorio |
|---|---|---|
| id | PK | sí |
| name | varchar | sí |
| description | text | no |
| category_id | FK → category | no |
| brand_id | FK → brand | no |
| vat_rate | decimal(5,2) | sí |
| allows_employee_discount | boolean | sí |
| is_active | boolean | sí |

- `vat_rate` — alícuota de IVA. Por defecto 21,00 y oculta en el alta; se edita solo en los casos excepcionales.
- `allows_employee_discount` — si admite el descuento de empleado. Se apaga en artículos de margen mínimo como cigarrillos.

### product_variant

Lo que efectivamente se vende, se escanea y se cuenta.

| Campo | Tipo | Obligatorio |
|---|---|---|
| id | PK | sí |
| product_id | FK → product | sí |
| sku | varchar, único | sí |
| label | varchar | sí |
| barcode | varchar | no |
| price | decimal(12,2) | sí |
| image_url | varchar | no |
| is_active | boolean | sí |

- `sku` — código interno autogenerado por el sistema. Existe siempre, incluso en mercadería sin código de fábrica, y sirve para imprimir una etiqueta con código de barras propio.
- `label` — lo que distingue esta variante: "Rojo", "Frozen", o "Única" cuando el producto no varía.
- `barcode` — sin restricción de unicidad. Los duplicados se advierten en el alta pero no se bloquean.

**Regla de aplicación:** todo producto tiene al menos una variante. Los productos que no varían se crean con una variante "Única" que la interfaz no muestra.

---

## Inventario

### stock

El saldo actual.

| Campo | Tipo | Obligatorio |
|---|---|---|
| id | PK | sí |
| product_variant_id | FK → product_variant | sí |
| branch_id | FK → branch | sí |
| quantity | integer | sí |
| min_quantity | integer | no |

Único sobre `(product_variant_id, branch_id)`. Es la restricción que sostiene la integridad del inventario.

- La **ausencia de fila** significa "nunca se inventarió". Una fila en cero significa "se contó y no hay".
- `quantity` puede ser negativo. No se bloquean ventas por falta de stock.

### stock_movement

El historial que explica el saldo.

| Campo | Tipo | Obligatorio |
|---|---|---|
| id | PK | sí |
| product_variant_id | FK → product_variant | sí |
| branch_id | FK → branch | sí |
| type | varchar | sí |
| quantity | integer | sí |
| sale_id | FK → sale | no |
| related_movement_id | FK → stock_movement | no |
| description | varchar | no |
| registered_by | FK → employee | no |
| movement_at | timestamp | sí |

- `type` — venta, anulación de venta, ajuste inicial, ajuste por recuento, consumo interno, traslado, entrada de mercadería, rotura o pérdida.
- `quantity` — con signo: negativo cuando sale, positivo cuando entra.
- `related_movement_id` — vincula las dos patas de un traslado entre sucursales.

**Regla de aplicación:** el saldo de `stock` nunca se escribe directamente. Siempre se registra un movimiento y el movimiento actualiza el saldo, en la misma transacción. Eso permite recalcular cualquier saldo desde cero.

---

## Empleados

### employee

| Campo | Tipo | Obligatorio |
|---|---|---|
| id | PK | sí |
| name | varchar | sí |
| document | varchar | no |
| phone | varchar | no |
| hourly_rate | decimal(10,2) | no |
| hire_date | date | sí |
| is_active | boolean | sí |

`hourly_rate` es opcional porque el dueño figura en esta tabla —necesita cuenta corriente para sus retiros— pero no cobra por hora ni se le liquida.

`hire_date` es necesaria para prorratear el aguinaldo.

### time_entry

Las fichadas, que reemplazan la planilla de horas.

| Campo | Tipo | Obligatorio |
|---|---|---|
| id | PK | sí |
| employee_id | FK → employee | sí |
| branch_id | FK → branch | sí |
| check_in | timestamp | sí |
| check_out | timestamp | no |
| note | varchar | no |

Ambos como timestamp completo, para que un turno que cruza la medianoche funcione sin casos especiales. Las horas trabajadas no se guardan: se calculan restando. Un turno partido son dos filas del mismo día.

### employee_account_movement

Cuenta corriente: vales, productos llevados a cuenta y ajustes.

| Campo | Tipo | Obligatorio |
|---|---|---|
| id | PK | sí |
| employee_id | FK → employee | sí |
| type | varchar | sí |
| amount | decimal(12,2) | sí |
| description | varchar | no |
| movement_date | timestamp | sí |
| sale_id | FK → sale | no |
| cash_movement_id | FK → cash_movement | no |
| payroll_id | FK → payroll | no |
| registered_by | FK → employee | no |

- `amount` — con signo. Negativo genera deuda, positivo es saldo a favor.
- `payroll_id` — la liquidación que descontó este movimiento. **Mientras está vacío, el movimiento está pendiente.** Es lo que evita descontar dos veces el mismo vale.

El saldo pendiente de una persona es la suma de sus movimientos con `payroll_id` vacío. Con este volumen no se cachea.

### payroll

Liquidaciones quincenales y aguinaldo.

| Campo | Tipo | Obligatorio |
|---|---|---|
| id | PK | sí |
| employee_id | FK → employee | sí |
| type | varchar | sí |
| period_start | date | sí |
| period_end | date | sí |
| total_hours | decimal(6,2) | no |
| hourly_rate | decimal(10,2) | no |
| gross_amount | decimal(12,2) | sí |
| deductions_amount | decimal(12,2) | sí |
| net_amount | decimal(12,2) | sí |
| note | text | no |

`total_hours` y `hourly_rate` van vacíos en el aguinaldo, donde el bruto se calcula como la mitad de la mejor remuneración del semestre, prorrateada por el tiempo trabajado.

Todos los importes quedan congelados: la liquidación es un documento cerrado y no se recalcula al abrirla.

### payroll_payment

| Campo | Tipo | Obligatorio |
|---|---|---|
| id | PK | sí |
| payroll_id | FK → payroll | sí |
| method | varchar | sí |
| amount | decimal(12,2) | sí |
| paid_at | timestamp | sí |
| reference | varchar | no |

Una quincena pagada mitad en efectivo y mitad por transferencia son dos filas. El estado no es un campo: se deduce comparando la suma de los pagos contra `net_amount`.

---

## Ventas

### sale

| Campo | Tipo | Obligatorio |
|---|---|---|
| id | PK | sí |
| branch_id | FK → branch | sí |
| cash_session_id | FK → cash_session | sí |
| type | varchar | sí |
| employee_id | FK → employee | no |
| customer_id | FK → customer | no |
| fiscal_document_id | FK → fiscal_document | no |
| replaces_sale_id | FK → sale | no |
| sold_at | timestamp | sí |
| subtotal | decimal(12,2) | sí |
| discount_amount | decimal(12,2) | sí |
| surcharge_amount | decimal(12,2) | sí |
| total | decimal(12,2) | sí |
| discount_authorized_by | FK → employee | no |
| status | varchar | sí |
| note | text | no |

- `cash_session_id` — obligatorio. Ninguna venta puede existir sin sesión de caja.
- `type` — normal o venta a empleado.
- `fiscal_document_id` — el comprobante que la cubre. Vacío mientras no se facturó. La clave está de este lado porque **un comprobante puede cubrir varias ventas**.
- `replaces_sale_id` — la venta anulada que esta corrige.
- `total` = `subtotal` − `discount_amount` + `surcharge_amount`.
- `discount_authorized_by` — se pide solo cuando hay descuento manual.

El número de comprobante interno que se imprime es el propio `id`: no tiene exigencia de correlatividad, así que no necesita secuencia propia.

### sale_line

| Campo | Tipo | Obligatorio |
|---|---|---|
| id | PK | sí |
| sale_id | FK → sale | sí |
| product_variant_id | FK → product_variant | sí |
| quantity | integer | sí |
| list_unit_price | decimal(12,2) | sí |
| line_total | decimal(12,2) | sí |
| vat_rate | decimal(5,2) | sí |
| promotion_id | FK → promotion | no |

- `list_unit_price` y `vat_rate` quedan congelados al momento de vender.
- `line_total` es lo efectivamente cobrado, con promociones y descuentos ya aplicados y redondeado.
- El descuento no es un campo: se deriva de `list_unit_price × quantity − line_total`.
- `promotion_id` registra qué promoción ganó el cálculo, para poder explicar cualquier ticket sin recalcular.

### sale_payment

| Campo | Tipo | Obligatorio |
|---|---|---|
| id | PK | sí |
| sale_id | FK → sale | sí |
| method | varchar | sí |
| installments | integer | no |
| amount | decimal(12,2) | sí |
| reference | varchar | no |
| confirmation_status | varchar | no |
| confirmed_at | timestamp | no |
| confirmed_by | FK → employee | no |

- `method` — efectivo, débito, crédito, transferencia, QR o cuenta de empleado.
- `installments` — solo para crédito. Con 2 o 3 cuotas se aplica el recargo, que va en `sale.surcharge_amount`.
- Los tres campos de confirmación reemplazan el control manual de pagos electrónicos contra la cuenta del comercio. Van vacíos para efectivo y cuenta de empleado.

El medio **cuenta de empleado** no mueve plata: no afecta el arqueo y genera un movimiento en la cuenta corriente que se descuenta en la liquidación.

### customer

Solo los clientes que piden factura. La mayoría de las ventas son a consumidor final y no generan fila acá.

| Campo | Tipo | Obligatorio |
|---|---|---|
| id | PK | sí |
| name | varchar | sí |
| document_type | varchar | sí |
| document_number | varchar | sí |
| tax_condition | varchar | sí |
| address | varchar | no |
| email | varchar | no |
| phone | varchar | no |
| is_active | boolean | sí |

Único sobre `(document_type, document_number)`. `tax_condition` define qué comprobante corresponde: factura A a responsables inscriptos, B al resto.

### fiscal_document

| Campo | Tipo | Obligatorio |
|---|---|---|
| id | PK | sí |
| branch_id | FK → branch | sí |
| customer_id | FK → customer | no |
| document_type | integer | sí |
| point_of_sale | integer | sí |
| number | bigint | no |
| issue_date | date | sí |
| net_amount | decimal(12,2) | sí |
| vat_amount | decimal(12,2) | sí |
| total_amount | decimal(12,2) | sí |
| cae | varchar | no |
| cae_expiration | date | no |
| status | varchar | sí |
| related_document_id | FK → fiscal_document | no |
| arca_response | text | no |

- `document_type` — el código numérico de ARCA (1 factura A, 6 factura B, 3 y 8 las notas de crédito correspondientes).
- `number` — vacío hasta que ARCA autoriza. No se reserva numeración por adelantado, para no dejar huecos en la correlatividad cuando un pedido falla.
- `status` — pendiente, autorizado, rechazado o anulado.
- `related_document_id` — para que una nota de crédito apunte a la factura que corrige.
- `arca_response` — la respuesta cruda del web service, para diagnosticar rechazos.

Único sobre `(point_of_sale, document_type, number)` cuando el número existe: la correlatividad es por punto de venta y tipo.

El comprobante guarda importes totales, no duplica las líneas de las ventas.

---

## Promociones

### promotion

| Campo | Tipo | Obligatorio |
|---|---|---|
| id | PK | sí |
| name | varchar | sí |
| type | varchar | sí |
| discount_percent | decimal(5,2) | no |
| starts_at | date | no |
| ends_at | date | no |
| is_active | boolean | sí |

`type` es escalonada o porcentual. Las fechas son opcionales: sin fecha de fin, la promoción corre hasta que se desactive.

### promotion_item

Qué participa de la promoción.

| Campo | Tipo | Obligatorio |
|---|---|---|
| id | PK | sí |
| promotion_id | FK → promotion | sí |
| product_id | FK → product | no |
| category_id | FK → category | no |

Exactamente uno de los dos debe estar completo, forzado con un check. Apunta a producto y no a variante porque las promociones por cantidad no distinguen color.

### promotion_tier

Los escalones de las promociones escalonadas.

| Campo | Tipo | Obligatorio |
|---|---|---|
| id | PK | sí |
| promotion_id | FK → promotion | sí |
| min_quantity | integer | sí |
| price | decimal(12,2) | sí |

`price` es el precio total de esa cantidad, tal como figura en el cartel: "3 por $10.000" se carga como `min_quantity` 3 y `price` 10000. El unitario se deriva dividiendo.

El escalón de una unidad no se carga: ese es el precio de la variante.

Único sobre `(promotion_id, min_quantity)`.

**Cómo se aplica:** el sistema agrupa las líneas cuyos productos pertenecen a la misma promoción y suma las cantidades cruzando líneas —dos alfajores de sabores distintos cuentan como dos unidades—. Con esa cantidad busca el escalón más alto alcanzado y lo aplica a todas las unidades. El redondeo se hace sobre el total del grupo, no línea por línea.

Cuando un producto cae en más de una promoción, se calculan todas y gana la más barata para el cliente. No se acumulan.

---

## Caja

### cash_session

La planilla del día. Una por sucursal y fecha, con único sobre esa combinación.

| Campo | Tipo | Obligatorio |
|---|---|---|
| id | PK | sí |
| branch_id | FK → branch | sí |
| business_date | date | sí |
| opening_amount | decimal(12,2) | sí |
| opening_counted_at | timestamp | no |
| opened_by | FK → employee | no |
| total_cash_sales | decimal(12,2) | no |
| total_cash_out | decimal(12,2) | no |
| closing_amount | decimal(12,2) | no |
| closed_at | timestamp | no |
| closed_by | FK → employee | no |
| note | text | no |

- `opening_amount` — se completa solo con lo que arrastra del día anterior cuando la sesión se abre automáticamente. El cajón no se vacía nunca, así que el saldo de apertura es el de cierre del día previo.
- `opening_counted_at` — vacío significa que ese monto es heredado y nadie lo verificó contando. Permite vender antes de contar sin que el sistema mienta.
- `total_cash_sales` y `total_cash_out` — congelados al cerrar. Son los únicos que vienen de otras tablas y por eso podrían mutar.
- `closing_amount` — vacío mientras la caja está abierta. Reemplaza a un campo de estado.

El **monto esperado** y la **diferencia** no se guardan: se derivan de cuatro campos congelados de la misma fila, así que su resultado es inmutable.

El "total tarjetas" y la facturación del día tampoco se guardan: son reportes que se calculan sumando las ventas.

### cash_movement

Entradas y salidas que no son ventas.

| Campo | Tipo | Obligatorio |
|---|---|---|
| id | PK | sí |
| cash_session_id | FK → cash_session | sí |
| type | varchar | sí |
| amount | decimal(12,2) | sí |
| employee_id | FK → employee | no |
| description | varchar | no |
| registered_by | FK → employee | no |
| movement_at | timestamp | sí |

`type` distingue cuatro casos: **retiro por resguardo** (plata que sale del cajón por seguridad y sigue siendo del negocio), **vale** (genera deuda del empleado), **gasto o pago a proveedor**, e **ingreso de efectivo** (cuando se repone cambio).

Los dos primeros se ven igual desde el cajón pero significan cosas opuestas, y por eso se distinguen desde el modelo.

`amount` va con signo, negativo cuando sale plata.

Las ventas **no** se registran acá: ya están en `sale` y `sale_payment`. El efectivo que entró se calcula sumando los pagos en efectivo de la sesión.

---

## Reglas que el modelo no expresa

Estas viven en la capa de aplicación porque implican comparar o sumar varias filas, algo que un DER no puede representar:

- Todo producto tiene al menos una variante.
- El saldo de `stock` solo cambia mediante movimientos, en la misma transacción.
- La suma de `sale_payment` debe igualar `sale.total`.
- La suma de `payroll_payment` no debería superar `payroll.net_amount`; si lo hace, se advierte y el excedente puede quedar como saldo a favor.
- Un vale genera dos filas —una en caja y otra en la cuenta del empleado— creadas juntas.
- Si hay un pago con crédito en 2 o 3 cuotas, ese pago es único y cubre el total.
- Una venta de un día ya cerrado se anula y se rehace; la corrección no toca la caja de aquel día, que ya registró el descuadre.
