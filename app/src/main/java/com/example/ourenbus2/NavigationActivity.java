package com.example.ourenbus2;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import com.example.ourenbus2.service.LocationService;
import com.example.ourenbus2.ui.fragments.NavigationMapFragment;
import com.example.ourenbus2.ui.fragments.NavigationStepsFragment;
import com.example.ourenbus2.ui.viewmodel.NavigationViewModel;

/**
 * Actividad para la navegación paso a paso
 */
public class NavigationActivity extends AppCompatActivity {
    
    private NavigationViewModel viewModel;
    private LocationService locationService;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);
        
        // Inicializar servicios
        locationService = LocationService.getInstance(this);
        
        // Inicializar ViewModel
        viewModel = new ViewModelProvider(this).get(NavigationViewModel.class);
        
        // Configurar toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        // Botón de retroceso
        ImageButton btnBack = toolbar.findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> onBackPressed());
        
        // Cargar fragmentos
        loadFragments();
        
        // Observar la ubicación actual para navegación en tiempo real
        observeLocation();
    }
    
    private void loadFragments() {
        // Fragmento del mapa de navegación
        NavigationMapFragment mapFragment = new NavigationMapFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_navigation_map_container, mapFragment)
                .commit();
        
        // Fragmento de pasos de navegación
        NavigationStepsFragment stepsFragment = new NavigationStepsFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_navigation_steps_container, stepsFragment)
                .commit();
    }
    
    private void observeLocation() {
        locationService.getLocationLiveData().observe(this, location -> {
            if (location != null) {
                viewModel.updateUserLocation(location);
            }
        });
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // No limpiamos el servicio de ubicación aquí porque lo hace MainActivity
    }
} 