package com.example.cajaicafeadministracion;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PrecioCalculatorTest {

    private static final double DELTA = 0.0001;

    @Test
    public void total_esPrecioPorCantidad() {
        assertEquals(75.0, PrecioCalculator.total(25, 3), DELTA);   // 3 bolsas de 1/2 kg
        assertEquals(60.0, PrecioCalculator.total(12, 5), DELTA);   // 5 bolsas de 1/4 kg
        assertEquals(37.5, PrecioCalculator.total(12.5, 3), DELTA);
    }

    @Test
    public void total_seRedondeaACentimos() {
        // 0.1 × 3 da 0.30000000000000004 en double
        assertEquals(0.3, PrecioCalculator.total(0.1, 3), DELTA);
        assertEquals(41.97, PrecioCalculator.total(13.99, 3), DELTA);
    }

    @Test
    public void total_esCeroSiFaltaPrecioOCantidad() {
        assertEquals(0, PrecioCalculator.total(0, 3), DELTA);
        assertEquals(0, PrecioCalculator.total(-5, 2), DELTA);
        assertEquals(0, PrecioCalculator.total(Double.NaN, 2), DELTA);
        assertEquals(0, PrecioCalculator.total(25, 0), DELTA);
        assertEquals(0, PrecioCalculator.total(25, -1), DELTA);
    }

    @Test
    public void oferta_medioKilo_elKiloA45() {
        // Bolsa 1/2 kg a S/25 con oferta 2 x S/45: cada kilo completo sale a 45.
        assertEquals(25.0, PrecioCalculator.total(25, 1, 2, 45), DELTA);
        assertEquals(45.0, PrecioCalculator.total(25, 2, 2, 45), DELTA);
        assertEquals(70.0, PrecioCalculator.total(25, 3, 2, 45), DELTA);
        assertEquals(90.0, PrecioCalculator.total(25, 4, 2, 45), DELTA);
        assertEquals(115.0, PrecioCalculator.total(25, 5, 2, 45), DELTA);
    }

    @Test
    public void sinOferta_cuartoKilo_noBaja() {
        // Bolsa 1/4 kg a S/15 sin oferta: 4 cuartos son 60, no 45.
        assertEquals(60.0, PrecioCalculator.total(15, 4, 0, 0), DELTA);
        assertEquals(15.0, PrecioCalculator.total(15, 1, 0, 0), DELTA);
    }

    @Test
    public void ofertaInvalida_seIgnora() {
        // Más cara que comprarlas sueltas, de 1 unidad o sin precio: se cobra el precio normal.
        assertEquals(50.0, PrecioCalculator.total(25, 2, 2, 60), DELTA);
        assertEquals(50.0, PrecioCalculator.total(25, 2, 1, 20), DELTA);
        assertEquals(50.0, PrecioCalculator.total(25, 2, 2, 0), DELTA);
        assertFalse(PrecioCalculator.ofertaValida(25, 2, 50));
        assertTrue(PrecioCalculator.ofertaValida(25, 2, 45));
    }

    @Test
    public void oferta_sinPrecioOCantidadEsCero() {
        assertEquals(0, PrecioCalculator.total(0, 2, 2, 45), DELTA);
        assertEquals(0, PrecioCalculator.total(25, 0, 2, 45), DELTA);
    }

    @Test
    public void etiqueta() {
        assertEquals("Bolsa 1/2 kg (S/ 25.00 · 2 x S/ 45.00)",
                PrecioCalculator.etiqueta("Bolsa 1/2 kg", 25, 2, 45));
        assertEquals("Bolsa 1/4 kg (S/ 15.00)",
                PrecioCalculator.etiqueta("Bolsa 1/4 kg", 15, 0, 0));
    }

    @Test
    public void precioValido() {
        assertTrue(PrecioCalculator.precioValido(25));
        assertTrue(PrecioCalculator.precioValido(0.5));
        assertFalse(PrecioCalculator.precioValido(0));
        assertFalse(PrecioCalculator.precioValido(-1));
        assertFalse(PrecioCalculator.precioValido(Double.NaN));
        assertFalse(PrecioCalculator.precioValido(Double.POSITIVE_INFINITY));
    }

    @Test
    public void parsearCantidad() {
        assertEquals(3, PrecioCalculator.parsearCantidad("3"));
        assertEquals(4, PrecioCalculator.parsearCantidad(" 4 "));
        assertEquals(0, PrecioCalculator.parsearCantidad(""));
        assertEquals(0, PrecioCalculator.parsearCantidad(null));
        assertEquals(0, PrecioCalculator.parsearCantidad("abc"));
        assertEquals(0, PrecioCalculator.parsearCantidad("2.5"));
    }

    @Test
    public void formatear() {
        assertEquals("S/ 25.00", PrecioCalculator.formatear(25));
        assertEquals("S/ 37.50", PrecioCalculator.formatear(37.5));
        assertEquals("S/ 0.00", PrecioCalculator.formatear(0));
    }
}
