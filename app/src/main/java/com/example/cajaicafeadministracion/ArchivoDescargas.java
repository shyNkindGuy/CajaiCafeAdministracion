package com.example.cajaicafeadministracion;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.webkit.MimeTypeMap;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Guarda un archivo (PDF, CSV) sin pedir permisos de almacenamiento y da intents para abrirlo o
 * compartirlo. Android 10+ (scoped storage): Descargas/CajaiCafe vía MediaStore. Android 7–9: carpeta
 * de la app (Android/data/…/files/Download), compartida con FileProvider (los file:// están prohibidos
 * desde la 7).
 */
final class ArchivoDescargas {

    static final String CARPETA = "CajaiCafe";
    static final String MIME_PDF = "application/pdf";

    /** Lo que se escribe en el archivo (ej. {@code pdf::writeTo}). */
    interface Contenido {
        void escribir(OutputStream out) throws IOException;
    }

    private ArchivoDescargas() {}

    /**
     * El tipo de un .csv según este celular ("text/csv" o, en algunos, "text/comma-separated-values"):
     * si no coincide con la extensión, MediaStore le agrega otra (ej. "…csv.txt").
     */
    static String mimeCsv() {
        String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension("csv");
        return mime != null ? mime : "text/csv";
    }

    /** Escribe el contenido y devuelve un content:// que se puede abrir o compartir. */
    static Uri guardar(Context ctx, String nombre, String mime, Contenido contenido) throws IOException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentResolver resolver = ctx.getContentResolver();
            ContentValues valores = new ContentValues();
            valores.put(MediaStore.MediaColumns.DISPLAY_NAME, nombre); // si ya existe, Android agrega " (1)"
            valores.put(MediaStore.MediaColumns.MIME_TYPE, mime);
            valores.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/" + CARPETA);
            valores.put(MediaStore.MediaColumns.IS_PENDING, 1);
            Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, valores);
            if (uri == null) throw new IOException("no se pudo crear el archivo en Descargas");
            try (OutputStream out = resolver.openOutputStream(uri)) {
                if (out == null) throw new IOException("no se pudo abrir el archivo en Descargas");
                contenido.escribir(out);
            } catch (IOException e) {
                resolver.delete(uri, null, null); // no dejar un archivo vacío a medias
                throw e;
            }
            valores.clear();
            valores.put(MediaStore.MediaColumns.IS_PENDING, 0);
            resolver.update(uri, valores, null, null);
            return uri;
        }

        File carpeta = ctx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (carpeta == null) carpeta = new File(ctx.getFilesDir(), "reportes"); // sin almacenamiento externo
        if (!carpeta.isDirectory() && !carpeta.mkdirs()) throw new IOException("no se pudo crear la carpeta");
        File archivo = new File(carpeta, nombre);
        try (FileOutputStream out = new FileOutputStream(archivo)) {
            contenido.escribir(out);
        }
        return FileProvider.getUriForFile(ctx, ctx.getPackageName() + ".fileprovider", archivo);
    }

    /** Dónde lo va a encontrar la persona con el administrador de archivos. */
    static String dondeQuedo() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                ? "Descargas/" + CARPETA
                : "la carpeta de la app (Android/data/…/files/Download)";
    }

    /** Compartir uno o varios archivos del mismo tipo (WhatsApp, Gmail, Drive…). */
    static Intent compartir(List<Uri> uris, String mime, String titulo) {
        Intent enviar;
        if (uris.size() == 1) {
            enviar = new Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, uris.get(0));
        } else {
            enviar = new Intent(Intent.ACTION_SEND_MULTIPLE)
                    .putParcelableArrayListExtra(Intent.EXTRA_STREAM, new ArrayList<Uri>(uris));
        }
        enviar.setType(mime)
                .putExtra(Intent.EXTRA_SUBJECT, titulo)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        return Intent.createChooser(enviar, "Compartir " + titulo);
    }

    static Intent abrir(Uri uri, String mime) {
        return new Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, mime)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    }
}
