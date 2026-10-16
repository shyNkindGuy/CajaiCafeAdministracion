package com.example.cajaicafeadministracion;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ValidacionGastoTest {

    private static final double DELTA = 0.0001;

    @Test
    public void categorias_fijasYEnOrden() {
        assertEquals(Arrays.asList("empaque", "transporte", "publicidad", "servicios", "mantenimiento", "otros"),
                ValidacionGasto.CATEGORIAS);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void categorias_noSePuedenCambiar() {
        ValidacionGasto.CATEGORIAS.add("alquiler");
    }

    @Test
    public void etiquetaCategoria() {
        assertEquals("Transporte", ValidacionGasto.etiquetaCategoria("transporte"));
        assertEquals("Mantenimiento", ValidacionGasto.etiquetaCategoria("mantenimiento"));
        // Algo escrito a mano en la consola se muestra tal cual.
        assertEquals("alquiler", ValidacionGasto.etiquetaCategoria("alquiler"));
        assertEquals("Sin categoría", ValidacionGasto.etiquetaCategoria(null));
        assertEquals("Sin categoría", ValidacionGasto.etiquetaCategoria(""));
    }

    @Test
    public void parsearMonto_puntoOComa() {
        assertEquals(12.5, ValidacionGasto.parsearMonto("12.5"), DELTA);
        assertEquals(12.5, ValidacionGasto.parsearMonto("12,50"), DELTA);
        assertEquals(39.0, ValidacionGasto.parsearMonto(" 39 "), DELTA);
        // Se redondea a céntimos.
        assertEquals(1.18, ValidacionGasto.parsearMonto("1.1799"), DELTA);
    }

    @Test
    public void parsearMonto_ceroSiNoEsNumero() {
        assertEquals(0, ValidacionGasto.parsearMonto(""), DELTA);
        assertEquals(0, ValidacionGasto.parsearMonto("   "), DELTA);
        assertEquals(0, ValidacionGasto.parsearMonto(null), DELTA);
        assertEquals(0, ValidacionGasto.parsearMonto("abc"), DELTA);
        assertEquals(0, ValidacionGasto.parsearMonto("1.2.3"), DELTA);
        assertEquals(0, ValidacionGasto.parsearMonto("NaN"), DELTA);
        assertEquals(0, ValidacionGasto.parsearMonto("Infinity"), DELTA);
    }

    @Test
    public void validar() {
        assertNull(ValidacionGasto.validar("transporte", 12.5));
        assertEquals("Elige una categoría", ValidacionGasto.validar(null, 12.5));
        assertEquals("Elige una categoría", ValidacionGasto.validar("alquiler", 12.5));
        assertEquals("Ingresa un monto mayor a 0", ValidacionGasto.validar("empaque", 0));
        assertEquals("Ingresa un monto mayor a 0", ValidacionGasto.validar("empaque", -5));
        assertEquals("Ingresa un monto mayor a 0", ValidacionGasto.validar("empaque", Double.NaN));
    }

    @Test
    public void etiqueta_conLoteYNota() {
        Gasto g = new Gasto("g1", "2026-10-16 09:30:00", "transporte", 12, " taxi a la tostadora ", "l1", "u1");
        assertEquals("2026-10-16 · Transporte · S/ 12.00 · LOTE1-NOV2025 — taxi a la tostadora",
                ValidacionGasto.etiqueta(g, "LOTE1-NOV2025"));
    }

    @Test
    public void etiqueta_gastoGeneralSinNota() {
        Gasto g = new Gasto("g2", "2026-10-01 18:00:00", "publicidad", 20, "", null, "u1");
        assertEquals("2026-10-01 · Publicidad · S/ 20.00", ValidacionGasto.etiqueta(g, null));
        g.nota = null;
        g.fecha = null;
        assertEquals("Publicidad · S/ 20.00", ValidacionGasto.etiqueta(g, null));
    }
}
