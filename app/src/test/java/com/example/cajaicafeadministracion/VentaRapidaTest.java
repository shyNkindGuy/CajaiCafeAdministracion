package com.example.cajaicafeadministracion;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class VentaRapidaTest {

    private static Venta venta(String cliente, String fecha) {
        Venta v = new Venta();
        v.cliente = cliente;
        v.fecha = fecha;
        return v;
    }

    @Test
    public void sumar_nuncaBajaDeUno() {
        assertEquals(2, VentaRapida.sumar("1", 1));
        assertEquals(4, VentaRapida.sumar(" 5 ", -1));
        assertEquals(1, VentaRapida.sumar("1", -1));
        assertEquals(1, VentaRapida.sumar("", 1));     // vacío cuenta como 0
        assertEquals(1, VentaRapida.sumar("dos", -1));
        assertEquals(1, VentaRapida.sumar("-3", 1));
        assertEquals(1, VentaRapida.sumar(null, 1));
        assertEquals(Integer.MAX_VALUE, VentaRapida.sumar(String.valueOf(Integer.MAX_VALUE), 1));
    }

    @Test
    public void productoInicial_mantieneElElegidoOMarcaElMedioKilo() {
        List<String> ids = Arrays.asList("cuarto_kilo", "grano", "medio_kilo");
        assertEquals("grano", VentaRapida.productoInicial(ids, "grano"));
        assertEquals("medio_kilo", VentaRapida.productoInicial(ids, null));
        assertEquals("medio_kilo", VentaRapida.productoInicial(ids, "borrado"));
        assertEquals("cuarto_kilo", VentaRapida.productoInicial(Arrays.asList("cuarto_kilo", "grano"), null));
        assertNull(VentaRapida.productoInicial(Collections.<String>emptyList(), "grano"));
        assertNull(VentaRapida.productoInicial(null, null));
    }

    @Test
    public void etiquetaChip_conYSinOferta() {
        assertEquals("Bolsa 1/2 kg · S/ 25 (2 × S/ 45)", VentaRapida.etiquetaChip("Bolsa 1/2 kg", 25, 2, 45));
        assertEquals("Bolsa 1/4 kg · S/ 15", VentaRapida.etiquetaChip("Bolsa 1/4 kg", 15, 0, 0));
        // Una "oferta" que no ahorra nada no se muestra.
        assertEquals("Bolsa 1/4 kg · S/ 15", VentaRapida.etiquetaChip("Bolsa 1/4 kg", 15, 2, 30));
        assertEquals("Grano · S/ 22.50", VentaRapida.etiquetaChip("Grano", 22.5, 0, 0));
        assertEquals("Bolsa 1 kg · sin precio", VentaRapida.etiquetaChip("Bolsa 1 kg", 0, 0, 0));
    }

    @Test
    public void montoCorto() {
        assertEquals("S/ 25", VentaRapida.montoCorto(25));
        assertEquals("S/ 22.50", VentaRapida.montoCorto(22.5));
        assertEquals("S/ 45", VentaRapida.montoCorto(44.999999)); // se redondea a céntimos primero
    }

    @Test
    public void clientesFrecuentes_porComprasYLuegoPorLaMasReciente() {
        List<Venta> ventas = Arrays.asList(
                venta("Rosa", "2026-09-01 10:00:00"),
                venta("Ana Pérez", "2026-09-02 10:00:00"),
                venta("rosa ", "2026-09-03 10:00:00"),
                venta("Luis", "2026-09-04 10:00:00"),
                venta("Ana  Pérez", "2026-10-01 10:00:00"),
                venta("ROSA", "2026-08-01 10:00:00"),
                venta("Mario", "2026-10-02 10:00:00"));
        // Rosa 3 compras (la última escrita "rosa"), Ana 2, y con 1 compra Mario (más reciente) antes que Luis.
        assertEquals(Arrays.asList("rosa", "Ana Pérez", "Mario", "Luis"),
                VentaRapida.clientesFrecuentes(ventas, 10));
        assertEquals(Arrays.asList("rosa", "Ana Pérez"), VentaRapida.clientesFrecuentes(ventas, 2));
    }

    @Test
    public void clientesFrecuentes_omiteVaciosYElClientePorDefecto() {
        List<Venta> ventas = Arrays.asList(
                venta("Cliente", "2026-10-01 10:00:00"),
                venta("cliente", "2026-10-02 10:00:00"),
                venta("   ", "2026-10-03 10:00:00"),
                venta(null, "2026-10-04 10:00:00"),
                null,
                venta("Doña Carmen", null));
        assertEquals(Collections.singletonList("Doña Carmen"), VentaRapida.clientesFrecuentes(ventas, 10));
        assertTrue(VentaRapida.clientesFrecuentes(null, 10).isEmpty());
        assertTrue(VentaRapida.clientesFrecuentes(ventas, 0).isEmpty());
    }
}
