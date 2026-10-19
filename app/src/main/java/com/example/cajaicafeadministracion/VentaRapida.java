package com.example.cajaicafeadministracion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Lo que necesita la pantalla de venta rápida (tarea 4.2): el texto de cada chip de bolsa, el
 * stepper de cantidad y la lista de clientes frecuentes para el autocompletado.
 * No depende de Android para probarla en el CI.
 */
public final class VentaRapida {

    /** Nombre que se guarda cuando no se escribe cliente; no es un cliente frecuente. */
    public static final String CLIENTE_POR_DEFECTO = "Cliente";

    /** Bolsa marcada al abrir la pantalla: la de 1/2 kg, la que más se vende. */
    public static final String PRODUCTO_POR_DEFECTO = "medio_kilo";

    private VentaRapida() {}

    /**
     * Chip que queda marcado cuando cambian los productos: el que ya estaba elegido si sigue
     * existiendo; si no, el de 1/2 kg; si tampoco está, el primero; null si no hay productos.
     */
    public static String productoInicial(Collection<String> ids, String actual) {
        if (ids == null || ids.isEmpty()) return null;
        if (actual != null && ids.contains(actual)) return actual;
        if (ids.contains(PRODUCTO_POR_DEFECTO)) return PRODUCTO_POR_DEFECTO;
        return ids.iterator().next();
    }

    /** Cantidad después de tocar − o +: nunca menos de 1; lo que no sea un número cuenta como 0. */
    public static int sumar(String texto, int delta) {
        long n = Math.max(0, PrecioCalculator.parsearCantidad(texto)) + (long) delta;
        return (int) Math.max(1, Math.min(Integer.MAX_VALUE, n));
    }

    /**
     * Texto del chip con el precio de Firebase, ej. "Bolsa 1/2 kg · S/ 25 (2 × S/ 45)" o
     * "Bolsa 1/4 kg · S/ 15"; sin precio configurado, "… · sin precio".
     */
    public static String etiquetaChip(String nombre, double precio, int ofertaCantidad, double ofertaPrecio) {
        String texto = (nombre == null ? "" : nombre) + " · ";
        if (!PrecioCalculator.precioValido(precio)) return texto + "sin precio";
        texto += montoCorto(precio);
        if (PrecioCalculator.ofertaValida(precio, ofertaCantidad, ofertaPrecio)) {
            texto += " (" + ofertaCantidad + " × " + montoCorto(ofertaPrecio) + ")";
        }
        return texto;
    }

    /** "S/ 25" si el monto es entero, "S/ 22.50" si tiene céntimos (para que el chip no sea tan largo). */
    public static String montoCorto(double monto) {
        double r = PrecioCalculator.redondear(monto);
        if (r == Math.rint(r)) return String.format(Locale.US, "S/ %.0f", r);
        return PrecioCalculator.formatear(r);
    }

    /**
     * Clientes de las ventas, del que más compra al que menos (a igual número de compras, el más
     * reciente primero), sin repetir: "ana  perez" y "Ana Perez" son la misma persona y se muestra
     * como se escribió en su última compra. Se omiten los vacíos y el "Cliente" por defecto.
     */
    public static List<String> clientesFrecuentes(Collection<Venta> ventas, int max) {
        Map<String, Frecuencia> porClave = new LinkedHashMap<>();
        if (ventas != null) {
            for (Venta v : ventas) {
                if (v == null) continue;
                String nombre = limpiar(v.cliente);
                if (nombre.isEmpty() || nombre.equalsIgnoreCase(CLIENTE_POR_DEFECTO)) continue;
                String fecha = v.fecha == null ? "" : v.fecha;
                String clave = nombre.toLowerCase(Locale.ROOT);
                Frecuencia f = porClave.get(clave);
                if (f == null) {
                    porClave.put(clave, new Frecuencia(nombre, fecha));
                } else {
                    f.veces++;
                    if (fecha.compareTo(f.ultimaFecha) > 0) {
                        f.ultimaFecha = fecha;
                        f.nombre = nombre;
                    }
                }
            }
        }
        List<Frecuencia> orden = new ArrayList<>(porClave.values());
        Collections.sort(orden, (a, b) -> {
            if (a.veces != b.veces) return Integer.compare(b.veces, a.veces);
            int porFecha = b.ultimaFecha.compareTo(a.ultimaFecha);
            return porFecha != 0 ? porFecha : a.nombre.compareToIgnoreCase(b.nombre);
        });
        List<String> nombres = new ArrayList<>();
        for (Frecuencia f : orden) {
            if (nombres.size() >= max) break;
            nombres.add(f.nombre);
        }
        return nombres;
    }

    /** Sin espacios al inicio ni al final, y uno solo entre palabras. */
    static String limpiar(String nombre) {
        return nombre == null ? "" : nombre.trim().replaceAll("\\s+", " ");
    }

    private static final class Frecuencia {
        String nombre;
        String ultimaFecha;
        int veces = 1;

        Frecuencia(String nombre, String ultimaFecha) {
            this.nombre = nombre;
            this.ultimaFecha = ultimaFecha;
        }
    }
}
