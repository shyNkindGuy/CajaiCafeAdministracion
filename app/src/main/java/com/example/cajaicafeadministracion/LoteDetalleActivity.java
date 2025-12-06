package com.example.cajaicafeadministracion;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.*;

import java.util.Locale;

public class LoteDetalleActivity extends AppCompatActivity {

    String loteId;
    Lote loteActual;

    TextView tvCodigo, tvEstado, tvFecha;
    TextView tvCostoPergamino, tvCostoPilado, tvCostoTostado, tvCostoFlete,
            tvCostoElectricidad, tvCostoEmpaque, tvTotalGastos;
    TextView tvVentaBruta, tvCobrado, tvGananciaParcial;
    TextView tvStock12, tvStock14;
    Button btnEmpacar, btnCerrarLote;

    DatabaseReference loteRef;
    DatabaseReference ventasRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lote_detalle);

        loteId = getIntent().getStringExtra("loteId");
        if (loteId == null) {
            Toast.makeText(this, "Lote no válido", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvCodigo = findViewById(R.id.tvDetCodigo);
        tvEstado = findViewById(R.id.tvDetEstado);
        tvFecha = findViewById(R.id.tvDetFecha);
        tvCostoPergamino = findViewById(R.id.tvDetPergamino);
        tvCostoPilado = findViewById(R.id.tvDetPilado);
        tvCostoTostado = findViewById(R.id.tvDetTostado);
        tvCostoFlete = findViewById(R.id.tvDetFlete);
        tvCostoElectricidad = findViewById(R.id.tvDetElectricidad);
        tvCostoEmpaque = findViewById(R.id.tvDetEmpaque);
        tvTotalGastos = findViewById(R.id.tvDetTotalGastos);
        tvVentaBruta = findViewById(R.id.tvDetVentaBruta);
        tvCobrado = findViewById(R.id.tvDetCobrado);
        tvGananciaParcial = findViewById(R.id.tvDetGanancia);
        tvStock12 = findViewById(R.id.tvDetStock12);
        tvStock14 = findViewById(R.id.tvDetStock14);
        btnEmpacar = findViewById(R.id.btnRegistrarEmpacado);
        btnCerrarLote = findViewById(R.id.btnCerrarLote);

        loteRef = FirebaseDatabase.getInstance().getReference("lotes").child(loteId);
        ventasRef = FirebaseDatabase.getInstance().getReference("ventas");

        cargarLote();
        cargarVentasDelLote();

        btnEmpacar.setOnClickListener(v -> mostrarDialogoEmpacado());
        btnCerrarLote.setOnClickListener(v -> confirmarCierre());
    }

    private void cargarLote() {
        loteRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Lote l = snapshot.getValue(Lote.class);
                if (l == null) return;
                l.id = loteId;
                loteActual = l;
                pintarLote(l);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(LoteDetalleActivity.this, "Error cargando el lote: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void pintarLote(Lote l) {
        tvCodigo.setText(l.codigo);
        tvEstado.setText("cerrado".equals(l.estado) ? "CERRADO" : "ABIERTO");
        tvFecha.setText("Inicio: " + l.fechaInicio);

        tvCostoPergamino.setText(String.format(Locale.US, "Café pergamino (%.2f kg): S/ %.2f", l.pergaminoKg, l.pergaminoTotal));
        tvCostoPilado.setText(String.format(Locale.US, "Servicio pilado (%.2f kg): S/ %.2f", l.piladoKg, l.piladoTotal));
        tvCostoTostado.setText(String.format(Locale.US, "Servicio tostado (%.2f kg): S/ %.2f", l.tostadoKg, l.tostadoTotal));
        tvCostoFlete.setText(String.format(Locale.US, "Transporte (flete): S/ %.2f", l.flete));
        tvCostoElectricidad.setText(String.format(Locale.US, "Electricidad (selladora+moledora): S/ %.2f", l.electricidadTotal));
        tvCostoEmpaque.setText(String.format(Locale.US, "Empaque (temporal, manual): S/ %.2f", l.empaqueTotal));
        tvTotalGastos.setText(String.format(Locale.US, "TOTAL GASTOS DEL LOTE: S/ %.2f", l.totalGastos));

        tvStock12.setText(String.valueOf(l.stockBolsas12));
        tvStock14.setText(String.valueOf(l.stockBolsas14));

        btnCerrarLote.setVisibility("cerrado".equals(l.estado) ? View.GONE : View.VISIBLE);

        recalcularGanancia();
    }

    private double ventaBrutaAcumulada = 0;
    private double cobradoAcumulado = 0;

    private void cargarVentasDelLote() {
        // Requiere loteId dentro de cada venta (se agrega cuando se conecte VentaFragment).
        // Mientras tanto, esta consulta simplemente no encontrará nada y mostrará S/0.00 — no falla.
        ventasRef.orderByChild("loteId").equalTo(loteId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ventaBrutaAcumulada = 0;
                cobradoAcumulado = 0;
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Venta v = ds.getValue(Venta.class);
                    if (v == null) continue;
                    ventaBrutaAcumulada += v.total;
                    if ("pagado".equals(v.estadoPago)) {
                        cobradoAcumulado += v.total;
                    } else if ("parcial".equals(v.estadoPago)) {
                        cobradoAcumulado += v.montoParcial;
                    }
                }
                recalcularGanancia();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Si no existe el índice ".indexOn": ["loteId"] en las reglas, esta consulta puede
                // fallar o ser lenta. Ver INSTRUCCIONES.md para la regla exacta a agregar.
            }
        });
    }

    private void recalcularGanancia() {
        tvVentaBruta.setText(String.format(Locale.US, "Ventas registradas: S/ %.2f", ventaBrutaAcumulada));
        tvCobrado.setText(String.format(Locale.US, "Cobrado hasta hoy: S/ %.2f", cobradoAcumulado));
        if (loteActual != null) {
            double ganancia = ventaBrutaAcumulada - loteActual.totalGastos;
            tvGananciaParcial.setText(String.format(Locale.US, "Ganancia parcial (ventas - gastos): S/ %.2f", ganancia));
        }
    }

    private void mostrarDialogoEmpacado() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 24);

        EditText et12 = new EditText(this);
        et12.setHint("Bolsas de 1/2 kg empacadas ahora");
        et12.setInputType(InputType.TYPE_CLASS_NUMBER);
        layout.addView(et12);

        EditText et14 = new EditText(this);
        et14.setHint("Bolsas de 1/4 kg empacadas ahora");
        et14.setInputType(InputType.TYPE_CLASS_NUMBER);
        layout.addView(et14);

        new AlertDialog.Builder(this)
                .setTitle("Registrar empacado")
                .setView(layout)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    int nuevas12 = parseIntSafe(et12.getText().toString());
                    int nuevas14 = parseIntSafe(et14.getText().toString());
                    if (nuevas12 == 0 && nuevas14 == 0) return;
                    sumarStockConTransaccion(nuevas12, nuevas14);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private int parseIntSafe(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private void sumarStockConTransaccion(int add12, int add14) {
        loteRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                Lote l = currentData.getValue(Lote.class);
                if (l == null) return Transaction.success(currentData);
                l.stockBolsas12 += add12;
                l.stockBolsas14 += add14;
                currentData.setValue(l);
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                if (error != null) {
                    Toast.makeText(LoteDetalleActivity.this, "Error actualizando stock: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                } else if (committed) {
                    Toast.makeText(LoteDetalleActivity.this, "Empacado registrado", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void confirmarCierre() {
        new AlertDialog.Builder(this)
                .setTitle("Cerrar lote")
                .setMessage("El lote deja de sugerirse para nuevas ventas, pero sigue disponible para consulta y reporte. ¿Confirmas el cierre?")
                .setPositiveButton("Cerrar lote", (dialog, which) ->
                        loteRef.child("estado").setValue("cerrado")
                                .addOnSuccessListener(unused -> Toast.makeText(this, "Lote cerrado", Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()))
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
