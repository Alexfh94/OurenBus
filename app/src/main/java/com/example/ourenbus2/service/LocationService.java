package com.example.ourenbus2.service;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

/**
 * Servicio singleton para gestionar la ubicación del usuario
 */
public class LocationService {
    
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final long LOCATION_UPDATE_INTERVAL = 10000; // 10 segundos
    private static final long FASTEST_UPDATE_INTERVAL = 5000; // 5 segundos
    
    private static LocationService instance;
    
    private final Context context;
    private final FusedLocationProviderClient fusedLocationClient;
    private final LocationRequest locationRequest;
    private final LocationCallback locationCallback;
    
    // LiveData para la ubicación
    private final MutableLiveData<Location> locationLiveData = new MutableLiveData<>();
    
    // LiveData para el estado de los permisos
    private final MutableLiveData<Boolean> locationPermissionGranted = new MutableLiveData<>(false);
    
    /**
     * Constructor privado (patrón Singleton)
     */
    private LocationService(Context context) {
        this.context = context.getApplicationContext();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this.context);
        
        // Configurar solicitud de ubicación
        locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, LOCATION_UPDATE_INTERVAL)
                .setMinUpdateIntervalMillis(FASTEST_UPDATE_INTERVAL)
                .build();
        
        // Configurar callback de ubicación
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    locationLiveData.setValue(location);
                }
            }
        };
        
        // Verificar permisos iniciales
        checkLocationPermission();
    }
    
    /**
     * Obtener instancia del servicio (Singleton)
     */
    public static synchronized LocationService getInstance(Context context) {
        if (instance == null) {
            instance = new LocationService(context);
        }
        return instance;
    }
    
    /**
     * Verifica si tenemos permiso de ubicación
     */
    private void checkLocationPermission() {
        boolean hasPermission = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        locationPermissionGranted.setValue(hasPermission);
        
        if (hasPermission) {
            startLocationUpdates();
        }
    }
    
    /**
     * Solicita permiso de ubicación
     */
    public void requestLocationPermission(Activity activity) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    activity,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE
            );
        } else {
            locationPermissionGranted.setValue(true);
            startLocationUpdates();
        }
    }
    
    /**
     * Procesa el resultado de la solicitud de permisos
     */
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            locationPermissionGranted.setValue(granted);
            
            if (granted) {
                startLocationUpdates();
            }
        }
    }
    
    /**
     * Inicia las actualizaciones de ubicación
     */
    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        
        fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
        );
    }
    
    /**
     * Detiene las actualizaciones de ubicación
     */
    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }
    
    /**
     * Obtiene la última ubicación conocida
     */
    public void getLastLocation() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                locationLiveData.setValue(location);
            }
        });
    }
    
    /**
     * Obtiene la última ubicación conocida
     * @return La última ubicación conocida o null si no hay
     */
    public Location getLastKnownLocation() {
        return locationLiveData.getValue();
    }
    
    /**
     * Libera recursos al destruir la actividad
     */
    public void cleanup() {
        stopLocationUpdates();
    }
    
    /**
     * Obtiene el LiveData de la ubicación
     */
    public LiveData<Location> getLocationLiveData() {
        return locationLiveData;
    }
    
    /**
     * Obtiene el LiveData del estado de los permisos
     */
    public LiveData<Boolean> isLocationPermissionGranted() {
        return locationPermissionGranted;
    }
} 