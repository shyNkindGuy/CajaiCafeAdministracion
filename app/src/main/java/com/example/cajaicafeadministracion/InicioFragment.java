package com.example.cajaicafeadministracion;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.text.InputType;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Pestaña Inicio: resumen del mes elegido (ResumenFinanciero) y punto de equilibrio de los lotes abiertos. */
public class InicioFragment extends Fragment {

    private Spinner spinnerMes;
    private TextView tvUtilidad, tvMargen, tvIngresos, tvCobrado, tvPorCobrar;
    private TextView tvCostoVendido, tvSinCosto, tvGastosMes, tvSinLotes, tvUmbralesStock;
    private View cardStockBajo;
    private LinearLayout contenedorAvisosStock;
    private LinearLayout contenedorLotes;

    private DatabaseReference ventasRef, gastosRef, lotesRef, productosRef;
    private ValueEventListener ventasListener, gastosListener, lotesListener, productosListener;

    private final List<Venta> ventas = new ArrayList<>();
    private final List<Gasto> gastos = new ArrayList<>();
    private final Map<String, Lote> lotes = new LinkedHashMap<>();
    private final Map<String, Producto> productos = new HashMap<>();
    private final List<String> meses = new ArrayList<>();
    private String mesElegido = mesActual();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_inicio, container, false);

        spinnerMes = view.findViewById(R.id.spinnerMes);
        tvUtilidad = view.findViewById(R.id.tvUtilidad);
        tvMargen = view.findViewById(R.id.tvMargen);
        tvIngresos = view.findViewById(R.id.tvIngresos);
        tvCobrado = view.findViewById(R.id.tvCobrado);
        tvPorCobrar = view.findViewById(R.id.tvPorCobrar);
        tvCostoVendido = view.findViewById(R.id.tvCostoVendido);
        tvSinCosto = view.findViewById(R.id.tvSinCosto);
        tvGastosMes = view.findViewById(R.id.tvGastosMes);
        tvSinLotes = view.findViewById(R.id.tvSinLotes);
        contenedorLotes = view.findViewById(R.id.contenedorLotes);
        tvUmbralesStock = view.findViewById(R.id.tvUmbralesStock);
        cardStockBajo = view.findViewById(R.id.cardStockBajo);
        contenedorAvisosStock = view.findViewById(R.id.contenedorAvisosStock);
        view.findViewById(R.id.btnUmbralStock).setOnClickListener(v -> cambiarUmbrales());
        view.findViewById(R.id.btnCuentasPorCobrar).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), CuentasPorCobrarActivity.class)));

        spinnerMes.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                if (position >= 0 && position < meses.size()) mesElegido = meses.get(position);
                recalcular();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Aquí getView() ya no es null: el mes actual aparece aunque Firebase todavía no responda.
        actualizarMeses();
        recalcular();

        FirebaseDatabase db = FirebaseDatabase.getInstance();
        ventasRef = db.getReference("ventas");
        gastosRef = db.getReference("gastos");
        lotesRef = db.getReference("lotes");
        productosRef = db.getReference("productos");

        ventasListener = ventasRef.addValueEventListener(new Recarga() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ventas.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Venta v = ds.getValue(Venta.class);
                    if (v != null) ventas.add(v);
                }
                actualizarMeses();
                recalcular();
            }
        });
        gastosListener = gastosRef.addValueEventListener(new Recarga() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                gastos.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Gasto g = ds.getValue(Gasto.class);
                    if (g != null) gastos.add(g);
                }
                actualizarMeses();
                recalcular();
            }
        });
        lotesListener = lotesRef.addValueEventListener(new Recarga() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                lotes.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Lote l = ds.getValue(Lote.class);
                    if (l == null) continue;
                    l.id = ds.getKey();
                    lotes.put(l.id, l);
                }
                recalcular();
            }
        });
        productosListener = productosRef.addValueEventListener(new Recarga() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                productos.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Producto p = ds.getValue(Producto.class);
                    if (p == null) continue;
                    p.id = ds.getKey();
                    productos.put(p.id, p);
                }
                recalcular();
            }
        });
    }

    @Override
    public void onDestroyView() {
        if (ventasListener != null) ventasRef.removeEventListener(ventasListener);
        if (gastosListener != null) gastosRef.removeEventListener(gastosListener);
        if (lotesListener != null) lotesRef.removeEventListener(lotesListener);
        if (productosListener != null) productosRef.removeEventListener(productosListener);
        super.onDestroyView();
    }

    /** Listener que solo redefine onDataChange: si falla una lectura, el resumen se queda como estaba. */
    private abstract static class Recarga implements ValueEventListener {
        @Override
        public void onCancelled(@NonNull DatabaseError error) {}
    }

    private static String mesActual() {
        return new SimpleDateFormat("yyyy-MM", Locale.US).format(new Date());
    }

    /**
     * Opciones del selector: meses con ventas o gastos más el actual. Adaptador nuevo solo si cambian,
     * manteniendo el mes elegido (ver VentaFragment.mostrarLotes).
     */
    private void actualizarMeses() {
        if (getView() == null) return;
        List<String> fechas = new ArrayList<>();
        for (Venta v : ventas) fechas.add(v.fecha);
        for (Gasto g : gastos) fechas.add(g.fecha);
        List<String> nuevos = ResumenFinanciero.meses(fechas, mesActual());
        if (nuevos.equals(meses)) return;
        meses.clear();
        meses.addAll(nuevos);

        List<String> etiquetas = new ArrayList<>();
        for (String m : meses) etiquetas.add(ResumenFinanciero.etiquetaMes(m));
        ArrayAdapter<String> adapter = new ArrayAdapter<>(spinnerMes.getContext(),
                android.R.layout.simple_spinner_item, etiquetas);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMes.setAdapter(adapter);
        int pos = meses.indexOf(mesElegido);
        spinnerMes.setSelection(Math.max(pos, 0));
    }

    private void recalcular() {
        if (getView() == null) return;
        Map<String, String> nombrePorId = new HashMap<>();
        for (Producto p : productos.values()) nombrePorId.put(p.id, p.nombre);

        ResumenFinanciero r = ResumenFinanciero.delMes(mesElegido, ventas, gastos, lotes, nombrePorId);
        tvUtilidad.setText(PrecioCalculator.formatear(r.utilidadNeta));
        tvUtilidad.setTextColor(ContextCompat.getColor(tvUtilidad.getContext(),
                r.utilidadNeta < 0 ? R.color.negativo : R.color.positivo));
        tvMargen.setText(ResumenFinanciero.textoMargen(r.margen()));

        tvIngresos.setText("Ventas registradas (" + r.ventas + "): " + PrecioCalculator.formatear(r.ingresos));
        tvCobrado.setText("Cobrado: " + PrecioCalculator.formatear(r.cobrado));
        tvPorCobrar.setText("Por cobrar: " + PrecioCalculator.formatear(r.porCobrar));

        tvCostoVendido.setText("Costo del café vendido: " + PrecioCalculator.formatear(r.costoVendido));
        if (r.ventasSinCosto > 0) {
            tvSinCosto.setText(r.ventasSinCosto + (r.ventasSinCosto == 1 ? " venta" : " ventas")
                    + " sin costo conocido (1 kg, grano, sin lote o lote sin kg tostado): no suman al costo.");
            tvSinCosto.setVisibility(View.VISIBLE);
        } else {
            tvSinCosto.setVisibility(View.GONE);
        }
        tvGastosMes.setText("Gastos del mes: " + PrecioCalculator.formatear(r.gastos));

        pintarLotes();
        pintarAvisosStock();
    }

    /** Tarjeta naranja arriba con los lotes abiertos que tienen pocas bolsas (AlertaStock). */
    private void pintarAvisosStock() {
        int umbral12 = AlertaStock.umbral(productos.get("medio_kilo"));
        int umbral14 = AlertaStock.umbral(productos.get("cuarto_kilo"));
        tvUmbralesStock.setText(AlertaStock.textoUmbrales(umbral12, umbral14));

        List<String> avisos = AlertaStock.avisos(lotes.values(), umbral12, umbral14);
        contenedorAvisosStock.removeAllViews();
        for (String aviso : avisos) {
            TextView fila = new TextView(contenedorAvisosStock.getContext());
            fila.setText("• " + aviso);
            contenedorAvisosStock.addView(fila);
        }
        cardStockBajo.setVisibility(avisos.isEmpty() ? View.GONE : View.VISIBLE);
    }

    /**
     * Cambia el umbral de cada tamaño (productos/medio_kilo|cuarto_kilo/stockMinimo). Solo escribe en
     * productos que ya existen: escribir en uno que no existe crearía un producto sin nombre ni precio.
     */
    private void cambiarUmbrales() {
        Producto medio = productos.get("medio_kilo");
        Producto cuarto = productos.get("cuarto_kilo");
        if (medio == null && cuarto == null) {
            Toast.makeText(requireContext(), "Primero crea los productos medio_kilo y cuarto_kilo en Firebase", Toast.LENGTH_LONG).show();
            return;
        }
        Context ctx = requireContext();
        LinearLayout layout = new LinearLayout(ctx);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 16, 48, 0);
        EditText et12 = campoUmbral(ctx, layout, "Bolsas de 1/2 kg", medio);
        EditText et14 = campoUmbral(ctx, layout, "Bolsas de 1/4 kg", cuarto);

        new AlertDialog.Builder(ctx)
                .setTitle("Aviso de stock bajo")
                .setMessage("Avisar cuando un lote abierto tenga esta cantidad de bolsas o menos. 0 = sin aviso.")
                .setView(layout)
                .setPositiveButton("Guardar", (d, w) -> {
                    int n12 = medio != null ? AlertaStock.parsearUmbral(et12.getText().toString()) : 0;
                    int n14 = cuarto != null ? AlertaStock.parsearUmbral(et14.getText().toString()) : 0;
                    if (n12 < 0 || n14 < 0) {
                        Toast.makeText(ctx, "Escribe un número entero (0 = sin aviso)", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Map<String, Object> cambios = new HashMap<>();
                    if (medio != null) cambios.put("medio_kilo/stockMinimo", n12);
                    if (cuarto != null) cambios.put("cuarto_kilo/stockMinimo", n14);
                    final Context appCtx = ctx.getApplicationContext();
                    productosRef.updateChildren(cambios)
                            .addOnSuccessListener(unused -> Toast.makeText(appCtx, "Aviso de stock guardado", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(appCtx, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private static EditText campoUmbral(Context ctx, LinearLayout layout, String nombre, @Nullable Producto p) {
        TextView etiqueta = new TextView(ctx);
        etiqueta.setText(nombre);
        layout.addView(etiqueta);
        EditText et = new EditText(ctx);
        et.setInputType(InputType.TYPE_CLASS_NUMBER);
        if (p != null) {
            et.setText(String.valueOf(AlertaStock.umbral(p)));
        } else {
            et.setEnabled(false);
            et.setHint("no existe en productos/");
        }
        layout.addView(et);
        return et;
    }

    /** Una fila por lote abierto (del más antiguo al más nuevo) con su punto de equilibrio. */
    private void pintarLotes() {
        contenedorLotes.removeAllViews();
        List<Lote> abiertos = StockLote.abiertosFifo(lotes.values());
        tvSinLotes.setVisibility(abiertos.isEmpty() ? View.VISIBLE : View.GONE);
        double precioBolsa = ResumenFinanciero.precioEfectivo(productos.get("medio_kilo"));
        int padding = Math.round(6 * getResources().getDisplayMetrics().density);
        for (Lote l : abiertos) {
            ResumenFinanciero.Equilibrio e = ResumenFinanciero.equilibrio(l, ventas, gastos, precioBolsa);
            TextView fila = new TextView(contenedorLotes.getContext());
            fila.setText(ResumenFinanciero.textoEquilibrio(l.codigo, e, precioBolsa));
            fila.setTextColor(ContextCompat.getColor(fila.getContext(), e.recuperado() ? R.color.positivo : R.color.textoMarca));
            fila.setPadding(0, padding, 0, padding);
            contenedorLotes.addView(fila);
        }
    }
}
