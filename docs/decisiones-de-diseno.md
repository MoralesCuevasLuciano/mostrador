# Decisiones de diseño

Cada decisión incluye el problema real que la originó, la alternativa que se descartó y el motivo. Muchas surgieron de particularidades del negocio que no eran evidentes al empezar.

---

## 1. El stock cuelga de la variante, no del producto

**Problema.** Buena parte del catálogo es el mismo artículo en distintos colores o diseños. Con un solo registro por producto no se puede saber que la cartuchera roja no se vende y la azul se agota.

**Decisión.** El inventario se lleva a nivel de variante: `stock` es única por combinación de variante y sucursal.

**Alternativa descartada.** Catálogo plano, con cada color como producto independiente. Se descartó porque obliga a editar N filas para cambiar un precio, ensucia la búsqueda y no permite ver el total de un modelo.

**Consecuencia.** Es la decisión más cara de revertir de todo el modelo, y por eso se tomó antes de escribir nada.

---

## 2. Todo producto tiene al menos una variante

**Problema.** Si solo algunos productos tienen variantes, cada consulta de precio o de stock necesita dos caminos: uno para productos simples y otro para los que varían.

**Decisión.** Los productos que no varían se crean con una única variante etiquetada "Única". El sistema siempre pregunta lo mismo.

**Consecuencia.** La interfaz esconde el concepto cuando hay una sola variante: dar de alta una resma es una pantalla sin la palabra "variante" en ningún lado. La complejidad se queda en el modelo.

**Beneficio secundario.** La granularidad la elige cada producto. Se puede llevar el detalle por color donde importa y agrupar donde no, sin cambiar el modelo y sin decidirlo de antemano para todo el catálogo.

---

## 3. El precio, el código de barras y la marca viven en la variante

**Problema.** La primera versión ponía el precio en el producto y un precio opcional en la variante que lo pisaba. Eso generaba campos aparentemente duplicados y obligaba a que toda lectura de precio resolviera una herencia.

**Decisión.** Como toda variante existe siempre, el precio vive únicamente ahí. El producto quedó como ficha descriptiva y agrupador.

**Alternativa descartada.** Precio base con override opcional. Se descartó porque la herencia se paga en cada consulta y el caso que justificaba el override —variantes con precio distinto— no ocurre en la práctica.

**Sobre el código de barras.** También vive solo en la variante. Cuando toda una línea comparte el mismo código de fábrica, se carga el mismo valor en cada variante. Eso simplifica el escaneo a una sola consulta: si devuelve una fila es esa, si devuelve varias se muestra la lista para elegir.

**Sobre la marca.** Estaba en el producto hasta que se definió el criterio de agrupación del punto siguiente: un producto puede reunir varias marcas, así que la marca también varía y se mudó a la variante.

---

## 3 bis. Los productos se agrupan por tipo de artículo, no por marca

**Problema.** Los cuadernos A4 rayados son de dos marcas, con dos códigos de barras distintos, pero en la góndola están juntos y el negocio los piensa como una sola cosa. Al contar stock interesa saber cuántos cuadernos A4 rayados hay, no cuántos de cada marca.

**Decisión.** El producto se define por el tipo de artículo —"Cuaderno A4 rayado"— y cada marca es una variante con su propio código de barras.

**Por qué el modelo lo soportaba sin cambios.** El stock ya vivía en la variante, que es lo que permite descontar la correcta al escanear. Ver el total por producto es una consulta agregada, no una estructura distinta: en el listado se muestra el total y se puede desplegar el desglose por marca.

**Lo único que cambió.** `brand_id` tuvo que mudarse de `product` a `product_variant`, porque un producto que agrupa dos marcas no puede tener una sola.

**Efecto secundario.** Simplifica las promociones. Con la agrupación anterior, el "2 por $7.000" de cuadernos exigía listar ocho productos distintos en `promotion_item`; con esta, son dos. Y el conteo cruzado del escalón sigue funcionando solo, porque dos cuadernos de marcas distintas son dos variantes del mismo producto.

---

## 3 ter. Una unidad fallada es una variante, no un descuento

**Problema.** De veinte linternas a $5.000, una anda solo enchufada y se vende a $3.500. No es una promoción: es un artículo con otro precio y otra condición.

**Decisión.** Se modela como una variante más, con su propio precio y un campo `condition` que la marca como defectuosa.

**Alternativa descartada.** Resolverlo con el descuento manual en la línea de venta. Se descartó porque depende de que quien atiende sepa que esa unidad vale menos y se acuerde de aplicarlo. Con variante propia, el precio está fijado de antemano y el sistema lo cobra solo.

