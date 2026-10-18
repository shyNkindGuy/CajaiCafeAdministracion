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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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

    /** Arma el reporte elegido (Reporte), lo pinta (GeneradorPdf) y lo guarda sin pedir permisos (ArchivoPdf). */
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
            uri = ArchivoPdf.guardar(requireContext(), pdf, reporte.nombreArchivo);
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
                .setMessage(reporte.titulo + "\n\nGuardado en " + ArchivoPdf.dondeQuedo() + " como " + reporte.nombreArchivo + ".")
                .setPositiveButton("Compartir", (d, w) -> abrirIntent(ArchivoPdf.compartir(uri, reporte.titulo)))
                .setNeutralButton("Abrir", (d, w) -> abrirIntent(ArchivoPdf.abrir(uri)))
                .setNegativeButton("Cerrar", null)
                .show();
    }

    private void abrirIntent(Intent intent) {
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(requireContext(), "No hay ninguna app para abrir PDF en este celular", Toast.LENGTH_LONG).show();
        }
    }

    private static String texto(String s) {
        return s == null ? "" : s;
    }
}
