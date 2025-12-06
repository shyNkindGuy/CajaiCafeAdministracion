package com.example.cajaicafeadministracion;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.firebase.database.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DashboardFragment extends Fragment {

    TextView tvTotalMedio, tvTotalCuarto, tvTotalDinero;
    EditText etLote, etRuc;
    Button btnExportar;
    DatabaseReference ventasRef;
    List<Venta> listaVentas = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        tvTotalMedio = view.findViewById(R.id.tvTotalMedioKilo);
        tvTotalCuarto = view.findViewById(R.id.tvTotalCuartoKilo);
        tvTotalDinero = view.findViewById(R.id.tvTotalDinero);
        etLote = view.findViewById(R.id.etLote);
        etRuc = view.findViewById(R.id.etRuc);
        btnExportar = view.findViewById(R.id.btnExportarPDF);

        ventasRef = FirebaseDatabase.getInstance().getReference("ventas");

        cargarDatos();

        btnExportar.setOnClickListener(v -> generarPDF());

        return view;
    }

    private void cargarDatos() {
        ventasRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                listaVentas.clear();
                int countMedio = 0;
                int countCuarto = 0;
                double totalDineroRecaudado = 0; // bolsas pagadas

                for (DataSnapshot ds : snapshot.getChildren()) {
                    Venta v = ds.getValue(Venta.class);
                    if (v != null) {
                        listaVentas.add(v);

                        if (v.estadoPago != null) {
                            if (v.estadoPago.equals("pagado")) {
                                // Si está pagado, sumamos el total completo
                                totalDineroRecaudado += v.total;
                            } else if (v.estadoPago.equals("parcial")) {
                                // Si es parcial, sumamos SOLO lo que dejó a cuenta
                                totalDineroRecaudado += v.montoParcial;
                            }
                        }
                        //bolsas
                        if (v.producto != null) {
                            if (v.producto.contains("1/2")) {
                                countMedio += v.cantidad;
                            } else if (v.producto.contains("1/4")) {
                                countCuarto += v.cantidad;
                            }
                        }
                    }
                }

                // Actualizamos la UI
                tvTotalMedio.setText(String.valueOf(countMedio));
                tvTotalCuarto.setText(String.valueOf(countCuarto));

                // Mostramos el total recaudado en la nueva tarjeta verde
                tvTotalDinero.setText("S/ " + String.format("%.2f", totalDineroRecaudado));
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void generarPDF() {
        PdfDocument pdfDocument = new PdfDocument();
        Paint paint = new Paint();
        Paint titlePaint = new Paint();

        // Página A4 estándar aprox en puntos (595 x 842)
        PdfDocument.PageInfo myPageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page myPage = pdfDocument.startPage(myPageInfo);
        Canvas canvas = myPage.getCanvas();

        // Estilos
        titlePaint.setTextAlign(Paint.Align.CENTER);
        titlePaint.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD));
        titlePaint.setTextSize(20);

        paint.setTextSize(12);
        paint.setColor(Color.BLACK);

        int startX = 40;
        int startY = 50;

        // --- CABECERA ---
        // Aquí podrías dibujar tu logo con: canvas.drawBitmap(bitmap, x, y, paint);

        canvas.drawText("REPORTE DE VENTAS - CAJAI CAFE", 595/2, startY, titlePaint);
        startY += 30;
        canvas.drawText("Lote: " + etLote.getText().toString(), startX, startY, paint);
        startY += 20;
        canvas.drawText("RUC Emisor: " + etRuc.getText().toString(), startX, startY, paint);
        startY += 30;

        canvas.drawLine(startX, startY, 550, startY, paint); // Línea separadora
        startY += 20;

        // Cabeceras
        canvas.drawText("Producto", startX, startY, paint);
        canvas.drawText("Cant.", 300, startY, paint);
        canvas.drawText("Total", 450, startY, paint);
        startY += 20;

        double granTotal = 0;
        for (Venta v : listaVentas) {
            String prod = v.producto.length() > 30 ? v.producto.substring(0,30) + "..." : v.producto;
            canvas.drawText(prod, startX, startY, paint);
            canvas.drawText(String.valueOf(v.cantidad), 300, startY, paint);
            canvas.drawText("S/ " + v.total, 450, startY, paint);
            granTotal += v.total;
            startY += 15;

            // Salto de página simple (si la lista es muy larga, necesitarías lógica extra)
            if (startY > 800) break;
        }

        startY += 20;
        canvas.drawLine(startX, startY, 550, startY, paint);
        startY += 30;

        paint.setTextSize(16);
        paint.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD));
        canvas.drawText("TOTAL GENERAL: S/ " + String.format("%.2f", granTotal), 350, startY, paint);

        pdfDocument.finishPage(myPage);

        // Guardar archivo
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Reporte_CajaiCafe.pdf");
        try {
            pdfDocument.writeTo(new FileOutputStream(file));
            Toast.makeText(getContext(), "PDF guardado en Descargas", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error generando PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        pdfDocument.close();
    }
}
