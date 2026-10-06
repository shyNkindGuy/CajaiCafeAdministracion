package com.example.cajaicafeadministracion;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

public class LogoActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logo);
        new Handler().postDelayed(()->{
            // Con sesión iniciada se entra directo; si no, al login.
            boolean conSesion = FirebaseAuth.getInstance().getCurrentUser() != null;
            startActivity(new Intent(LogoActivity.this, conSesion ? MainActivity.class : LoginActivity.class));
            finish();
        }, 2000);
    }
}
