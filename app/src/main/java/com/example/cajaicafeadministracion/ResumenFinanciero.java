package com.example.cajaicafeadministracion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

/**
 * Resumen de un mes ("yyyy-MM") para la pestaña Inicio y punto de equilibrio de cada lote.
 * <ul>
 *   <li>Ingresos: total de las ventas registradas en el mes (con oferta, el {@code total} de la venta).</li>
 *   <li>Cobrado: pagado → total; parcial → lo dejado a cuenta; pendiente → 0. Por cobrar = ingresos − cobrado.</li>
 *   <li>Costo de lo vendido: el café de cada bolsa vendida según el costeo de su lote
 *       ({@link CosteoLote#costoCafePorBolsa}). Las ventas sin lote, de 1 kg o grano, o de un lote sin
 *       kg tostado no se pueden costear: se cuentan aparte en {@link #ventasSinCosto}.</li>
 *   <li>Gastos: los de {@code gastos/} con fecha en el mes, de todas las categorías (el empaque entra
 *       aquí cuando se compra, por eso el costo de lo vendido es solo el café).</li>
 *   <li>Utilidad neta = ingresos − costo de lo vendido − gastos; margen = utilidad / ingresos.</li>
 * </ul>
 * No depende de Android para poder probarla con tests unitarios en el CI.
 */
public final class ResumenFinanciero {

    private static final String[] NOMBRES_MES = {"Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"};

    public final String mes;
    public final int ventas;
    public final double ingresos;
    public final double cobrado;
    public final double porCobrar;
    public final double costoVendido;
    public final int ventasSinCosto;
    public final double gastos;
    public final double utilidadNeta;

    private ResumenFinanciero(String mes, int ventas, double ingresos, double cobrado, double costoVendido,
                              int ventasSinCosto, double gastos) {
        this.mes = mes;
        this.ventas = ventas;
        this.ingresos = PrecioCalculator.redondear(ingresos);
        this.cobrado = PrecioCalculator.redondear(cobrado);
        this.porCobrar = PrecioCalculator.redondear(ingresos - cobrado);
        this.costoVendido = PrecioCalculator.redondear(costoVendido);
        this.ventasSinCosto = ventasSinCosto;
        this.gastos = PrecioCalculator.redondear(gastos);
        this.utilidadNeta = PrecioCalculator.redondear(ingresos - costoVendido - gastos);
    }

    /** Margen neto en % (una decimal), o NaN si el mes no tiene ingresos. */
    public double margen() {
        if (!(ingresos > 0)) return Double.NaN;
        return Math.round(utilidadNeta / ingresos * 1000.0) / 10.0;
    }

    /**
     * @param lotesPorId          lotes por id, para costear cada venta con el costal del que salió
     * @param nombrePorProductoId catálogo (id → nombre) para ventas antiguas que solo guardan el nombre
     */
    public static ResumenFinanciero delMes(String mes, Collection<Venta> ventas, Collection<Gasto> gastos,
                                           Map<String, Lote> lotesPorId, Map<String, String> nombrePorProductoId) {
        int n = 0;
        int sinCosto = 0;
        double ingresos = 0, cobrado = 0, costo = 0, totalGastos = 0;
        for (Venta v : ventas) {
            if (v == null || mes == null || !mes.equals(mesDe(v.fecha))) continue;
            n++;
            ingresos += v.total;
            cobrado += cobradoDe(v);
            double c = costoCafeDeVenta(v, lotesPorId, nombrePorProductoId);
            if (c < 0) sinCosto++;
            else costo += c;
        }
        for (Gasto g : gastos) {
            if (g != null && mes != null && mes.equals(mesDe(g.fecha))) totalGastos += g.monto;
        }
        return new ResumenFinanciero(mes, n, ingresos, cobrado, costo, sinCosto, totalGastos);
    }

    /** Lo que ya pagó el cliente: el total si está pagado, lo dejado a cuenta si es parcial, 0 si no. */
    public static double cobradoDe(Venta v) {
        if ("pagado".equals(v.estadoPago)) return v.total;
        if ("parcial".equals(v.estadoPago)) return Math.min(Math.max(v.montoParcial, 0), v.total);
        return 0;
    }

    /** Café de las bolsas de una venta (cantidad × café por bolsa de su lote), o -1 si no se puede costear. */
    public static double costoCafeDeVenta(Venta v, Map<String, Lote> lotesPorId, Map<String, String> nombrePorProductoId) {
        String productoId = StockLote.productoIdDeVenta(v.productoId, v.producto, nombrePorProductoId);
        Lote lote = v.loteId == null ? null : lotesPorId.get(v.loteId);
        if (lote == null || CosteoLote.pesoKg(productoId) <= 0 || !CosteoLote.calculable(lote)) return -1;
        return CosteoLote.costoCafePorBolsa(lote, productoId) * v.cantidad;
    }

