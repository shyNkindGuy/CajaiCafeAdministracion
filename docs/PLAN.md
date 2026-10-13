# Plan de trabajo — Cajai Café Administración

Cola de tareas en orden de prioridad. Claude toma **la primera tarea sin marcar y sin PR abierto**,
la implementa en su propia rama y abre un PR en borrador. El dueño revisa y fusiona desde el
celular. Reglas completas en `CLAUDE.md`.

Leyenda: `[ ]` pendiente · `[x]` hecha (se marca dentro del PR que la resuelve)

## Fase 0 — Seguridad y orden (urgente: el repo es público)
- [x] **0.1 Login + reglas de base de datos.** Firebase Auth email/contraseña (2 cuentas, sin
  registro abierto), pantalla de login antes de `MainActivity`, cerrar sesión. Agregar
  `database.rules.json` con `auth != null` en `productos`, `ventas`, `lotes`, `gastos` e
  `.indexOn: ["loteId", "fecha"]` en `ventas`. En el PR, explicar cómo publicar las reglas
  desde la consola de Firebase.
- [x] **0.2 Limpieza.** Borrar `VentaActivity.java` (el layout `activity_venta.xml` sí lo usa
  `VentaFragment`: no borrarlo), quitar `.HistorialFragment` del manifest, sacar `app/debug/app-debug.aab` del repo y agregar
  `*.aab`/`*.apk` al `.gitignore`.
- [ ] **0.3 Listeners y ciclo de vida.** Quitar cada `ValueEventListener` en `onDestroyView`/
  `onDestroy`; no usar `requireContext()` dentro de callbacks que pueden llegar después de salir.

## Fase 1 — Ventas correctas
- [ ] **1.1 Precio único de verdad.** Mover el cálculo a una clase pura `PrecioCalculator`
  (con tests), usar `Producto.precio` de Firebase, borrar el hardcode, recalcular al cambiar de
  producto y guardar el total como `double` (no parseando el `TextView`).
- [ ] **1.2 Venta ligada a lote.** Selector de lote abierto en la venta (sugerir el más antiguo
  con stock — FIFO), guardar `loteId` y descontar `stockBolsas12/14` del lote con
  `runTransaction`. Con esto `LoteDetalleActivity` empieza a mostrar ventas reales.
- [ ] **1.3 Borrar venta devuelve stock.** Transacción inversa sobre producto y lote.

## Fase 2 — Finanzas: ¿gano o pierdo?
- [ ] **2.1 Costo unitario por lote.** Clase pura `CosteoLote` (con tests):
  costo café por kg tostado = (pergamino + pilado + tostado + flete + electricidad) / kg tostado;
  costo por bolsa = costo kg × peso + bolsa + sticker. Mostrarlo en el detalle del lote.
- [ ] **2.2 Gastos generales.** Nodo `gastos/{id}` (fecha, categoría, monto, nota, loteId
  opcional, uid) + pantalla "Registrar gasto" con categorías fijas (empaque, transporte,
  publicidad, servicios, mantenimiento, otros).
- [ ] **2.3 Pestaña Inicio (resumen del mes).** Clase pura `ResumenFinanciero` (con tests):
  ingresos, cobrado, por cobrar, costo de lo vendido, gastos, utilidad neta y margen; selector
  de mes; tarjeta por lote con punto de equilibrio ("faltan N bolsas para recuperar la
  inversión").
- [ ] **2.4 Cuentas por cobrar.** Lista de ventas `pendiente`/`parcial` con saldo y botón de
  recordatorio por WhatsApp.

## Fase 3 — Reportes
- [ ] **3.1 PDF por lote o por mes** guardado con `MediaStore` (Android 10+) y botón compartir.
- [ ] **3.2 Exportar CSV** (abre en Excel/Google Sheets) desde el dispositivo, sin Cloud Functions.
- [ ] **3.3 Alerta de stock bajo** por lote (umbral configurable).

## Fase 4 — Rediseño UI/UX
- [ ] **4.1 Tema Material 3 con colores de marca** (café/crema), modo oscuro, tipografía.
- [ ] **4.2 Venta rápida**: chips de producto con precio, stepper de cantidad, cliente con
  autocompletado de clientes frecuentes.
- [ ] **4.3 Navegación final**: Inicio · Vender · Lotes · Movimientos · Más.

## Fase 5 — Web (después, producto aparte)
- [ ] **5.1 cajaicafe.me en Cloudflare Pages**: catálogo, precios y botón de pedido por WhatsApp.
