package com.example.cajaicafeadministracion;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CosteoLoteTest {

    private static final double DELTA = 0.0001;

    /** Lote 1 real (nov-2025): pergamino 397.50, pilado 37, tostado 18 kg × 3 = 54, flete 39. */
    private static Lote lote1() {
        Lote l = new Lote("l1", "LOTE1-NOV2025", "2025-11-17");
        l.pergaminoTotal = 397.50;
        l.piladoTotal = 37;
        l.tostadoKg = 18;
        l.tostadoTotal = 54;
        l.flete = 39;
        l.empaqueTotal = 178; // no entra al costo por kg
        return l;
    }

    @Test
    public void costoCafe_sumaTodoMenosEmpaque() {
        assertEquals(527.50, CosteoLote.costoCafe(lote1()), DELTA);
        Lote conLuz = lote1();
        conLuz.electricidadTotal = 4.50;
        assertEquals(532.00, CosteoLote.costoCafe(conLuz), DELTA);
        assertEquals(0, CosteoLote.costoCafe(null), DELTA);
    }

    @Test
    public void costoPorKg_entreKgTostado() {
        // 527.50 / 18 = 29.3055…
        assertEquals(29.31, CosteoLote.costoPorKg(lote1()), DELTA);
    }

    @Test
    public void sinKgTostadoNoSeCalcula() {
        Lote l = lote1();
        l.tostadoKg = 0;
        assertFalse(CosteoLote.calculable(l));
        assertFalse(CosteoLote.calculable(null));
        assertTrue(CosteoLote.calculable(lote1()));
        assertEquals(0, CosteoLote.costoPorKg(l), DELTA);
        assertEquals(0, CosteoLote.costoPorBolsa(l, "medio_kilo", 1.18, 0.60), DELTA);
    }

    @Test
    public void pesoKg_porIdDeProducto() {
        assertEquals(0.5, CosteoLote.pesoKg("medio_kilo"), DELTA);
        assertEquals(0.25, CosteoLote.pesoKg("cuarto_kilo"), DELTA);
        assertEquals(0, CosteoLote.pesoKg("un_kilo"), DELTA);
        assertEquals(0, CosteoLote.pesoKg(null), DELTA);
    }

    @Test
    public void costoPorBolsa_cafePorPesoMasBolsaYSticker() {
        // 1/2 kg: 14.65 de café + 1.18 + 0.60
        assertEquals(14.65, CosteoLote.costoCafePorBolsa(lote1(), "medio_kilo"), DELTA);
        assertEquals(16.43, CosteoLote.costoPorBolsa(lote1(), "medio_kilo", 1.18, 0.60), DELTA);
        // 1/4 kg: 7.33 de café + 1.18 + 0.60
        assertEquals(7.33, CosteoLote.costoCafePorBolsa(lote1(), "cuarto_kilo"), DELTA);
        assertEquals(9.11, CosteoLote.costoPorBolsa(lote1(), "cuarto_kilo", 1.18, 0.60), DELTA);
        // Sin costos de empaque configurados: solo el café.
        assertEquals(14.65, CosteoLote.costoPorBolsa(lote1(), "medio_kilo", 0, 0), DELTA);
        // 1 kg y grano no salen de un lote.
        assertEquals(0, CosteoLote.costoPorBolsa(lote1(), "un_kilo", 1.18, 0.60), DELTA);
    }

    @Test
    public void detalleBolsa() {
        assertEquals("Bolsa 1/2 kg: S/ 16.43 (café S/ 14.65 + bolsa S/ 1.18 + sticker S/ 0.60)",
                CosteoLote.detalleBolsa("Bolsa 1/2 kg", lote1(), "medio_kilo", 1.18, 0.60));
        assertEquals("Bolsa 1/4 kg: S/ 7.33 (café S/ 7.33; falta el costo de bolsa y sticker en productos/cuarto_kilo)",
                CosteoLote.detalleBolsa("Bolsa 1/4 kg", lote1(), "cuarto_kilo", 0, 0));
        Lote sinKg = lote1();
        sinKg.tostadoKg = 0;
        assertEquals("Bolsa 1/2 kg: falta el kg tostado del lote",
                CosteoLote.detalleBolsa("Bolsa 1/2 kg", sinKg, "medio_kilo", 1.18, 0.60));
    }
}
