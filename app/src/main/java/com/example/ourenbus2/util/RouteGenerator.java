package com.example.ourenbus2.util;

import com.example.ourenbus2.model.BusLine;
import com.example.ourenbus2.model.BusStop;
import com.example.ourenbus2.model.Location;
import com.example.ourenbus2.model.Route;
import com.example.ourenbus2.model.RouteSegment;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Utilidad para generar rutas de ejemplo para el prototipo
 */
public class RouteGenerator {
    
    // Líneas de autobús disponibles en Ourense
    private static final BusLine[] BUS_LINES = {
            new BusLine(1, "Línea 1", "Campus - Centro"),
            new BusLine(2, "Línea 2", "Hospital - Estación"),
            new BusLine(3, "Línea 3", "Polígono - Centro"),
            new BusLine(4, "Línea 4", "Circular"),
            new BusLine(5, "Línea 5", "Zona Norte - Centro")
    };
    
    // Paradas de autobús en Ourense
    private static final BusStop[] BUS_STOPS = {
            new BusStop("Parada Campus", 42.3450, -7.8500),
            new BusStop("Parada Hospital", 42.3380, -7.8670),
            new BusStop("Parada Estación", 42.3520, -7.8650),
            new BusStop("Parada Centro", 42.3370, -7.8630),
            new BusStop("Parada Polígono", 42.3320, -7.8750),
            new BusStop("Parada Zona Norte", 42.3430, -7.8580)
    };
    
    /**
     * Genera una ruta de ejemplo entre origen y destino
     */
    public static Route generateSampleRoute(Location origin, Location destination) {
        Route route = new Route();
        route.setOrigin(origin);
        route.setDestination(destination);
        
        // Generar segmentos aleatorios para la ruta
        List<RouteSegment> segments = generateRandomSegments(origin, destination);
        route.setSegments(segments);
        
        // Calcular tiempo estimado (suma de tiempos de los segmentos)
        int totalTime = 0;
        for (RouteSegment segment : segments) {
            totalTime += segment.getTimeInMinutes();
        }
        route.setEstimatedTimeInMinutes(totalTime);
        
        return route;
    }
    
    /**
     * Genera segmentos aleatorios para una ruta
     */
    private static List<RouteSegment> generateRandomSegments(Location origin, Location destination) {
        List<RouteSegment> segments = new ArrayList<>();
        Random random = new Random();
        
        // Primer segmento: Caminar hasta la parada
        BusStop firstStop = BUS_STOPS[random.nextInt(BUS_STOPS.length)];
        // Convertir BusStop a Location
        Location firstStopLocation = new Location(
                firstStop.getName(), 
                firstStop.getName(),
                firstStop.getLatitude(),
                firstStop.getLongitude()
        );
        segments.add(createWalkingSegment(origin, firstStopLocation, 5 + random.nextInt(10)));
        
        // Si el destino está cerca, solo un segmento de caminar
        double distanceToDestination = calculateDistance(
                firstStop.getLatitude(), firstStop.getLongitude(),
                destination.getLatitude(), destination.getLongitude());
        
        if (distanceToDestination < 0.5) { // Si está a menos de 500m
            segments.add(createWalkingSegment(firstStopLocation, destination, 5 + random.nextInt(5)));
            return segments;
        }
        
        // Segundo segmento: Autobús
        BusLine busLine = BUS_LINES[random.nextInt(BUS_LINES.length)];
        BusStop secondStop = BUS_STOPS[random.nextInt(BUS_STOPS.length)];
        // Asegurar que la segunda parada sea diferente de la primera
        while (secondStop.getName().equals(firstStop.getName())) {
            secondStop = BUS_STOPS[random.nextInt(BUS_STOPS.length)];
        }
        
        // Convertir BusStop a Location
        Location secondStopLocation = new Location(
                secondStop.getName(),
                secondStop.getName(),
                secondStop.getLatitude(),
                secondStop.getLongitude()
        );
        
        segments.add(createBusSegment(firstStop, secondStop, busLine, 10 + random.nextInt(15)));
        
        // Tercer segmento: Caminar hasta el destino
        segments.add(createWalkingSegment(secondStopLocation, destination, 5 + random.nextInt(10)));
        
        return segments;
    }
    
    /**
     * Crea un segmento de caminar
     */
    private static RouteSegment createWalkingSegment(Location start, Location end, int timeInMinutes) {
        RouteSegment segment = new RouteSegment();
        segment.setType(RouteSegment.SegmentType.WALKING);
        segment.setStartLocation(start);
        segment.setEndLocation(end);
        segment.setTimeInMinutes(timeInMinutes);
        
        segment.setDistance((int)(calculateDistance(
                start.getLatitude(), start.getLongitude(),
                end.getLatitude(), end.getLongitude()) * 1000)); // convertir a metros
        
        // Instrucciones
        String endName = end.getName() != null ? end.getName() : "el destino";
        String instructions = "Camina hasta " + endName + " (aprox. " + timeInMinutes + " min, " + segment.getDistance() + " m)";
        segment.setInstructions(instructions);
        return segment;
    }
    
    /**
     * Crea un segmento de autobús
     */
    private static RouteSegment createBusSegment(BusStop start, BusStop end, BusLine busLine, int timeInMinutes) {
        RouteSegment segment = new RouteSegment();
        segment.setType(RouteSegment.SegmentType.BUS);
        
        // Convertir BusStop a Location
        Location startLocation = new Location(
                start.getName(),
                start.getName(),
                start.getLatitude(),
                start.getLongitude()
        );
        
        Location endLocation = new Location(
                end.getName(),
                end.getName(),
                end.getLatitude(),
                end.getLongitude()
        );
        
        segment.setStartLocation(startLocation);
        segment.setEndLocation(endLocation);
        segment.setBusLine(busLine);
        segment.setBusStop(start);
        segment.setTimeInMinutes(timeInMinutes);
        
        segment.setDistance((int)(calculateDistance(
                start.getLatitude(), start.getLongitude(),
                end.getLatitude(), end.getLongitude()) * 1000)); // convertir a metros
        
        // Instrucciones
        String lineName = busLine != null ? ("línea " + busLine.getLineNumber()) : "autobús";
        int stops = Math.max(2, timeInMinutes / 3); // estimación simple de paradas
        String instructions = "Toma la " + lineName + " desde " + start.getName() + " hasta " + end.getName() +
                " (aprox. " + timeInMinutes + " min, " + stops + " paradas, " + segment.getDistance() + " m)";
        segment.setInstructions(instructions);
        return segment;
    }
    
    /**
     * Calcula la distancia entre dos puntos en coordenadas geográficas (fórmula de Haversine)
     * @return distancia en kilómetros
     */
    private static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radio de la Tierra en km
        
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c;
    }
} 