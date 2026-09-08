# Pendientes

Cinco categorías: lo que falta implementar del esqueleto, mejoras del código que no bloquean, lo que falta modelar, lo que espera una definición del negocio, y lo que se dejó afuera a propósito.

---

## Implementación en curso

**Hecho (catálogo + sucursales + inventario).** Flyway V1–V4. API de marcas, categorías, productos, sucursales y stock. Frontend: listado/alta/edición/baja de productos, gestión de marcas y categorías, switch de sucursal, pantalla Inventario (sin contar / ya contado, recuento, entrada, consumo, extravío, traslado, historial).

**Siguiente bloque: caja y después ventas.** Todavía no hay tablas ni API. La venta necesita sesión de caja abierta, por eso va segunda. Cuando exista, va a persistir movimientos `VENTA` / `ANULACION_VENTA` (el enum ya está; no hay endpoint). `sale_id` y `registered_by` en `stock_movement` no están en V4: se agregan cuando existan `sale` y `employee`.

**Inventario que queda para más adelante.** `min_quantity` está en la tabla y no se edita en la UI. Ventas que dejen saldo negativo (la regla ya está: no se bloquea).

---

## Mejoras no urgentes del código

Ninguna rompe lo que ya funciona. Se cierran cuando duelan o cuando haya una tarde suelta.

**La lista de “sin contar” se arma en el navegador.** La pantalla de inventario pide todos los productos y todos los saldos, y resta. Con decenas de artículos alcanza; con miles se va a sentir. Cuando duela, el backend tiene que devolver esa lista ya filtrada.

**Falta el handler de validación de campos.** El manejador global cubre 404 y reglas de negocio, pero no `MethodArgumentNotValidException`. Si un formulario manda un precio inválido, el front se come un 500 en vez de marcar el campo. Es una tarde.

**`EXTRAVÍO` con tilde en el enum.** Funciona, pero los enums guardados como texto con acento son un clásico de problemas al cambiar de entorno o de cliente HTTP. `EXTRAVIO` o `PERDIDA` evitan ese roce. Cosmético, y hay que migrar las filas que ya existan.

---

## Cosas con fecha implícita

No bloquean caja ni venta, pero cada una tiene un “cuándo” y no conviene olvidarlas.

**Paginación del listado de productos.** El endpoint de catálogo hoy devuelve todo con sus variantes. Con miles de artículos se va a sentir, y cambiarlo después toca API, DTO y pantalla. Conviene hacerla antes de que el catálogo crezca de verdad.

**Etiquetas con el SKU.** El SKU existe para la mercadería sin código de fábrica. Sin un botón que imprima el código, cuando llegue la venta un porcentaje grande no se va a poder escanear. **Antes del módulo de ventas.**

**Actualización masiva de precios.** Con la inflación es una tarea semanal. Hoy cambiar el precio de una góndola es entrar producto por producto. No bloquea el siguiente módulo, pero es de las primeras cosas que van a pedir cuando usen el catálogo en serio.

**Buscador del listado que acepte código de barras.** El mismo campo: si parece un código, busca por código; si no, por nombre. Barato, y cambia el uso: tenés el producto en la mano, escaneás y aparece la ficha.

---

## Bloques del modelo sin diseñar

### Usuarios y permisos

Hoy `employee` representa a una persona que trabaja, pero no tiene credenciales ni nada que defina qué puede hacer dentro del sistema.

Ya hay campos que asumen esa capa: `discount_authorized_by` en las ventas y `registered_by` en los movimientos de caja, de stock y de cuenta corriente. Alguien tiene que estar identificado para completarlos.

Operaciones que claramente no debería poder hacer cualquiera: ver las liquidaciones de otras personas, autorizar descuentos, anular ventas, cambiar precios, registrar ajustes de stock y cerrar la caja.

### Compras y proveedores

La mercadería hoy entra sin registro de compra. El inventario ya contempla un movimiento de tipo `ENTRADA`, que funciona sin depender de este bloque —una decisión intencional, para que el inventario pueda arrancar antes de tener el circuito completo de compras.

Falta modelar el proveedor, el comprobante de compra y el precio de costo, que es lo que después habilita calcular margen.

### Devoluciones y saldo a favor del cliente

Es el bloque más grande de los tres, porque toca tres cosas a la vez: el stock, que vuelve a entrar; la plata, que se devuelve o queda a favor; y el comprobante, ya que si la venta se facturó hay que emitir una nota de crédito.

El saldo a favor convierte a `customer` en una cuenta corriente similar a la de empleados, con movimientos y saldo. Hoy esa tabla es mínima porque solo contiene a quienes piden factura.

