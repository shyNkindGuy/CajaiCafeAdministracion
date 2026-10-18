package com.example.cajaicafeadministracion;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.List;

/** Ventas pendientes o parciales con su saldo y un botón para recordar el pago por WhatsApp. */
public class CuentasPorCobrarActivity extends AppCompatActivity {

    // WhatsApp normal y WhatsApp Business: se prueba en ese orden y, si no hay ninguno, se ofrece compartir.
    private static final String[] PAQUETES_WHATSAPP = {"com.whatsapp", "com.whatsapp.w4b"};

    TextView tvTotal, tvSinCuentas;
    LinearLayout contenedor;

    DatabaseReference ventasRef;
    ValueEventListener ventasListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cuentas_por_cobrar);

        tvTotal = findViewById(R.id.tvTotalPorCobrar);
        tvSinCuentas = findViewById(R.id.tvSinCuentas);
        contenedor = findViewById(R.id.contenedorCuentas);

        ventasRef = FirebaseDatabase.getInstance().getReference("ventas");
        ventasListener = ventasRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Venta> ventas = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Venta v = ds.getValue(Venta.class);
                    if (v == null) continue;
                    v.id = ds.getKey();
                    ventas.add(v);
                }
                pintar(CuentasPorCobrar.pendientes(ventas));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CuentasPorCobrarActivity.this, "Error cargando ventas: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (ventasListener != null) ventasRef.removeEventListener(ventasListener);
        super.onDestroy();
    }

    private void pintar(List<Venta> pendientes) {
        tvTotal.setText("Total por cobrar: " + PrecioCalculator.formatear(CuentasPorCobrar.total(pendientes))
                + " (" + pendientes.size() + (pendientes.size() == 1 ? " venta)" : " ventas)"));
        tvSinCuentas.setVisibility(pendientes.isEmpty() ? View.VISIBLE : View.GONE);

        contenedor.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (Venta v : pendientes) {
            View fila = inflater.inflate(R.layout.item_cuenta, contenedor, false);
            String nombre = CuentasPorCobrar.nombreCliente(v);
            ((TextView) fila.findViewById(R.id.tvClienteCuenta)).setText(nombre == null ? "Sin nombre" : nombre);
            ((TextView) fila.findViewById(R.id.tvSaldoCuenta)).setText(PrecioCalculator.formatear(CuentasPorCobrar.saldo(v)));
            ((TextView) fila.findViewById(R.id.tvDetalleCuenta)).setText(CuentasPorCobrar.detalle(v));
            fila.findViewById(R.id.btnWhatsAppCuenta).setOnClickListener(b -> recordar(v));
            contenedor.addView(fila);
        }
    }

    /**
     * Abre WhatsApp con el recordatorio escrito; la venta no guarda el teléfono, así que WhatsApp pide
     * elegir el chat. Sin WhatsApp instalado, se ofrece compartir el texto por otra app.
     */
    private void recordar(Venta v) {
        String mensaje = CuentasPorCobrar.mensaje(v);
        for (String paquete : PAQUETES_WHATSAPP) {
            Intent whatsapp = new Intent(Intent.ACTION_SEND)
                    .setType("text/plain")
                    .putExtra(Intent.EXTRA_TEXT, mensaje)
                    .setPackage(paquete);
            try {
                startActivity(whatsapp);
                return;
            } catch (ActivityNotFoundException e) {
                // No está instalado: probar el siguiente.
            }
        }
        Intent compartir = new Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, mensaje);
        try {
            startActivity(Intent.createChooser(compartir, "Enviar recordatorio"));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No hay ninguna app para enviar el mensaje", Toast.LENGTH_SHORT).show();
        }
    }
}