**Por qué `condition` y no un booleano de exclusión.** Acá sí conviene deducir la regla del estado, al revés que en las promociones: "una unidad fallada no entra en promoción" es universal y no hay escenario donde se quiera lo contrario. Además el campo sirve para avisar en pantalla, imprimirlo en la etiqueta y saber cuánto hay inmovilizado en mercadería fallada.

**Detalles operativos.** La variante defectuosa va sin código de barras y se identifica con una etiqueta impresa con su SKU, para que el escaneo no pregunte cuál de las dos es en cada venta. Y se desactiva al llegar a stock cero, para que el catálogo no acumule variantes muertas.

**Límite conocido.** Una variante representa un tipo, no una unidad individual. Si aparecen varias unidades falladas con distintos defectos y distintos precios, cada una necesita su propia variante y el modelo empieza a estirarse.

---

## 4. Ausencia de fila no es lo mismo que cantidad cero

**Problema.** El inventario arranca completamente sin cargar. Un sistema que muestre cero en todo el catálogo estaría mintiendo.

**Decisión.** Si no existe fila en `stock`, esa variante nunca se inventarió. Una fila en cero significa que se contó y no hay.

**Consecuencia.** Se puede cargar el catálogo sin contar nada, y el sistema informa honestamente qué sabe y qué no. Además habilita la consulta "productos sin contar en tal sucursal", que es lo que guía el trabajo de inventariar de a poco.

**Implicancia operativa.** No se crean filas en cero automáticamente al dar de alta un producto.

---

## 4 bis. Hay productos que no llevan inventario

**Problema.** Los caramelos se venden surtidos: nadie anota cinco de un sabor y seis de otro, se anotan diecisiete caramelos y se repone a ojo. Y las fotocopias directamente no existen hasta que alguien las pide, aunque sí exista stock de resmas.

**Decisión.** Un campo `tracks_stock` en `product`. Cuando está en falso, vender no genera movimientos de stock y el producto queda fuera de las pantallas de inventario.

**Por qué no alcanzaba con no cargarles stock.** Sin el campo, "no tiene fila en `stock`" pasaría a significar dos cosas opuestas: que todavía no se inventarió —donde la venta debe descontar y dejar el saldo negativo como señal de recuento— o que no se lleva inventario a propósito, donde no hay que generar nada. El sistema no puede adivinar cuál es.

Es la misma clase de ambigüedad que se resolvió distinguiendo la ausencia de fila de la fila en cero.

**Qué pasaría sin él.** Los caramelos acumularían saldo negativo indefinidamente —miles de unidades en rojo al cabo de un año— y aparecerían para siempre en la lista de productos pendientes de contar.

**Va en `product` y no en la variante**, porque es una política del artículo entero, igual que la alícuota de IVA.

**Sobre las resmas y las fotocopias.** Se dejan desacopladas a propósito: la fotocopia no descuenta una fracción de resma. Las resmas se descuentan cuando se abre una, con un movimiento de consumo interno, que es el mismo mecanismo del papel higiénico y la lavandina. Hacer que cada fotocopia consuma 1/500 de resma sería una precisión que nadie va a sostener.

**Lo que se gana igual.** Aunque no haya stock, sí hay historial de ventas. Eso da la rotación real de los caramelos, que hoy no existe: la reposición deja de ser a ojo y pasa a tener un número atrás.

---

## 5. El stock puede quedar negativo

**Problema.** El inventario va a estar mal durante mucho tiempo. Si el sistema bloquea la venta por falta de stock, le va a decir "no hay" al cajero con el producto en la mano.

**Decisión.** Se permite vender aunque el saldo quede negativo.

**Motivo.** El stock tiene que reflejar la realidad, no impedirla. Un saldo negativo es además la mejor señal disponible de que esa variante necesita recuento.

**Principio general.** El sistema advierte, no bloquea. Se aplica también a los códigos de barras duplicados y a los pagos que exceden el neto de una liquidación.

---

## 6. Saldo más historial, y cuándo vale la pena cachear

**Problema.** Un saldo guardado responde rápido pero no explica nada. Un historial explica todo pero es lento de sumar.

**Decisión.** Se usan las dos cosas: `stock` guarda el saldo y `stock_movement` lo explica. El saldo nunca se escribe directo, siempre a través de un movimiento y en la misma transacción, de modo que se pueda recalcular desde cero.

**El matiz.** El mismo patrón conceptual aparece en tres módulos, pero no se resuelve igual en los tres. En la cuenta corriente de empleados y en el estado de pago de una liquidación **no se cachea el saldo**: son decenas de filas y sumarlas es instantáneo, así que un saldo guardado solo agregaría una oportunidad de desincronización.

El caché se justifica por volumen, no por simetría.

---

## 7. Los documentos cerrados congelan sus valores

**Problema.** Los precios cambian, las tarifas cambian, las promociones se editan. Si los documentos recalculan al abrirse, cada cambio reescribe el pasado.

