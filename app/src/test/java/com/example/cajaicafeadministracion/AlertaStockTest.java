package com.example.cajaicafeadministracion;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AlertaStockTest {

    private static Lote lote(String id, String codigo, String fecha, String estado, int s12, int s14) {
        Lote l = new Lote(id, codigo, fecha);
        l.estado = estado;
        l.stockBolsas12 = s12;
        l.stockBolsas14 = s14;
        return l;
    }

    @Test
    public void umbral_porDefectoConfiguradoOApagado() {
        assertEquals(5, AlertaStock.umbral(null));
        Producto p = new Producto("medio_kilo", "Bolsa 1/2 kg", 25, 0);
        assertEquals(5, AlertaStock.umbral(p)); // sin stockMinimo en Firebase
        p.stockMinimo = 8;
        assertEquals(8, AlertaStock.umbral(p));
        p.stockMinimo = 0;
        assertEquals(0, AlertaStock.umbral(p)); // aviso apagado
        p.stockMinimo = -3;
        assertEquals(0, AlertaStock.umbral(p));
    }

    @Test
    public void bajo_incluyeElUmbralYRespetaElApagado() {
        assertTrue(AlertaStock.bajo(5, 5));
        assertTrue(AlertaStock.bajo(0, 5));
        assertFalse(AlertaStock.bajo(6, 5));
        assertFalse(AlertaStock.bajo(0, 0));
    }

    @Test
    public void tieneAlerta_soloLotesAbiertos() {
        assertTrue(AlertaStock.tieneAlerta(lote("l1", "L1", "2026-01-01", "abierto", 3, 20), 5, 5));
        assertTrue(AlertaStock.tieneAlerta(lote("l1", "L1", "2026-01-01", "abierto", 20, 0), 5, 5));
        assertFalse(AlertaStock.tieneAlerta(lote("l1", "L1", "2026-01-01", "abierto", 20, 0), 5, 0));
        assertFalse(AlertaStock.tieneAlerta(lote("l1", "L1", "2026-01-01", "cerrado", 0, 0), 5, 5));
        assertFalse(AlertaStock.tieneAlerta(null, 5, 5));
    }

    @Test
    public void avisos_delMasAntiguoYPorTamano() {
        List<Lote> lotes = Arrays.asList(
                lote("l2", "LOTE2-OCT2026", "2026-10-01", "abierto", 30, 4),
                lote("l1", "LOTE1-NOV2025", "2025-11-17", "abierto", 1, 0),
                lote("l0", "LOTE0", "2025-01-01", "cerrado", 0, 0),
                lote("l3", "LOTE3", "2026-10-10", "abierto", 6, 6));
        assertEquals(Arrays.asList(
                "LOTE1-NOV2025: queda 1 bolsa de 1/2 kg",
                "LOTE1-NOV2025: se acabaron las bolsas de 1/4 kg",
                "LOTE2-OCT2026: quedan 4 bolsas de 1/4 kg"),
                AlertaStock.avisos(lotes, 5, 5));
        // Con el aviso de 1/4 kg apagado solo queda el de 1/2 kg.
        assertEquals(Collections.singletonList("LOTE1-NOV2025: queda 1 bolsa de 1/2 kg"),
                AlertaStock.avisos(lotes, 5, 0));
        assertTrue(AlertaStock.avisos(Collections.<Lote>emptyList(), 5, 5).isEmpty());
    }

    @Test
    public void textoUmbrales() {
        assertEquals("Aviso con: 1/2 kg ≤ 5 · 1/4 kg ≤ 3", AlertaStock.textoUmbrales(5, 3));
        assertEquals("Aviso con: 1/2 kg ≤ 10 · 1/4 kg sin aviso", AlertaStock.textoUmbrales(10, 0));
    }

    @Test
    public void parsearUmbral() {
        assertEquals(5, AlertaStock.parsearUmbral(" 5 "));
        assertEquals(0, AlertaStock.parsearUmbral("0"));
        assertEquals(-1, AlertaStock.parsearUmbral("-2"));
        assertEquals(-1, AlertaStock.parsearUmbral("dos"));
        assertEquals(-1, AlertaStock.parsearUmbral(""));
        assertEquals(-1, AlertaStock.parsearUmbral(null));
    }
}
