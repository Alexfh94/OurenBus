package com.example.ourenbus2;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ourenbus2.util.PreferencesUtil;

/**
 * Actividad de pantalla de carga inicial
 */
public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY = 1500; // 1.5 segundos

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Aplicar tema según preferencias
        PreferencesUtil.applyTheme(this);
        
        // No necesitamos setContentView porque usamos un tema con fondo personalizado
        
        // Programar la transición a la actividad principal
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Iniciar la actividad principal
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            
            // Finalizar esta actividad para que no se pueda volver atrás
            finish();
        }, SPLASH_DELAY);
    }
} 