package com.example.cajaicafeadministracion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Qué stock de lote descuenta cada producto y qué lote sugerir al vender (FIFO: primero el
 * costal más antiguo). No depende de Android para poder probarla en el CI.
 */
public final class StockLote {

    public static final String CAMPO_MEDIO_KILO = "stockBolsas12";
    public static final String CAMPO_CUARTO_KILO = "stockBolsas14";

    private StockLote() {}

    /**
     * Campo de stock del lote que usa el producto (por su id en {@code productos/}), o null si el
     * producto no se descuenta de lotes (1 kg, grano): esos siguen usando {@code productos/{id}/stock}.
     */
    public static String campoStock(String productoId) {
        if ("medio_kilo".equals(productoId)) return CAMPO_MEDIO_KILO;
        if ("cuarto_kilo".equals(productoId)) return CAMPO_CUARTO_KILO;
        return null;
    }

    public static int stockDe(Lote lote, String campo) {
        if (lote == null || campo == null) return 0;
        if (CAMPO_MEDIO_KILO.equals(campo)) return lote.stockBolsas12;
        if (CAMPO_CUARTO_KILO.equals(campo)) return lote.stockBolsas14;
        return 0;
    }

    public static boolean abierto(Lote lote) {
        return lote != null && !"cerrado".equals(lote.estado);
    }

    /** Lotes abiertos del más antiguo al más nuevo (por fecha de inicio y, si empatan, por id). */
    public static List<Lote> abiertosFifo(Collection<Lote> lotes) {
        List<Lote> abiertos = new ArrayList<>();
        for (Lote l : lotes) {
            if (abierto(l)) abiertos.add(l);
        }
        abiertos.sort((a, b) -> {
            int porFecha = texto(a.fechaInicio).compareTo(texto(b.fechaInicio));
            return porFecha != 0 ? porFecha : texto(a.id).compareTo(texto(b.id));
        });
        return abiertos;
    }

    /**
     * Posición del lote a sugerir en {@code abiertos} (ya ordenados FIFO): el más antiguo que tenga
     * bolsas del producto. Si ninguno tiene, o el producto no usa stock de lote, el más antiguo.
     * -1 si no hay lotes abiertos.
     */
    public static int sugerido(List<Lote> abiertos, String campo) {
        if (abiertos.isEmpty()) return -1;
        if (campo != null) {
            for (int i = 0; i < abiertos.size(); i++) {
                if (stockDe(abiertos.get(i), campo) > 0) return i;
            }
        }
        return 0;
    }

    /**
     * Posición a mostrar en el selector: la del lote que se eligió a mano ({@code elegidoId}) mientras
     * siga abierto y tenga bolsas del producto; si no, la sugerida FIFO. Así un cambio en Firebase
     * (otra venta, un empacado) no mueve la venta a otro costal sin que la persona lo note.
     */
    public static int elegido(List<Lote> abiertos, String campo, String elegidoId) {
        if (elegidoId != null) {
            for (int i = 0; i < abiertos.size(); i++) {
                Lote l = abiertos.get(i);
                if (elegidoId.equals(l.id) && (campo == null || stockDe(l, campo) > 0)) return i;
            }
        }
        return sugerido(abiertos, campo);
    }

    /** Texto del selector de lotes, ej. "LOTE1-NOV2025 · 1/2 kg: 6 · 1/4 kg: 2". */
    public static String etiqueta(Lote lote) {
        return texto(lote.codigo) + " · 1/2 kg: " + lote.stockBolsas12 + " · 1/4 kg: " + lote.stockBolsas14;
    }

    private static String texto(String s) {
        return s == null ? "" : s;
    }
}
