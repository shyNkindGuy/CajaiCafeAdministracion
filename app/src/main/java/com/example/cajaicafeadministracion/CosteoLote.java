package com.example.cajaicafeadministracion;

/**
 * Cuánto cuesta producir el café de un lote (un costal):
 * costo por kg tostado = (pergamino + pilado + tostado + flete + electricidad) / kg tostado, y
 * costo por bolsa = costo por kg × peso de la bolsa + bolsa + sticker.
 * El empaque manual del lote ({@code empaqueTotal}) no entra: la bolsa y el sticker se cuentan por
 * unidad con los costos de {@code productos/{id}} (se compran por mayor y se reparten entre costales).
 * No depende de Android para poder probarla en el CI.
 */
public final class CosteoLote {

    private CosteoLote() {}

    /** Lo que costó el café del costal hasta quedar molido, sin empaque. */
    public static double costoCafe(Lote lote) {
        if (lote == null) return 0;
        return lote.pergaminoTotal + lote.piladoTotal + lote.tostadoTotal + lote.flete + lote.electricidadTotal;
    }

    /** Sin kg tostado no hay costo por kg (sería dividir entre cero). */
    public static boolean calculable(Lote lote) {
        return lote != null && lote.tostadoKg > 0 && !Double.isInfinite(lote.tostadoKg);
    }

    /** S/ por kg de café tostado, redondeado a céntimos; 0 si el lote no tiene kg tostado. */
    public static double costoPorKg(Lote lote) {
        if (!calculable(lote)) return 0;
        return PrecioCalculator.redondear(costoCafe(lote) / lote.tostadoKg);
    }

    /** Peso de café en la bolsa del producto (por su id en {@code productos/}); 0 si no sale de un lote. */
    public static double pesoKg(String productoId) {
        if ("medio_kilo".equals(productoId)) return 0.5;
        if ("cuarto_kilo".equals(productoId)) return 0.25;
        return 0;
    }

    /** Costo del café que lleva una bolsa del producto, sin bolsa ni sticker. */
    public static double costoCafePorBolsa(Lote lote, String productoId) {
        if (!calculable(lote)) return 0;
        return PrecioCalculator.redondear(costoCafe(lote) / lote.tostadoKg * pesoKg(productoId));
    }

    /**
     * Costo total de una bolsa: su café + la bolsa + el sticker. 0 si el lote no tiene kg tostado o el
     * producto no sale de un lote. Ej. lote 1 (S/ 527.50 de café, 18 kg tostado), bolsa S/ 1.18 y
     * sticker S/ 0.60: 1/2 kg → 14.65 + 1.18 + 0.60 = S/ 16.43.
     */
    public static double costoPorBolsa(Lote lote, String productoId, double costoBolsa, double costoSticker) {
        double peso = pesoKg(productoId);
        if (!calculable(lote) || peso <= 0) return 0;
        return PrecioCalculator.redondear(costoCafe(lote) / lote.tostadoKg * peso + costoBolsa + costoSticker);
    }

    /**
     * Línea del detalle del lote, ej. "Bolsa 1/2 kg: S/ 16.43 (café S/ 14.65 + bolsa S/ 1.18 + sticker S/ 0.60)".
     * Si el producto no tiene costo de bolsa ni de sticker en Firebase, lo avisa en vez de mostrar un
     * costo que parece completo y no lo es.
     */
    public static String detalleBolsa(String nombre, Lote lote, String productoId, double costoBolsa, double costoSticker) {
        if (!calculable(lote)) return nombre + ": falta el kg tostado del lote";
        String texto = nombre + ": " + PrecioCalculator.formatear(costoPorBolsa(lote, productoId, costoBolsa, costoSticker))
                + " (café " + PrecioCalculator.formatear(costoCafePorBolsa(lote, productoId));
        if (costoBolsa <= 0 && costoSticker <= 0) {
            return texto + "; falta el costo de bolsa y sticker en productos/" + productoId + ")";
        }
        return texto + " + bolsa " + PrecioCalculator.formatear(costoBolsa)
                + " + sticker " + PrecioCalculator.formatear(costoSticker) + ")";
    }
}
