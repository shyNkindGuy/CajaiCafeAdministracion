/**
 * Catálogo con los precios de Firebase y el botón que arma el pedido para WhatsApp.
 * La lógica (totales, oferta, mensaje) está en tienda.js, con tests.
 */
import { CONFIG } from "./config.js";
import {
  enlaceWhatsApp, leerCantidadOferta, leerPrecio, lineasPedido, mensajePedido, montoCorto, resumen, sumar,
  textoOferta,
} from "./tienda.js";

// Si Firebase no responde en este tiempo, la bolsa dice "Precio por WhatsApp" (igual se puede pedir).
const ESPERA_MAXIMA_MS = 8000;

const productos = CONFIG.productos.map((p) => ({ ...p, precio: null, ofertaCantidad: 0, ofertaPrecio: null }));
const cantidades = new Map(productos.map((p) => [p.id, 0]));
const tarjetas = new Map(); // id del producto → elementos de su tarjeta

const resumenPedido = document.getElementById("resumenPedido");
const botonPedir = document.getElementById("botonPedir");

function crearTarjetas() {
  const plantilla = document.getElementById("plantillaBolsa");
  const contenedor = document.getElementById("bolsas");
  productos.forEach((p, i) => {
    const nodo = plantilla.content.firstElementChild.cloneNode(true);
    nodo.style.setProperty("--orden", String(i));
    nodo.querySelector(".bolsa-peso").textContent = p.peso;
    nodo.querySelector(".bolsa-nombre").textContent = p.nombre;
    nodo.querySelector(".bolsa-descripcion").textContent = p.descripcion;
    const menos = nodo.querySelector(".menos");
    const mas = nodo.querySelector(".mas");
    menos.setAttribute("aria-label", "Quitar una " + p.nombre.toLowerCase());
    mas.setAttribute("aria-label", "Agregar una " + p.nombre.toLowerCase());
    menos.addEventListener("click", () => cambiar(p.id, -1));
    mas.addEventListener("click", () => cambiar(p.id, 1));
    tarjetas.set(p.id, {
      nodo,
      menos,
      precio: nodo.querySelector(".bolsa-precio"),
      oferta: nodo.querySelector(".bolsa-oferta"),
      cantidad: nodo.querySelector(".cantidad"),
    });
    contenedor.appendChild(nodo);
  });
}

function pintarPrecios(cargando) {
  for (const p of productos) {
    const t = tarjetas.get(p.id);
    t.precio.textContent = cargando ? "Cargando precio…" : (p.precio !== null ? montoCorto(p.precio) : "Precio por WhatsApp");
    t.precio.classList.toggle("cargando", cargando);
    t.precio.classList.toggle("sin-precio", !cargando && p.precio === null);
    const oferta = cargando ? "" : textoOferta(p);
    t.oferta.textContent = oferta;
    t.oferta.hidden = oferta === "";
  }
}

function pintarPedido() {
  const lineas = lineasPedido(productos, cantidades);
  for (const p of productos) {
    const t = tarjetas.get(p.id);
    const n = cantidades.get(p.id);
    t.cantidad.textContent = String(n);
    t.menos.disabled = n === 0;
    t.nodo.classList.toggle("elegida", n > 0);
  }
  resumenPedido.textContent = resumen(lineas) || "Elige tus bolsas arriba.";
  botonPedir.textContent = lineas.length > 0 ? "Pedir por WhatsApp" : "Escríbenos por WhatsApp";
  botonPedir.href = enlaceWhatsApp(CONFIG.whatsapp, mensajePedido(lineas));
}

function cambiar(id, delta) {
  cantidades.set(id, sumar(cantidades.get(id), delta));
  pintarPedido();
  // Un pequeño latido en el número para que se note el cambio.
  const cantidad = tarjetas.get(id).cantidad;
  cantidad.classList.remove("late");
  void cantidad.offsetWidth;
  cantidad.classList.add("late");
}

/** Un campo de productos/{id}; null si no existe, si las reglas no lo dejan leer o si no hay señal. */
async function leerCampo(id, campo, senal) {
  try {
    const url = CONFIG.baseDatos + "/productos/" + encodeURIComponent(id) + "/" + campo + ".json";
    const respuesta = await fetch(url, { signal: senal, cache: "no-store" });
    return respuesta.ok ? await respuesta.json() : null;
  } catch {
    return null;
  }
}

/**
 * Solo se leen precio y oferta: las reglas (database.rules.json) dejan leer esos tres campos sin
 * cuenta, y nada más de productos/ (ni stock ni costos).
 */
async function cargarPrecios() {
  const control = new AbortController();
  const reloj = setTimeout(() => control.abort(), ESPERA_MAXIMA_MS);
  await Promise.all(productos.map(async (p) => {
    const [precio, ofertaCantidad, ofertaPrecio] = await Promise.all(
      ["precio", "ofertaCantidad", "ofertaPrecio"].map((campo) => leerCampo(p.id, campo, control.signal)));
    p.precio = leerPrecio(precio);
    p.ofertaCantidad = leerCantidadOferta(ofertaCantidad);
    p.ofertaPrecio = leerPrecio(ofertaPrecio);
  }));
  clearTimeout(reloj);
  document.getElementById("avisoPrecios").hidden = productos.some((p) => p.precio !== null);
}

crearTarjetas();
pintarPrecios(true);
pintarPedido();
cargarPrecios().then(() => {
  pintarPrecios(false);
  pintarPedido();
});
