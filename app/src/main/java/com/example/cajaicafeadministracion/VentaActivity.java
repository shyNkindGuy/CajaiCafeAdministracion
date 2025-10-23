package com.example.cajaicafeadministracion;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.cajaicafeadministracion.Producto;
import com.example.cajaicafeadministracion.Venta;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.*;
public class VentaActivity extends AppCompatActivity {
    Spinner spinnerProducto, spinnerPago;
    EditText etCantidad, etNombre, etMontoParcial;
    TextView tvStock, tvMensaje;
    Button btnRegitrar;

    DatabaseReference productosRef, ventasRef;
    Map<String, Producto> productoMap = new LinkedHashMap<>();
    List <String> productoNombres = new ArrayList<>();
    List <String> productoIds = new ArrayList<>();
    ArrayAdapter <String> productoAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_venta);

        spinnerProducto = findViewById(R.id.spinnerProducto);
        spinnerPago = findViewById(R.id.spinnerPago);
        etCantidad = findViewById(R.id.etCantidad);
        etNombre = findViewById(R.id.etNombre);
        tvStock = findViewById(R.id.tvStock);
        tvMensaje = findViewById(R.id.tvMensaje);
        btnRegitrar = findViewById(R.id.btnRegistrar);

        productosRef = FirebaseDatabase.getInstance().getReference("productos");
        ventasRef = FirebaseDatabase.getInstance().getReference("ventas");

        // pago spinner
        ArrayAdapter <String> pagoAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                Arrays.asList("pagado", "parcial", "pendiente"));
        pagoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPago.setAdapter(pagoAdapter);
        spinnerPago.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String sel = (String) parent.getItemAtPosition(position);
                etMontoParcial.setVisibility("parcial".equals(sel) ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        productoAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, productoNombres);
        productoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerProducto.setAdapter(productoAdapter);

        spinnerProducto.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < productoIds.size()){
                    String pid = productoIds.get(position);
                    Producto p = productoMap.get(pid);
                    if (p != null) tvStock.setText("Stock disponible: " + p.stock);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        productosRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                productoMap.clear();
                productoNombres.clear();
                productoIds.clear();
                for (DataSnapshot ds : snapshot.getChildren()){
                    Producto p = ds.getValue(Producto.class);
                    if (p != null){
                        if (p.id == null) p.id = ds.getKey();
                        productoMap.put(p.id, p);
                        productoNombres.add(p.nombre + "(S/" + p.precio + ")");
                        productoIds.add(p.id);
                    }
                }
                productoAdapter.notifyDataSetChanged();
                if (!productoIds.isEmpty()){
                    Producto first = productoMap.get(productoIds.get(0));
                    if (first != null) tvStock.setText("Stock Disponible: " + first.stock);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                tvMensaje.setText("Error cargando productos: " + error.getMessage());
            }
        });
        btnRegitrar.setOnClickListener(v -> registrarVenta());
    }
    private void registrarVenta(){
        int pos = spinnerProducto.getSelectedItemPosition();
        if (pos < 0 || pos >= productoIds.size()){
            Toast.makeText(this, "Selecciona un producto", Toast.LENGTH_SHORT).show();
            return;
        }
        String pid = productoIds.get(pos);
        Producto p = productoMap.get(pid);
        if(p == null) {tvMensaje.setText("Producto no válido"); return;}

        String cantidadStr = etCantidad.getText().toString().trim();
        if (cantidadStr.isEmpty()) {Toast.makeText(this, "Ingresa la cantidad", Toast.LENGTH_SHORT).show(); return;};
        int cantidad = 0;
        try{
            cantidad = Integer.parseInt(cantidadStr);
        }catch (NumberFormatException e){
            Toast.makeText(this, "Cantidad no válida", Toast.LENGTH_SHORT).show();
        }
        if (cantidad <= 0) {tvMensaje.setText("Cantidad debe ser mayor a 0"); return;}
        if (cantidad > p.stock) {tvMensaje.setText("No hay suficiente stock"); return;}

        String nombre = etNombre.getText().toString().trim();
        if (nombre.isEmpty()) nombre = "Cliente";

        String estadoPago = (String) spinnerPago.getSelectedItem();
        double montoParcial = 0;
        if ("parcial".equals(estadoPago)){
            String mp = etMontoParcial.getText().toString().trim();
            if (mp.isEmpty()) {tvMensaje.setText("Ingresa el monto parcial pagado");}
            montoParcial = Double.parseDouble(mp);
            if (montoParcial <= 0 || montoParcial >= p.precio * cantidad) {
                //permitir
            }
        }
        double total = p.precio * cantidad;

        String key = ventasRef.push().getKey();
        final String idVenta = (key != null) ? key: "v" + System.currentTimeMillis();

        //fecha simple
        String fecha = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        Venta venta = new Venta(idVenta, fecha, p.nombre, nombre, p.precio, cantidad, estadoPago, montoParcial, total);

        //actualizar stock automaticamente

        DatabaseReference stockRef = productosRef.child(pid).child("stock");
        int finalCantidad = cantidad;
        stockRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                Long cur = currentData.getValue(Long.class);
                if (cur == null) return Transaction.success(currentData);
                if (cur < finalCantidad){
                    return Transaction.abort();
                }
                currentData.setValue(cur - finalCantidad);
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                if (error != null){
                    tvMensaje.setText("Error actualizando stock: " + error.getMessage());
                    return;
                }
                if (!committed){
                    tvMensaje.setText("Stock insuficiente (transaccion)");
                    return;
                }

                ventasRef.child(idVenta).setValue(venta)
                        .addOnSuccessListener(aVoid -> {
                            tvMensaje.setText("Venta registrada correctamente. Total S/ " + total);
                            etCantidad.setText("");
                            etNombre.setText("");
                        })
                        .addOnFailureListener(e -> tvMensaje.setText("Error guardando venta: " + e.getMessage()));
            }
        });

    }
}
