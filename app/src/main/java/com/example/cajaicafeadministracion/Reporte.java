package com.example.cajaicafeadministracion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Contenido del reporte PDF (por mes o por lote) como una lista de líneas: qué dice el reporte, sin
 * nada de dibujo. GeneradorPdf lo pinta en páginas A4. No depende de Android para probarlo en el CI.
 */
public final class Reporte {

    public static final String AVISO = "Reporte interno de Cajai Café — no es comprobante de pago.";

    private static final float[] ANCHOS_DATO = {0.62f, 0.38f};
    private static final float[] ANCHOS_VENTAS = {0.12f, 0.22f, 0.27f, 0.08f, 0.15f, 0.16f};
    private static final String[] ENCABEZADO_VENTAS = {"Fecha", "Cliente", "Producto", "Cant.", "Total", "Estado"};
    private static final float[] ANCHOS_GASTOS = {0.12f, 0.18f, 0.40f, 0.15f, 0.15f};
    private static final String[] ENCABEZADO_GASTOS = {"Fecha", "Categoría", "Nota", "Lote", "Monto"};

    public final String titulo;
    public final String nombreArchivo;
    public final List<Linea> lineas;

    private Reporte(String titulo, String nombreArchivo, List<Linea> lineas) {
        this.titulo = titulo;
        this.nombreArchivo = nombreArchivo;
        this.lineas = Collections.unmodifiableList(lineas);
    }

    /** Una línea del reporte. Las de tabla (ENCABEZADO, FILA, TOTAL) traen el ancho de cada columna. */
    public static final class Linea {
        public enum Tipo { SECCION, TEXTO, ENCABEZADO, FILA, TOTAL }

        public final Tipo tipo;
        public final String[] columnas;
        /** Fracción del ancho útil de la página para cada columna (suman 1); null en SECCION y TEXTO. */
        public final float[] anchos;

        Linea(Tipo tipo, String[] columnas, float[] anchos) {
            this.tipo = tipo;
            this.columnas = columnas;
            this.anchos = anchos;
        }

        /** El texto de una SECCION o TEXTO (la única columna). */
        public String texto() {
            return columnas.length > 0 ? columnas[0] : "";
        }
    }

    /**
     * Reporte de un mes ("yyyy-MM"): el mismo resumen que la pestaña Inicio, y el detalle de las ventas y
     * los gastos con fecha en ese mes.
     */
    public static Reporte delMes(String mes, Collection<Venta> ventas, Collection<Gasto> gastos,
                                 Map<String, Lote> lotesPorId, Map<String, String> nombrePorProductoId,
                                 String generadoEl) {
        List<Linea> l = new ArrayList<>();
        cabecera(l, generadoEl);

        ResumenFinanciero r = ResumenFinanciero.delMes(mes, ventas, gastos, lotesPorId, nombrePorProductoId);
        seccion(l, "Resumen");
        dato(l, "Ventas registradas (" + r.ventas + ")", r.ingresos);
        dato(l, "Cobrado", r.cobrado);
        dato(l, "Por cobrar", r.porCobrar);
        dato(l, "Costo del café vendido", r.costoVendido);
        dato(l, "Gastos del mes", r.gastos);
        l.add(new Linea(Linea.Tipo.TOTAL, new String[]{"Utilidad neta", PrecioCalculator.formatear(r.utilidadNeta)}, ANCHOS_DATO));
        texto(l, ResumenFinanciero.textoMargen(r.margen()));
        if (r.ventasSinCosto > 0) {
            texto(l, r.ventasSinCosto + (r.ventasSinCosto == 1 ? " venta" : " ventas")
                    + " sin costo conocido (1 kg, grano, sin lote o lote sin kg tostado): no suman al costo.");
        }

        List<Venta> delMes = new ArrayList<>();
        for (Venta v : ventas) {
            if (v != null && mes != null && mes.equals(ResumenFinanciero.mesDe(v.fecha))) delMes.add(v);
        }
        tablaVentas(l, delMes, "Sin ventas en este mes.");

        List<Gasto> gastosMes = new ArrayList<>();
        for (Gasto g : gastos) {
            if (g != null && mes != null && mes.equals(ResumenFinanciero.mesDe(g.fecha))) gastosMes.add(g);
        }
        tablaGastos(l, gastosMes, lotesPorId, "Sin gastos registrados en este mes.");

        return new Reporte("Reporte de " + ResumenFinanciero.etiquetaMes(mes),
                "CajaiCafe_Reporte_" + seguro(mes) + ".pdf", l);
    }

