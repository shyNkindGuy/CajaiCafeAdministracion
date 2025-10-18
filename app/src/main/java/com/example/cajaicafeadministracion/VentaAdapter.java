package com.example.cajaicafeadministracion;

import androidx.recyclerview.widget.RecyclerView;
import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;

import com.google.firebase.database.DatabaseReference;
import java.util.List;

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

        holder.tvCliente.setText(v.cliente);
        holder.tvProducto.setText(v.producto);
        holder.tvCantidad.setText("x" + v.cantidad);
        holder.tvTotal.setText("S/ " + v.total);
        holder.tvEstado.setText(v.estadoPago);
        holder.tvFecha.setText(v.fecha);

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
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    static class VentaViewHolder extends RecyclerView.ViewHolder {
        TextView tvCliente, tvProducto, tvCantidad, tvTotal, tvEstado, tvFecha;
        ImageButton btnEditar;

        VentaViewHolder(View itemView) {
            super(itemView);
            tvCliente = itemView.findViewById(R.id.tvCliente);
            tvProducto = itemView.findViewById(R.id.tvProducto);
            tvCantidad = itemView.findViewById(R.id.tvCantidad);
            tvTotal = itemView.findViewById(R.id.tvTotal);
            tvEstado = itemView.findViewById(R.id.tvEstado);
            tvFecha = itemView.findViewById(R.id.tvFecha);
            btnEditar = itemView.findViewById(R.id.btnEditar);
        }
    }
}
