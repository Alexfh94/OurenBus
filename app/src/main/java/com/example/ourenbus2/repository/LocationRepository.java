package com.example.ourenbus2.repository;

import android.content.Context;

import com.example.ourenbus2.model.Location;
import com.example.ourenbus2.service.LocationService;

import java.util.ArrayList;
import java.util.List;

/**
 * Repositorio para gestionar operaciones relacionadas con ubicaciones
 */
public class LocationRepository {
    
    private final Context context;
    private final LocationService locationService;
    
    // Radio máximo para buscar ubicaciones (en km)
    private static final float MAX_SEARCH_RADIUS_KM = 20.0f;
    
    public LocationRepository(Context context) {
        this.context = context;
        this.locationService = LocationService.getInstance(context);
    }
    
    /**
     * Busca ubicaciones según un término de búsqueda y filtra por distancia
     * En una app real, esto podría consultar una API o base de datos local
     */
    public List<Location> searchLocations(String query) {
        // Obtener todas las ubicaciones que coinciden con la consulta
        List<Location> allLocations = searchAllLocations(query);
        List<Location> nearbyLocations = new ArrayList<>();
        
        // Obtener la ubicación actual
        android.location.Location currentLocation = locationService.getLastKnownLocation();
        
        if (currentLocation == null) {
            // Si no hay ubicación actual, devolver todas las ubicaciones sin filtrar
            return allLocations;
        }
        
        // Filtrar ubicaciones por distancia (máximo 20km)
        for (Location location : allLocations) {
            float[] results = new float[1];
            android.location.Location.distanceBetween(
                    currentLocation.getLatitude(), currentLocation.getLongitude(),
                    location.getLatitude(), location.getLongitude(),
                    results);
            
            float distanceInKm = results[0] / 1000; // Convertir a km
            
            if (distanceInKm <= MAX_SEARCH_RADIUS_KM) {
                // Añadir la distancia a la descripción de la ubicación
                String description = location.getDescription();
                if (distanceInKm < 1) {
                    description += " (" + Math.round(distanceInKm * 1000) + "m)";
                } else {
                    description += " (" + String.format("%.1f", distanceInKm) + "km)";
                }
                location.setDescription(description);
                
                nearbyLocations.add(location);
            }
        }
        
        return nearbyLocations;
    }
    
    /**
     * Busca todas las ubicaciones que coinciden con la consulta
     */
    private List<Location> searchAllLocations(String query) {
        List<Location> results = new ArrayList<>();
        
        if (query == null || query.trim().isEmpty()) {
            return results;
        }
        
        query = query.toLowerCase().trim();
        
        // Crear algunas ubicaciones de ejemplo relevantes para Ourense
        if ("campus".contains(query) || "universidad".contains(query) || query.contains("camp") || query.contains("univ")) {
            results.add(new Location("Campus Universitario", "Campus As Lagoas, Ourense", 42.3450, -7.8500));
        }
        
        if ("hospital".contains(query) || query.contains("hosp") || query.contains("chuo")) {
            results.add(new Location("Hospital CHUO", "Rúa Ramón Puga, Ourense", 42.3380, -7.8670));
        }
        
        if ("estacion".contains(query) || "tren".contains(query) || "bus".contains(query) || 
            query.contains("esta") || query.contains("tren") || query.contains("bus")) {
            results.add(new Location("Estación de Tren", "Estación de Ourense-Empalme", 42.3520, -7.8650));
            results.add(new Location("Estación de Autobuses", "Rúa Progreso, Ourense", 42.3400, -7.8590));
        }
        
        if ("centro".contains(query) || "plaza".contains(query) || "mayor".contains(query) || 
            query.contains("cent") || query.contains("plaz") || query.contains("mayo")) {
            results.add(new Location("Plaza Mayor", "Centro de Ourense", 42.3370, -7.8630));
        }
        
        if ("termal".contains(query) || "termas".contains(query) || "burga".contains(query) || 
            query.contains("term") || query.contains("burg")) {
            results.add(new Location("As Burgas", "Termas de Ourense", 42.3355, -7.8645));
            results.add(new Location("Termas de Outariz", "Termas a las afueras de Ourense", 42.3795, -7.9221));
        }
        
        if ("parque".contains(query) || query.contains("parq") || query.contains("jardin")) {
            results.add(new Location("Parque San Lázaro", "Parque en el centro de Ourense", 42.3390, -7.8620));
            results.add(new Location("Parque Barbaña", "Parque junto al río Barbaña", 42.3320, -7.8701));
        }
        
        if ("ayuntamiento".contains(query) || "concello".contains(query) || 
            query.contains("ayun") || query.contains("conc")) {
            results.add(new Location("Concello de Ourense", "Plaza Mayor, Ourense", 42.3365, -7.8635));
        }
        
        // Añadir más ubicaciones comunes
        if ("alameda".contains(query) || query.contains("alam")) {
            results.add(new Location("Alameda", "Alameda del Concello", 42.3368, -7.8640));
        }
        
        if ("puente".contains(query) || "ponte".contains(query) || "romano".contains(query) || 
            query.contains("puen") || query.contains("pont")) {
            results.add(new Location("Ponte Romana", "Puente Romano sobre el Miño", 42.3407, -7.8636));
        }
        
        if ("catedral".contains(query) || query.contains("cate") || query.contains("igle")) {
            results.add(new Location("Catedral de Ourense", "Catedral de San Martín", 42.3366, -7.8648));
        }
        
        if ("miño".contains(query) || "minho".contains(query) || "rio".contains(query) || 
            query.contains("miñ") || query.contains("rio")) {
            results.add(new Location("Río Miño", "Paseo fluvial", 42.3410, -7.8670));
        }
        
        if ("polideportivo".contains(query) || "pabellón".contains(query) || "deportes".contains(query) || 
            query.contains("poli") || query.contains("depor")) {
            results.add(new Location("Pabellón de Deportes", "Pabellón Polideportivo de Ourense", 42.3426, -7.8680));
        }
        
        if ("biblioteca".contains(query) || query.contains("biblio") || query.contains("libro")) {
            results.add(new Location("Biblioteca Pública", "Biblioteca Central de Ourense", 42.3395, -7.8642));
        }
        
        // Localizaciones específicas
        if ("cai".contains(query) || "conservatorio".contains(query) || query.contains("conser")) {
            results.add(new Location("Conservatorio de Música", "Conservatorio Profesional de Ourense", 42.3382, -7.8662));
        }
        
        if ("xinzo".contains(query) || query.contains("xinz")) {
            results.add(new Location("Xinzo de Limia", "Localidad cercana a Ourense", 42.0623, -7.7243));
        }
        
        if ("celanova".contains(query) || query.contains("cela")) {
            results.add(new Location("Celanova", "Villa histórica cercana a Ourense", 42.1523, -7.9548));
        }
        
        if ("allariz".contains(query) || query.contains("alla")) {
            results.add(new Location("Allariz", "Villa histórica cercana a Ourense", 42.1889, -7.8020));
        }
        
        if ("pereiro".contains(query) || query.contains("perei")) {
            results.add(new Location("Pereiro de Aguiar", "Localidad cercana a Ourense", 42.3612, -7.8141));
        }
        
        return results;
    }
} 