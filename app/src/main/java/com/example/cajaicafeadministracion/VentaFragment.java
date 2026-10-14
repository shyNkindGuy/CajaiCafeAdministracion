package com.example.cajaicafeadministracion;

import android.content.Context;
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

    Spinner spinnerProducto, spinnerPago, spinnerLote;
    EditText etCantidad, etNombre, etMontoParcial;
    TextView tvStock, tvMensaje, tvTotal;
    Button btnRegistrar;

    DatabaseReference productosRef, ventasRef;
    Map<String, Producto> productoMap = new LinkedHashMap<>();
    List<String> productoNombres = new ArrayList<>();
    List<String> productoIds = new ArrayList<>();
    ArrayAdapter<String> productoAdapter;
    ValueEventListener productosListener;

    // Lotes abiertos (FIFO): las bolsas de 1/2 y 1/4 kg se descuentan del lote elegido.
    DatabaseReference lotesRef;
    ValueEventListener lotesListener;
    final List<Lote> lotesAbiertos = new ArrayList<>();
    String loteElegidoId; // lote elegido a mano (distinto del sugerido); null = seguir la sugerencia FIFO

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.activity_venta, container, false);

        spinnerProducto = view.findViewById(R.id.spinnerProducto);
        spinnerPago = view.findViewById(R.id.spinnerPago);
        spinnerLote = view.findViewById(R.id.spinnerLote);
        etCantidad = view.findViewById(R.id.etCantidad);
        etNombre = view.findViewById(R.id.etNombre);
        etMontoParcial = view.findViewById(R.id.etMontoParcial);
        tvStock = view.findViewById(R.id.tvStock);
        tvMensaje = view.findViewById(R.id.tvMensaje);
        tvTotal = view.findViewById(R.id.tvTotal);
        btnRegistrar = view.findViewById(R.id.btnRegistrar);

        productosRef = FirebaseDatabase.getInstance().getReference("productos");
        ventasRef = FirebaseDatabase.getInstance().getReference("ventas");
        lotesRef = FirebaseDatabase.getInstance().getReference("lotes");

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
                // Otra bolsa puede salir de otro lote (FIFO) y cambia el total.
                elegirLote();
                calcularTotal();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        productosListener = productosRef.addValueEventListener(new ValueEventListener() {
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
                        productoNombres.add(PrecioCalculator.etiqueta(p.nombre, p.precio, p.ofertaCantidad, p.ofertaPrecio));
                        productoIds.add(p.id);
                    }
                }

                productoAdapter.notifyDataSetChanged();

                elegirLote();
                // Si el precio cambió en Firebase, el total en pantalla se actualiza solo.
                calcularTotal();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                tvMensaje.setText("Error cargando productos: " + error.getMessage());
            }
        });

        // Spinner de lotes abiertos
        mostrarLotes();

        spinnerLote.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Un lote distinto del sugerido es una elección a mano (el costal que se abrió): se respeta.
                if (position >= 0 && position < lotesAbiertos.size()) {
                    boolean esSugerido = position == StockLote.sugerido(lotesAbiertos, campoStockSeleccionado());
                    loteElegidoId = esSugerido ? null : lotesAbiertos.get(position).id;
                }
                actualizarStock();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        lotesListener = lotesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Lote> todos = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Lote l = ds.getValue(Lote.class);
                    if (l != null) {
                        l.id = ds.getKey();
                        todos.add(l);
                    }
                }
                lotesAbiertos.clear();
                lotesAbiertos.addAll(StockLote.abiertosFifo(todos));
                mostrarLotes();
                // Después de una venta el stock cambia: si el lote se quedó sin bolsas pasa al siguiente (FIFO).
                elegirLote();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                tvMensaje.setText("Error cargando lotes: " + error.getMessage());
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

    @Override
    public void onDestroyView() {
        // Cada cambio de pestaña crea un fragment nuevo: sin esto los listeners se acumulan.
        if (productosListener != null) productosRef.removeEventListener(productosListener);
        if (lotesListener != null) lotesRef.removeEventListener(lotesListener);
        super.onDestroyView();
    }

    /**
     * Pone en el selector el lote elegido a mano si todavía tiene bolsas del producto; si no, el
     * abierto más antiguo que tenga (FIFO). Ver StockLote.elegido.
     */
    private void elegirLote() {
        int pos = StockLote.elegido(lotesAbiertos, campoStockSeleccionado(), loteElegidoId);
        if (pos < 0 || !lotesAbiertos.get(pos).id.equals(loteElegidoId)) loteElegidoId = null;
        if (pos >= 0) spinnerLote.setSelection(pos);
        actualizarStock();
    }

    /**
     * Adaptador nuevo en cada cambio de lotes: con notifyDataSetChanged el Spinner vuelve a la
     * posición anterior en su siguiente layout y se pierde el setSelection de elegirLote().
     */
    private void mostrarLotes() {
        List<String> nombres = new ArrayList<>();
        for (Lote l : lotesAbiertos) nombres.add(StockLote.etiqueta(l));
        ArrayAdapter<String> adapter = new ArrayAdapter<>(spinnerLote.getContext(),
                android.R.layout.simple_spinner_item, nombres);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLote.setAdapter(adapter);
    }

    /** Campo de stock del lote que usa el producto elegido, o null si no sale de lotes. */
    @Nullable
    private String campoStockSeleccionado() {
        Producto p = productoSeleccionado();
        return (p == null) ? null : StockLote.campoStock(p.id);
    }

    @Nullable
    private Lote loteSeleccionado() {
        int pos = spinnerLote.getSelectedItemPosition();
        if (pos < 0 || pos >= lotesAbiertos.size()) return null;
        return lotesAbiertos.get(pos);
    }

    /** Muestra el stock que va a usar la venta: el del lote (1/2 y 1/4 kg) o el del producto. */
    private void actualizarStock() {
        Producto p = productoSeleccionado();
        if (p == null) {
            tvStock.setText("Stock disponible: -");
            return;
        }
        String campo = StockLote.campoStock(p.id);
        if (campo == null) {
            tvStock.setText("Stock disponible: " + p.stock);
            return;
        }
        Lote lote = loteSeleccionado();
        if (lote == null) {
            tvStock.setText("No hay lotes abiertos: crea uno en la pestaña Lotes");
        } else {
            tvStock.setText("Stock en " + lote.codigo + ": " + StockLote.stockDe(lote, campo) + " bolsas");
        }
    }

    /** Muestra el total con el precio y la oferta de Firebase del producto elegido (ver PrecioCalculator). */
    private void calcularTotal() {
        Producto p = productoSeleccionado();
        int cantidad = PrecioCalculator.parsearCantidad(etCantidad.getText().toString());
        if (p == null) {
            tvTotal.setText("Total: " + PrecioCalculator.formatear(0));
            return;
        }
        double total = PrecioCalculator.total(p.precio, cantidad, p.ofertaCantidad, p.ofertaPrecio);
        double sinOferta = PrecioCalculator.total(p.precio, cantidad);
        tvTotal.setText("Total: " + PrecioCalculator.formatear(total) + (total < sinOferta ? " (con oferta)" : ""));
    }

    @Nullable
    private Producto productoSeleccionado() {
        int pos = spinnerProducto.getSelectedItemPosition();
        if (pos < 0 || pos >= productoIds.size()) return null;
        return productoMap.get(productoIds.get(pos));
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

        // 1/2 y 1/4 kg salen del lote (stockBolsas12/14); 1 kg y grano, de productos/{id}/stock.
        Lote lote = loteSeleccionado();
        String campoLote = StockLote.campoStock(pid);
        final String loteId = (lote != null) ? lote.id : null;
        final DatabaseReference stockRef;
        if (campoLote != null) {
            if (lote == null) {
                Toast.makeText(requireContext(), "No hay lotes abiertos. Crea uno en la pestaña Lotes y registra el empacado.",
                        Toast.LENGTH_LONG).show();
                return;
            }
            int disponible = StockLote.stockDe(lote, campoLote);
            if (cantidad > disponible) {
                Toast.makeText(requireContext(), "El lote " + lote.codigo + " solo tiene " + disponible + " bolsas de " + p.nombre
                        + ". Registra el empacado en la pestaña Lotes o elige otro lote.", Toast.LENGTH_LONG).show();
                return;
            }
            stockRef = lotesRef.child(lote.id).child(campoLote);
        } else {
            if (cantidad > p.stock) {
                Toast.makeText(requireContext(), "No hay suficiente stock", Toast.LENGTH_SHORT).show();
                return;
            }
            stockRef = productosRef.child(pid).child("stock");
        }

        if (!PrecioCalculator.precioValido(p.precio)) {
            Toast.makeText(requireContext(), "\"" + p.nombre + "\" no tiene precio. Ponlo en Firebase: productos → precio",
                    Toast.LENGTH_LONG).show();
            return;
        }

        // Se recalcula aquí con el producto y la cantidad actuales; nunca se lee del TextView.
        final double total = PrecioCalculator.total(p.precio, cantidad, p.ofertaCantidad, p.ofertaPrecio);

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

            if (montoParcial > total) {
                Toast.makeText(requireContext(), "El monto parcial no puede ser mayor al total", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        String key = ventasRef.push().getKey();
        final String idVenta = (key != null) ? key : "v" + System.currentTimeMillis();
        String fecha = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        // loteId también en 1 kg y grano: así el detalle del lote cuenta todo lo que se vendió de ese costal.
        Venta venta = new Venta(idVenta, fecha, p.nombre, nombre, p.precio, cantidad, estadoPago, montoParcial, total, loteId);
        venta.productoId = pid; // para devolver el stock al producto correcto si se borra la venta

        int finalCantidad = cantidad;
        // Las respuestas de Firebase pueden llegar después de cambiar de pestaña: requireContext()
        // lanzaría una excepción. El contexto de la app sigue vivo y el aviso se ve igual.
        final Context appCtx = requireContext().getApplicationContext();

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
                if (error != null) {
                    Toast.makeText(appCtx, "Error actualizando stock: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    return;
                }
                // No se guardó (otra venta se llevó las bolsas) o el campo de stock no existe en Firebase.
                if (!committed || currentData == null || currentData.getValue() == null) {
                    Toast.makeText(appCtx, "Stock insuficiente: la venta no se registró", Toast.LENGTH_LONG).show();
                    return;
                }

                ventasRef.child(idVenta).setValue(venta)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(appCtx, "Venta registrada. Total " + PrecioCalculator.formatear(total), Toast.LENGTH_LONG).show();
                            if (getView() == null) return; // la pestaña ya se cerró
                            etCantidad.setText("");
                            etNombre.setText("");
                            etMontoParcial.setText("");
                            tvTotal.setText("Total: S/ 0.00");
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(appCtx, "Error guardando venta: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                        );
            }
        });
    }
}
