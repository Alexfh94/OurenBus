package com.example.ourenbus2.ui.viewmodel;

import com.example.ourenbus2.model.BusLine;
import com.example.ourenbus2.model.BusStop;
import com.example.ourenbus2.model.Location;
import com.example.ourenbus2.model.Route;
import com.example.ourenbus2.model.RouteSegment;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;

/**
 * Clase para generar rutas de ejemplo para la aplicación
 * 
 * NOTA: Esta clase es temporal para simular la generación de rutas. En una implementación real,
 * esta funcionalidad se reemplazaría por una API de rutas real (por ejemplo, Google Directions API)
 */
public class RouteGenerator {

    private static final Random random = new Random();
    
    // Coordenadas de Ourense
    private static final double OURENSE_LAT = 42.3402;
    private static final double OURENSE_LON = -7.8636;
    
    // Datos de ejemplo para buses
    private static final BusLine[] BUS_LINES = {
            new BusLine(1, "Línea 1", "#F44336"),
            new BusLine(2, "Línea 2", "#9C27B0"),
            new BusLine(3, "Línea 3", "#2196F3"),
            new BusLine(4, "Línea 4", "#FF9800"),
            new BusLine(5, "Línea 5", "#009688")
    };
    
    // Paradas de bus de ejemplo
    private static final BusStop[] BUS_STOPS = {
            new BusStop("Plaza Mayor", 42.3367, -7.8633),
            new BusStop("Estación de Autobuses", 42.3420, -7.8660),
            new BusStop("Campus Universitario", 42.3492, -7.8471),
            new BusStop("Hospital", 42.3370, -7.8830),
            new BusStop("Alameda", 42.3352, -7.8642),
            new BusStop("Centro Comercial", 42.3280, -7.8720),
            new BusStop("Parque San Lázaro", 42.3412, -7.8638),
            new BusStop("Auditorio", 42.3462, -7.8571),
            new BusStop("Polideportivo", 42.3450, -7.8720),
            new BusStop("Estación de Tren", 42.3438, -7.8732)
    };
    
    /**
     * Genera una ruta entre dos ubicaciones
     * 
     * @param origin Ubicación de origen
     * @param destination Ubicación de destino
     * @return Ruta generada
     */
    public static Route generateRoute(Location origin, Location destination) {
        if (origin == null || destination == null) {
            return null;
        }
        
        Route route = new Route();
        route.setOrigin(origin);
        route.setDestination(destination);
        
        // Generar entre 2 y 5 segmentos para la ruta
        int numSegments = 2 + random.nextInt(4);
        List<RouteSegment> segments = new ArrayList<>();
        
        // Generar ubicaciones intermedias
        List<Location> intermediateLocations = generateIntermediateLocations(origin, destination, numSegments - 1);
        
        // Hora actual
        Calendar calendar = Calendar.getInstance();
        Date startTime = calendar.getTime();
        
        // Primer segmento (siempre a pie)
        RouteSegment firstSegment = new RouteSegment();
        firstSegment.setType(RouteSegment.SegmentType.WALKING);
        firstSegment.setStartLocation(origin);
        
        // Obtener la parada más cercana y convertirla a Location para el segmento
        BusStop nearestStop = getNearestBusStop(origin);
        Location stopLocation = new Location(nearestStop.getName(), nearestStop.getLatitude(), nearestStop.getLongitude());
        
        firstSegment.setEndLocation(stopLocation);
        firstSegment.setDistance(calculateDistance(origin, stopLocation));
        firstSegment.setDuration(firstSegment.getDistance() / 80); // Velocidad media: 80 metros/minuto
        firstSegment.setStartTime(startTime);
        
        calendar.add(Calendar.MINUTE, firstSegment.getDuration());
        firstSegment.setEndTime(calendar.getTime());
        firstSegment.setInstructions("Caminar hasta la parada " + stopLocation.getName());
        segments.add(firstSegment);
        
        // Segmentos intermedios
        for (int i = 0; i < numSegments - 2; i++) {
            RouteSegment segment = new RouteSegment();
            
            // Alternar entre bus y caminar
            if (i % 2 == 0) {
                // Segmento de bus
                segment.setType(RouteSegment.SegmentType.BUS);
                segment.setBusLine(BUS_LINES[random.nextInt(BUS_LINES.length)]);
                
                BusStop currentStop = getNearestBusStop(segments.get(segments.size() - 1).getEndLocation());
                segment.setBusStop(currentStop);
                segment.setStartLocation(segments.get(segments.size() - 1).getEndLocation());
                segment.setEndLocation(intermediateLocations.get(i));
                
                BusStop nextStop = getNearestBusStop(segment.getEndLocation());
                segment.setNextStop(nextStop);
                
                segment.setDistance(calculateDistance(segment.getStartLocation(), segment.getEndLocation()));
                segment.setDuration(segment.getDistance() / 400); // Velocidad media bus: 400 metros/minuto
                
                segment.setStartTime(calendar.getTime());
                calendar.add(Calendar.MINUTE, segment.getDuration());
                segment.setEndTime(calendar.getTime());
                
                segment.setInstructions("Tomar bus " + segment.getBusLine().getLineNumber() + 
                        " desde " + segment.getStartLocation().getName() + 
                        " hasta " + segment.getEndLocation().getName());
                
            } else {
                // Segmento a pie
                segment.setType(RouteSegment.SegmentType.WALKING);
                segment.setStartLocation(segments.get(segments.size() - 1).getEndLocation());
                segment.setEndLocation(intermediateLocations.get(i));
                
                segment.setDistance(calculateDistance(segment.getStartLocation(), segment.getEndLocation()));
                segment.setDuration(segment.getDistance() / 80); // Velocidad media: 80 metros/minuto
                
                segment.setStartTime(calendar.getTime());
                calendar.add(Calendar.MINUTE, segment.getDuration());
                segment.setEndTime(calendar.getTime());
                
                segment.setInstructions("Caminar desde " + segment.getStartLocation().getName() + 
                        " hasta " + segment.getEndLocation().getName());
            }
            
            segments.add(segment);
        }
        
        // Último segmento (siempre a pie)
        RouteSegment lastSegment = new RouteSegment();
        lastSegment.setType(RouteSegment.SegmentType.WALKING);
        lastSegment.setStartLocation(segments.get(segments.size() - 1).getEndLocation());
        lastSegment.setEndLocation(destination);
        
        lastSegment.setDistance(calculateDistance(lastSegment.getStartLocation(), lastSegment.getEndLocation()));
        lastSegment.setDuration(lastSegment.getDistance() / 80); // Velocidad media: 80 metros/minuto
        
        lastSegment.setStartTime(calendar.getTime());
        calendar.add(Calendar.MINUTE, lastSegment.getDuration());
        lastSegment.setEndTime(calendar.getTime());
        
        lastSegment.setInstructions("Caminar hasta el destino " + destination.getName());
        segments.add(lastSegment);
        
        // Asignar segmentos a la ruta
        route.setSegments(segments);
        route.calculateTotalDistance();
        route.calculateTotalDuration();
        
        return route;
    }
    
