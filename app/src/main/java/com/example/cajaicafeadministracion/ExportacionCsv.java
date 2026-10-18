package com.example.cajaicafeadministracion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Ventas y gastos como CSV para abrir en Excel o Google Sheets: separado por comas, decimales con
 * punto, fechas "yyyy-MM-dd HH:mm:ss", UTF-8 con BOM (sin él Excel muestra mal las tildes) y fin de
 * línea \r\n. No depende de Android para poder probarlo en el CI.
 */
public final class ExportacionCsv {

    /** Marca de orden de bytes: le dice a Excel que el archivo es UTF-8. */
    public static final String BOM = "﻿";
    private static final String FIN = "\r\n";

    private ExportacionCsv() {}

    /** Ventas de un mes ("yyyy-MM") o de un lote (su id), según {@code porMes}, de la más antigua a la más nueva. */
    public static List<Venta> ventasDe(Collection<Venta> ventas, boolean porMes, String clave) {
        List<Venta> elegidas = new ArrayList<>();
        for (Venta v : ventas) {
            if (v == null || clave == null) continue;
            if (porMes ? clave.equals(ResumenFinanciero.mesDe(v.fecha)) : clave.equals(v.loteId)) elegidas.add(v);
        }
        elegidas.sort((a, b) -> texto(a.fecha).compareTo(texto(b.fecha)));
        return elegidas;
    }

    /** Gastos de un mes o de un lote, igual que {@link #ventasDe}. */
    public static List<Gasto> gastosDe(Collection<Gasto> gastos, boolean porMes, String clave) {
        List<Gasto> elegidos = new ArrayList<>();
        for (Gasto g : gastos) {
            if (g == null || clave == null) continue;
            if (porMes ? clave.equals(ResumenFinanciero.mesDe(g.fecha)) : clave.equals(g.loteId)) elegidos.add(g);
        }
        elegidos.sort((a, b) -> texto(a.fecha).compareTo(texto(b.fecha)));
        return elegidos;
    }

    public static String ventas(List<Venta> ventas, Map<String, Lote> lotesPorId) {
        StringBuilder sb = new StringBuilder(BOM);
        fila(sb, "Fecha", "Cliente", "Producto", "Cantidad", "Precio unitario", "Total", "Estado", "Pagado", "Saldo", "Lote");
        for (Venta v : ventas) {
            fila(sb, campo(v.fecha), campo(v.cliente), campo(v.producto), String.valueOf(v.cantidad),
                    numero(v.precioUnitario), numero(v.total), campo(v.estadoPago),
                    numero(ResumenFinanciero.cobradoDe(v)), numero(CuentasPorCobrar.saldo(v)),
                    campo(codigoLote(v.loteId, lotesPorId)));
        }
        return sb.toString();
    }

    public static String gastos(List<Gasto> gastos, Map<String, Lote> lotesPorId) {
        StringBuilder sb = new StringBuilder(BOM);
        fila(sb, "Fecha", "Categoría", "Monto", "Nota", "Lote");
        for (Gasto g : gastos) {
            fila(sb, campo(g.fecha), campo(ValidacionGasto.etiquetaCategoria(g.categoria)), numero(g.monto),
                    campo(g.nota), campo(codigoLote(g.loteId, lotesPorId)));
        }
        return sb.toString();
    }

    /** Ej. ("Ventas", "2026-10") → "CajaiCafe_Ventas_2026-10.csv". */
    public static String nombreArchivo(String que, String clave) {
        return "CajaiCafe_" + que + "_" + Reporte.seguro(clave) + ".csv";
    }

    /**
     * Un texto listo para el CSV: entre comillas si lleva coma, comillas o salto de línea (las comillas
     * se duplican), y con un apóstrofo delante si empieza con = + - @, para que Excel no lo ejecute
     * como fórmula si alguien escribió eso en un nombre o una nota.
     */
    public static String campo(String s) {
        if (s == null) return "";
        String t = s;
        if (!t.isEmpty() && "=+-@".indexOf(t.charAt(0)) >= 0) t = "'" + t;
        if (t.contains(",") || t.contains("\"") || t.contains("\n") || t.contains("\r")) {
            t = "\"" + t.replace("\"", "\"\"") + "\"";
        }
        return t;
    }

    /** Monto con punto decimal y dos decimales, sin "S/" para que la hoja lo tome como número. */
    public static String numero(double monto) {
        return String.format(Locale.US, "%.2f", PrecioCalculator.redondear(monto));
    }

    private static String codigoLote(String loteId, Map<String, Lote> lotesPorId) {
        if (loteId == null) return "";
        Lote l = lotesPorId == null ? null : lotesPorId.get(loteId);
        return l != null ? texto(l.codigo) : "(borrado)";
    }

    private static void fila(StringBuilder sb, String... columnas) {
        for (int i = 0; i < columnas.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(columnas[i]);
        }
        sb.append(FIN);
    }

    private static String texto(String s) {
        return s == null ? "" : s;
    }
}
