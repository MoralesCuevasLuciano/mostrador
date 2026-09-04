# Frontend de Mostrador

React 19 + TypeScript + Vite 8. CSS propio, sin router ni librería de UI.

## Pantallas

- **Productos** — listado activo o dados de baja; desde acá se gestionan marcas y categorías. Las variantes dadas de baja no se muestran en la tarjeta.
- **Cargar / editar producto** — ficha + variantes, foto, “Lleva inventario”. En edición, un botón arriba a la derecha abre un popup para reactivar variantes dadas de baja.
- **Inventario** — según la sucursal de la barra: pendientes de contar y ya contados. Recuento, entrada, consumo interno, extravío, traslado e historial.

La sucursal de trabajo vive en la Nav (arriba a la derecha) y se guarda en `localStorage` (`mostrador.branchId`). No filtra el catálogo.

## Cómo levantarlo

El proxy de Vite manda `/api` y `/uploads` a `http://127.0.0.1:8080`. El backend tiene que estar corriendo.

```powershell
npm install
npm run dev
```

UI en [http://localhost:5173](http://localhost:5173).

```powershell
npm run build
```

Typecheck (`tsc -b`) y bundle de producción.

## Organización

```
src/
├── components/   tarjetas, formulario, nav, modales
├── hooks/        sucursal de trabajo, escáner de barras
├── mappers/      API ↔ draft del formulario
├── models/       tipos TypeScript
├── pages/        listados y formulario
├── services/     llamadas a /api
└── utils/        plata, texto, etiquetas de stock
```

Los comentarios de propósito van en español (`/** … */`).
