package com.example.cajaicafeadministracion;

import android.app.DatePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * "Registrar gasto": gastos que no son del costeo de un costal (gastos/{id}). Al guardar vuelve a la
 * pantalla de antes; la lista de gastos (y borrarlos) está en Movimientos → Gastos (GastosFragment).
 */
public class GastoActivity extends AppCompatActivity {

    Spinner spinnerCategoria, spinnerLote;
    EditText etMonto, etNota;
    Button btnFecha;

    DatabaseReference gastosRef, lotesRef;
    ValueEventListener lotesListener;

    // Opción 0 del selector de lote = gasto general; la opción i + 1 es lotesSelector.get(i).
    final List<Lote> lotesSelector = new ArrayList<>();
    // Día del gasto (por defecto hoy); la hora se toma al guardar.
    final Calendar fechaGasto = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gasto);

        spinnerCategoria = findViewById(R.id.spinnerCategoriaGasto);
        spinnerLote = findViewById(R.id.spinnerLoteGasto);
        etMonto = findViewById(R.id.etMontoGasto);
        etNota = findViewById(R.id.etNotaGasto);
        btnFecha = findViewById(R.id.btnFechaGasto);

        List<String> categorias = new ArrayList<>();
        for (String c : ValidacionGasto.CATEGORIAS) categorias.add(ValidacionGasto.etiquetaCategoria(c));
        ArrayAdapter<String> categoriaAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categorias);
        categoriaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategoria.setAdapter(categoriaAdapter);

        mostrarLotes(null);
        pintarFecha();
        btnFecha.setOnClickListener(v -> elegirFecha());
        findViewById(R.id.btnGuardarGasto).setOnClickListener(v -> guardarGasto());

        gastosRef = FirebaseDatabase.getInstance().getReference("gastos");
        lotesRef = FirebaseDatabase.getInstance().getReference("lotes");
        cargarLotes();
    }

    @Override
    protected void onDestroy() {
        if (lotesListener != null) lotesRef.removeEventListener(lotesListener);
        super.onDestroy();
    }

    private void cargarLotes() {
        lotesListener = lotesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String elegido = loteSeleccionadoId();
                lotesSelector.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Lote l = ds.getValue(Lote.class);
                    if (l == null) continue;
                    l.id = ds.getKey();
                    lotesSelector.add(l);
                }
                // El costal más reciente primero: es al que suele ir un gasto nuevo.
                lotesSelector.sort((a, b) -> {
                    int porFecha = texto(b.fechaInicio).compareTo(texto(a.fechaInicio));
                    return porFecha != 0 ? porFecha : texto(b.id).compareTo(texto(a.id));
                });
                mostrarLotes(elegido);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(GastoActivity.this, "Error cargando lotes: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /** Adaptador nuevo en cada cambio de lotes, manteniendo el lote que estaba elegido (ver VentaFragment). */
    private void mostrarLotes(@Nullable String elegidoId) {
        List<String> nombres = new ArrayList<>();
        nombres.add("Ninguno (gasto general)");
        int seleccion = 0;
        for (int i = 0; i < lotesSelector.size(); i++) {
            Lote l = lotesSelector.get(i);
            nombres.add(texto(l.codigo) + ("cerrado".equals(l.estado) ? " (cerrado)" : ""));
            if (l.id.equals(elegidoId)) seleccion = i + 1;
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, nombres);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLote.setAdapter(adapter);
        spinnerLote.setSelection(seleccion);
    }

    @Nullable
    private String loteSeleccionadoId() {
        int pos = spinnerLote.getSelectedItemPosition() - 1;
        if (pos < 0 || pos >= lotesSelector.size()) return null;
        return lotesSelector.get(pos).id;
    }

    private void elegirFecha() {
        DatePickerDialog dialogo = new DatePickerDialog(this, (picker, anio, mes, dia) -> {
            fechaGasto.set(anio, mes, dia);
            pintarFecha();
        }, fechaGasto.get(Calendar.YEAR), fechaGasto.get(Calendar.MONTH), fechaGasto.get(Calendar.DAY_OF_MONTH));
        dialogo.getDatePicker().setMaxDate(System.currentTimeMillis()); // un gasto no es a futuro
        dialogo.show();
    }

    private void pintarFecha() {
        String dia = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(fechaGasto.getTime());
        String hoy = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().getTime());
        btnFecha.setText(dia.equals(hoy) ? "Hoy (" + dia + ")" : dia);
    }

    /** Día elegido + hora actual, en el mismo formato que las ventas (yyyy-MM-dd HH:mm:ss). */
    private String fechaParaGuardar() {
        Calendar ahora = Calendar.getInstance();
        Calendar c = (Calendar) fechaGasto.clone();
        c.set(Calendar.HOUR_OF_DAY, ahora.get(Calendar.HOUR_OF_DAY));
        c.set(Calendar.MINUTE, ahora.get(Calendar.MINUTE));
        c.set(Calendar.SECOND, ahora.get(Calendar.SECOND));
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(c.getTime());
    }

    private void guardarGasto() {
        int pos = spinnerCategoria.getSelectedItemPosition();
        String categoria = (pos >= 0 && pos < ValidacionGasto.CATEGORIAS.size()) ? ValidacionGasto.CATEGORIAS.get(pos) : null;
        double monto = ValidacionGasto.parsearMonto(etMonto.getText().toString());
        String error = ValidacionGasto.validar(categoria, monto);
        if (error != null) {
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario == null) {
            Toast.makeText(this, "Tu sesión se cerró: vuelve a entrar", Toast.LENGTH_LONG).show();
            return;
        }

        String nota = etNota.getText().toString().trim();
        String key = gastosRef.push().getKey();
        String id = (key != null) ? key : "g" + System.currentTimeMillis();
        Gasto gasto = new Gasto(id, fechaParaGuardar(), categoria, monto, nota.isEmpty() ? null : nota,
                loteSeleccionadoId(), usuario.getUid());

        // Se limpia ya: un segundo toque en "Guardar" antes de cerrar no duplica el gasto (pide el monto otra vez).
        etMonto.setText("");
        etNota.setText("");

        String resumen = PrecioCalculator.formatear(monto) + " (" + ValidacionGasto.etiquetaCategoria(categoria) + ")";
        // La respuesta puede llegar después de cerrar la pantalla: el aviso usa el contexto de la app.
        final Context appCtx = getApplicationContext();
        gastosRef.child(id).setValue(gasto)
                .addOnSuccessListener(unused ->
                        Toast.makeText(appCtx, "Gasto registrado: " + resumen, Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(appCtx, "No se guardó el gasto de " + resumen + ": " + e.getMessage(), Toast.LENGTH_LONG).show());
        // Firebase lo escribe primero en el celular (aunque no haya señal): en Movimientos → Gastos ya aparece.
        finish();
    }

    private static String texto(String s) {
        return s == null ? "" : s;
    }
}
