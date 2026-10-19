package com.example.cajaicafeadministracion;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Movimientos → Gastos: los últimos gastos (gastos/{id}), el más reciente arriba, y el botón para
 * registrar otro (GastoActivity). Mantener presionado un gasto lo borra, con confirmación.
 */
public class GastosFragment extends Fragment {

    private static final int MAX_GASTOS_LISTA = 100;

    LinearLayout contenedorGastos;
    TextView tvSinGastos;

    DatabaseReference gastosRef, lotesRef;
    Query ultimosGastos;
    ValueEventListener gastosListener, lotesListener;

    final List<Gasto> ultimos = new ArrayList<>();
    // Código de cada lote, para mostrar en la fila a qué costal fue el gasto.
    final Map<String, String> codigoPorLote = new HashMap<>();
    boolean gastosCargados; // hasta que llegan, no se dice "no hay gastos"

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_gastos, container, false);
        contenedorGastos = view.findViewById(R.id.contenedorGastos);
        tvSinGastos = view.findViewById(R.id.tvSinGastos);
        view.findViewById(R.id.btnNuevoGasto).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), GastoActivity.class)));

        gastosRef = FirebaseDatabase.getInstance().getReference("gastos");
        lotesRef = FirebaseDatabase.getInstance().getReference("lotes");
        // Los errores pueden llegar después de cambiar de pestaña: el aviso usa el contexto de la app.
        final Context appCtx = requireContext().getApplicationContext();

        lotesListener = lotesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                codigoPorLote.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Lote l = ds.getValue(Lote.class);
                    if (l != null) codigoPorLote.put(ds.getKey(), l.codigo);
                }
                pintarGastos();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(appCtx, "Error cargando lotes: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        ultimosGastos = gastosRef.orderByChild("fecha").limitToLast(MAX_GASTOS_LISTA);
        gastosListener = ultimosGastos.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ultimos.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Gasto g = ds.getValue(Gasto.class);
                    if (g == null) continue;
                    g.id = ds.getKey();
                    ultimos.add(g);
                }
                Collections.reverse(ultimos); // el más reciente arriba
                gastosCargados = true;
                pintarGastos();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(appCtx, "Error cargando gastos: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    @Override
    public void onDestroyView() {
        if (lotesListener != null) lotesRef.removeEventListener(lotesListener);
        if (gastosListener != null) ultimosGastos.removeEventListener(gastosListener);
        super.onDestroyView();
    }

    private void pintarGastos() {
        if (getView() == null) return;
        Context ctx = contenedorGastos.getContext();
        contenedorGastos.removeAllViews();
        tvSinGastos.setVisibility(gastosCargados && ultimos.isEmpty() ? View.VISIBLE : View.GONE);
        int padding = Math.round(10 * getResources().getDisplayMetrics().density);
        for (Gasto g : ultimos) {
            TextView fila = new TextView(ctx);
            fila.setText(ValidacionGasto.etiqueta(g, codigoLote(g)));
            fila.setTextSize(14);
            fila.setTextColor(ContextCompat.getColor(ctx, R.color.texto));
            fila.setPadding(0, padding, 0, padding);
            fila.setOnLongClickListener(v -> {
                confirmarBorrado(g);
                return true;
            });
            contenedorGastos.addView(fila);
        }
    }

    @Nullable
    private String codigoLote(Gasto g) {
        return g.loteId == null ? null : codigoPorLote.get(g.loteId);
    }

    private void confirmarBorrado(Gasto g) {
        final Context appCtx = requireContext().getApplicationContext();
        new AlertDialog.Builder(requireContext())
                .setTitle("Borrar gasto")
                .setMessage(ValidacionGasto.etiqueta(g, codigoLote(g)))
                .setPositiveButton("Borrar", (dialog, which) ->
                        gastosRef.child(g.id).removeValue()
                                .addOnSuccessListener(unused -> Toast.makeText(appCtx, "Gasto borrado", Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e -> Toast.makeText(appCtx, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()))
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
