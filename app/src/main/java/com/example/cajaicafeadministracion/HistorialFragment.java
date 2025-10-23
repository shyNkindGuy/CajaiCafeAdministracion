package com.example.cajaicafeadministracion;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.*;
import java.util.*;

public class HistorialFragment extends Fragment {

    private RecyclerView recyclerVentas;
    private DatabaseReference ventasRef;
    private List<Venta> listaVentas = new ArrayList<>();
    private VentaAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_historial_ventas, container, false);

        recyclerVentas = view.findViewById(R.id.recyclerVentas);
        recyclerVentas.setLayoutManager(new LinearLayoutManager(getContext()));

        ventasRef = FirebaseDatabase.getInstance().getReference("ventas");

        adapter = new VentaAdapter(getContext(), listaVentas, ventasRef);
        recyclerVentas.setAdapter(adapter);

        cargarVentas();

        return view;
    }

    private void cargarVentas() {
        ventasRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                listaVentas.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Venta venta = ds.getValue(Venta.class);
                    if (venta != null) listaVentas.add(venta);
                }
                Collections.reverse(listaVentas);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // manejar errores si es necesario
            }
        });
    }
}
