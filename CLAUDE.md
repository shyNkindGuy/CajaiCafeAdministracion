# Cajai Café Administración

App Android interna para administrar la venta de bolsas de café molido de **Cajai Café**
(negocio familiar, 2 usuarios). Compra café pergamino → pilado y tostado por servicio externo →
molido con moledora propia → empacado en bolsas de 1/2 kg y 1/4 kg → venta.

## Stack
- Android, **Java 11**, `minSdk 24`, `targetSdk 35`, AGP 8.8 / Gradle 8.10.2 (JDK 17 para compilar).
- **Firebase Realtime Database** (no Firestore — decisión tomada, no migrar sin razón).
- `versionCode`/`versionName` se calculan con `git rev-list --count HEAD` (`app/build.gradle.kts`).
- Package: `com.example.cajaicafeadministracion` (todo en un solo paquete, sin capas todavía).

## Acceso
`LogoActivity` (splash) → `LoginActivity` si no hay sesión → `MainActivity`. Login con correo y
contraseña de **Firebase Auth**, sin registro en la app: las cuentas se crean en la consola y solo
entran las que estén en `usuarios/{uid} = true`. Cerrar sesión está al final de la pestaña Reportes.
Las reglas de la base de datos viven en `database.rules.json` (se publican a mano en la consola o
con `firebase deploy --only database`); cada nodo nuevo necesita su entrada ahí o quedará bloqueado.

## Pantallas (bottom nav en `MainActivity`)
| Tab | Clase | Qué hace |
|---|---|---|
| Inicio (se abre al entrar) | `InicioFragment` | Resumen del mes elegido (`ResumenFinanciero`, con tests) y punto de equilibrio de cada lote abierto |
| Venta | `VentaFragment` | Registra una venta y descuenta el stock (del lote o del producto, ver Stock) con `runTransaction` |
| Historial | `HistorialFragment` + `VentaAdapter` | Lista ventas, editar estado de pago, borrar (devuelve el stock) |
| Lotes | `LotesFragment` → `NuevoLoteActivity`, `LoteDetalleActivity` | Costeo por costal y costo unitario (`CosteoLote`, con tests), stock por lote, cerrar lote |
| Reportes | `DashboardFragment` → `GastoActivity` | Totales de bolsas, total cobrado, PDF interno; "Registrar gasto" (categorías fijas y validación en `ValidacionGasto`, con tests) |

Precios: el total de una venta se calcula solo en `PrecioCalculator` (con tests). Nada de precios
escritos en el código: todo se configura en `productos/{id}` en Firebase. Regla del negocio: la bolsa
de 1/2 kg vale S/25, pero cada kilo completo (2 bolsas) sale a S/45 → oferta `ofertaCantidad: 2,
ofertaPrecio: 45`. La de 1/4 kg vale S/15 y no tiene oferta (4 cuartos = S/60). Con oferta,
`precioUnitario × cantidad` puede ser mayor que `total`; el que vale es `total`.

Costo unitario (`CosteoLote`): costo por kg tostado = (pergamino + pilado + tostado + flete +
electricidad) / `tostadoKg` (kg que salen del tueste); costo por bolsa = costo kg × peso (0.5 o
0.25 kg) + `costoBolsa` + `costoSticker` del producto. El `empaqueTotal` manual del lote no entra en
el costo unitario (sí en `totalGastos`).

Resumen del mes (`ResumenFinanciero`, por prefijo `yyyy-MM` de `fecha`): ingresos = Σ `total`;
cobrado = pagado → total, parcial → `montoParcial`; costo de lo vendido = solo el **café** de cada
bolsa (`CosteoLote.costoCafePorBolsa` × cantidad; 1 kg, grano y ventas sin lote quedan "sin costo");
gastos = todo `gastos/` del mes (el empaque entra ahí al comprarse, por eso no va en el costo de lo
vendido); utilidad neta = ingresos − costo − gastos. Punto de equilibrio de un lote: invertido =
`totalGastos` + gastos con su `loteId`, contra todas sus ventas; las bolsas que faltan se cuentan a
precio efectivo de 1/2 kg con oferta (S/22.50).

