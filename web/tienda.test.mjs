// Tests de la tienda web: `node --test tienda.test.mjs` dentro de web/ (lo corre .github/workflows/web-ci.yml).
import { test } from "node:test";
import assert from "node:assert/strict";
import {
  enlaceWhatsApp, formatear, leerCantidadOferta, leerPrecio, lineasPedido, mensajePedido, montoCorto,
  resumen, sumar, textoOferta, total, totalPedido,
} from "./sitio/tienda.js";

const medio = { id: "medio_kilo", nombre: "Bolsa 1/2 kg", peso: "500 g", precio: 25, ofertaCantidad: 2, ofertaPrecio: 45 };
const cuarto = { id: "cuarto_kilo", nombre: "Bolsa 1/4 kg", peso: "250 g", precio: 15, ofertaCantidad: 0, ofertaPrecio: null };
const sinPrecio = { id: "un_kilo", nombre: "Bolsa 1 kg", peso: "1 kg", precio: null, ofertaCantidad: 0, ofertaPrecio: null };

test("total con la oferta por kilo, igual que PrecioCalculator", () => {
  assert.equal(total(medio, 1), 25);
  assert.equal(total(medio, 2), 45);
  assert.equal(total(medio, 3), 70);
  assert.equal(total(medio, 4), 90);
  assert.equal(total(cuarto, 4), 60); // 1/4 kg no tiene oferta
  assert.equal(total({ precio: 15, ofertaCantidad: 2, ofertaPrecio: 30 }, 2), 30); // oferta que no ahorra: se ignora
  assert.equal(total({ precio: 0.1, ofertaCantidad: 0, ofertaPrecio: null }, 3), 0.3); // redondeo a céntimos
});

test("total es 0 sin precio o sin cantidad", () => {
  assert.equal(total(sinPrecio, 2), 0);
  assert.equal(total(medio, 0), 0);
  assert.equal(total(medio, -1), 0);
});

test("lo que llega de Firebase", () => {
  assert.equal(leerPrecio(25), 25);
  assert.equal(leerPrecio(22.5), 22.5);
  assert.equal(leerPrecio(null), null); // no existe o sin permiso
  assert.equal(leerPrecio("25"), null);
  assert.equal(leerPrecio(0), null);
  assert.equal(leerPrecio(-3), null);
  assert.equal(leerCantidadOferta(2), 2);
  assert.equal(leerCantidadOferta(null), 0);
  assert.equal(leerCantidadOferta(1.5), 0);
});

test("montos", () => {
  assert.equal(formatear(45), "S/ 45.00");
  assert.equal(formatear(22.5), "S/ 22.50");
  assert.equal(montoCorto(25), "S/ 25");
  assert.equal(montoCorto(22.5), "S/ 22.50");
  assert.equal(montoCorto(44.999999), "S/ 45");
});

test("texto de la oferta", () => {
  assert.equal(textoOferta(medio), "2 por S/ 45");
  assert.equal(textoOferta(cuarto), "");
  assert.equal(textoOferta({ ...medio, precio: null }), "");
});

test("stepper entre 0 y 99", () => {
  assert.equal(sumar(0, 1), 1);
  assert.equal(sumar(0, -1), 0);
  assert.equal(sumar(99, 1), 99);
  assert.equal(sumar(undefined, 1), 1);
});

test("pedido, resumen y mensaje", () => {
  const cantidades = new Map([["medio_kilo", 3], ["cuarto_kilo", 1], ["un_kilo", 0]]);
  const lineas = lineasPedido([medio, cuarto, sinPrecio], cantidades);
  assert.deepEqual(lineas.map((l) => [l.producto.id, l.cantidad, l.subtotal]), [["medio_kilo", 3, 70], ["cuarto_kilo", 1, 15]]);
  assert.equal(totalPedido(lineas), 85);
  assert.equal(resumen(lineas), "4 bolsas · S/ 85.00");
  assert.equal(mensajePedido(lineas), [
    "Hola, Cajai Café. Quiero hacer un pedido:",
    "- 3 × Bolsa 1/2 kg (500 g): S/ 70.00",
    "- 1 × Bolsa 1/4 kg (250 g): S/ 15.00",
    "Total: S/ 85.00",
    "",
    "Mi nombre:",
    "Lugar de entrega:",
  ].join("\n"));
});

test("sin precio en Firebase el total se confirma por WhatsApp", () => {
  const lineas = lineasPedido([medio, sinPrecio], new Map([["medio_kilo", 1], ["un_kilo", 1]]));
  assert.equal(totalPedido(lineas), null);
  assert.equal(resumen(lineas), "2 bolsas · total por confirmar");
  const mensaje = mensajePedido(lineas);
  assert.ok(mensaje.includes("- 1 × Bolsa 1 kg (1 kg)\n"));
  assert.ok(mensaje.includes("Total: por confirmar"));
  assert.equal(resumen(lineasPedido([medio], new Map([["medio_kilo", 1]]))), "1 bolsa · S/ 25.00");
});

test("sin bolsas elegidas es una consulta", () => {
  assert.equal(resumen([]), "");
  assert.equal(mensajePedido([]), "Hola, Cajai Café. Quiero información sobre su café.");
});

test("enlace de WhatsApp con el mensaje codificado", () => {
  assert.equal(enlaceWhatsApp("51991538843", "Hola & chau\nS/ 25"),
    "https://wa.me/51991538843?text=Hola%20%26%20chau%0AS%2F%2025");
  assert.equal(enlaceWhatsApp("+51 991 538 843", "x"), "https://wa.me/51991538843?text=x");
});
