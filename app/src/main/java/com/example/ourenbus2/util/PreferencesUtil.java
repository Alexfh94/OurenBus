package com.example.ourenbus2.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

/**
 * Utilidad para gestionar las preferencias de la aplicación
 */
public class PreferencesUtil {
    
    private static final String KEY_THEME = "theme_mode";
    
    /**
     * Obtiene la preferencia del tema
     * @param context Contexto de la aplicación
     * @return El tema seleccionado
     */
    public static int getThemePreference(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }
    
    /**
     * Guarda la preferencia del tema
     * @param context Contexto de la aplicación
     * @param themeMode El tema a guardar
     */
    public static void saveThemePreference(Context context, int themeMode) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putInt(KEY_THEME, themeMode);
        editor.apply();
    }
    
    /**
     * Obtiene si el tema actual es oscuro
     * @param context Contexto de la aplicación
     * @return true si es tema oscuro, false si no
     */
    public static boolean isDarkTheme(Context context) {
        int currentNightMode = context.getResources().getConfiguration().uiMode 
                & Configuration.UI_MODE_NIGHT_MASK;
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES;
    }
    
    /**
     * Aplica el tema guardado a la actividad
     * @param activity Actividad a la que aplicar el tema
     */
    public static void applyTheme(AppCompatActivity activity) {
        int themeMode = getThemePreference(activity);
        AppCompatDelegate.setDefaultNightMode(themeMode);
    }
    
    /**
     * Cambia entre tema claro y oscuro
     * @param activity Actividad en la que cambiar el tema
     */
    public static void toggleTheme(AppCompatActivity activity) {
        boolean isDark = isDarkTheme(activity);
        int newTheme = isDark ? 
                AppCompatDelegate.MODE_NIGHT_NO : 
                AppCompatDelegate.MODE_NIGHT_YES;
        
        saveThemePreference(activity, newTheme);
        AppCompatDelegate.setDefaultNightMode(newTheme);
    }
    
    /**
     * Obtiene el texto para el botón de cambio de tema
     * @param context Contexto de la aplicación
     * @param lightThemeTextResId ID del recurso de texto para tema claro
     * @param darkThemeTextResId ID del recurso de texto para tema oscuro
     * @return El ID del recurso de texto a mostrar
     */
    public static int getThemeButtonTextResId(Context context, int lightThemeTextResId, int darkThemeTextResId) {
        boolean isDark = isDarkTheme(context);
        return isDark ? lightThemeTextResId : darkThemeTextResId;
    }
} 