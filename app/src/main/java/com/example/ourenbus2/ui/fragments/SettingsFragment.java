package com.example.ourenbus2.ui.fragments;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;

import com.example.ourenbus2.R;
import com.example.ourenbus2.util.PreferencesUtil;

/**
 * Fragmento para mostrar y gestionar los ajustes de la aplicación
 */
public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
        
        // Configurar preferencia de notificaciones
        setupNotificationPreference();
        
        // Configurar preferencia de tema
        setupThemePreference();
        
        // Configurar preferencia de ubicación
        setupLocationPreference();
        
        // Configurar preferencia de datos
        setupDataPreference();
        
        // Configurar preferencia de información de la aplicación
        setupAppInfoPreference();
    }
    
    private void setupNotificationPreference() {
        SwitchPreference notificationPref = findPreference("notifications_enabled");
        if (notificationPref != null) {
            notificationPref.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean enabled = (Boolean) newValue;
                // En una aplicación real, aquí configuraríamos las notificaciones
                String message = enabled ? 
                        getString(R.string.notifications_enabled) : 
                        getString(R.string.notifications_disabled);
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                return true;
            });
        }
    }
    
    private void setupThemePreference() {
        ListPreference themePref = findPreference("theme");
        if (themePref != null) {
            // Establecer el valor actual
            int currentTheme = AppCompatDelegate.getDefaultNightMode();
            if (currentTheme == AppCompatDelegate.MODE_NIGHT_YES) {
                themePref.setValue("dark");
            } else if (currentTheme == AppCompatDelegate.MODE_NIGHT_NO) {
                themePref.setValue("light");
            } else {
                themePref.setValue("system");
            }
            
            // Configurar listener
            themePref.setOnPreferenceChangeListener((preference, newValue) -> {
                String themeValue = (String) newValue;
                int themeMode;
                
                switch (themeValue) {
                    case "light":
                        themeMode = AppCompatDelegate.MODE_NIGHT_NO;
                        break;
                    case "dark":
                        themeMode = AppCompatDelegate.MODE_NIGHT_YES;
                        break;
                    default:
                        themeMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                        break;
                }
                
                // Guardar y aplicar el tema sin salir de Ajustes
                PreferencesUtil.saveThemePreference(requireContext(), themeMode);
                AppCompatDelegate.setDefaultNightMode(themeMode);
                requireActivity().recreate();
                return true;
            });
        }
    }
    
    private void setupLocationPreference() {
        SwitchPreference locationPref = findPreference("location_always");
        if (locationPref != null) {
            locationPref.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean alwaysEnabled = (Boolean) newValue;
                // En una aplicación real, aquí configuraríamos los permisos de ubicación
                String message = alwaysEnabled ? 
                        getString(R.string.location_always_enabled) : 
                        getString(R.string.location_when_using);
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                return true;
            });
        }
    }
    
    private void setupDataPreference() {
        Preference clearDataPref = findPreference("clear_data");
        if (clearDataPref != null) {
            clearDataPref.setOnPreferenceClickListener(preference -> {
                // En una aplicación real, aquí limpiaríamos los datos
                Toast.makeText(requireContext(), R.string.data_cleared, Toast.LENGTH_SHORT).show();
                return true;
            });
        }

        Preference clearFavorites = findPreference("clear_favorites");
        if (clearFavorites != null) {
            clearFavorites.setOnPreferenceClickListener(pref -> {
                new Thread(() -> {
                    com.example.ourenbus2.database.AppDatabase db = com.example.ourenbus2.database.AppDatabase.getInstance(requireContext());
                    db.favoriteRouteDao().deleteAll();
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), R.string.favorites_cleared, Toast.LENGTH_SHORT).show());
                }).start();
                return true;
            });
        }
    }
    
    private void setupAppInfoPreference() {
        Preference appInfoPref = findPreference("app_info");
        if (appInfoPref != null) {
            appInfoPref.setSummary(getString(R.string.app_version, "1.0.0"));
        }
    }
} 