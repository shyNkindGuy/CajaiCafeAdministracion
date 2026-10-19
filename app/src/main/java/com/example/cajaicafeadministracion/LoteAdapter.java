package com.example.cajaicafeadministracion;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
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

    // Umbrales del aviso de stock bajo; hasta que carguen los productos, los de por defecto.
    private int umbral12 = AlertaStock.UMBRAL_POR_DEFECTO;
    private int umbral14 = AlertaStock.UMBRAL_POR_DEFECTO;

    public LoteAdapter(Context context, List<Lote> lista) {
        this.context = context;
        this.lista = lista;
    }

    public void setUmbrales(int umbral12, int umbral14) {
        this.umbral12 = umbral12;
        this.umbral14 = umbral14;
        notifyDataSetChanged();
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
        // Lote abierto con pocas bolsas (AlertaStock): en rojo para que se vea al pasar la lista.
        boolean alerta = AlertaStock.tieneAlerta(l, umbral12, umbral14);
        holder.tvStock.setText(String.format(Locale.US, "%s: %d bolsas 1/2kg · %d bolsas 1/4kg",
                alerta ? "⚠ Stock bajo" : "Stock", l.stockBolsas12, l.stockBolsas14));
        if (alerta) {
            holder.tvStock.setTextColor(ContextCompat.getColor(context, R.color.estadoPendiente));
        } else {
            holder.tvStock.setTextColor(holder.colorStockNormal);
        }

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
        final ColorStateList colorStockNormal;

        public LoteViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCodigo = itemView.findViewById(R.id.tvCodigoLote);
            tvFecha = itemView.findViewById(R.id.tvFechaLote);
            tvEstado = itemView.findViewById(R.id.tvEstadoLote);
            tvGastos = itemView.findViewById(R.id.tvGastosLote);
            tvStock = itemView.findViewById(R.id.tvStockLote);
            colorStockNormal = tvStock.getTextColors(); // para volver al color normal al reciclar la fila
        }
    }
}
