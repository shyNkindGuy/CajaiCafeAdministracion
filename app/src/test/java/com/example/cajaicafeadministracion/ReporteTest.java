package com.example.cajaicafeadministracion;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ReporteTest {

    private static Lote lote1() {
        Lote l = new Lote("l1", "LOTE1-NOV2025", "2025-11-17");
        l.pergaminoKg = 26.5;
        l.pergaminoTotal = 397.50;
        l.piladoKg = 22;
        l.piladoTotal = 37;
        l.tostadoKg = 18;
        l.tostadoTotal = 54;
        l.flete = 39;
        l.empaqueTotal = 178;
        l.totalGastos = 705.50;
        l.stockBolsas12 = 6;
        l.stockBolsas14 = 2;
        return l;
    }

    private static Venta venta(String fecha, String cliente, int cantidad, double total, String estado,
                               double aCuenta, String loteId) {
        Venta v = new Venta("v", fecha, "Bolsa 1/2 kg", cliente, 25, cantidad, estado, aCuenta, total, loteId);
        v.productoId = "medio_kilo";
        return v;
    }

    private static Map<String, Lote> lotes() {
        Map<String, Lote> m = new HashMap<>();
        m.put("l1", lote1());
        return m;
    }

    private static Map<String, String> catalogo() {
        Map<String, String> m = new HashMap<>();
        m.put("medio_kilo", "Bolsa 1/2 kg");
        return m;
    }

    /** Textos de todas las líneas, columnas unidas por " | ", para buscar en ellas. */
    private static List<String> textos(Reporte r) {
        List<String> t = new ArrayList<>();
        for (Reporte.Linea l : r.lineas) t.add(l.tipo + ": " + String.join(" | ", l.columnas));
        return t;
    }

    @Test
    public void delMes_resumenVentasYGastos() {
        List<Venta> ventas = Arrays.asList(
                venta("2026-10-10 11:00:00", "Ana", 2, 45, "pagado", 0, "l1"),
                venta("2026-10-03 10:00:00", "Cliente", 1, 25, "parcial", 10, "l1"),
                venta("2026-09-30 10:00:00", "Beto", 4, 90, "pagado", 0, "l1"));
        List<Gasto> gastos = Arrays.asList(
                new Gasto("g1", "2026-10-05 08:00:00", "transporte", 12.5, " taxi ", null, "u1"),
                new Gasto("g2", "2026-10-20 08:00:00", "empaque", 59, null, "l1", "u1"),
                new Gasto("g3", "2026-09-28 08:00:00", "publicidad", 20, null, null, "u1"));

        Reporte r = Reporte.delMes("2026-10", ventas, gastos, lotes(), catalogo(), "18/10/2026 23:10");

        assertEquals("Reporte de Octubre 2026", r.titulo);
        assertEquals("CajaiCafe_Reporte_2026-10.pdf", r.nombreArchivo);
        List<String> t = textos(r);
        assertEquals("TEXTO: " + Reporte.AVISO, t.get(0));
        assertEquals("TEXTO: Generado: 18/10/2026 23:10", t.get(1));
        assertTrue(t.contains("FILA: Ventas registradas (2) | S/ 70.00"));
        assertTrue(t.contains("FILA: Cobrado | S/ 55.00"));
        assertTrue(t.contains("FILA: Por cobrar | S/ 15.00"));
        assertTrue(t.contains("FILA: Costo del café vendido | S/ 43.95")); // 14.65 × 3
        assertTrue(t.contains("FILA: Gastos del mes | S/ 71.50"));
        assertTrue(t.contains("TOTAL: Utilidad neta | S/ -45.45"));
        assertTrue(t.contains("SECCION: Ventas (2)"));
        // Ordenadas por fecha; "Cliente" (sin nombre) sale como "—".
        int i = t.indexOf("SECCION: Ventas (2)");
        assertEquals("ENCABEZADO: Fecha | Cliente | Producto | Cant. | Total | Estado", t.get(i + 1));
        assertEquals("FILA: 03/10 | — | Bolsa 1/2 kg | 1 | S/ 25.00 | parcial", t.get(i + 2));
        assertEquals("FILA: 10/10 | Ana | Bolsa 1/2 kg | 2 | S/ 45.00 | pagado", t.get(i + 3));
        assertEquals("TOTAL:  |  |  | Total | S/ 70.00 | ", t.get(i + 4));
        int j = t.indexOf("SECCION: Gastos (2)");
        assertEquals("FILA: 05/10 | Transporte | taxi |  | S/ 12.50", t.get(j + 2));
        assertEquals("FILA: 20/10 | Empaque |  | LOTE1-NOV2025 | S/ 59.00", t.get(j + 3));
        assertEquals("TOTAL:  |  |  | Total | S/ 71.50", t.get(j + 4));
    }

    @Test
    public void delMes_sinMovimientos() {
        Reporte r = Reporte.delMes("2026-11", Collections.<Venta>emptyList(), Collections.<Gasto>emptyList(),
                lotes(), catalogo(), null);
        List<String> t = textos(r);
        assertEquals("TEXTO: " + Reporte.AVISO, t.get(0));
        assertTrue(t.contains("SECCION: Ventas (0)"));
        assertTrue(t.contains("TEXTO: Sin ventas en este mes."));
        assertTrue(t.contains("TEXTO: Sin gastos registrados en este mes."));
        assertTrue(t.contains("TEXTO: Margen: —"));
    }

    @Test
    public void delLote_costeoVentasEquilibrioYStock() {
        List<Venta> ventas = Arrays.asList(
                venta("2025-11-20 10:00:00", "Ana", 24, 600, "pagado", 0, "l1"),
                venta("2025-11-21 10:00:00", "Beto", 2, 45, "pendiente", 0, "l1"),
                venta("2025-11-22 10:00:00", "Otro", 3, 75, "pagado", 0, "l2"));
        List<Gasto> gastos = Arrays.asList(
                new Gasto("g1", "2025-11-25 10:00:00", "transporte", 14.50, "flete extra", "l1", "u1"),
                new Gasto("g2", "2025-11-25 10:00:00", "publicidad", 50, null, null, "u1"));

        Reporte r = Reporte.delLote(lote1(), ventas, gastos, 22.5, "18/10/2026 23:10");

        assertEquals("Reporte del lote LOTE1-NOV2025", r.titulo);
        assertEquals("CajaiCafe_Lote_LOTE1-NOV2025.pdf", r.nombreArchivo);
        List<String> t = textos(r);
        assertTrue(t.contains("TEXTO: Inicio: 2025-11-17 · Estado: abierto"));
        assertTrue(t.contains("FILA: Café pergamino (26.50 kg) | S/ 397.50"));
        assertTrue(t.contains("TOTAL: Total del costal | S/ 705.50"));
        assertTrue(t.contains("FILA: Café por kg tostado | S/ 29.31"));
        // Solo el gasto con este lote; en el reporte de un lote la columna Lote queda vacía.
        assertTrue(t.contains("SECCION: Gastos (1)"));
        assertTrue(t.contains("FILA: 25/11 | Transporte | flete extra |  | S/ 14.50"));
        // Solo las ventas de este lote.
        assertTrue(t.contains("SECCION: Ventas (2)"));
        assertTrue(t.contains("TOTAL:  |  |  | Total | S/ 645.00 | "));
        assertTrue(t.contains("FILA: Cobrado | S/ 600.00"));
        assertTrue(t.contains("FILA: Por cobrar | S/ 45.00"));
        // 705.50 + 14.50 = 720 invertidos contra 645 vendidos.
        assertTrue(t.contains("TEXTO: LOTE1-NOV2025: faltan S/ 75.00 de S/ 720.00 → unas 4 bolsas de 1/2 kg (a S/ 22.50)"));
        assertTrue(t.contains("FILA: Bolsas de 1/2 kg | 6"));
        assertTrue(t.contains("FILA: Bolsas de 1/4 kg | 2"));
    }

    @Test
    public void lineasDeTabla_anchosSuman1YCuadranConColumnas() {
        Reporte r = Reporte.delLote(lote1(),
                Collections.singletonList(venta("2025-11-20 10:00:00", "Ana", 1, 25, "pagado", 0, "l1")),
                Collections.singletonList(new Gasto("g1", "2025-11-25", "otros", 5, null, "l1", "u1")), 22.5, null);
        for (Reporte.Linea l : r.lineas) {
            if (l.tipo == Reporte.Linea.Tipo.SECCION || l.tipo == Reporte.Linea.Tipo.TEXTO) {
                assertNull(l.anchos);
                assertEquals(1, l.columnas.length);
                continue;
            }
            assertEquals(l.columnas.length, l.anchos.length);
            float suma = 0;
            for (float a : l.anchos) suma += a;
            assertEquals(1f, suma, 0.001f);
        }
    }

    @Test
    public void nombreDeArchivoSeguro() {
        assertEquals("LOTE_2_DIC_2025", Reporte.seguro("LOTE 2/DIC 2025"));
        assertEquals("Lote_caf_", Reporte.seguro("Lote café"));
        assertEquals("sin_nombre", Reporte.seguro("  "));
        assertEquals("sin_nombre", Reporte.seguro(null));
    }
}
