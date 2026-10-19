/**
 * Lógica de la tienda sin DOM, para probarla con `node --test` (web/tienda.test.mjs).
 *
 * El total sigue la misma regla que la app (PrecioCalculator.java): cada grupo completo de
 * `ofertaCantidad` bolsas cuesta `ofertaPrecio` y las sueltas van a `precio`. Los precios no se
 * escriben aquí: llegan de Firebase (productos/{id}) y los pone app.js en cada producto.
 */

/** Un precio sirve si es un número positivo y finito. */
export function precioValido(precio) {
  return typeof precio === "number" && Number.isFinite(precio) && precio > 0;
}

/** La oferta solo aplica si agrupa 2 o más bolsas y sale más barata que comprarlas sueltas. */
export function ofertaValida(precio, ofertaCantidad, ofertaPrecio) {
  return Number.isInteger(ofertaCantidad) && ofertaCantidad >= 2
    && precioValido(ofertaPrecio) && ofertaPrecio < precio * ofertaCantidad;
}

/** Lo que devuelve Firebase para un precio: el número si sirve, o null (no existe, sin permiso, texto). */
export function leerPrecio(valor) {
  return precioValido(valor) ? valor : null;
}

/** Lo que devuelve Firebase para ofertaCantidad: un entero, o 0 si no hay oferta. */
export function leerCantidadOferta(valor) {
  return Number.isInteger(valor) && valor > 0 ? valor : 0;
}

export function redondear(monto) {
  return Math.round(monto * 100) / 100;
}

/**
 * Total de `cantidad` bolsas de un producto ({precio, ofertaCantidad, ofertaPrecio}).
 * Ej. 1/2 kg a S/25 con oferta 2 × S/45: 1 → 25, 2 → 45, 3 → 70. Sin precio, 0.
 */
export function total(producto, cantidad) {
  const { precio, ofertaCantidad, ofertaPrecio } = producto;
  if (!precioValido(precio) || !(cantidad > 0)) return 0;
  if (!ofertaValida(precio, ofertaCantidad, ofertaPrecio)) return redondear(precio * cantidad);
  const grupos = Math.floor(cantidad / ofertaCantidad);
  const sueltas = cantidad % ofertaCantidad;
  return redondear(grupos * ofertaPrecio + sueltas * precio);
}

/** "S/ 25.00", como en la app. */
export function formatear(monto) {
  return "S/ " + redondear(monto).toFixed(2);
}

/** "S/ 25" si el monto es entero, "S/ 22.50" si tiene céntimos (para la tarjeta de cada bolsa). */
export function montoCorto(monto) {
  const r = redondear(monto);
  return Number.isInteger(r) ? "S/ " + r : formatear(r);
}

/** Ej. "2 por S/ 45", o "" si el producto no tiene una oferta que ahorre algo. */
export function textoOferta(producto) {
  const { precio, ofertaCantidad, ofertaPrecio } = producto;
  if (!precioValido(precio) || !ofertaValida(precio, ofertaCantidad, ofertaPrecio)) return "";
  return ofertaCantidad + " por " + montoCorto(ofertaPrecio);
}

/** Cantidad después de tocar − o +: entre 0 y 99. */
export function sumar(cantidad, delta) {
  const n = Number.isInteger(cantidad) ? cantidad : 0;
  return Math.min(99, Math.max(0, n + delta));
}

/** Las bolsas elegidas (cantidad > 0), en el orden del catálogo, con su subtotal (0 si no hay precio). */
export function lineasPedido(productos, cantidades) {
  const lineas = [];
  for (const producto of productos) {
    const cantidad = cantidades.get(producto.id) || 0;
    if (cantidad > 0) lineas.push({ producto, cantidad, subtotal: total(producto, cantidad) });
  }
  return lineas;
}

/** Suma de las líneas, o null si alguna no tiene precio (el total se confirma por WhatsApp). */
export function totalPedido(lineas) {
  let suma = 0;
  for (const l of lineas) {
    if (!(l.subtotal > 0)) return null;
    suma += l.subtotal;
  }
  return redondear(suma);
}

/** Ej. "3 bolsas · S/ 70.00", "1 bolsa · total por confirmar", o "" si no se eligió nada. */
export function resumen(lineas) {
  if (lineas.length === 0) return "";
  const bolsas = lineas.reduce((n, l) => n + l.cantidad, 0);
  const total = totalPedido(lineas);
  return (bolsas === 1 ? "1 bolsa" : bolsas + " bolsas") + " · "
    + (total === null ? "total por confirmar" : formatear(total));
}

/** Mensaje que se abre en WhatsApp: el pedido con precios, o una consulta si no se eligió nada. */
export function mensajePedido(lineas) {
  if (lineas.length === 0) return "Hola, Cajai Café. Quiero información sobre su café.";
  const partes = ["Hola, Cajai Café. Quiero hacer un pedido:"];
  for (const l of lineas) {
    const bolsa = l.cantidad + " × " + l.producto.nombre + " (" + l.producto.peso + ")";
    partes.push("- " + bolsa + (l.subtotal > 0 ? ": " + formatear(l.subtotal) : ""));
  }
  const total = totalPedido(lineas);
  partes.push("Total: " + (total === null ? "por confirmar" : formatear(total)));
  partes.push("", "Mi nombre:", "Lugar de entrega:");
  return partes.join("\n");
}

/** Enlace que abre el chat con el número (con código de país, sin "+") y el mensaje ya escrito. */
export function enlaceWhatsApp(numero, mensaje) {
  return "https://wa.me/" + String(numero).replace(/\D/g, "") + "?text=" + encodeURIComponent(mensaje);
}
