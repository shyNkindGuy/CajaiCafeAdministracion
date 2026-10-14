package com.example.cajaicafeadministracion;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class StockLoteTest {

    private static Lote lote(String id, String fecha, String estado, int s12, int s14) {
        Lote l = new Lote(id, id.toUpperCase(), fecha);
        l.estado = estado;
        l.stockBolsas12 = s12;
        l.stockBolsas14 = s14;
        return l;
    }

    @Test
    public void campoStock_porIdDeProducto() {
        assertEquals("stockBolsas12", StockLote.campoStock("medio_kilo"));
        assertEquals("stockBolsas14", StockLote.campoStock("cuarto_kilo"));
        assertNull(StockLote.campoStock("un_kilo"));
        assertNull(StockLote.campoStock("grano"));
        assertNull(StockLote.campoStock(null));
    }

    @Test
    public void stockDe() {
        Lote l = lote("l1", "2025-11-17", "abierto", 6, 2);
        assertEquals(6, StockLote.stockDe(l, StockLote.CAMPO_MEDIO_KILO));
        assertEquals(2, StockLote.stockDe(l, StockLote.CAMPO_CUARTO_KILO));
        assertEquals(0, StockLote.stockDe(l, null));
        assertEquals(0, StockLote.stockDe(null, StockLote.CAMPO_MEDIO_KILO));
    }

    @Test
    public void abiertosFifo_sinCerradosYDelMasAntiguo() {
        Lote nuevo = lote("l3", "2026-10-01", "abierto", 10, 4);
        Lote cerrado = lote("l1", "2025-11-17", "cerrado", 0, 0);
        Lote viejo = lote("l2", "2026-02-10", null, 3, 0); // sin estado cuenta como abierto
        List<Lote> fifo = StockLote.abiertosFifo(Arrays.asList(nuevo, cerrado, viejo));
        assertEquals(2, fifo.size());
        assertEquals("l2", fifo.get(0).id);
        assertEquals("l3", fifo.get(1).id);
    }

    @Test
    public void sugerido_elMasAntiguoConStockDelProducto() {
        Lote viejoSinMedio = lote("l1", "2026-01-01", "abierto", 0, 5);
        Lote nuevoConMedio = lote("l2", "2026-03-01", "abierto", 8, 0);
        List<Lote> fifo = Arrays.asList(viejoSinMedio, nuevoConMedio);
        assertEquals(1, StockLote.sugerido(fifo, StockLote.CAMPO_MEDIO_KILO));
        assertEquals(0, StockLote.sugerido(fifo, StockLote.CAMPO_CUARTO_KILO));
    }

    @Test
    public void sugerido_sinStockOSinCampoEsElMasAntiguo() {
        List<Lote> fifo = Arrays.asList(
                lote("l1", "2026-01-01", "abierto", 0, 0),
                lote("l2", "2026-03-01", "abierto", 0, 0));
        assertEquals(0, StockLote.sugerido(fifo, StockLote.CAMPO_MEDIO_KILO));
        assertEquals(0, StockLote.sugerido(fifo, null));
        assertEquals(-1, StockLote.sugerido(Collections.emptyList(), StockLote.CAMPO_MEDIO_KILO));
    }

    @Test
    public void elegido_respetaElLoteElegidoMientrasTengaBolsas() {
        List<Lote> fifo = Arrays.asList(
                lote("l1", "2026-01-01", "abierto", 4, 0),
                lote("l2", "2026-03-01", "abierto", 8, 0));
        assertEquals(1, StockLote.elegido(fifo, StockLote.CAMPO_MEDIO_KILO, "l2"));
        // 1 kg y grano no usan stock de lote: el elegido se mantiene igual.
        assertEquals(1, StockLote.elegido(fifo, null, "l2"));
        // Sin elección a mano: FIFO.
        assertEquals(0, StockLote.elegido(fifo, StockLote.CAMPO_MEDIO_KILO, null));
    }

    @Test
    public void elegido_vuelveAFifoSiElElegidoNoSirve() {
        List<Lote> fifo = Arrays.asList(
                lote("l1", "2026-01-01", "abierto", 4, 3),
                lote("l2", "2026-03-01", "abierto", 8, 0));
        // l2 no tiene bolsas de 1/4 kg.
        assertEquals(0, StockLote.elegido(fifo, StockLote.CAMPO_CUARTO_KILO, "l2"));
        // El elegido ya no está entre los abiertos (se cerró).
        assertEquals(0, StockLote.elegido(fifo, StockLote.CAMPO_MEDIO_KILO, "l9"));
        assertEquals(-1, StockLote.elegido(Collections.emptyList(), StockLote.CAMPO_MEDIO_KILO, "l2"));
    }

    @Test
    public void etiqueta() {
        Lote l = lote("l1", "2025-11-17", "abierto", 6, 2);
        l.codigo = "LOTE1-NOV2025";
        assertEquals("LOTE1-NOV2025 · 1/2 kg: 6 · 1/4 kg: 2", StockLote.etiqueta(l));
    }
}