    /** "yyyy-MM" de una fecha guardada como "yyyy-MM-dd HH:mm:ss"; null si no tiene ese formato. */
    public static String mesDe(String fecha) {
        if (fecha == null || fecha.length() < 7) return null;
        String mes = fecha.substring(0, 7);
        return mes.matches("\\d{4}-(0[1-9]|1[0-2])") ? mes : null;
    }

    /** Meses con movimientos más el actual, del más reciente al más antiguo y sin repetir. */
    public static List<String> meses(Collection<String> fechas, String mesActual) {
        TreeSet<String> set = new TreeSet<>(Collections.reverseOrder());
        if (mesDe(mesActual) != null) set.add(mesActual);
        for (String f : fechas) {
            String m = mesDe(f);
            if (m != null) set.add(m);
        }
        return new ArrayList<>(set);
    }

    /** "2026-10" → "Octubre 2026" (sin depender del idioma del celular). */
    public static String etiquetaMes(String mes) {
        if (mesDe(mes) == null) return mes == null ? "" : mes;
        int numero = Integer.parseInt(mes.substring(5, 7));
        return NOMBRES_MES[numero - 1] + " " + mes.substring(0, 4);
    }

    /** "Margen: 12.5 %", o "Margen: —" sin ingresos. */
    public static String textoMargen(double margen) {
        if (Double.isNaN(margen)) return "Margen: —";
        return String.format(Locale.US, "Margen: %.1f %%", margen);
    }

    /**
     * Lo que deja una bolsa en el peor caso: con oferta (ej. 2 × S/45) cada una sale a 22.50; sin
     * oferta, su precio. 0 si el producto no tiene precio.
     */
    public static double precioEfectivo(Producto p) {
        if (p == null || !PrecioCalculator.precioValido(p.precio)) return 0;
        if (PrecioCalculator.ofertaValida(p.precio, p.ofertaCantidad, p.ofertaPrecio)) {
            return PrecioCalculator.redondear(p.ofertaPrecio / p.ofertaCantidad);
        }
        return p.precio;
    }

    /** Punto de equilibrio de un lote: cuánto se invirtió en el costal y cuánto se ha vendido de él. */
    public static final class Equilibrio {
        /** Costeo del lote ({@code totalGastos}) + gastos registrados con su loteId. */
        public final double invertido;
        /** Total de las ventas registradas con su loteId (todas las fechas). */
        public final double vendido;
        /** Lo que falta vender para recuperar lo invertido; 0 si ya se recuperó. */
        public final double falta;
        /** Bolsas que faltan al precio dado (redondeado hacia arriba); 0 si ya se recuperó, -1 sin precio. */
        public final int bolsasFaltan;

        Equilibrio(double invertido, double vendido, double precioBolsa) {
            this.invertido = PrecioCalculator.redondear(invertido);
            this.vendido = PrecioCalculator.redondear(vendido);
            this.falta = Math.max(0, PrecioCalculator.redondear(invertido - vendido));
            if (falta <= 0) bolsasFaltan = 0;
            else if (PrecioCalculator.precioValido(precioBolsa)) bolsasFaltan = (int) Math.ceil(falta / precioBolsa - 1e-9);
            else bolsasFaltan = -1;
        }

        public boolean recuperado() {
            return falta <= 0;
        }
    }

    public static Equilibrio equilibrio(Lote lote, Collection<Venta> ventas, Collection<Gasto> gastos, double precioBolsa) {
        double invertido = lote.totalGastos;
        for (Gasto g : gastos) {
            if (g != null && lote.id != null && lote.id.equals(g.loteId)) invertido += g.monto;
        }
        double vendido = 0;
        for (Venta v : ventas) {
            if (v != null && lote.id != null && lote.id.equals(v.loteId)) vendido += v.total;
        }
        return new Equilibrio(invertido, vendido, precioBolsa);
    }

    /**
     * Línea de la tarjeta del lote, ej. "LOTE1-NOV2025: faltan S/ 120.00 de S/ 705.50 → unas 6 bolsas de
     * 1/2 kg (a S/ 22.50)" o "LOTE1-NOV2025: inversión recuperada (+S/ 29.50 sobre S/ 705.50)".
     */
    public static String textoEquilibrio(String codigo, Equilibrio e, double precioBolsa) {
        String inicio = (codigo == null ? "" : codigo) + ": ";
        if (e.recuperado()) {
            return inicio + "inversión recuperada (+" + PrecioCalculator.formatear(e.vendido - e.invertido)
                    + " sobre " + PrecioCalculator.formatear(e.invertido) + ")";
        }
        String texto = inicio + "faltan " + PrecioCalculator.formatear(e.falta) + " de " + PrecioCalculator.formatear(e.invertido);
        if (e.bolsasFaltan > 0) {
            texto += (e.bolsasFaltan == 1 ? " → una bolsa" : " → unas " + e.bolsasFaltan + " bolsas")
                    + " de 1/2 kg (a " + PrecioCalculator.formatear(precioBolsa) + ")";
        }
        return texto;
    }
}
