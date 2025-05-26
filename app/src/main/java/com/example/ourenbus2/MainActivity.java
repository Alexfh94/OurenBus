package com.example.ourenbus2;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.example.ourenbus2.model.Route;
import com.example.ourenbus2.service.LocationService;
import com.example.ourenbus2.ui.fragments.FavoritesFragment;
import com.example.ourenbus2.ui.fragments.MapFragment;
import com.example.ourenbus2.ui.fragments.ProfileFragment;
import com.example.ourenbus2.ui.fragments.RouteInputFragment;
import com.example.ourenbus2.ui.fragments.SettingsFragment;
import com.example.ourenbus2.ui.viewmodel.RouteViewModel;
import com.example.ourenbus2.util.PreferencesUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

/**
 * Actividad principal de la aplicación
 */
public class MainActivity extends AppCompatActivity {
    
    private RouteViewModel viewModel;
    private Button btnStartNavigation;
    private FloatingActionButton fabSaveFavorite;
    private ConstraintLayout bottomActions;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Aplicar tema según preferencias
        PreferencesUtil.applyTheme(this);
        
        setContentView(R.layout.activity_main);
        
        // Configurar Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        // Configurar título clickeable
        toolbar.setTitle(R.string.app_name);
        toolbar.setOnClickListener(v -> {
            // Volver a la vista principal
            returnToMainView();
        });
        
        // Inicializar ViewModel
        viewModel = new ViewModelProvider(this).get(RouteViewModel.class);
        
        // Inicializar vistas
        btnStartNavigation = findViewById(R.id.btn_start_navigation);
        fabSaveFavorite = findViewById(R.id.fab_save_favorite);
        bottomActions = findViewById(R.id.bottom_actions);
        
        // Configurar listeners
        setupListeners();
        
        // Cargar fragmentos
        loadFragments();
        
        // Observar cambios en el ViewModel
        observeViewModel();
    }
    
    private void setupListeners() {
        btnStartNavigation.setOnClickListener(v -> {
            Route currentRoute = viewModel.getCurrentRoute().getValue();
            if (currentRoute != null && currentRoute.isValid()) {
                Intent intent = new Intent(this, NavigationActivity.class);
                startActivity(intent);
            }
        });
        
        fabSaveFavorite.setOnClickListener(v -> showSaveFavoriteDialog());
    }
    
    private void loadFragments() {
        // Cargar fragmento de entrada de ruta
        RouteInputFragment routeInputFragment = new RouteInputFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_route_input_container, routeInputFragment)
                .commit();
        
        // Cargar fragmento de mapa
        MapFragment mapFragment = new MapFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_map_container, mapFragment)
                .commit();
    }
    
    private void observeViewModel() {
        // Observar ruta actual para habilitar/deshabilitar botones
        viewModel.getCurrentRoute().observe(this, route -> {
            boolean validRoute = route != null && route.isValid();
            btnStartNavigation.setEnabled(validRoute);
            fabSaveFavorite.setEnabled(validRoute);
        });
    }
    
    private void showSaveFavoriteDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.save_favorite)
                .setMessage(R.string.save_favorite_message)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    // Obtener nombre de la ruta (aquí se podría mostrar un campo de texto)
                    String routeName = getString(R.string.favorite_route_default_name);
                    viewModel.saveRouteToFavorites(routeName);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        
        // Actualizar el texto del botón de tema según el modo actual
        MenuItem themeMenuItem = menu.findItem(R.id.action_theme);
        if (themeMenuItem != null) {
            int textResId = PreferencesUtil.getThemeButtonTextResId(
                    this, R.string.theme_light, R.string.theme_dark);
            themeMenuItem.setTitle(textResId);
        }
        
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.action_profile) {
            showProfileFragment();
            return true;
        } else if (id == R.id.action_favorites) {
            showFavoritesFragment();
            return true;
        } else if (id == R.id.action_settings) {
            showSettingsFragment();
            return true;
        } else if (id == R.id.action_theme) {
            toggleTheme();
            return true;
        } else if (id == android.R.id.home) {
            // Volver a la vista principal al pulsar el botón de navegación
            returnToMainView();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    private void showProfileFragment() {
        ProfileFragment fragment = new ProfileFragment();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.main_container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
        
        // Ocultar los fragmentos de entrada y mapa, así como el panel inferior
        hideMapComponents();
        
        // Mostrar botón para volver
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }
    
    private void showFavoritesFragment() {
        FavoritesFragment fragment = new FavoritesFragment();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.main_container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
        
        // Ocultar los fragmentos de entrada y mapa, así como el panel inferior
        hideMapComponents();
        
        // Mostrar botón para volver
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }
    
    private void showSettingsFragment() {
        SettingsFragment fragment = new SettingsFragment();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.main_container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
        
        // Ocultar los fragmentos de entrada y mapa, así como el panel inferior
        hideMapComponents();
        
        // Mostrar botón para volver
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }
    
    private void hideMapComponents() {
        // Ocultar los fragmentos y el panel inferior cuando se muestra otro fragmento
        findViewById(R.id.fragment_route_input_container).setVisibility(View.GONE);
        findViewById(R.id.fragment_map_container).setVisibility(View.GONE);
        bottomActions.setVisibility(View.GONE);
    }
    
    /**
     * Vuelve a la vista principal del mapa
     */
    private void returnToMainView() {
        // Si hay fragmentos en la pila, los quitamos
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            // Limpiar cualquier fragmento del contenedor principal
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.main_container);
            if (currentFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .remove(currentFragment)
                        .commit();
            }
            
            // Vaciar completamente el backstack
            getSupportFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            
            // Volver a cargar los fragmentos principales si es necesario
            if (getSupportFragmentManager().findFragmentById(R.id.fragment_route_input_container) == null ||
                getSupportFragmentManager().findFragmentById(R.id.fragment_map_container) == null) {
                loadFragments();
            }
            
            // Mostrar nuevamente los fragmentos y el panel inferior
            findViewById(R.id.fragment_route_input_container).setVisibility(View.VISIBLE);
            findViewById(R.id.fragment_map_container).setVisibility(View.VISIBLE);
            bottomActions.setVisibility(View.VISIBLE);
            
            // Ocultar botón para volver
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            }
            
            // Actualizar el menú después de volver al mapa
            invalidateOptionsMenu();
        }
    }
    
    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            returnToMainView();
        } else {
            super.onBackPressed();
        }
    }
    
    private void toggleTheme() {
        // Verificar si estamos en la pantalla de ajustes
        boolean isInSettingsScreen = false;
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.main_container);
        
        // Comprobar si el fragmento actual es SettingsFragment
        if (currentFragment instanceof SettingsFragment) {
            isInSettingsScreen = true;
        }
        
        // Usar el método para cambiar el tema
        PreferencesUtil.toggleTheme(this);
        
        // Actualizar el menú después de cambiar el tema
        invalidateOptionsMenu();
        
        // Si estamos en un fragmento secundario que no es ajustes, volver a la pantalla principal
        if (getSupportFragmentManager().getBackStackEntryCount() > 0 && !isInSettingsScreen) {
            returnToMainView();
        }
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // Actualizar el texto del botón de tema cuando se cambia el tema
        MenuItem themeMenuItem = menu.findItem(R.id.action_theme);
        if (themeMenuItem != null) {
            int textResId = PreferencesUtil.getThemeButtonTextResId(
                    this, R.string.theme_light, R.string.theme_dark);
            themeMenuItem.setTitle(textResId);
        }
        return super.onPrepareOptionsMenu(menu);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Liberar recursos
        LocationService.getInstance(this).cleanup();
    }
}