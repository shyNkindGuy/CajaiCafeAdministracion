package com.example.cajaicafeadministracion;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


public class MainActivity extends AppCompatActivity {
    EditText editMensaje;
    Button btnEnviar;
    TextView txtMensajes;
    DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("mensajes");
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        dbRef.setValue("Hola Mundo al dispositivo 1");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        editMensaje = findViewById(R.id.editMensaje);
        btnEnviar = findViewById(R.id.btnEnviar);
        txtMensajes = findViewById(R.id.txtMensajes);


        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String mensaje = dataSnapshot.getValue(String.class);
                Log.d("FIREBASE", "MENSAJE: " +  mensaje);
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d("FIREBASE", "ERROR: " + databaseError.getMessage());
            }
        });
        btnEnviar.setOnClickListener(new View.OnClickListener()  {
            @Override
            public void onClick(View view) {
                String mensaje = editMensaje.getText().toString().trim();
                if (!mensaje.isEmpty()) {
                    dbRef.setValue(mensaje);
                    editMensaje.setText("");
                }
            }
        });
    }
}
