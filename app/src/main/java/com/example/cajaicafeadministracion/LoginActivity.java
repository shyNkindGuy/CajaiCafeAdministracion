package com.example.cajaicafeadministracion;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**
 * Login con correo y contraseña. No hay registro: las cuentas se crean a mano en la consola
 * de Firebase, y solo entran las que figuran en usuarios/{uid} = true (ver database.rules.json).
 */
public class LoginActivity extends AppCompatActivity {

    EditText etCorreo, etContrasena;
    Button btnIngresar;
    TextView tvError, tvOlvide;
    ProgressBar progress;

    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etCorreo = findViewById(R.id.etCorreo);
        etContrasena = findViewById(R.id.etContrasena);
        btnIngresar = findViewById(R.id.btnIngresar);
        tvError = findViewById(R.id.tvErrorLogin);
        tvOlvide = findViewById(R.id.tvOlvideContrasena);
        progress = findViewById(R.id.progressLogin);

        auth = FirebaseAuth.getInstance();

        btnIngresar.setOnClickListener(v -> ingresar());
        tvOlvide.setOnClickListener(v -> recuperarContrasena());
    }

    private void ingresar() {
        String correo = etCorreo.getText().toString().trim();
        String contrasena = etContrasena.getText().toString();

        String error = ValidacionLogin.validar(correo, contrasena);
        if (error != null) {
            mostrarError(error);
            return;
        }

        cargando(true);
        // Listeners sin atar a la Activity: si el usuario sale y vuelve durante el login,
        // la respuesta igual llega y el botón no queda deshabilitado para siempre.
        auth.signInWithEmailAndPassword(correo, contrasena)
                .addOnSuccessListener(resultado -> verificarAutorizacion(resultado.getUser()))
                .addOnFailureListener(e -> {
                    cargando(false);
                    if (e instanceof FirebaseAuthInvalidUserException
                            || e instanceof FirebaseAuthInvalidCredentialsException) {
                        mostrarError("Correo o contraseña incorrectos");
                    } else {
                        mostrarError("No se pudo ingresar: " + e.getMessage());
                    }
                });
    }

    /** La cuenta existe en Firebase Auth; ahora se confirma que esté en la lista de usuarios/. */
    private void verificarAutorizacion(FirebaseUser usuario) {
        if (usuario == null) {
            cargando(false);
            mostrarError("No se pudo ingresar, intenta de nuevo");
            return;
        }

        String uid = usuario.getUid();
        FirebaseDatabase.getInstance().getReference("usuarios").child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (Boolean.TRUE.equals(snapshot.getValue())) {
                            abrirApp();
                        } else {
                            rechazar("Esta cuenta todavía no está autorizada. En la consola de Firebase, "
                                    + "agrega en Realtime Database: usuarios → " + uid + " = true");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        rechazar("Esta cuenta no tiene permiso (" + error.getMessage() + "). "
                                + "Su código es: " + uid);
                    }
                });
    }

    private void abrirApp() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void rechazar(String mensaje) {
        auth.signOut();
        cargando(false);
        mostrarError(mensaje);
    }

    private void recuperarContrasena() {
        String correo = etCorreo.getText().toString().trim();
        if (correo.isEmpty()) {
            mostrarError("Escribe tu correo y vuelve a tocar \"¿Olvidaste tu contraseña?\"");
            return;
        }
        auth.sendPasswordResetEmail(correo)
                .addOnSuccessListener(unused -> Toast.makeText(this,
                        "Te enviamos un correo para cambiar la contraseña", Toast.LENGTH_LONG).show())
                .addOnFailureListener(e -> mostrarError("No se pudo enviar el correo: " + e.getMessage()));
    }

    private void cargando(boolean activo) {
        progress.setVisibility(activo ? View.VISIBLE : View.GONE);
        btnIngresar.setEnabled(!activo);
        if (activo) tvError.setVisibility(View.GONE);
    }

    private void mostrarError(String mensaje) {
        tvError.setText(mensaje);
        tvError.setVisibility(View.VISIBLE);
    }
}
