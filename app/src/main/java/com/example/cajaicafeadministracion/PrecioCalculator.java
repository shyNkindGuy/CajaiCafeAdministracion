package com.example.cajaicafeadministracion;

import java.util.Locale;

/**
 * Única fuente de verdad del total de una venta: precio de Firebase × cantidad.
 * No depende de Android para poder probarla con tests unitarios en el CI.
 */
public final class PrecioCalculator {

    private PrecioCalculator() {}

    /** Total redondeado a céntimos; 0 si el precio o la cantidad no sirven para vender. */
    public static double total(double precioUnitario, int cantidad) {
        if (!precioValido(precioUnitario) || cantidad <= 0) return 0;
        return redondear(precioUnitario * cantidad);
    }

    /** Un precio sirve para vender si es un número positivo (NaN e infinito no). */
    public static boolean precioValido(double precio) {
        return precio > 0 && !Double.isInfinite(precio);
    }

    /** Cantidad escrita en el formulario; 0 si está vacía o no es un entero. */
    public static int parsearCantidad(String texto) {
        if (texto == null) return 0;
        try {
            return Integer.parseInt(texto.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static double redondear(double monto) {
        return Math.round(monto * 100.0) / 100.0;
    }

    /** Monto en soles para mostrar, por ejemplo "S/ 25.00". */
    public static String formatear(double monto) {
        return String.format(Locale.US, "S/ %.2f", monto);
    }
}
