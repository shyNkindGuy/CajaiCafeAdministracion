package com.example.cajaicafeadministracion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Aviso de pocas bolsas empacadas en un lote abierto, para empacar más o abrir otro costal. El umbral
 * es por tamaño y se guarda en {@code productos/medio_kilo|cuarto_kilo/stockMinimo}: sin configurar
 * avisa con 5 o menos; 0 apaga el aviso de ese tamaño. No depende de Android para probarla en el CI.
 */
public final class AlertaStock {

    public static final int UMBRAL_POR_DEFECTO = 5;

    private AlertaStock() {}

    /** Umbral del producto: su {@code stockMinimo}, o 5 si no está configurado. Nunca negativo. */
    public static int umbral(Producto p) {
        if (p == null || p.stockMinimo == null) return UMBRAL_POR_DEFECTO;
        return Math.max(0, p.stockMinimo);
    }

    /** Quedan {@code umbral} bolsas o menos (y el aviso no está apagado). */
    public static boolean bajo(int stock, int umbral) {
        return umbral > 0 && stock <= umbral;
    }

    /** Un lote abierto con poco stock de 1/2 kg o de 1/4 kg. Los cerrados no avisan. */
    public static boolean tieneAlerta(Lote lote, int umbral12, int umbral14) {
        return StockLote.abierto(lote) && (bajo(lote.stockBolsas12, umbral12) || bajo(lote.stockBolsas14, umbral14));
    }

    /** Avisos de los lotes abiertos, del más antiguo al más nuevo (el orden en que se venden). */
    public static List<String> avisos(Collection<Lote> lotes, int umbral12, int umbral14) {
        List<String> avisos = new ArrayList<>();
        for (Lote l : StockLote.abiertosFifo(lotes)) {
            if (bajo(l.stockBolsas12, umbral12)) avisos.add(aviso(l.codigo, l.stockBolsas12, "1/2 kg"));
            if (bajo(l.stockBolsas14, umbral14)) avisos.add(aviso(l.codigo, l.stockBolsas14, "1/4 kg"));
        }
        return avisos;
    }

    /** Ej. "LOTE1-NOV2025: quedan 3 bolsas de 1/2 kg" o "LOTE1-NOV2025: se acabaron las bolsas de 1/4 kg". */
    public static String aviso(String codigoLote, int stock, String tamano) {
        String inicio = (codigoLote == null ? "" : codigoLote) + ": ";
        if (stock <= 0) return inicio + "se acabaron las bolsas de " + tamano;
        if (stock == 1) return inicio + "queda 1 bolsa de " + tamano;
        return inicio + "quedan " + stock + " bolsas de " + tamano;
    }

    /** Ej. "Aviso con: 1/2 kg ≤ 5 · 1/4 kg sin aviso". */
    public static String textoUmbrales(int umbral12, int umbral14) {
        return "Aviso con: " + parte("1/2 kg", umbral12) + " · " + parte("1/4 kg", umbral14);
    }

    /** Lo escrito en el diálogo: un entero de 0 en adelante, o -1 si no sirve. */
    public static int parsearUmbral(String texto) {
        if (texto == null) return -1;
        try {
            int n = Integer.parseInt(texto.trim());
            return n >= 0 ? n : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static String parte(String tamano, int umbral) {
        return umbral > 0 ? tamano + " ≤ " + umbral : tamano + " sin aviso";
    }
}
