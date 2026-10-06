# Cajai Café Administración

App Android interna para administrar la venta de bolsas de café molido de **Cajai Café**
(negocio familiar, 2 usuarios). Compra café pergamino → pilado y tostado por servicio externo →
molido con moledora propia → empacado en bolsas de 1/2 kg y 1/4 kg → venta.

## Stack
- Android, **Java 11**, `minSdk 24`, `targetSdk 35`, AGP 8.8 / Gradle 8.10.2 (JDK 17 para compilar).
- **Firebase Realtime Database** (no Firestore — decisión tomada, no migrar sin razón).
- `versionCode`/`versionName` se calculan con `git rev-list --count HEAD` (`app/build.gradle.kts`).
- Package: `com.example.cajaicafeadministracion` (todo en un solo paquete, sin capas todavía).

## Pantallas (bottom nav en `MainActivity`)
| Tab | Clase | Qué hace |
|---|---|---|
| Venta | `VentaFragment` | Registra una venta y descuenta `productos/{id}/stock` con `runTransaction` |
| Historial | `HistorialFragment` + `VentaAdapter` | Lista ventas, editar estado de pago, borrar |
| Lotes | `LotesFragment` → `NuevoLoteActivity`, `LoteDetalleActivity` | Costeo por costal, stock por lote, cerrar lote |
| Reportes | `DashboardFragment` | Totales de bolsas, total cobrado, PDF interno |

`VentaActivity` es código muerto (duplicado de `VentaFragment`, no está en la navegación).

## Nodos de Firebase
- `productos/{id}`: `nombre, precio, stock` (catálogo global)
- `ventas/{id}`: `fecha, producto, cliente, precioUnitario, cantidad, estadoPago (pagado|parcial|pendiente), montoParcial, total, loteId`
- `lotes/{id}`: costos (pergamino/pilado/tostado kg×precio, flete, electricidad kW×h×S//kWh, empaque), `totalGastos`, `stockBolsas12`, `stockBolsas14`, `estado (abierto|cerrado)`
- Pendiente de crear: `gastosGenerales/` (gastos que no son de un costal: alquiler, mantenimiento, etc.)

## Deuda conocida (no re-descubrir; ver `docs/PLAN.md` para el orden de trabajo)
- `VentaFragment.calcularTotal()` usa precios **hardcodeados** (1/2 kg = 25, 1/4 kg = 15, 1 kg = 45) e ignora `Producto.precio`; además guarda `precioUnitario = p.precio`, así que `precioUnitario × cantidad ≠ total`.
- Cambiar de producto en el spinner **no recalcula** el total; `registrarVenta()` lee el total parseando el texto del `TextView`.
- `VentaFragment` **no asigna `loteId`** ni descuenta el stock del lote → el detalle de lote siempre muestra S/ 0 en ventas.
- `DashboardFragment`: el campo "Lote" no filtra nada; el PDF escribe directo en Descargas públicas (falla con scoped storage en Android 10+); no es comprobante SUNAT.
- Ningún `ValueEventListener` se quita en `onDestroyView` (se acumulan al cambiar de tab).
- No hay Firebase Auth en uso aunque la dependencia está. El repo es **público** y `google-services.json` está commiteado → las reglas de la base de datos DEBEN exigir `auth != null`.
- `AndroidManifest.xml` declara `.HistorialFragment` como `<activity>` (es un Fragment).
- `app/debug/app-debug.aab` (10 MB) está versionado.

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
