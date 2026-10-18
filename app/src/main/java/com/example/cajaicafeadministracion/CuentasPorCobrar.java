package com.example.cajaicafeadministracion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Ventas que todavía se deben (pendientes o parciales) y el texto del recordatorio por WhatsApp.
 * El saldo usa la misma regla que el resumen del mes (ResumenFinanciero.cobradoDe), así el total de
 * esta lista cuadra con el "Por cobrar" de Inicio. No depende de Android para probarla en el CI.
 */
public final class CuentasPorCobrar {

    /** Lo que pone VentaFragment cuando no se escribe el nombre: no sirve para saludar. */
    private static final String CLIENTE_SIN_NOMBRE = "Cliente";

    private CuentasPorCobrar() {}

    /** Lo que falta pagar de una venta: total − lo cobrado; 0 si está pagada. */
    public static double saldo(Venta v) {
        return Math.max(0, PrecioCalculator.redondear(v.total - ResumenFinanciero.cobradoDe(v)));
    }

    /** Ventas con saldo, de la deuda más antigua a la más nueva (por fecha y, si empatan, por id). */
    public static List<Venta> pendientes(Collection<Venta> ventas) {
        List<Venta> conSaldo = new ArrayList<>();
        for (Venta v : ventas) {
            if (v != null && saldo(v) > 0) conSaldo.add(v);
        }
        conSaldo.sort((a, b) -> {
            int porFecha = texto(a.fecha).compareTo(texto(b.fecha));
            return porFecha != 0 ? porFecha : texto(a.id).compareTo(texto(b.id));
        });
        return conSaldo;
    }

    public static double total(Collection<Venta> ventas) {
        double suma = 0;
        for (Venta v : ventas) {
            if (v != null) suma += saldo(v);
        }
        return PrecioCalculator.redondear(suma);
    }

    /** Nombre para mostrar y saludar, o null si la venta no lo tiene ("Cliente" es el valor por defecto). */
    public static String nombreCliente(Venta v) {
        String nombre = v.cliente == null ? "" : v.cliente.trim();
        if (nombre.isEmpty() || nombre.equalsIgnoreCase(CLIENTE_SIN_NOMBRE)) return null;
        return nombre;
    }

    /** "2026-10-03 10:00:00" → "03/10/2026"; null si la fecha no tiene ese formato. */
    public static String fechaCorta(String fecha) {
        if (fecha == null || !fecha.matches("\\d{4}-\\d{2}-\\d{2}.*")) return null;
        return fecha.substring(8, 10) + "/" + fecha.substring(5, 7) + "/" + fecha.substring(0, 4);
    }

    /** Detalle de la fila, ej. "2 × Bolsa 1/2 kg · 03/10/2026 · total S/ 45.00, pagó S/ 25.00". */
    public static String detalle(Venta v) {
        String texto = v.cantidad + " × " + texto(v.producto);
        String fecha = fechaCorta(v.fecha);
        if (fecha != null) texto += " · " + fecha;
        texto += " · total " + PrecioCalculator.formatear(v.total);
        double cobrado = ResumenFinanciero.cobradoDe(v);
        if (cobrado > 0) texto += ", pagó " + PrecioCalculator.formatear(cobrado);
        return texto;
    }

    /**
     * Recordatorio amable para mandar por WhatsApp, ej. "Hola Juan, te saludamos de Cajai Café. Te
     * recordamos que tienes un saldo pendiente de S/ 20.00 por 2 × Bolsa 1/2 kg (compra del
     * 03/10/2026; ya abonaste S/ 25.00). ¡Muchas gracias!"
     */
    public static String mensaje(Venta v) {
        String nombre = nombreCliente(v);
        StringBuilder sb = new StringBuilder(nombre == null ? "Hola" : "Hola " + nombre);
        sb.append(", te saludamos de Cajai Café. Te recordamos que tienes un saldo pendiente de ")
                .append(PrecioCalculator.formatear(saldo(v)))
                .append(" por ").append(v.cantidad).append(" × ").append(texto(v.producto));

        // Sin String.join: no existe antes de Android 8 y la app corre desde Android 7 (minSdk 24).
        String fecha = fechaCorta(v.fecha);
        double cobrado = ResumenFinanciero.cobradoDe(v);
        String entreParentesis = fecha != null ? "compra del " + fecha : "";
        if (cobrado > 0) {
            entreParentesis += (entreParentesis.isEmpty() ? "" : "; ") + "ya abonaste " + PrecioCalculator.formatear(cobrado);
        }
        if (!entreParentesis.isEmpty()) sb.append(" (").append(entreParentesis).append(")");

        return sb.append(". ¡Muchas gracias!").toString();
    }

    private static String texto(String s) {
        return s == null ? "" : s;
    }
}
