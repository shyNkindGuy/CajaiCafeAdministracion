package com.example.cajaicafeadministracion;

import java.util.Locale;

/**
 * Única fuente de verdad del total de una venta: precio de Firebase × cantidad, con la oferta
 * por cantidad del producto si tiene una (también configurada en Firebase).
 * No depende de Android para poder probarla con tests unitarios en el CI.
 */
public final class PrecioCalculator {

    private PrecioCalculator() {}

    /** Total redondeado a céntimos; 0 si el precio o la cantidad no sirven para vender. */
    public static double total(double precioUnitario, int cantidad) {
        if (!precioValido(precioUnitario) || cantidad <= 0) return 0;
        return redondear(precioUnitario * cantidad);
    }

    /**
     * Total con oferta por cantidad: cada grupo completo de {@code ofertaCantidad} unidades cuesta
     * {@code ofertaPrecio} y las sueltas van a {@code precioUnitario}.
     * Ej. 1/2 kg a S/25 con oferta 2 x S/45: 1 → 25, 2 → 45, 3 → 70, 4 → 90.
     * Sin oferta válida es precio × cantidad (ej. 1/4 kg a S/15: 4 → 60).
     */
    public static double total(double precioUnitario, int cantidad, int ofertaCantidad, double ofertaPrecio) {
        if (!precioValido(precioUnitario) || cantidad <= 0) return 0;
        if (!ofertaValida(precioUnitario, ofertaCantidad, ofertaPrecio)) return total(precioUnitario, cantidad);
        int grupos = cantidad / ofertaCantidad;
        int sueltas = cantidad % ofertaCantidad;
        return redondear(grupos * ofertaPrecio + sueltas * precioUnitario);
    }

    /** La oferta solo aplica si agrupa 2 o más unidades y sale más barata que comprarlas sueltas. */
    public static boolean ofertaValida(double precioUnitario, int ofertaCantidad, double ofertaPrecio) {
        return ofertaCantidad >= 2 && precioValido(ofertaPrecio) && ofertaPrecio < precioUnitario * ofertaCantidad;
    }

    /** Texto de la lista de bolsas, ej. "Bolsa 1/2 kg (S/ 25.00 · 2 x S/ 45.00)". */
    public static String etiqueta(String nombre, double precioUnitario, int ofertaCantidad, double ofertaPrecio) {
        String texto = nombre + " (" + formatear(precioUnitario);
        if (ofertaValida(precioUnitario, ofertaCantidad, ofertaPrecio)) {
            texto += " · " + ofertaCantidad + " x " + formatear(ofertaPrecio);
        }
        return texto + ")";
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
