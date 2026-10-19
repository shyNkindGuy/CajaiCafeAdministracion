# cajaicafe.me — tienda web (tarea 5.1)

Página estática: catálogo de bolsas con los precios de Firebase y un botón que arma el pedido y lo
abre en WhatsApp (991 538 843). No hay servidor ni paso de build: Cloudflare Pages publica la carpeta
`web/sitio` tal cual.

```
web/
  sitio/            ← lo que se publica
    index.html      página
    estilos.css     estilos (pergamino, bolsas en negro mate, Alegreya)
    app.js          lee los precios de Firebase y maneja el pedido
    tienda.js       lógica pura: total con oferta, mensaje y enlace de WhatsApp (con tests)
    config.js       número de WhatsApp, URL de la base y bolsas que se muestran  ← se edita aquí
    _headers        cabeceras de seguridad de Cloudflare Pages (CSP)
    img/logo.png    logo del dueño (el mismo de la app, achicado)
  tienda.test.mjs   tests: `cd web && node --test tienda.test.mjs`
```

## Precios
Salen de `productos/{id}/precio`, `ofertaCantidad` y `ofertaPrecio` en Firebase, los mismos que usa la
app: si cambias un precio en la consola, la web lo muestra al recargar. Para que la web pueda leerlos
sin cuenta, `database.rules.json` deja leer **solo esos tres campos** (nada más de `productos/`: ni
stock ni costos). Las reglas se publican a mano en la consola de Firebase → Realtime Database → Reglas.
Si no se pueden leer (reglas sin publicar, sin señal), cada bolsa dice "Precio por WhatsApp" y el
pedido igual se puede enviar con el total "por confirmar".

## Publicar en Cloudflare Pages (una sola vez)
1. Cloudflare → Workers & Pages → Create → Pages → **Connect to Git** → este repositorio.
2. Production branch: `master`. Framework preset: **None**. Build command: *(vacío)*.
   Build output directory: **`web/sitio`**.
3. Cada PR tiene su vista previa en `https://<rama>.<proyecto>.pages.dev`; lo que se fusiona a
   `master` se publica en producción.
4. Custom domains → agregar `cajaicafe.me` (y `www.cajaicafe.me` si se quiere).

## Probar en la computadora
`python3 -m http.server -d web/sitio 8000` y abrir http://localhost:8000 (las cabeceras de `_headers`
solo las aplica Cloudflare).
