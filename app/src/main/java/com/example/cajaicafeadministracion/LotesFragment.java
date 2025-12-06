package com.example.cajaicafeadministracion;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LotesFragment extends Fragment {

    private RecyclerView recyclerLotes;
    private DatabaseReference lotesRef;
    private final List<Lote> listaLotes = new ArrayList<>();
    private LoteAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_lotes, container, false);

        recyclerLotes = view.findViewById(R.id.recyclerLotes);
        recyclerLotes.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new LoteAdapter(getContext(), listaLotes);
        recyclerLotes.setAdapter(adapter);

        lotesRef = FirebaseDatabase.getInstance().getReference("lotes");
        cargarLotes();

        FloatingActionButton fab = view.findViewById(R.id.fabNuevoLote);
        fab.setOnClickListener(v -> startActivity(new Intent(getContext(), NuevoLoteActivity.class)));

        return view;
    }

    private void cargarLotes() {
        lotesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                listaLotes.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Lote l = ds.getValue(Lote.class);
                    if (l != null) {
                        if (l.id == null) l.id = ds.getKey();
                        listaLotes.add(l);
                    }
                }
                // Más reciente primero
                Collections.reverse(listaLotes);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Si falla, la lista simplemente no se actualiza; se podría mostrar un Toast aquí.
            }
        });
    }
}
