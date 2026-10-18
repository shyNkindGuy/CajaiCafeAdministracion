package com.example.cajaicafeadministracion;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DashboardFragment extends Fragment {

    TextView tvTotalMedio, tvTotalCuarto, tvTotalDinero;
    RadioButton rbPorMes;
    Spinner spinnerReporte;
    Button btnExportar;
    DatabaseReference ventasRef, gastosRef, lotesRef, productosRef;
    ValueEventListener ventasListener, gastosListener, lotesListener, productosListener;
    List<Venta> listaVentas = new ArrayList<>();

    // Para el PDF por mes o por lote (Reporte).
    final List<Gasto> gastos = new ArrayList<>();
    final Map<String, Lote> lotes = new LinkedHashMap<>();
    final Map<String, Producto> productos = new HashMap<>();
    // Lo que muestra el selector: meses "yyyy-MM" o ids de lote, en el mismo orden.
    final List<String> clavesReporte = new ArrayList<>();
    boolean opcionesPorMes = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        tvTotalMedio = view.findViewById(R.id.tvTotalMedioKilo);
        tvTotalCuarto = view.findViewById(R.id.tvTotalCuartoKilo);
        tvTotalDinero = view.findViewById(R.id.tvTotalDinero);
        rbPorMes = view.findViewById(R.id.rbPorMes);
        spinnerReporte = view.findViewById(R.id.spinnerReporte);
        btnExportar = view.findViewById(R.id.btnExportarPDF);

        FirebaseDatabase db = FirebaseDatabase.getInstance();
        ventasRef = db.getReference("ventas");
        gastosRef = db.getReference("gastos");
        lotesRef = db.getReference("lotes");
        productosRef = db.getReference("productos");

        cargarDatos();
        cargarDatosReporte();

        ((RadioGroup) view.findViewById(R.id.rgTipoReporte)).setOnCheckedChangeListener((g, id) -> actualizarOpcionesReporte());
        btnExportar.setOnClickListener(v -> generarPDF());
        view.findViewById(R.id.btnExportarCSV).setOnClickListener(v -> exportarCsv());
        view.findViewById(R.id.btnRegistrarGasto).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), GastoActivity.class)));

        TextView tvSesion = view.findViewById(R.id.tvSesion);
        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        tvSesion.setText(usuario != null ? "Sesión: " + usuario.getEmail() : "Sin sesión");
        view.findViewById(R.id.btnCerrarSesion).setOnClickListener(v -> confirmarCierreSesion());

        return view;
    }

    private void confirmarCierreSesion() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Cerrar sesión")
                .setMessage("¿Quieres salir de tu cuenta en este celular?")
                .setPositiveButton("Salir", (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(requireContext(), LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        if (ventasListener != null) ventasRef.removeEventListener(ventasListener);
        if (gastosListener != null) gastosRef.removeEventListener(gastosListener);
        if (lotesListener != null) lotesRef.removeEventListener(lotesListener);
        if (productosListener != null) productosRef.removeEventListener(productosListener);
        super.onDestroyView();
    }

    private void cargarDatos() {
        ventasListener = ventasRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                listaVentas.clear();
                int countMedio = 0;
                int countCuarto = 0;
                double totalDineroRecaudado = 0; // bolsas pagadas

                for (DataSnapshot ds : snapshot.getChildren()) {
                    Venta v = ds.getValue(Venta.class);
                    if (v != null) {
                        listaVentas.add(v);

                        if (v.estadoPago != null) {
                            if (v.estadoPago.equals("pagado")) {
                                // Si está pagado, sumamos el total completo
                                totalDineroRecaudado += v.total;
                            } else if (v.estadoPago.equals("parcial")) {
                                // Si es parcial, sumamos SOLO lo que dejó a cuenta
                                totalDineroRecaudado += v.montoParcial;
                            }
                        }
                        //bolsas
                        if (v.producto != null) {
                            if (v.producto.contains("1/2")) {
                                countMedio += v.cantidad;
                            } else if (v.producto.contains("1/4")) {
                                countCuarto += v.cantidad;
                            }
                        }
                    }
                }

                // Actualizamos la UI
                tvTotalMedio.setText(String.valueOf(countMedio));
                tvTotalCuarto.setText(String.valueOf(countCuarto));

                // Mostramos el total recaudado en la nueva tarjeta verde
                tvTotalDinero.setText("S/ " + String.format("%.2f", totalDineroRecaudado));
                actualizarOpcionesReporte();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    /** Gastos, lotes y productos: lo que el PDF necesita además de las ventas. */
    private void cargarDatosReporte() {
        gastosListener = gastosRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                gastos.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Gasto g = ds.getValue(Gasto.class);
                    if (g != null) gastos.add(g);
                }
                actualizarOpcionesReporte();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
        lotesListener = lotesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                lotes.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Lote l = ds.getValue(Lote.class);
                    if (l == null) continue;
                    l.id = ds.getKey();
                    lotes.put(l.id, l);
                }
                actualizarOpcionesReporte();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
        productosListener = productosRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                productos.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Producto p = ds.getValue(Producto.class);
                    if (p == null) continue;
                    p.id = ds.getKey();
                    productos.put(p.id, p);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    /** Opciones del selector según el tipo: meses con movimientos (más el actual) o lotes, el más reciente primero. */
    private void actualizarOpcionesReporte() {
        if (getView() == null) return;
        String elegido = claveElegida();
        boolean porMes = rbPorMes.isChecked();
        List<String> claves = new ArrayList<>();
        List<String> etiquetas = new ArrayList<>();
        if (porMes) {
            List<String> fechas = new ArrayList<>();
            for (Venta v : listaVentas) fechas.add(v.fecha);
            for (Gasto g : gastos) fechas.add(g.fecha);
            for (String mes : ResumenFinanciero.meses(fechas, new SimpleDateFormat("yyyy-MM", Locale.US).format(new Date()))) {
                claves.add(mes);
                etiquetas.add(ResumenFinanciero.etiquetaMes(mes));
            }
        } else {
            List<Lote> ordenados = new ArrayList<>(lotes.values());
            ordenados.sort((a, b) -> texto(b.fechaInicio).compareTo(texto(a.fechaInicio)));
            for (Lote l : ordenados) {
                claves.add(l.id);
                etiquetas.add(texto(l.codigo) + ("cerrado".equals(l.estado) ? " (cerrado)" : ""));
            }
            if (ordenados.isEmpty()) etiquetas.add("No hay lotes todavía");
        }
        if (porMes == opcionesPorMes && claves.equals(clavesReporte)) return; // nada cambió: no mover el selector
        opcionesPorMes = porMes;
        clavesReporte.clear();
        clavesReporte.addAll(claves);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(spinnerReporte.getContext(),
                android.R.layout.simple_spinner_item, etiquetas);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerReporte.setAdapter(adapter);
        spinnerReporte.setSelection(Math.max(clavesReporte.indexOf(elegido), 0));
    }

    @Nullable
    private String claveElegida() {
        int pos = spinnerReporte.getSelectedItemPosition();
        return (pos >= 0 && pos < clavesReporte.size()) ? clavesReporte.get(pos) : null;
    }

    /** Arma el reporte elegido (Reporte), lo pinta (GeneradorPdf) y lo guarda sin pedir permisos (ArchivoDescargas). */
    private void generarPDF() {
        String clave = claveElegida();
        if (clave == null) {
            Toast.makeText(requireContext(), "Elige un mes o un lote", Toast.LENGTH_SHORT).show();
            return;
        }
        String generadoEl = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US).format(new Date());
        Reporte reporte;
        if (opcionesPorMes) {
            Map<String, String> nombrePorId = new HashMap<>();
            for (Producto p : productos.values()) nombrePorId.put(p.id, p.nombre);
            reporte = Reporte.delMes(clave, listaVentas, gastos, lotes, nombrePorId, generadoEl);
        } else {
            Lote lote = lotes.get(clave);
            if (lote == null) return;
            double precioBolsa = ResumenFinanciero.precioEfectivo(productos.get("medio_kilo"));
            reporte = Reporte.delLote(lote, listaVentas, gastos, precioBolsa, generadoEl);
        }

        PdfDocument pdf = GeneradorPdf.generar(reporte);
        Uri uri;
        try {
            uri = ArchivoDescargas.guardar(requireContext(), reporte.nombreArchivo, ArchivoDescargas.MIME_PDF, pdf::writeTo);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "No se pudo guardar el PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        } finally {
            pdf.close();
        }
        mostrarPdfListo(reporte, uri);
    }

    private void mostrarPdfListo(Reporte reporte, Uri uri) {
        new AlertDialog.Builder(requireContext())
                .setTitle("PDF listo")
                .setMessage(reporte.titulo + "\n\nGuardado en " + ArchivoDescargas.dondeQuedo() + " como " + reporte.nombreArchivo + ".")
                .setPositiveButton("Compartir", (d, w) -> abrirIntent(ArchivoDescargas.compartir(
                        Collections.singletonList(uri), ArchivoDescargas.MIME_PDF, reporte.titulo)))
                .setNeutralButton("Abrir", (d, w) -> abrirIntent(ArchivoDescargas.abrir(uri, ArchivoDescargas.MIME_PDF)))
                .setNegativeButton("Cerrar", null)
                .show();
    }

    /** Ventas y gastos de lo elegido (mes o lote) en dos CSV (ExportacionCsv) para abrir en Excel o Google Sheets. */
    private void exportarCsv() {
        String clave = claveElegida();
        if (clave == null) {
            Toast.makeText(requireContext(), "Elige un mes o un lote", Toast.LENGTH_SHORT).show();
            return;
        }
        Lote lote = opcionesPorMes ? null : lotes.get(clave);
        if (!opcionesPorMes && lote == null) return;
        String nombre = opcionesPorMes ? clave : lote.codigo; // para el archivo: "2026-10" o "LOTE1-NOV2025"
        String titulo = opcionesPorMes ? ResumenFinanciero.etiquetaMes(clave) : "lote " + lote.codigo;

        List<Venta> ventasElegidas = ExportacionCsv.ventasDe(listaVentas, opcionesPorMes, clave);
        List<Gasto> gastosElegidos = ExportacionCsv.gastosDe(gastos, opcionesPorMes, clave);
        byte[] csvVentas = ExportacionCsv.ventas(ventasElegidas, lotes).getBytes(StandardCharsets.UTF_8);
        byte[] csvGastos = ExportacionCsv.gastos(gastosElegidos, lotes).getBytes(StandardCharsets.UTF_8);
        String archivoVentas = ExportacionCsv.nombreArchivo("Ventas", nombre);
        String archivoGastos = ExportacionCsv.nombreArchivo("Gastos", nombre);
        String mime = ArchivoDescargas.mimeCsv();

        List<Uri> uris = new ArrayList<>();
        try {
            uris.add(ArchivoDescargas.guardar(requireContext(), archivoVentas, mime, out -> out.write(csvVentas)));
            uris.add(ArchivoDescargas.guardar(requireContext(), archivoGastos, mime, out -> out.write(csvGastos)));
        } catch (Exception e) {
            Toast.makeText(requireContext(), "No se pudo guardar el CSV: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }
        new AlertDialog.Builder(requireContext())
                .setTitle("CSV listos")
                .setMessage("Ventas de " + titulo + ": " + ventasElegidas.size() + " filas.\nGastos: " + gastosElegidos.size()
                        + " filas.\n\nGuardados en " + ArchivoDescargas.dondeQuedo() + " como " + archivoVentas + " y "
                        + archivoGastos + ". Se abren con Excel o Google Sheets.")
                .setPositiveButton("Compartir", (d, w) -> abrirIntent(ArchivoDescargas.compartir(uris, mime, "CSV de " + titulo)))
                .setNeutralButton("Abrir ventas", (d, w) -> abrirIntent(ArchivoDescargas.abrir(uris.get(0), mime)))
                .setNegativeButton("Cerrar", null)
                .show();
    }

    private void abrirIntent(Intent intent) {
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(requireContext(), "No hay ninguna app para abrir este archivo en el celular", Toast.LENGTH_LONG).show();
        }
    }

    private static String texto(String s) {
        return s == null ? "" : s;
    }
}