    /**
     * Reporte de un lote (todas las fechas): su costeo, los gastos y ventas registrados con él, el punto
     * de equilibrio y el stock que le queda. {@code precioBolsa} es el de ResumenFinanciero.precioEfectivo.
     */
    public static Reporte delLote(Lote lote, Collection<Venta> ventas, Collection<Gasto> gastos,
                                  double precioBolsa, String generadoEl) {
        List<Linea> l = new ArrayList<>();
        cabecera(l, generadoEl);
        texto(l, "Inicio: " + texto(lote.fechaInicio) + " · Estado: " + ("cerrado".equals(lote.estado) ? "cerrado" : "abierto"));

        seccion(l, "Costeo del costal");
        dato(l, String.format(Locale.US, "Café pergamino (%.2f kg)", lote.pergaminoKg), lote.pergaminoTotal);
        dato(l, String.format(Locale.US, "Servicio de pilado (%.2f kg)", lote.piladoKg), lote.piladoTotal);
        dato(l, String.format(Locale.US, "Servicio de tostado (%.2f kg)", lote.tostadoKg), lote.tostadoTotal);
        dato(l, "Transporte (flete)", lote.flete);
        dato(l, "Electricidad (selladora + moledora)", lote.electricidadTotal);
        dato(l, "Empaque (manual)", lote.empaqueTotal);
        l.add(new Linea(Linea.Tipo.TOTAL, new String[]{"Total del costal", PrecioCalculator.formatear(lote.totalGastos)}, ANCHOS_DATO));
        if (CosteoLote.calculable(lote)) dato(l, "Café por kg tostado", CosteoLote.costoPorKg(lote));

        List<Gasto> gastosLote = new ArrayList<>();
        for (Gasto g : gastos) {
            if (g != null && lote.id != null && lote.id.equals(g.loteId)) gastosLote.add(g);
        }
        tablaGastos(l, gastosLote, null, "Sin gastos registrados con este lote.");

        List<Venta> ventasLote = new ArrayList<>();
        for (Venta v : ventas) {
            if (v != null && lote.id != null && lote.id.equals(v.loteId)) ventasLote.add(v);
        }
        tablaVentas(l, ventasLote, "Sin ventas registradas de este lote.");
        double cobrado = 0;
        for (Venta v : ventasLote) cobrado += ResumenFinanciero.cobradoDe(v);
        if (!ventasLote.isEmpty()) {
            dato(l, "Cobrado", cobrado);
            dato(l, "Por cobrar", CuentasPorCobrar.total(ventasLote));
        }

        seccion(l, "Punto de equilibrio");
        ResumenFinanciero.Equilibrio e = ResumenFinanciero.equilibrio(lote, ventas, gastos, precioBolsa);
        texto(l, ResumenFinanciero.textoEquilibrio(lote.codigo, e, precioBolsa));

        seccion(l, "Stock que queda");
        l.add(new Linea(Linea.Tipo.FILA, new String[]{"Bolsas de 1/2 kg", String.valueOf(lote.stockBolsas12)}, ANCHOS_DATO));
        l.add(new Linea(Linea.Tipo.FILA, new String[]{"Bolsas de 1/4 kg", String.valueOf(lote.stockBolsas14)}, ANCHOS_DATO));

        return new Reporte("Reporte del lote " + texto(lote.codigo),
                "CajaiCafe_Lote_" + seguro(lote.codigo) + ".pdf", l);
    }

