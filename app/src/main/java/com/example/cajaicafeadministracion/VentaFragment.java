package com.example.cajaicafeadministracion;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.firebase.database.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class VentaFragment extends Fragment {

    Spinner spinnerProducto, spinnerPago;
    EditText etCantidad, etNombre, etMontoParcial;
    TextView tvStock, tvMensaje, tvTotal;
    Button btnRegistrar;

    DatabaseReference productosRef, ventasRef;
    Map<String, Producto> productoMap = new LinkedHashMap<>();
    List<String> productoNombres = new ArrayList<>();
    List<String> productoIds = new ArrayList<>();
    ArrayAdapter<String> productoAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.activity_venta, container, false);

        spinnerProducto = view.findViewById(R.id.spinnerProducto);
        spinnerPago = view.findViewById(R.id.spinnerPago);
        etCantidad = view.findViewById(R.id.etCantidad);
        etNombre = view.findViewById(R.id.etNombre);
        etMontoParcial = view.findViewById(R.id.etMontoParcial);
        tvStock = view.findViewById(R.id.tvStock);
        tvMensaje = view.findViewById(R.id.tvMensaje);
        tvTotal = view.findViewById(R.id.tvTotal);
        btnRegistrar = view.findViewById(R.id.btnRegistrar);

        productosRef = FirebaseDatabase.getInstance().getReference("productos");
        ventasRef = FirebaseDatabase.getInstance().getReference("ventas");

        // Spinner de estado de pago
        ArrayAdapter<String> pagoAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item,
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

        // Spinner de productos
        productoAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, productoNombres);
        productoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerProducto.setAdapter(productoAdapter);

        spinnerProducto.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < productoIds.size()) {
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

                for (DataSnapshot ds : snapshot.getChildren()) {
                    Producto p = ds.getValue(Producto.class);
                    if (p != null) {
                        if (p.id == null) p.id = ds.getKey();
                        productoMap.put(p.id, p);
                        productoNombres.add(p.nombre + " (S/" + p.precio + ")");
                        productoIds.add(p.id);
                    }
                }

                productoAdapter.notifyDataSetChanged();

                if (!productoIds.isEmpty()) {
                    Producto first = productoMap.get(productoIds.get(0));
                    if (first != null)
                        tvStock.setText("Stock disponible: " + first.stock);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                tvMensaje.setText("Error cargando productos: " + error.getMessage());
            }
        });

        // Calcular total automáticamente
        etCantidad.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                calcularTotal();
            }
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        btnRegistrar.setOnClickListener(v -> registrarVenta());

        return view;
    }

    private void calcularTotal() {
        int pos = spinnerProducto.getSelectedItemPosition();
        if (pos < 0 || pos >= productoIds.size()) {
            tvTotal.setText("Total: S/ 0.00");
            return;
        }

        Producto p = productoMap.get(productoIds.get(pos));
        if (p == null) {
            tvTotal.setText("Total: S/ 0.00");
            return;
        }

        String tipo = p.nombre;
        String cantidadStr = etCantidad.getText().toString();

        if (cantidadStr.isEmpty()) {
            tvTotal.setText("Total: S/ 0.00");
            return;
        }

        int cantidad;
        try {
            cantidad = Integer.parseInt(cantidadStr);
        } catch (NumberFormatException e) {
            tvTotal.setText("Total: S/ 0.00");
            return;
        }

        double total;

        String nombreProducto = p.nombre.trim();

        if (nombreProducto.contains("1/2")) {
            int pares = cantidad / 2;
            int impares = cantidad % 2;
            total = (pares * 45.0) + (impares * 25.0);
        } else if (nombreProducto.contains("1/4")) {
            total = cantidad * 15.0;
        } else if (nombreProducto.contains("1 kg")) {
            total = cantidad * 45.0;
        } else {
            total = p.precio * cantidad;
        }

        tvTotal.setText(String.format(Locale.US, "Total: S/ %.2f", total));
    }

    private void registrarVenta() {

        int pos = spinnerProducto.getSelectedItemPosition();
        if (pos < 0 || pos >= productoIds.size()) {
            Toast.makeText(requireContext(), "Selecciona un producto", Toast.LENGTH_SHORT).show();
            return;
        }

        String pid = productoIds.get(pos);
        Producto p = productoMap.get(pid);
        if (p == null) return;

        String cantidadStr = etCantidad.getText().toString().trim();
        if (cantidadStr.isEmpty()) {
            Toast.makeText(requireContext(), "Ingresa la cantidad", Toast.LENGTH_SHORT).show();
            return;
        }

        int cantidad;
        try {
            cantidad = Integer.parseInt(cantidadStr);
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Cantidad no válida", Toast.LENGTH_SHORT).show();
            return;
        }

        if (cantidad <= 0) {
            Toast.makeText(requireContext(), "Cantidad debe ser mayor a 0", Toast.LENGTH_SHORT).show();
            return;
        }

        if (cantidad > p.stock) {
            Toast.makeText(requireContext(), "No hay suficiente stock", Toast.LENGTH_SHORT).show();
            return;
        }

        String nombre = etNombre.getText().toString().trim();
        if (nombre.isEmpty()) nombre = "Cliente";

        String estadoPago = (String) spinnerPago.getSelectedItem();
        double montoParcial = 0;

        if ("parcial".equals(estadoPago)) {
            String mp = etMontoParcial.getText().toString().trim();
            if (mp.isEmpty()) {
                Toast.makeText(requireContext(), "Ingresa el monto parcial pagado", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                montoParcial = Double.parseDouble(mp);
            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), "Monto parcial no válido", Toast.LENGTH_SHORT).show();
                return;
            }

            double total = Double.parseDouble(tvTotal.getText().toString().replace("Total: S/ ", ""));
            if (montoParcial > total) {
                Toast.makeText(requireContext(), "El monto parcial no puede ser mayor al total", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        double total = Double.parseDouble(tvTotal.getText().toString().replace("Total: S/ ", ""));
        String key = ventasRef.push().getKey();
        final String idVenta = (key != null) ? key : "v" + System.currentTimeMillis();
        String fecha = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        Venta venta = new Venta(idVenta, fecha, p.nombre, nombre, p.precio, cantidad, estadoPago, montoParcial, total);

        DatabaseReference stockRef = productosRef.child(pid).child("stock");
        int finalCantidad = cantidad;

        stockRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                Long cur = currentData.getValue(Long.class);
                if (cur == null) return Transaction.success(currentData);
                if (cur < finalCantidad) return Transaction.abort();
                currentData.setValue(cur - finalCantidad);
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                if (error != null || !committed) {
                    Toast.makeText(requireContext(), "Error actualizando stock", Toast.LENGTH_SHORT).show();
                    return;
                }

                ventasRef.child(idVenta).setValue(venta)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(requireContext(), "Venta registrada correctamente. Total S/ " + total, Toast.LENGTH_LONG).show();
                            etCantidad.setText("");
                            etNombre.setText("");
                            etMontoParcial.setText("");
                            tvTotal.setText("Total: S/ 0.00");
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(requireContext(), "Error guardando venta: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                        );
            }
        });
    }
}