`fiscal_document.related_document_id` ya existe en el diseño para que una nota de crédito apunte a la factura que corrige.

---

## Definiciones del negocio a confirmar

Ninguna afecta la estructura de las tablas: todas se resuelven en la lógica de cálculo.

**¿Se factura venta por venta?** Es lo recomendado y lo que resuelve de raíz el fraccionamiento manual actual, pero requiere confirmación del dueño y del contador.

**¿Qué fecha lleva un comprobante emitido con días de atraso?** Hasta dónde se puede retroceder la fecha de emisión es algo a confirmar con el contador.

---

## Integraciones pendientes

### ARCA

Los dos puntos de venta actuales de las sucursales son de tipo "Factura en Línea" y no sirven para emitir desde un sistema propio. Hay que dar de alta dos nuevos del tipo **RECE para aplicativo y Web Services**, uno por sucursal.

También hace falta generar el certificado digital y autorizar dos servicios por separado con ese mismo certificado: `wsfe` para emitir comprobantes y `ws_sr_constancia_inscripcion` para completar automáticamente los datos de un cliente a partir de su CUIT.

Ese segundo servicio permite además **validar qué comprobante corresponde**: si el CUIT consultado no está inscripto en IVA, no corresponde factura A.

Ambos tienen entorno de homologación, así que todo el desarrollo se puede hacer sin emitir comprobantes reales.

### Conciliación de pagos electrónicos

El control manual de pagos digitales contra la cuenta del comercio se reemplaza por los campos de confirmación en `sale_payment`. Al principio los completa una persona; la integración solo cambia quién los completa.

Sobre las opciones evaluadas:

- **Consultar transferencias entrantes por API** no es viable con Mercado Pago. Su API cubre los pagos que pasan por su infraestructura de cobro, no las transferencias recibidas en la cuenta.
- **Importar reportes automáticamente** sí es viable y no requiere cambiar la operatoria. El sistema los descarga solo con una credencial configurada una vez.
- **QR generado por el sistema** confirmaría en el momento, pero se descartó por una restricción física: hay un solo monitor y está orientado al mostrador.
- **Mercado Pago Point** es la opción más completa, porque unificaría la confirmación de todos los medios electrónicos, pero implica comprar el lector y cambiar cómo se cobra.

---

## Postergaciones deliberadas

Cada una se evaluó y se descartó por no justificar su costo todavía. Todas son aditivas: agregarlas después no rompe nada de lo modelado.

**Múltiples códigos de barras por variante.** Se resuelve con un solo código nullable. Una tabla de códigos alternativos se justifica solo si aparece el caso de un mismo artículo que se escanea de varias formas.

**Precio por sucursal.** En teoría el precio es el mismo en ambos locales. Lo importante no es la tabla de excepciones sino que ninguna parte del sistema lea la columna de precio directamente: si todo pregunta "el precio de esta variante en esta sucursal", el día que aparezca la excepción cambia un solo lugar.

**Lotes y vencimientos.** Aunque hay kiosco, nadie lleva control de lotes y no se justifica.

**Sistema configurable de atributos de variante.** La variación no es solo por color: hay diseño, personaje y tamaño, a veces combinados. Un campo de texto libre cubre prácticamente todos los casos y se entiende de un vistazo. Un motor de atributos se justificaría recién si hiciera falta filtrar por ellos.

**Cabecera de traslado entre sucursales.** Los traslados son de pocos artículos, así que los movimientos vinculados de a pares alcanzan. Si alguna vez se mueven lotes grandes, un campo de agrupación resuelve sin necesidad de tabla nueva.

**Desglose por alícuota en el comprobante.** Los importes actuales asumen una sola alícuota por comprobante, lo cual es correcto mientras todo sea 21%. Si alguna vez se vende algo exento en el mismo comprobante que artículos gravados, hace falta una tabla hija con una fila por alícuota.

**Vigencia en la exclusión del descuento de empleado.** El caso de las figuritas mostró que la exclusión a veces es temporal, pero se resuelve encendiendo y apagando el booleano un par de veces al año. Un rango de fechas sería sobreingeniería.

**Historial de valores hora.** El valor vigente vive en `employee` y cada liquidación congela el que usó, así que el historial de liquidaciones ya provee la trazabilidad. Una tabla de vigencias sería redundante.

**Integración con la terminal de pago.** El posnet se opera aparte y en el sistema se registra lo que se hizo. Automatizarlo es un proyecto en sí mismo y no es necesario para arrancar.
