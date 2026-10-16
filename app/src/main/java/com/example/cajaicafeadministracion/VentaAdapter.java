package com.example.cajaicafeadministracion;

import androidx.recyclerview.widget.RecyclerView;
import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class VentaAdapter extends RecyclerView.Adapter<VentaAdapter.VentaViewHolder> {
    private final Context context;
    private final List<Venta> lista;
    private final DatabaseReference ventasRef;

    public VentaAdapter(Context context, List<Venta> lista, DatabaseReference ventasRef) {
        this.context = context;
        this.lista = lista;
        this.ventasRef = ventasRef;
    }

    @NonNull
    @Override
    public VentaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_venta, parent, false);
        return new VentaViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VentaViewHolder holder, int position) {
        Venta v = lista.get(position);

        holder.tvCliente.setText("Cliente: " + v.cliente);
        holder.tvProducto.setText(v.producto);
        holder.tvCantidad.setText("x" + v.cantidad);
        holder.tvTotal.setText("S/ " + v.total);
        holder.tvEstado.setText(v.estadoPago);
        holder.tvMontoParcial.setText("Pagó S/ " + v.montoParcial);
        holder.tvFecha.setText(v.fecha);

        int colorREs = 0;
        switch (v.estadoPago){
            case "pagado":
                colorREs = R.color.estadoPagado;
                break;
            case "parcial":
                colorREs = R.color.estadoParcial;
                break;
            case "pendiente":
                colorREs = R.color.estadoPendiente;
                break;
        }
        holder.tvEstado.setTextColor(ContextCompat.getColor(context, colorREs));

        if ("parcial".equals(v.estadoPago) && v.montoParcial > 0){
            holder.tvMontoParcial.setText(String.format(Locale.US, "Pagó S/ %.2f", v.montoParcial));
            holder.tvMontoParcial.setVisibility(View.VISIBLE);
        }else{
            holder.tvMontoParcial.setVisibility(View.GONE);
        }

        holder.btnEditar.setOnClickListener(view -> {
            Context ctx = view.getContext();
            AlertDialog.Builder dialog = new AlertDialog.Builder(ctx);
            dialog.setTitle("Editar estado de pago");

            String[] estados = {"pagado", "parcial", "pendiente"};
            dialog.setItems(estados, (d, which) -> {
                String nuevoEstado = estados[which];
                v.estadoPago = nuevoEstado;
                ventasRef.child(v.id).child("estadoPago").setValue(nuevoEstado);

                if ("parcial".equals(nuevoEstado)) {
                    EditText input = new EditText(ctx);
                    input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                    input.setHint("Monto parcial");
                    new AlertDialog.Builder(ctx)
                            .setTitle("Ingrese monto parcial")
                            .setView(input)
                            .setPositiveButton("Guardar", (di, w) -> {
                                try {
                                    double monto = Double.parseDouble(input.getText().toString());
                                    if (monto == v.total){
                                        Toast.makeText(ctx, "El monto parcial no puede ser igual al total", Toast.LENGTH_SHORT).show();
                                    }
                                    if (monto > v.total) {
                                        Toast.makeText(ctx, "El monto parcial no puede ser mayor al total", Toast.LENGTH_SHORT).show();
                                    } else {
                                        ventasRef.child(v.id).child("montoParcial").setValue(monto);
                                        Toast.makeText(ctx, "Monto parcial actualizado", Toast.LENGTH_SHORT).show();
                                    }
                                } catch (NumberFormatException e) {
                                    Toast.makeText(ctx, "Ingrese un monto válido", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .setNegativeButton("Cancelar", null)
                            .show();
                }
            });
            dialog.show();
        });
        holder.btnEliminar.setOnClickListener(view -> {
            new AlertDialog.Builder(context)
                    .setTitle("Eliminar Venta")
                    .setMessage("¿Estás seguro de que quieres eliminar esta venta?\n\nCliente: " + v.cliente
                            + "\n" + v.cantidad + " × " + v.producto
                            + "\n\nLas bolsas vuelven al stock de donde salieron.")
                    .setPositiveButton("Eliminar", (dialog, which) -> eliminarDevolviendoStock(v))
                    .setNegativeButton("Cancelar", null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        });

    }

    /** Borra la venta y devuelve sus bolsas al lote o producto de donde salieron (ver StockLote.rutaDevolucion). */
    private void eliminarDevolviendoStock(Venta v) {
        // Las respuestas llegan después: el contexto de la app sigue vivo aunque se cambie de pestaña.
        final Context appCtx = context.getApplicationContext();
        if (v.productoId != null) {
            devolverYBorrar(v, v.productoId, appCtx);
            return;
        }
        // Ventas anteriores a la tarea 1.3 no guardan el id del producto: se busca por nombre.
        ventasRef.getRoot().child("productos").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot productos) {
                Map<String, String> nombrePorId = new HashMap<>();
                for (DataSnapshot ds : productos.getChildren()) {
                    nombrePorId.put(ds.getKey(), ds.child("nombre").getValue(String.class));
                }
                devolverYBorrar(v, StockLote.productoIdDeVenta(null, v.producto, nombrePorId), appCtx);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(appCtx, "Error al eliminar la venta: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void devolverYBorrar(Venta v, @Nullable String productoId, Context appCtx) {
        String ruta = StockLote.rutaDevolucion(productoId, v.loteId);
        if (ruta == null) {
            borrarSinDevolver(v, "no encontré el producto \"" + v.producto + "\"", appCtx);
            return;
        }
        DatabaseReference raiz = ventasRef.getRoot();
        // El lote o producto dueño del stock: si ya no existe, sumar crearía un nodo suelto con solo el stock.
        raiz.child(ruta).getParent().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dueno) {
                if (!dueno.exists()) {
                    borrarSinDevolver(v, "el lote o producto ya no existe", appCtx);
                    return;
                }
                String destino = dueno.hasChild("codigo")
                        ? "el lote " + dueno.child("codigo").getValue(String.class)
                        : String.valueOf(dueno.child("nombre").getValue(String.class));
                // Una sola escritura: se borra la venta y se suma el stock, o no pasa ninguna de las dos.
                Map<String, Object> cambios = new HashMap<>();
                cambios.put("ventas/" + v.id, null);
                cambios.put(ruta, ServerValue.increment((long) v.cantidad));
                raiz.updateChildren(cambios)
                        .addOnSuccessListener(aVoid -> Toast.makeText(appCtx,
                                "Venta eliminada. " + v.cantidad + " bolsas devueltas a " + destino, Toast.LENGTH_LONG).show())
                        .addOnFailureListener(e -> Toast.makeText(appCtx,
                                "Error al eliminar la venta: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(appCtx, "Error al eliminar la venta: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void borrarSinDevolver(Venta v, String motivo, Context appCtx) {
        ventasRef.child(v.id).removeValue()
                .addOnSuccessListener(aVoid -> Toast.makeText(appCtx,
                        "Venta eliminada, pero el stock no se devolvió: " + motivo + ". Ajústalo a mano.", Toast.LENGTH_LONG).show())
                .addOnFailureListener(e -> Toast.makeText(appCtx,
                        "Error al eliminar la venta: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    static class VentaViewHolder extends RecyclerView.ViewHolder {
        TextView tvCliente, tvProducto, tvCantidad, tvTotal, tvEstado, tvFecha, tvMontoParcial;
        ImageButton btnEditar, btnEliminar;

        VentaViewHolder(View itemView) {
            super(itemView);
            tvCliente = itemView.findViewById(R.id.tvCliente);
            tvProducto = itemView.findViewById(R.id.tvProducto);
            tvCantidad = itemView.findViewById(R.id.tvCantidad);
            tvTotal = itemView.findViewById(R.id.tvTotal);
            tvEstado = itemView.findViewById(R.id.tvEstado);
            tvMontoParcial = itemView.findViewById(R.id.tvMontoParcial);
            tvFecha = itemView.findViewById(R.id.tvFecha);
            btnEditar = itemView.findViewById(R.id.btnEditar);
            btnEliminar = itemView.findViewById(R.id.btnEliminar);
        }
    }
}
