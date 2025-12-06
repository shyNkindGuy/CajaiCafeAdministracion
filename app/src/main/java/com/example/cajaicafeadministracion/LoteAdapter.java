package com.example.cajaicafeadministracion;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

public class LoteAdapter extends RecyclerView.Adapter<LoteAdapter.LoteViewHolder> {

    private final Context context;
    private final List<Lote> lista;

    public LoteAdapter(Context context, List<Lote> lista) {
        this.context = context;
        this.lista = lista;
    }

    @NonNull
    @Override
    public LoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_lote, parent, false);
        return new LoteViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull LoteViewHolder holder, int position) {
        Lote l = lista.get(position);

        holder.tvCodigo.setText(l.codigo);
        holder.tvFecha.setText(l.fechaInicio);
        holder.tvEstado.setText(l.estado);
        holder.tvEstado.setTextColor(ContextCompat.getColor(context,
                "cerrado".equals(l.estado) ? R.color.estadoPendiente : R.color.estadoPagado));

        holder.tvGastos.setText(String.format(Locale.US, "Gastos: S/ %.2f", l.totalGastos));
        holder.tvStock.setText(String.format(Locale.US, "Stock: %d bolsas 1/2kg · %d bolsas 1/4kg",
                l.stockBolsas12, l.stockBolsas14));

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, LoteDetalleActivity.class);
            intent.putExtra("loteId", l.id);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    public static class LoteViewHolder extends RecyclerView.ViewHolder {
        TextView tvCodigo, tvFecha, tvEstado, tvGastos, tvStock;

        public LoteViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCodigo = itemView.findViewById(R.id.tvCodigoLote);
            tvFecha = itemView.findViewById(R.id.tvFechaLote);
            tvEstado = itemView.findViewById(R.id.tvEstadoLote);
            tvGastos = itemView.findViewById(R.id.tvGastosLote);
            tvStock = itemView.findViewById(R.id.tvStockLote);
        }
    }
}