    /**
     * Genera ubicaciones intermedias entre el origen y el destino
     */
    private static List<Location> generateIntermediateLocations(Location origin, Location destination, int count) {
        List<Location> locations = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            double factor = (i + 1.0) / (count + 1);
            double lat = origin.getLatitude() + factor * (destination.getLatitude() - origin.getLatitude());
            double lon = origin.getLongitude() + factor * (destination.getLongitude() - origin.getLongitude());
            
            // Añadir algo de aleatoriedad
            lat += (random.nextDouble() - 0.5) * 0.01;
            lon += (random.nextDouble() - 0.5) * 0.01;
            
            Location location = new Location("Punto intermedio " + (i + 1), lat, lon);
            locations.add(location);
        }
        
        return locations;
    }
    
    /**
     * Calcula la distancia entre dos ubicaciones (usando la fórmula de Haversine)
     */
    private static int calculateDistance(Location origin, Location destination) {
        double earthRadius = 6371000; // metros
        
        double dLat = Math.toRadians(destination.getLatitude() - origin.getLatitude());
        double dLon = Math.toRadians(destination.getLongitude() - origin.getLongitude());
        
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(origin.getLatitude())) * Math.cos(Math.toRadians(destination.getLatitude())) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return (int) (earthRadius * c);
    }
    
    /**
     * Obtiene la parada de bus más cercana a una ubicación
     */
    private static BusStop getNearestBusStop(Location location) {
        BusStop nearest = BUS_STOPS[0];
        double minDistance = Double.MAX_VALUE;
        
        for (BusStop stop : BUS_STOPS) {
            // Crear ubicación a partir de la parada para calcular la distancia
            Location stopLocation = new Location(stop.getName(), stop.getLatitude(), stop.getLongitude());
            double distance = calculateDistance(location, stopLocation);
            if (distance < minDistance) {
                minDistance = distance;
                nearest = stop;
            }
        }
        
        return nearest;
    }
    
    /**
     * Genera ubicaciones de ejemplo para las sugerencias
     */
    public static List<Location> generateSuggestions(String query) {
        List<Location> suggestions = new ArrayList<>();
        
        if (query == null || query.isEmpty()) {
            return suggestions;
        }
        
        // Lugares de ejemplo en Ourense
        String[] places = {
                "Plaza Mayor",
                "Estación de Autobuses",
                "Campus Universitario",
                "Hospital",
                "Alameda",
                "Centro Comercial",
                "Parque San Lázaro",
                "Auditorio",
                "Polideportivo",
                "Estación de Tren",
                "Calle del Paseo",
                "Avenida de Portugal",
                "Rúa do Progreso",
                "Praza de Abastos",
                "Puente Romano",
                "Termas As Burgas",
                "Catedral de San Martín",
                "Jardines del Posío",
                "Centro Cultural Marcos Valcárcel",
                "Pazo Provincial"
        };
        
        // Filtrar sugerencias que contengan la consulta
        query = query.toLowerCase();
        for (String place : places) {
            if (place.toLowerCase().contains(query)) {
                // Generar coordenadas aleatorias cercanas a Ourense
                double lat = OURENSE_LAT + (random.nextDouble() - 0.5) * 0.05;
                double lon = OURENSE_LON + (random.nextDouble() - 0.5) * 0.05;
                
                suggestions.add(new Location(place, place, lat, lon));
                
                // Limitar a 5 sugerencias
                if (suggestions.size() >= 5) {
                    break;
                }
            }
        }
        
        return suggestions;
    }
}