**Decisión.** Cada documento congela lo que necesita para reconstruirse:

- La línea de venta guarda el precio de lista y la alícuota de IVA vigentes al vender.
- La liquidación guarda las horas, la tarifa y todos los importes.
- El arqueo guarda el total de ventas en efectivo y el total de salidas del día.

**El límite.** Solo se congela **lo que viene de otras tablas**. Lo que se deriva de campos ya congelados de la misma fila no se guarda: el monto esperado de un arqueo y su diferencia se calculan de la apertura, las ventas, las salidas y el conteo, que ya están fijos. Guardarlos además sería redundancia con riesgo de inconsistencia.

**Caso ilustrativo.** Si una venta mal cargada se corrige días después, el arqueo de aquel día sigue mostrando el descuadre que hubo. Eso es correcto: el arqueo detectó un problema real y esa evidencia no debe desaparecer. El total de ventas del mes, en cambio, sí refleja la corrección, porque se calcula desde `sale`. Son dos preguntas distintas con dos respuestas correctas.

---

## 8. La venta está separada del comprobante fiscal

**Problema.** Una venta es un hecho comercial; un comprobante es un documento con reglas de ARCA —numeración correlativa sin huecos, CAE, discriminación de IVA—. Mezclarlos obliga a cargar toda venta con campos fiscales vacíos y ata operaciones internas a la numeración fiscal.

**Decisión.** Son tablas separadas y la relación es de muchas ventas a un comprobante, con la clave foránea en `sale`.

**Por qué muchos a uno.** Cubre los dos escenarios con la misma estructura: un cliente que pide factura es una venta apuntando a un comprobante, y una facturación agrupada son muchas apuntando al mismo. No hay dos caminos en el código.

**Consecuencia.** Un comprobante agrupado puede decir "Varios" y seguir siendo rastreable hasta el último artículo, siguiendo las ventas que lo referencian.

---

## 9. La venta nunca depende de ARCA

**Problema.** El servicio de facturación electrónica se cae. Si la caja depende de que responda, el sistema se vuelve peor que la planilla.

**Decisión.** La venta se cierra, el stock se descuenta y el pago se registra sin esperar autorización. El comprobante se crea en estado pendiente y el CAE se pide de forma asíncrona, con reintentos.

**Consecuencia en el modelo.** `fiscal_document.number` es nullable: no se reserva numeración antes de que ARCA confirme, porque un pedido fallido dejaría un hueco en la correlatividad. El estado y la respuesta cruda del servicio existen para el mismo escenario.

**Lo que se agrega en la aplicación.** Un proceso de reintento y una pantalla de comprobantes pendientes.

---

## 10. Los pagos son una tabla, no un campo

**Problema.** Pagar mitad en efectivo y mitad con tarjeta es lo habitual, no la excepción. Un campo de "medio de pago" obliga a elegir cuál registrar mal.

**Decisión.** `sale_payment` y `payroll_payment`, una fila por cada medio usado.

**Beneficio no buscado.** Habilita el pago parcial sin ningún trabajo extra: una liquidación pagada en dos veces son dos filas con fechas distintas, y el estado se deduce comparando la suma contra el neto.

**Por qué dos tablas y no una compartida.** Una registra plata que entra y otra plata que sale, se relacionan con entidades distintas y van a evolucionar distinto. Unificarlas exigiría una relación polimórfica que complica más de lo que ahorra.

---

## 11. Las promociones agrupan productos explícitamente

**Problema.** El "2 por $7.000" de cuadernos aplica a artículos de distintas marcas y distinto tipo de hoja, que están juntos en la góndola pero son productos separados en el sistema. No hay forma de deducir el grupo desde los datos.

**Decisión.** La promoción tiene una lista explícita de productos o categorías participantes. Quien la crea arma el grupo.

**Alternativa descartada.** Crear categorías como "cuadernos A4 tapa blanda" para que el grupo saliera de la taxonomía. Se descartó porque la jerarquía de categorías se llenaría de nodos que existen solo para sostener promociones.

**Consecuencia en el cálculo.** La promoción no se evalúa por línea sino por grupo: el sistema junta todas las líneas cuyos productos participan, suma las cantidades cruzándolas y recién ahí determina el escalón. Un alfajor de un sabor y otro de otro cuentan como dos unidades.

---

## 12. Gana el mejor precio para el cliente

**Problema.** Un producto puede caer al mismo tiempo en una promoción por cantidad y en una porcentual.

**Decisión.** Se calculan todas las opciones aplicables y se cobra la más barata. No se acumulan.

**Motivo.** Es la regla más fácil de explicar en el mostrador y la única que da un resultado determinista. La excepción es el descuento manual del dueño, que se aplica sobre el mejor precio porque es una orden explícita y no una regla automática.