    private static void cabecera(List<Linea> l, String generadoEl) {
        texto(l, AVISO);
        if (generadoEl != null) texto(l, "Generado: " + generadoEl);
    }

    private static void tablaVentas(List<Linea> l, List<Venta> ventas, String siVacia) {
        ventas.sort((a, b) -> texto(a.fecha).compareTo(texto(b.fecha)));
        seccion(l, "Ventas (" + ventas.size() + ")");
        if (ventas.isEmpty()) {
            texto(l, siVacia);
            return;
        }
        l.add(new Linea(Linea.Tipo.ENCABEZADO, ENCABEZADO_VENTAS, ANCHOS_VENTAS));
        double total = 0;
        for (Venta v : ventas) {
            String cliente = CuentasPorCobrar.nombreCliente(v);
            l.add(new Linea(Linea.Tipo.FILA, new String[]{
                    fechaCorta(v.fecha), cliente == null ? "—" : cliente, texto(v.producto),
                    String.valueOf(v.cantidad), PrecioCalculator.formatear(v.total),
                    v.estadoPago == null ? "—" : v.estadoPago}, ANCHOS_VENTAS));
            total += v.total;
        }
        l.add(new Linea(Linea.Tipo.TOTAL, new String[]{"", "", "", "Total", PrecioCalculator.formatear(total), ""}, ANCHOS_VENTAS));
    }

    /** {@code lotesPorId} null = reporte de un solo lote: la columna Lote queda vacía. */
    private static void tablaGastos(List<Linea> l, List<Gasto> gastos, Map<String, Lote> lotesPorId, String siVacia) {
        gastos.sort((a, b) -> texto(a.fecha).compareTo(texto(b.fecha)));
        seccion(l, "Gastos (" + gastos.size() + ")");
        if (gastos.isEmpty()) {
            texto(l, siVacia);
            return;
        }
        l.add(new Linea(Linea.Tipo.ENCABEZADO, ENCABEZADO_GASTOS, ANCHOS_GASTOS));
        double total = 0;
        for (Gasto g : gastos) {
            String lote = "";
            if (lotesPorId != null && g.loteId != null) {
                Lote dueno = lotesPorId.get(g.loteId);
                lote = dueno != null ? texto(dueno.codigo) : "(borrado)";
            }
            l.add(new Linea(Linea.Tipo.FILA, new String[]{
                    fechaCorta(g.fecha), ValidacionGasto.etiquetaCategoria(g.categoria),
                    g.nota == null ? "" : g.nota.trim(), lote, PrecioCalculator.formatear(g.monto)}, ANCHOS_GASTOS));
            total += g.monto;
        }
        l.add(new Linea(Linea.Tipo.TOTAL, new String[]{"", "", "", "Total", PrecioCalculator.formatear(total)}, ANCHOS_GASTOS));
    }

    private static void seccion(List<Linea> l, String titulo) {
        l.add(new Linea(Linea.Tipo.SECCION, new String[]{titulo}, null));
    }

    private static void texto(List<Linea> l, String texto) {
        l.add(new Linea(Linea.Tipo.TEXTO, new String[]{texto}, null));
    }

    private static void dato(List<Linea> l, String nombre, double monto) {
        l.add(new Linea(Linea.Tipo.FILA, new String[]{nombre, PrecioCalculator.formatear(monto)}, ANCHOS_DATO));
    }

    /** "2026-10-03 10:00:00" → "03/10"; el año ya está en el título o en la fecha de inicio. */
    private static String fechaCorta(String fecha) {
        String f = CuentasPorCobrar.fechaCorta(fecha);
        return f == null ? "—" : f.substring(0, 5);
    }

    /** Solo letras, números, guion y guion bajo en el nombre del archivo. */
    static String seguro(String s) {
        String limpio = texto(s).trim().replaceAll("[^A-Za-z0-9_-]+", "_");
        return limpio.isEmpty() ? "sin_nombre" : limpio;
    }

    private static String texto(String s) {
        return s == null ? "" : s;
    }
}
