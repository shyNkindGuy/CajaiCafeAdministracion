package com.example.cajaicafeadministracion;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ResumenFinancieroTest {

    private static final double DELTA = 0.0001;

    /** Lote 1 real (nov-2025): S/ 527.50 de café para 18 kg tostado + S/ 178 de empaque manual. */
    private static Lote lote1() {
        Lote l = new Lote("l1", "LOTE1-NOV2025", "2025-11-17");
        l.pergaminoTotal = 397.50;
        l.piladoTotal = 37;
        l.tostadoKg = 18;
        l.tostadoTotal = 54;
        l.flete = 39;
        l.empaqueTotal = 178;
        l.totalGastos = 705.50;
        return l;
    }

    private static Venta venta(String fecha, String productoId, String nombre, int cantidad, double total,
                               String estado, double aCuenta, String loteId) {
        Venta v = new Venta("v", fecha, nombre, "Cliente", 0, cantidad, estado, aCuenta, total, loteId);
        v.productoId = productoId;
        return v;
    }

    private static Gasto gasto(String fecha, String categoria, double monto, String loteId) {
        return new Gasto("g", fecha, categoria, monto, null, loteId, "u1");
    }

    private static Map<String, Lote> lotes() {
        Map<String, Lote> m = new HashMap<>();
        m.put("l1", lote1());
        return m;
    }

    private static Map<String, String> catalogo() {
        Map<String, String> m = new HashMap<>();
        m.put("medio_kilo", "Bolsa 1/2 kg");
        m.put("cuarto_kilo", "Bolsa 1/4 kg");
        m.put("un_kilo", "Bolsa 1 kg");
        return m;
    }

    @Test
    public void delMes_sumaSoloElMesElegido() {
        List<Venta> ventas = Arrays.asList(
                // 2 bolsas de 1/2 kg con oferta: café 14.65 × 2
                venta("2026-10-03 10:00:00", "medio_kilo", "Bolsa 1/2 kg", 2, 45, "pagado", 0, "l1"),
                // 1/4 kg con S/ 5 a cuenta: café 7.33
                venta("2026-10-10 11:00:00", "cuarto_kilo", "Bolsa 1/4 kg", 1, 15, "parcial", 5, "l1"),
                // 1 kg no tiene peso de lote: sin costo
                venta("2026-10-15 12:00:00", "un_kilo", "Bolsa 1 kg", 1, 45, "pendiente", 0, "l1"),
                // Venta antigua: sin productoId (se busca por nombre) pero con lote → café 14.65
                venta("2026-10-02 09:00:00", null, "Bolsa 1/2 kg", 1, 25, "pagado", 0, "l1"),
                // Anterior a los lotes: sin costo
                venta("2026-10-01 08:00:00", null, "Bolsa 1/2 kg", 1, 25, "pagado", 0, null),
                // Otro mes: no entra
                venta("2026-09-30 23:59:59", "medio_kilo", "Bolsa 1/2 kg", 4, 90, "pagado", 0, "l1"));
        List<Gasto> gastos = Arrays.asList(
                gasto("2026-10-05 08:00:00", "transporte", 12.5, null),
                gasto("2026-10-20 08:00:00", "empaque", 59, "l1"),
                gasto("2026-09-28 08:00:00", "publicidad", 20, null));

        ResumenFinanciero r = ResumenFinanciero.delMes("2026-10", ventas, gastos, lotes(), catalogo());

        assertEquals(5, r.ventas);
        assertEquals(155.00, r.ingresos, DELTA);
        assertEquals(100.00, r.cobrado, DELTA);
        assertEquals(55.00, r.porCobrar, DELTA);
        assertEquals(51.28, r.costoVendido, DELTA); // 29.30 + 7.33 + 14.65
        assertEquals(2, r.ventasSinCosto);
        assertEquals(71.50, r.gastos, DELTA);
        assertEquals(32.22, r.utilidadNeta, DELTA); // 155 - 51.28 - 71.50
        assertEquals(20.8, r.margen(), DELTA);      // 32.22 / 155
    }

    @Test
    public void delMes_sinMovimientos() {
        ResumenFinanciero r = ResumenFinanciero.delMes("2026-11", Collections.<Venta>emptyList(),
                Collections.<Gasto>emptyList(), lotes(), catalogo());
        assertEquals(0, r.ventas);
        assertEquals(0, r.ingresos, DELTA);
        assertEquals(0, r.utilidadNeta, DELTA);
        assertTrue(Double.isNaN(r.margen()));
    }

    @Test
    public void delMes_soloGastosDaPerdida() {
        ResumenFinanciero r = ResumenFinanciero.delMes("2026-10", Collections.<Venta>emptyList(),
                Collections.singletonList(gasto("2026-10-05 08:00:00", "servicios", 30, null)), lotes(), catalogo());
        assertEquals(-30.00, r.utilidadNeta, DELTA);
        assertTrue(Double.isNaN(r.margen()));
    }

    @Test
    public void cobradoDe() {
        assertEquals(45, ResumenFinanciero.cobradoDe(venta("2026-10-01", "medio_kilo", "", 2, 45, "pagado", 0, null)), DELTA);
        assertEquals(10, ResumenFinanciero.cobradoDe(venta("2026-10-01", "medio_kilo", "", 2, 45, "parcial", 10, null)), DELTA);
        assertEquals(0, ResumenFinanciero.cobradoDe(venta("2026-10-01", "medio_kilo", "", 2, 45, "pendiente", 0, null)), DELTA);
        // Estado vacío (ventas muy antiguas) cuenta como no cobrado.
        assertEquals(0, ResumenFinanciero.cobradoDe(venta("2026-10-01", "medio_kilo", "", 2, 45, null, 0, null)), DELTA);
        // A cuenta mayor al total (dato mal escrito): no se cobra más que el total.
        assertEquals(45, ResumenFinanciero.cobradoDe(venta("2026-10-01", "medio_kilo", "", 2, 45, "parcial", 60, null)), DELTA);
    }

    @Test
    public void costoCafeDeVenta_sinKgTostadoNoSeCostea() {
        Map<String, Lote> m = lotes();
        m.get("l1").tostadoKg = 0;
        Venta v = venta("2026-10-03 10:00:00", "medio_kilo", "Bolsa 1/2 kg", 2, 45, "pagado", 0, "l1");
        assertEquals(-1, ResumenFinanciero.costoCafeDeVenta(v, m, catalogo()), DELTA);
        // Lote borrado: tampoco.
        Venta otra = venta("2026-10-03 10:00:00", "medio_kilo", "Bolsa 1/2 kg", 2, 45, "pagado", 0, "l9");
        assertEquals(-1, ResumenFinanciero.costoCafeDeVenta(otra, lotes(), catalogo()), DELTA);
    }

    @Test
    public void mesDe() {
        assertEquals("2026-10", ResumenFinanciero.mesDe("2026-10-16 17:50:00"));
        assertEquals("2026-10", ResumenFinanciero.mesDe("2026-10"));
        assertNull(ResumenFinanciero.mesDe("16/10/2026"));
        assertNull(ResumenFinanciero.mesDe("2026-13-01"));
        assertNull(ResumenFinanciero.mesDe(""));
        assertNull(ResumenFinanciero.mesDe(null));
    }

    @Test
    public void meses_delMasRecienteSinRepetirYConElActual() {
        List<String> m = ResumenFinanciero.meses(Arrays.asList(
                "2025-11-20 10:00:00", "2026-10-01 10:00:00", "2025-11-21 10:00:00", null, "basura"), "2026-12");
        assertEquals(Arrays.asList("2026-12", "2026-10", "2025-11"), m);
        assertEquals(Collections.singletonList("2026-10"),
                ResumenFinanciero.meses(Collections.<String>emptyList(), "2026-10"));
    }

    @Test
    public void etiquetaMes() {
        assertEquals("Octubre 2026", ResumenFinanciero.etiquetaMes("2026-10"));
        assertEquals("Enero 2027", ResumenFinanciero.etiquetaMes("2027-01"));
        assertEquals("Diciembre 2025", ResumenFinanciero.etiquetaMes("2025-12"));
        assertEquals("otra-cosa", ResumenFinanciero.etiquetaMes("otra-cosa"));
        assertEquals("", ResumenFinanciero.etiquetaMes(null));
    }

    @Test
    public void textoMargen() {
        assertEquals("Margen: 20.8 %", ResumenFinanciero.textoMargen(20.8));
        assertEquals("Margen: -12.0 %", ResumenFinanciero.textoMargen(-12));
        assertEquals("Margen: —", ResumenFinanciero.textoMargen(Double.NaN));
    }

    @Test
    public void precioEfectivo_conYSinOferta() {
        Producto medio = new Producto("medio_kilo", "Bolsa 1/2 kg", 25, 0);
        medio.ofertaCantidad = 2;
        medio.ofertaPrecio = 45;
        assertEquals(22.50, ResumenFinanciero.precioEfectivo(medio), DELTA);
        assertEquals(15.00, ResumenFinanciero.precioEfectivo(new Producto("cuarto_kilo", "Bolsa 1/4 kg", 15, 0)), DELTA);
        assertEquals(0, ResumenFinanciero.precioEfectivo(new Producto("x", "Sin precio", 0, 0)), DELTA);
        assertEquals(0, ResumenFinanciero.precioEfectivo(null), DELTA);
    }

    @Test
    public void equilibrio_lote1Recuperado() {
        // 27 bolsas de 1/2 kg × 25 + 5 de 1/4 kg × 12 = 735 contra 705.50 invertidos.
        List<Venta> ventas = Arrays.asList(
                venta("2025-11-20 10:00:00", "medio_kilo", "Bolsa 1/2 kg", 27, 675, "pagado", 0, "l1"),
                venta("2025-11-21 10:00:00", "cuarto_kilo", "Bolsa 1/4 kg", 5, 60, "pendiente", 0, "l1"),
                venta("2025-11-21 10:00:00", "medio_kilo", "Bolsa 1/2 kg", 3, 75, "pagado", 0, "l2"));
        ResumenFinanciero.Equilibrio e = ResumenFinanciero.equilibrio(lote1(), ventas, Collections.<Gasto>emptyList(), 22.5);
        assertTrue(e.recuperado());
        assertEquals(705.50, e.invertido, DELTA);
        assertEquals(735.00, e.vendido, DELTA);
        assertEquals(0, e.falta, DELTA);
        assertEquals(0, e.bolsasFaltan);
        assertEquals("LOTE1-NOV2025: inversión recuperada (+S/ 29.50 sobre S/ 705.50)",
                ResumenFinanciero.textoEquilibrio("LOTE1-NOV2025", e, 22.5));
    }

    @Test
    public void equilibrio_faltanBolsasConGastosDelLote() {
        List<Venta> ventas = Collections.singletonList(
                venta("2025-11-20 10:00:00", "medio_kilo", "Bolsa 1/2 kg", 24, 600, "pagado", 0, "l1"));
        // Un flete extra cargado al lote y un gasto general que no cuenta para el lote.
        List<Gasto> gastos = Arrays.asList(
                gasto("2025-11-25 10:00:00", "transporte", 14.50, "l1"),
                gasto("2025-11-25 10:00:00", "publicidad", 50, null));
        ResumenFinanciero.Equilibrio e = ResumenFinanciero.equilibrio(lote1(), ventas, gastos, 22.5);
        assertFalse(e.recuperado());
        assertEquals(720.00, e.invertido, DELTA);
        assertEquals(120.00, e.falta, DELTA);
        assertEquals(6, e.bolsasFaltan); // 120 / 22.50 = 5.33 → 6
        assertEquals("LOTE1-NOV2025: faltan S/ 120.00 de S/ 720.00 → unas 6 bolsas de 1/2 kg (a S/ 22.50)",
                ResumenFinanciero.textoEquilibrio("LOTE1-NOV2025", e, 22.5));
    }

    @Test
    public void equilibrio_justoUnaBolsaYSinPrecio() {
        List<Venta> ventas = Collections.singletonList(
                venta("2025-11-20 10:00:00", "medio_kilo", "Bolsa 1/2 kg", 1, 683, "pagado", 0, "l1"));
        ResumenFinanciero.Equilibrio e = ResumenFinanciero.equilibrio(lote1(), ventas, Collections.<Gasto>emptyList(), 22.5);
        assertEquals(22.50, e.falta, DELTA);
        assertEquals(1, e.bolsasFaltan);
        assertEquals("LOTE1-NOV2025: faltan S/ 22.50 de S/ 705.50 → una bolsa de 1/2 kg (a S/ 22.50)",
                ResumenFinanciero.textoEquilibrio("LOTE1-NOV2025", e, 22.5));

        ResumenFinanciero.Equilibrio sinPrecio = ResumenFinanciero.equilibrio(lote1(),
                Collections.<Venta>emptyList(), Collections.<Gasto>emptyList(), 0);
        assertEquals(-1, sinPrecio.bolsasFaltan);
        assertEquals("LOTE1-NOV2025: faltan S/ 705.50 de S/ 705.50",
                ResumenFinanciero.textoEquilibrio("LOTE1-NOV2025", sinPrecio, 0));
    }
}
