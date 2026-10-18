package com.example.cajaicafeadministracion;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ExportacionCsvTest {

    private static Venta venta(String fecha, String cliente, int cantidad, double total, String estado,
                               double aCuenta, String loteId) {
        return new Venta("v", fecha, "Bolsa 1/2 kg", cliente, 25, cantidad, estado, aCuenta, total, loteId);
    }

    private static Map<String, Lote> lotes() {
        Map<String, Lote> m = new HashMap<>();
        m.put("l1", new Lote("l1", "LOTE1-NOV2025", "2025-11-17"));
        return m;
    }

    @Test
    public void campo_comillasComasSaltosYFormulas() {
        assertEquals("Ana", ExportacionCsv.campo("Ana"));
        assertEquals("", ExportacionCsv.campo(null));
        assertEquals("\"Pérez, Ana\"", ExportacionCsv.campo("Pérez, Ana"));
        assertEquals("\"dijo \"\"fiado\"\"\"", ExportacionCsv.campo("dijo \"fiado\""));
        assertEquals("\"línea 1\nlínea 2\"", ExportacionCsv.campo("línea 1\nlínea 2"));
        // Un nombre o nota que empieza como fórmula no se ejecuta en Excel.
        assertEquals("'=1+1", ExportacionCsv.campo("=1+1"));
        assertEquals("'+51 999", ExportacionCsv.campo("+51 999"));
        assertEquals("'-taxi", ExportacionCsv.campo("-taxi"));
        assertEquals("'@cajai", ExportacionCsv.campo("@cajai"));
        assertEquals("\"'=SUMA(A1,A2)\"", ExportacionCsv.campo("=SUMA(A1,A2)"));
    }

    @Test
    public void numero_conPuntoYDosDecimales() {
        assertEquals("25.00", ExportacionCsv.numero(25));
        assertEquals("22.50", ExportacionCsv.numero(22.5));
        assertEquals("0.30", ExportacionCsv.numero(0.1 + 0.2));
        assertEquals("-45.45", ExportacionCsv.numero(-45.45));
    }

    @Test
    public void ventas_encabezadoYFilas() {
        List<Venta> ventas = Arrays.asList(
                venta("2026-10-03 10:00:00", "Pérez, Ana", 2, 45, "parcial", 25, "l1"),
                venta("2026-10-04 11:00:00", "Beto", 1, 25, "pendiente", 0, null),
                venta("2026-10-05 12:00:00", "Carla", 1, 25, "pagado", 0, "l9"));
        String csv = ExportacionCsv.ventas(ventas, lotes());
        assertTrue(csv.startsWith(ExportacionCsv.BOM));
        String[] lineas = csv.substring(1).split("\r\n");
        assertEquals("Fecha,Cliente,Producto,Cantidad,Precio unitario,Total,Estado,Pagado,Saldo,Lote", lineas[0]);
        assertEquals("2026-10-03 10:00:00,\"Pérez, Ana\",Bolsa 1/2 kg,2,25.00,45.00,parcial,25.00,20.00,LOTE1-NOV2025", lineas[1]);
        assertEquals("2026-10-04 11:00:00,Beto,Bolsa 1/2 kg,1,25.00,25.00,pendiente,0.00,25.00,", lineas[2]);
        // Lote que ya no existe.
        assertEquals("2026-10-05 12:00:00,Carla,Bolsa 1/2 kg,1,25.00,25.00,pagado,25.00,0.00,(borrado)", lineas[3]);
        assertEquals(4, lineas.length);
        assertTrue(csv.endsWith("\r\n"));
    }

    @Test
    public void gastos_encabezadoYFilas() {
        List<Gasto> gastos = Arrays.asList(
                new Gasto("g1", "2026-10-05 08:00:00", "transporte", 12.5, "taxi, ida y vuelta", null, "u1"),
                new Gasto("g2", "2026-10-20 08:00:00", "empaque", 59, null, "l1", "u1"));
        String[] lineas = ExportacionCsv.gastos(gastos, lotes()).substring(1).split("\r\n");
        assertEquals("Fecha,Categoría,Monto,Nota,Lote", lineas[0]);
        assertEquals("2026-10-05 08:00:00,Transporte,12.50,\"taxi, ida y vuelta\",", lineas[1]);
        assertEquals("2026-10-20 08:00:00,Empaque,59.00,,LOTE1-NOV2025", lineas[2]);
    }

    @Test
    public void sinFilas_soloEncabezado() {
        assertEquals(ExportacionCsv.BOM + "Fecha,Categoría,Monto,Nota,Lote\r\n",
                ExportacionCsv.gastos(Collections.<Gasto>emptyList(), lotes()));
    }

    @Test
    public void filtrarPorMesOPorLote_ordenadoPorFecha() {
        List<Venta> ventas = Arrays.asList(
                venta("2026-10-10 10:00:00", "A", 1, 25, "pagado", 0, "l1"),
                venta("2026-09-30 10:00:00", "B", 1, 25, "pagado", 0, "l1"),
                venta("2026-10-01 10:00:00", "C", 1, 25, "pagado", 0, "l2"),
                null);
        List<Venta> octubre = ExportacionCsv.ventasDe(ventas, true, "2026-10");
        assertEquals(2, octubre.size());
        assertEquals("C", octubre.get(0).cliente);
        assertEquals("A", octubre.get(1).cliente);
        List<Venta> lote1 = ExportacionCsv.ventasDe(ventas, false, "l1");
        assertEquals(2, lote1.size());
        assertEquals("B", lote1.get(0).cliente);
        assertEquals(0, ExportacionCsv.ventasDe(ventas, true, null).size());

        List<Gasto> gastos = Arrays.asList(
                new Gasto("g1", "2026-10-05", "otros", 1, null, "l1", "u1"),
                new Gasto("g2", "2026-11-05", "otros", 2, null, null, "u1"));
        assertEquals(1, ExportacionCsv.gastosDe(gastos, true, "2026-10").size());
        assertEquals(1, ExportacionCsv.gastosDe(gastos, false, "l1").size());
        assertEquals(0, ExportacionCsv.gastosDe(gastos, false, "l2").size());
    }

    @Test
    public void nombreArchivo() {
        assertEquals("CajaiCafe_Ventas_2026-10.csv", ExportacionCsv.nombreArchivo("Ventas", "2026-10"));
        assertEquals("CajaiCafe_Gastos_LOTE1-NOV2025.csv", ExportacionCsv.nombreArchivo("Gastos", "LOTE1-NOV2025"));
    }
}
