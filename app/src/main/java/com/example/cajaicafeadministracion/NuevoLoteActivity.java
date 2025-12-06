package com.example.cajaicafeadministracion;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NuevoLoteActivity extends AppCompatActivity {

    EditText etCodigo;
    EditText etPergaminoKg, etPergaminoPrecio;
    EditText etPiladoKg, etPiladoPrecio;
    EditText etTostadoKg, etTostadoPrecio;
    EditText etFlete;
    EditText etPotSelladora, etHorasSelladora, etPotMoledora, etHorasMoledora, etPrecioKwh;
    EditText etEmpaqueTotal;
    EditText etStockBolsas12, etStockBolsas14;

    DatabaseReference lotesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nuevo_lote);

        etCodigo = findViewById(R.id.etCodigoLote);
        etPergaminoKg = findViewById(R.id.etPergaminoKg);
        etPergaminoPrecio = findViewById(R.id.etPergaminoPrecio);
        etPiladoKg = findViewById(R.id.etPiladoKg);
        etPiladoPrecio = findViewById(R.id.etPiladoPrecio);
        etTostadoKg = findViewById(R.id.etTostadoKg);
        etTostadoPrecio = findViewById(R.id.etTostadoPrecio);
        etFlete = findViewById(R.id.etFlete);
        etPotSelladora = findViewById(R.id.etPotSelladora);
        etHorasSelladora = findViewById(R.id.etHorasSelladora);
        etPotMoledora = findViewById(R.id.etPotMoledora);
        etHorasMoledora = findViewById(R.id.etHorasMoledora);
        etPrecioKwh = findViewById(R.id.etPrecioKwh);
        etEmpaqueTotal = findViewById(R.id.etEmpaqueTotal);
        etStockBolsas12 = findViewById(R.id.etStockBolsas12);
        etStockBolsas14 = findViewById(R.id.etStockBolsas14);

        // Sugerencia de código: LOTE-<mes-año>. El usuario puede editarlo.
        String sugerencia = "LOTE-" + new SimpleDateFormat("MMMyyyy", new Locale("es", "PE")).format(new Date()).toUpperCase(Locale.ROOT);
        etCodigo.setText(sugerencia);

        lotesRef = FirebaseDatabase.getInstance().getReference("lotes");

        findViewById(R.id.btnGuardarLote).setOnClickListener(v -> guardarLote());
    }

    private double d(EditText et) {
        String s = et.getText().toString().trim();
        if (s.isEmpty()) return 0;
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private int i(EditText et) {
        String s = et.getText().toString().trim();
        if (s.isEmpty()) return 0;
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void guardarLote() {
        String codigo = etCodigo.getText().toString().trim();
        if (codigo.isEmpty()) {
            Toast.makeText(this, "Ponle un código al lote (ej. LOTE2-DIC2025)", Toast.LENGTH_SHORT).show();
            return;
        }

        String key = lotesRef.push().getKey();
        final String idLote = (key != null) ? key : "l" + System.currentTimeMillis();
        String fecha = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        Lote lote = new Lote(idLote, codigo, fecha);

        lote.pergaminoKg = d(etPergaminoKg);
        lote.pergaminoPrecioKg = d(etPergaminoPrecio);
        lote.pergaminoTotal = lote.pergaminoKg * lote.pergaminoPrecioKg;

        lote.piladoKg = d(etPiladoKg);
        lote.piladoPrecioKg = d(etPiladoPrecio);
        lote.piladoTotal = lote.piladoKg * lote.piladoPrecioKg;

        lote.tostadoKg = d(etTostadoKg);
        lote.tostadoPrecioKg = d(etTostadoPrecio);
        lote.tostadoTotal = lote.tostadoKg * lote.tostadoPrecioKg;

        lote.flete = d(etFlete);

        lote.potSelladoraKw = d(etPotSelladora);
        lote.horasSelladora = d(etHorasSelladora);
        lote.potMoledoraKw = d(etPotMoledora);
        lote.horasMoledora = d(etHorasMoledora);
        lote.precioKwh = d(etPrecioKwh);
        lote.electricidadTotal = (lote.potSelladoraKw * lote.horasSelladora
                + lote.potMoledoraKw * lote.horasMoledora) * lote.precioKwh;

        lote.empaqueTotal = d(etEmpaqueTotal);

        lote.totalGastos = lote.pergaminoTotal + lote.piladoTotal + lote.tostadoTotal
                + lote.flete + lote.electricidadTotal + lote.empaqueTotal;

        lote.stockBolsas12 = i(etStockBolsas12);
        lote.stockBolsas14 = i(etStockBolsas14);

        lotesRef.child(idLote).setValue(lote)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Lote guardado: " + codigo, Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error guardando el lote: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
}
