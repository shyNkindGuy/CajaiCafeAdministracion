package com.example.cajaicafeadministracion;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Categorías fijas de gastos, lectura del monto y validación del formulario "Registrar gasto".
 * No depende de Android para poder probarla con tests unitarios en el CI.
 */
public final class ValidacionGasto {

    /** Lo que se guarda en {@code gastos/{id}/categoria}, en el orden del selector. */
    public static final List<String> CATEGORIAS = Collections.unmodifiableList(Arrays.asList(
            "empaque", "transporte", "publicidad", "servicios", "mantenimiento", "otros"));

    private ValidacionGasto() {}

    /** Nombre para mostrar, ej. "transporte" → "Transporte". Si no es una categoría conocida, tal cual. */
    public static String etiquetaCategoria(String categoria) {
        if (categoria == null || categoria.isEmpty()) return "Sin categoría";
        if (!CATEGORIAS.contains(categoria)) return categoria;
        return Character.toUpperCase(categoria.charAt(0)) + categoria.substring(1);
    }

    /**
     * Monto escrito en el formulario, redondeado a céntimos. Acepta coma decimal ("12,50"), que es lo
     * que ponen algunos teclados en español. 0 si está vacío o no es un número.
     */
    public static double parsearMonto(String texto) {
        if (texto == null) return 0;
        String t = texto.trim().replace(',', '.');
        if (t.isEmpty()) return 0;
        try {
            double monto = Double.parseDouble(t);
            if (Double.isNaN(monto) || Double.isInfinite(monto)) return 0;
            return PrecioCalculator.redondear(monto);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Devuelve el mensaje de error a mostrar, o null si el gasto se puede guardar. */
    public static String validar(String categoria, double monto) {
        if (categoria == null || !CATEGORIAS.contains(categoria)) return "Elige una categoría";
        if (!(monto > 0)) return "Ingresa un monto mayor a 0";
        return null;
    }

    /**
     * Línea de la lista de últimos gastos, ej. "2026-10-16 · Transporte · S/ 12.00 · LOTE1-NOV2025 — taxi a la tostadora".
     * {@code codigoLote} es null si el gasto es general; la nota se omite si está vacía.
     */
    public static String etiqueta(Gasto g, String codigoLote) {
        String fecha = g.fecha == null ? "" : (g.fecha.length() >= 10 ? g.fecha.substring(0, 10) : g.fecha);
        String texto = (fecha.isEmpty() ? "" : fecha + " · ") + etiquetaCategoria(g.categoria)
                + " · " + PrecioCalculator.formatear(g.monto);
        if (codigoLote != null && !codigoLote.isEmpty()) texto += " · " + codigoLote;
        if (g.nota != null && !g.nota.trim().isEmpty()) texto += " — " + g.nota.trim();
        return texto;
    }
}
