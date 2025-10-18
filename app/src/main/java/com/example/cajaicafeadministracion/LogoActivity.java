package com.example.cajaicafeadministracion;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;

public class LogoActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logo);
        new Handler().postDelayed(()->{
            startActivity(new Intent(LogoActivity.this, VentaActivity.class));
            finish();
        }, 1800);
    }
}