**El descuento de empleado es un caso aparte, y depende de la promoción.** No compite con las promociones: se aplica encima de la ganadora, pero solo si esa promoción lo permite. Las que existen para incentivar que se lleven más unidades lo admiten; las que liquidan mercadería por vencimiento próximo no, porque ahí el margen ya está resignado.

Por eso la regla no es global sino un booleano en cada promoción. Se descartó deducirla de un "motivo" de la promoción: el día que aparezca una promo por vencimiento que sí deba acumular, esa deducción se rompe. El booleano dice directamente lo que importa.

**Detalle de precisión.** El redondeo se hace sobre el total del grupo promocional, no unidad por unidad, para que el total coincida siempre con lo que dice el cartel.

---

## 13. Las ventas se atribuyen a la sesión de caja, no al vendedor

**Problema.** Varias personas venden alternadamente durante el mismo turno, rotando cada pocos minutos. Pedir identificación en cada venta frenaría el mostrador, y lo más probable es que alguien deje un nombre fijo seleccionado y el dato quede mintiendo todo el día.

**Decisión.** La venta pertenece a la sesión de caja del día y de la sucursal. Para saber quién estaba a determinada hora se cruza con las fichadas de `time_entry`.

**Dónde sí se pide identificación.** En las operaciones sensibles y poco frecuentes: autorizar un descuento, registrar un vale, cargar un ajuste de stock. La fricción se aplica en proporción al riesgo.

**Principio.** Un dato de trazabilidad falso es peor que no tenerlo, porque se le termina creyendo.

---

## 14. El código de barras no lleva restricción de unicidad

**Problema.** En un polirrubro entra mercadería sin código, con códigos repetidos y con códigos apócrifos. Un índice único bloquearía la carga en el peor momento.

**Decisión.** Índice para que el escaneo sea rápido, pero sin unicidad. Los duplicados se advierten en el alta y la persona decide.

**Contraste.** El SKU sí es único, porque lo genera el sistema y no depende de que un proveedor haya hecho las cosas bien. Es también lo que permite imprimir etiquetas propias para la mercadería sin código de fábrica.

---

## 15. Una venta cerrada se anula y se rehace

**Problema.** Una venta ya movió stock, movió caja y puede haberse facturado. Editarla en silencio deja todos esos efectos apuntando a datos viejos.

**Decisión.** La venta original queda marcada como anulada, se crea una nueva que la referencia, y los movimientos correctivos llevan fecha del día en que se corrigen.

**El matiz de la caja.** Si la venta es del día en curso, la corrección afecta la caja normalmente. Si es de un día ya cerrado, no la toca: el descuadre ya fue registrado en el arqueo de aquel día, y volver a aplicarlo contaría el mismo error dos veces.

**Coincidencia con el requisito fiscal.** Del lado de ARCA las facturas no se editan: se emiten notas de crédito. La decisión operativa y la obligación legal apuntan al mismo lugar.

---

## 16. La duplicación se resuelve en la interfaz, no en el modelo

**Problema.** Varias comodidades deseables parecían exigir herencia o valores por defecto en cascada: replicar un precio a doce variantes, excluir del descuento de empleado a todos los cigarrillos, cargar una marca nueva sin salir del alta.

**Decisión.** El modelo guarda un valor concreto por fila, sin herencia. La comodidad se implementa como acciones masivas en la pantalla.

**Motivo.** La herencia con override obliga a que toda lectura se pregunte de dónde sale el valor, y ese costo se paga en cada consulta y para siempre. Una acción de "aplicar a todas" cuesta una pantalla y se paga una sola vez.

---

## 17. El esquema lo versiona Flyway, no Hibernate

**Problema.** Crear las tablas desde las entidades Java (`ddl-auto=update`) es rápido al prototipar y caro después: no hay historial en Git, las restricciones finas se escapan y cada entorno puede terminar con un esquema distinto.

**Decisión.** Primero el SQL en Flyway, después las entidades JPA que mapean esas columnas. `spring.jpa.hibernate.ddl-auto` queda en `none` (más adelante `validate`). Motor: MySQL 8. Stack: Java 21 y Spring Boot 4.1, en `mostrador/backend`.

**Alternativa descartada.** Que Hibernate cree o altere tablas al arrancar. Se descartó porque este modelo vive de unicidades, FKs y `decimal` para plata, y eso tiene que ser explícito y repetible en el servidor.

**Consecuencia.** Un archivo Flyway ya aplicado no se edita: el cambio va en `V2`, `V3`, etc.

---

## Sobre el alcance

Varias cosas se dejaron deliberadamente afuera. Están registradas con su motivo en [pendientes.md](pendientes.md), en la sección de postergaciones. Que no estén no es un olvido: cada una se evaluó y se descartó por no justificar su costo todavía.
