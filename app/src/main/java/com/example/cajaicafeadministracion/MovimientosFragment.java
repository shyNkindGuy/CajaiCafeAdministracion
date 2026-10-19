package com.example.cajaicafeadministracion;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButtonToggleGroup;

/**
 * Pestaña Movimientos: lo que entra (ventas, con editar pago y borrar: HistorialFragment) y lo que
 * sale (gastos: GastosFragment), uno a la vez según el selector de arriba. Cada lista es un fragment
 * hijo que quita sus propios listeners al salir.
 */
public class MovimientosFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_movimientos, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        MaterialButtonToggleGroup selector = view.findViewById(R.id.selectorMovimientos);
        selector.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) mostrar(checkedId);
        });
        // Al girar la pantalla el fragment hijo se restaura solo: no se crea otro encima.
        if (savedInstanceState == null) mostrar(selector.getCheckedButtonId());
    }

    private void mostrar(int botonId) {
        Fragment lista = (botonId == R.id.btnVerGastos) ? new GastosFragment() : new HistorialFragment();
        getChildFragmentManager().beginTransaction()
                .replace(R.id.contenedorMovimientos, lista)
                .commit();
    }
}
