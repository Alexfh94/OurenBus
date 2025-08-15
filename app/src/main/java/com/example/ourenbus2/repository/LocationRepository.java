package com.example.ourenbus2.repository;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;

import com.example.ourenbus2.model.Location;
import com.example.ourenbus2.service.LocationService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Repositorio para gestionar operaciones relacionadas con ubicaciones
 */
public class LocationRepository {
    
    private final Context context;
    private final LocationService locationService;
    private final Geocoder geocoder;
    
    // Radio máximo para buscar ubicaciones (en km)
    private static final float MAX_SEARCH_RADIUS_KM = 20.0f;
    
    public LocationRepository(Context context) {
        this.context = context;
        this.locationService = LocationService.getInstance(context);
        this.geocoder = new Geocoder(context, Locale.getDefault());
    }
    
    /**
     * Busca ubicaciones según un término de búsqueda y filtra por distancia
     * Combina resultados locales con geocodificación del sistema
     */
    public List<Location> searchLocations(String query) {
        // Obtener ubicaciones locales que coinciden con la consulta
        List<Location> allLocations = searchAllLocations(query);
        
        // Añadir resultados de geocodificación si la consulta es suficientemente específica
        List<Location> geocoded = geocodeLocations(query, 5);
        if (geocoded != null && !geocoded.isEmpty()) {
            // Evitar duplicados simples por nombre + coords
            for (Location loc : geocoded) {
                boolean exists = false;
                for (Location existing : allLocations) {
                    if (existing.getName().equalsIgnoreCase(loc.getName()) &&
                        Math.abs(existing.getLatitude() - loc.getLatitude()) < 1e-5 &&
                        Math.abs(existing.getLongitude() - loc.getLongitude()) < 1e-5) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    allLocations.add(loc);
                }
            }
        }
        
        // Preparar lista final (sin filtrar por distancia para no ocultar Ourense en emuladores)
        List<Location> resultsWithDistance = new ArrayList<>(allLocations);
        
        // Añadir etiqueta de distancia si hay ubicación actual (solo informativa)
        android.location.Location currentLocation = locationService.getLastKnownLocation();
        if (currentLocation != null) {
            for (Location location : resultsWithDistance) {
                float[] d = new float[1];
                android.location.Location.distanceBetween(
                        currentLocation.getLatitude(), currentLocation.getLongitude(),
                        location.getLatitude(), location.getLongitude(),
                        d);
                float distanceInKm = d[0] / 1000f;
                String description = location.getDescription();
                if (description == null) description = "";
                String distanceLabel = distanceInKm < 1 ? (Math.round(distanceInKm * 1000) + "m")
                        : (String.format(Locale.getDefault(), "%.1fkm", distanceInKm));
                if (!description.contains(distanceLabel)) {
                    description = description.isEmpty() ? distanceLabel : (description + " (" + distanceLabel + ")");
                }
                location.setDescription(description);
            }
        }
        
        return resultsWithDistance;
    }
    
    /**
     * Busca todas las ubicaciones que coinciden con la consulta (listas locales de Ourense)
     */
    private List<Location> searchAllLocations(String query) {
        List<Location> results = new ArrayList<>();
        
        if (query == null || query.trim().isEmpty()) {
            return results;
        }
        
        query = query.toLowerCase(Locale.getDefault()).trim();
        
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
    
    /**
     * Usa Geocoder para obtener direcciones reales según el texto
     */
    private List<Location> geocodeLocations(String query, int maxResults) {
        if (query == null || query.trim().length() < 3) return new ArrayList<>();
        try {
            // Restringir a Ourense/Galicia para mayor relevancia
            List<Address> addresses = geocoder.getFromLocationName(query + ", Ourense", maxResults);
            List<Location> results = new ArrayList<>();
            if (addresses != null) {
                for (Address a : addresses) {
                    if (a == null || a.getLatitude() == 0 && a.getLongitude() == 0) continue;
                    String name = a.getFeatureName() != null ? a.getFeatureName() : (a.getThoroughfare() != null ? a.getThoroughfare() : "Dirección");
                    StringBuilder desc = new StringBuilder();
                    if (a.getThoroughfare() != null) desc.append(a.getThoroughfare()).append(" ");
                    if (a.getSubThoroughfare() != null) desc.append(a.getSubThoroughfare()).append(", ");
                    if (a.getLocality() != null) desc.append(a.getLocality()).append(", ");
                    if (a.getAdminArea() != null) desc.append(a.getAdminArea()).append(", ");
                    if (a.getCountryName() != null) desc.append(a.getCountryName());
                    String description = desc.toString().replaceAll(", $", "").trim();
                    results.add(new Location(name, description, a.getLatitude(), a.getLongitude()));
                }
            }
            return results;
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }
} 