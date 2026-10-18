package com.example.cajaicafeadministracion;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;

import java.util.ArrayList;
import java.util.List;

/**
 * Pinta un Reporte en hojas A4 (595 × 842 puntos) con PdfDocument. Cuando una línea ya no entra pasa
 * a otra hoja, y si estaba en medio de una tabla repite su encabezado.
 */
final class GeneradorPdf {

    private static final int ANCHO = 595;
    private static final int ALTO = 842;
    private static final int MARGEN = 40;
    private static final float UTIL = ANCHO - 2 * MARGEN;
    private static final float LIMITE = ALTO - MARGEN - 16; // deja sitio al número de página

    private final PdfDocument doc = new PdfDocument();
    private final Paint titulo = paint(17, true, Color.parseColor("#5B4442"));
    private final Paint seccion = paint(13, true, Color.parseColor("#6D4C41"));
    private final Paint normal = paint(10, false, Color.BLACK);
    private final Paint negrita = paint(10, true, Color.BLACK);
    private final Paint pie = paint(8, false, Color.GRAY);
    private final Paint linea = new Paint();

    private PdfDocument.Page pagina;
    private Canvas canvas;
    private int numeroPagina;
    private float y;
    private Reporte.Linea encabezadoActual; // para repetirlo si la tabla sigue en otra hoja

    private GeneradorPdf() {
        linea.setColor(Color.LTGRAY);
        linea.setStrokeWidth(0.8f);
    }

    /** El documento queda abierto: quien lo recibe lo guarda y luego llama a close(). */
    static PdfDocument generar(Reporte reporte) {
        GeneradorPdf g = new GeneradorPdf();
        g.dibujar(reporte);
        return g.doc;
    }

    private static Paint paint(float tamano, boolean enNegrita, int color) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setTextSize(tamano);
        p.setColor(color);
        p.setTypeface(Typeface.create(Typeface.DEFAULT, enNegrita ? Typeface.BOLD : Typeface.NORMAL));
        return p;
    }

    private void dibujar(Reporte reporte) {
        nuevaPagina();
        for (String parte : partir("Cajai Café · " + reporte.titulo, titulo, UTIL)) {
            canvas.drawText(parte, MARGEN, y, titulo);
            y += 22;
        }
        y += 4;

        for (Reporte.Linea l : reporte.lineas) {
            switch (l.tipo) {
                case SECCION:
                    encabezadoActual = null;
                    asegurar(40);
                    y += 12;
                    canvas.drawText(l.texto(), MARGEN, y, seccion);
                    y += 5;
                    canvas.drawLine(MARGEN, y, ANCHO - MARGEN, y, linea);
                    y += 14;
                    break;
                case TEXTO:
                    for (String parte : partir(l.texto(), normal, UTIL)) {
                        asegurar(14);
                        canvas.drawText(parte, MARGEN, y, normal);
                        y += 14;
                    }
                    break;
                case ENCABEZADO:
                    asegurar(30); // el encabezado no queda solo al final de una hoja
                    encabezadoActual = l;
                    dibujarEncabezado(l);
                    break;
                case FILA:
                    terminoTabla(l);
                    if (asegurar(14) && encabezadoActual != null) dibujarEncabezado(encabezadoActual);
                    fila(l, normal);
                    y += 14;
                    break;
                case TOTAL:
                    terminoTabla(l);
                    if (asegurar(20) && encabezadoActual != null) dibujarEncabezado(encabezadoActual);
                    canvas.drawLine(MARGEN, y - 10, ANCHO - MARGEN, y - 10, linea);
                    y += 2;
                    fila(l, negrita);
                    y += 18;
                    break;
            }
        }
        terminarPagina();
    }

    /** Una fila con otras columnas (ej. "Cobrado" después de la tabla de ventas) ya no es de esa tabla. */
    private void terminoTabla(Reporte.Linea l) {
        if (encabezadoActual != null && encabezadoActual.anchos != l.anchos) encabezadoActual = null;
    }

    private void dibujarEncabezado(Reporte.Linea l) {
        fila(l, negrita);
        y += 4;
        canvas.drawLine(MARGEN, y, ANCHO - MARGEN, y, linea);
        y += 12;
    }

    /** Una fila de tabla: cada columna en su ancho, recortada con "…" si no entra. */
    private void fila(Reporte.Linea l, Paint paint) {
        float x = MARGEN;
        for (int i = 0; i < l.columnas.length; i++) {
            float ancho = UTIL * l.anchos[i];
            canvas.drawText(recortar(l.columnas[i], paint, ancho - 4), x, y, paint);
            x += ancho;
        }
    }

    /** Si no caben {@code alto} puntos más, pasa a una hoja nueva. Devuelve true si cambió de hoja. */
    private boolean asegurar(float alto) {
        if (y + alto <= LIMITE) return false;
        terminarPagina();
        nuevaPagina();
        return true;
    }

    private void nuevaPagina() {
        numeroPagina++;
        pagina = doc.startPage(new PdfDocument.PageInfo.Builder(ANCHO, ALTO, numeroPagina).create());
        canvas = pagina.getCanvas();
        y = MARGEN + 12;
    }

    private void terminarPagina() {
        String texto = "Página " + numeroPagina + " · " + Reporte.AVISO;
        canvas.drawText(texto, (ANCHO - pie.measureText(texto)) / 2, ALTO - MARGEN / 2f, pie);
        doc.finishPage(pagina);
    }

    private static String recortar(String texto, Paint paint, float ancho) {
        if (texto == null) return "";
        if (paint.measureText(texto) <= ancho) return texto;
        String t = texto;
        while (t.length() > 1 && paint.measureText(t + "…") > ancho) t = t.substring(0, t.length() - 1);
        return t + "…";
    }

    /** Parte un texto en renglones que entren en {@code ancho}, cortando entre palabras. */
    private static List<String> partir(String texto, Paint paint, float ancho) {
        List<String> renglones = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        for (String palabra : (texto == null ? "" : texto).split(" ")) {
            String prueba = actual.length() == 0 ? palabra : actual + " " + palabra;
            if (paint.measureText(prueba) <= ancho || actual.length() == 0) {
                actual.setLength(0);
                actual.append(prueba);
            } else {
                renglones.add(actual.toString());
                actual.setLength(0);
                actual.append(palabra);
            }
        }
        renglones.add(actual.toString());
        return renglones;
    }
}