Stock: las bolsas de 1/2 y 1/4 kg salen del lote (`lotes/{id}/stockBolsas12|14`, se suman con
"Registrar empacado" en el detalle del lote); 1 kg y grano siguen usando `productos/{id}/stock`. El
mapeo es por id de producto (`medio_kilo`, `cuarto_kilo`) en `StockLote` (con tests). La venta sugiere
el lote abierto más antiguo con bolsas (FIFO), respeta el que se elija a mano y guarda su `loteId`.
Borrar una venta devuelve las bolsas a donde salieron (`StockLote.rutaDevolucion`) en una sola
escritura atómica (`updateChildren` con `ServerValue.increment`).

Convención: todo `addValueEventListener` guarda el listener en un campo y se quita en
`onDestroyView` (fragments) u `onDestroy` (activities). En callbacks asíncronos no usar
`requireContext()`: usar el contexto de la app capturado antes, y `getView() != null` antes de tocar vistas.

## Nodos de Firebase
- `productos/{id}`: `nombre, precio, stock`, y opcional `ofertaCantidad, ofertaPrecio` y `costoBolsa, costoSticker` (empaque por unidad, para el costo por bolsa del lote) (catálogo global)
- `ventas/{id}`: `fecha, producto, cliente, precioUnitario, cantidad, estadoPago (pagado|parcial|pendiente), montoParcial, total, loteId, productoId`
- `lotes/{id}`: costos (pergamino/pilado/tostado kg×precio, flete, electricidad kW×h×S//kWh, empaque), `totalGastos`, `stockBolsas12`, `stockBolsas14`, `estado (abierto|cerrado)`
- `usuarios/{uid}`: `true` para cada cuenta autorizada (solo se edita desde la consola)
- `gastos/{id}`: `fecha, categoria (empaque|transporte|publicidad|servicios|mantenimiento|otros), monto, nota, loteId (opcional), uid` — gastos que no son del costeo de un costal. Las reglas exigen `monto > 0` y `uid` = quien escribe; índice en `fecha` y `loteId`

## Deuda conocida (no re-descubrir; ver `docs/PLAN.md` para el orden de trabajo)
- Las ventas anteriores a 1.2 no tienen `loteId` y las anteriores a 1.3 no tienen `productoId` (al borrarlas se busca el producto por nombre).
- `DashboardFragment`: el campo "Lote" no filtra nada; el PDF escribe directo en Descargas públicas (falla con scoped storage en Android 10+); no es comprobante SUNAT.
- El repo es **público** y `google-services.json` está commiteado: cualquiera puede crear una cuenta
  de Firebase Auth con esa API key. Por eso las reglas piden estar en `usuarios/`, no solo `auth != null`.

## Comandos
```bash
./gradlew assembleDebug        # APK en app/build/outputs/apk/debug/
./gradlew testDebugUnitTest
```
El contenedor en la nube de Claude no tiene Android SDK (dl.google.com bloqueado): la validación
real es el workflow `.github/workflows/android-ci.yml` en cada PR.

## Reglas de trabajo para Claude (modo autónomo)
1. Una tarea de `docs/PLAN.md` = una rama `claude/<slug>` = un PR en **borrador** contra `master`.
2. Nunca hacer push a `master`. Nunca hacer merge: el dueño aprueba y fusiona desde el celular.
3. Antes de empezar, saltar tareas que ya tengan un PR abierto. Si ya hay **2 PRs de Claude
   abiertos** sin fusionar, no empezar nada nuevo: terminar el turno (ahorra consumo y evita
   conflictos entre PRs apilados).
4. Al terminar: marcar la tarea como `[x]` en `docs/PLAN.md` dentro del mismo PR, y dejar en la
   descripción del PR cómo probarlo en el celular (pasos concretos).
5. Commits y textos de UI en español. Mantener Java (no migrar a Kotlin sin pedirlo).
6. No tocar `google-services.json` ni datos reales de Firebase.
