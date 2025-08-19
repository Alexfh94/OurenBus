package com.example.ourenbus2.util.gtfs;

import android.content.Context;

import com.example.ourenbus2.database.AppDatabase;
import com.example.ourenbus2.database.dao.GtfsDao;
import com.example.ourenbus2.database.entity.gtfs.GtfsRouteEntity;
import com.example.ourenbus2.database.entity.gtfs.GtfsStopEntity;
import com.example.ourenbus2.database.entity.gtfs.GtfsStopTimeEntity;
import com.example.ourenbus2.database.entity.gtfs.GtfsTripEntity;
import com.example.ourenbus2.database.entity.gtfs.GtfsCalendarEntity;
import com.example.ourenbus2.database.entity.gtfs.GtfsCalendarDateEntity;
import com.example.ourenbus2.model.BusLine;
import com.example.ourenbus2.model.BusStop;
import com.example.ourenbus2.model.Location;
import com.example.ourenbus2.model.Route;
import com.example.ourenbus2.model.RouteSegment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * Enrutador básico con GTFS local: selecciona la parada origen/destino más cercana y
 * busca el primer trip que pase por ambas en orden, con salida dentro de los próximos 15 minutos.
 */
public class GtfsRouter {

    public static boolean hasData(Context context) {
        try {
            AppDatabase db = AppDatabase.getInstance(context);
            GtfsDao dao = db.gtfsDao();
            List<GtfsStopEntity> stops = dao.getAllStops();
            return stops != null && !stops.isEmpty();
        } catch (Exception e) { return false; }
    }

    public static Route findSimpleRoute(Context context, Location origin, Location destination) {
        AppDatabase db = AppDatabase.getInstance(context);
        GtfsDao dao = db.gtfsDao();
        List<GtfsStopEntity> stops = dao.getAllStops();
        if (stops == null || stops.isEmpty()) return null;

        // Probar múltiples paradas cercanas (top 3) para origen y destino
        List<GtfsStopEntity> startCandidates = kNearestStops(stops, origin, 20);
        List<GtfsStopEntity> endCandidates = kNearestStops(stops, destination, 20);

        Route best = null;
        int bestArrival = Integer.MAX_VALUE;
        int nowSec = currentSecondsOfDay();
        for (GtfsStopEntity startStop : startCandidates) {
            for (GtfsStopEntity endStop : endCandidates) {
                // Ventana de salida: máximo 10 min de espera tras caminar hasta la parada
                Location startLoc = new Location(startStop.name, startStop.name, startStop.lat, startStop.lon);
                int walk1Min = estimateWalkMinutes(origin, startLoc);
                int earliestBoard = nowSec + walk1Min * 60;
                int latestDep = earliestBoard + 10 * 60;

                List<String> startTrips = dao.getTripIdsByStop(startStop.stopId);
                if (startTrips == null || startTrips.isEmpty()) continue;
                for (String tripId : startTrips) {
                    List<GtfsStopTimeEntity> times = dao.getStopTimesByTrip(tripId);
                    // Filtrar por servicio activo hoy
                    GtfsTripEntity trip = dao.getTripById(tripId);
                    if (!isServiceActiveToday(dao, trip != null ? trip.serviceId : null)) continue;
                    int startSeq = -1, endSeq = -1, depStart = -1, arrEnd = -1;
                    for (GtfsStopTimeEntity st : times) {
                        if (st.stopId.equals(startStop.stopId)) { startSeq = st.stopSequence; depStart = st.departureSeconds; }
                        if (st.stopId.equals(endStop.stopId)) { endSeq = st.stopSequence; arrEnd = st.arrivalSeconds; }
                    }
                    if (startSeq == -1 || endSeq == -1 || endSeq <= startSeq) continue;
                    if (depStart < earliestBoard || depStart > latestDep) continue;
                    if (arrEnd <= depStart) continue;
                    if (arrEnd < bestArrival) {
                        bestArrival = arrEnd;
                        // Construir ruta
                        Route r = new Route();
                        r.setOrigin(origin);
                        r.setDestination(destination);
                        List<RouteSegment> segments = new ArrayList<>();
                        // Caminar a parada origen
                        RouteSegment walk1 = new RouteSegment();
                        walk1.setType(RouteSegment.SegmentType.WALKING);
                        walk1.setStartLocation(origin);
                        walk1.setEndLocation(startLoc);
                        walk1.setDuration(walk1Min);
                        walk1.setDistance(estimateDistanceMeters(origin, startLoc));
                        Date t0 = new Date();
                        walk1.setStartTime(t0);
                        walk1.setEndTime(new Date(t0.getTime() + walk1Min * 60000L));
                        segments.add(walk1);
                        // Espera si aplica
                        int waitMin = Math.max(0, (depStart - earliestBoard) / 60);
                        if (waitMin > 0) {
                            RouteSegment wait = new RouteSegment();
                            wait.setType(RouteSegment.SegmentType.WAIT);
                            wait.setStartLocation(startLoc);
                            wait.setEndLocation(startLoc);
                            wait.setDuration(waitMin);
                            wait.setDistance(0);
                            Date ws = walk1.getEndTime();
                            wait.setStartTime(ws);
                            wait.setEndTime(new Date(ws.getTime() + waitMin * 60000L));
                            wait.setInstructions("Esperar " + waitMin + " min en " + startLoc.getName());
                            segments.add(wait);
                        }
                        // Bus segment
                        GtfsRouteEntity route = trip != null ? dao.getRouteById(trip.routeId) : null;
                        RouteSegment bus = new RouteSegment();
                        bus.setType(RouteSegment.SegmentType.BUS);
                        bus.setStartLocation(startLoc);
                        Location endLoc = new Location(endStop.name, endStop.name, endStop.lat, endStop.lon);
                        bus.setEndLocation(endLoc);
                        int busMin = Math.max(1, (arrEnd - depStart) / 60);
                        bus.setDuration(busMin);
                        bus.setDistance(estimateDistanceMeters(startLoc, endLoc));
                        BusLine busLine = new BusLine(parseIntSafe(route != null ? route.shortName : null),
                                route != null && route.shortName != null ? ("Línea " + route.shortName) : "Autobús",
                                route != null ? route.color : null);
                        bus.setBusLine(busLine);
                        bus.setBusStop(new BusStop(startLoc.getName(), startLoc.getLatitude(), startLoc.getLongitude()));
                        bus.setNextStop(new BusStop(endLoc.getName(), endLoc.getLatitude(), endLoc.getLongitude()));
                        Date bs = new Date((long) depStart * 1000L);
                        Date be = new Date((long) arrEnd * 1000L);
                        bus.setStartTime(bs);
                        bus.setEndTime(be);
                        bus.setInstructions("Tomar " + busLine.getLineNumber() + " desde " + startLoc.getName() + " hasta " + endLoc.getName());
                        segments.add(bus);
                        // Caminar a destino
                        RouteSegment walk2 = new RouteSegment();
                        walk2.setType(RouteSegment.SegmentType.WALKING);
                        walk2.setStartLocation(endLoc);
                        walk2.setEndLocation(destination);
                        int walk2Min = estimateWalkMinutes(endLoc, destination);
                        walk2.setDuration(walk2Min);
                        walk2.setDistance(estimateDistanceMeters(endLoc, destination));
                        walk2.setStartTime(bus.getEndTime());
                        walk2.setEndTime(new Date(bus.getEndTime().getTime() + walk2Min * 60000L));
                        walk2.setInstructions("Caminar hasta destino");
                        segments.add(walk2);

                        r.setSegments(segments);
                        r.calculateTotalDistance();
                        r.calculateTotalDuration();
                        best = r;
                    }
                }
                if (best != null) return best;
                // Intentar con transferencias (1 o 2) con espera ≤10min entre estos candidatos
                Route transfer = findRouteWithSingleTransfer(context, origin, destination, startStop, endStop);
                if (transfer != null) return transfer;
                Route transfer2 = findRouteWithDoubleTransfer(context, origin, destination, startStop, endStop);
                if (transfer2 != null) return transfer2;
            }
        }
        return null;
    }

    private static Route findRouteWithSingleTransfer(Context context, Location origin, Location destination,
                                                     GtfsStopEntity startStop, GtfsStopEntity endStop) {
        AppDatabase db = AppDatabase.getInstance(context);
        GtfsDao dao = db.gtfsDao();
        List<String> startTrips = dao.getTripIdsByStop(startStop.stopId);
        if (startTrips == null) return null;
        int bestArr = Integer.MAX_VALUE;
        Route best = null;
        int nowSec = currentSecondsOfDay();
        Location startLocBase = new Location(startStop.name, startStop.name, startStop.lat, startStop.lon);
        int walk1MinBase = estimateWalkMinutes(origin, startLocBase);
        int earliestBoard = nowSec + walk1MinBase * 60;
        int latestDep = earliestBoard + 10 * 60;
        for (String t1 : startTrips) {
            List<GtfsStopTimeEntity> times1 = dao.getStopTimesByTrip(t1);
            GtfsTripEntity trip1Meta = dao.getTripById(t1);
            if (!isServiceActiveToday(dao, trip1Meta != null ? trip1Meta.serviceId : null)) continue;
            int depStart = -1; int startSeq = -1;
            for (GtfsStopTimeEntity st : times1) {
                if (st.stopId.equals(startStop.stopId)) { depStart = st.departureSeconds; startSeq = st.stopSequence; break; }
            }
            if (depStart < earliestBoard || depStart > latestDep) continue;
            // Explorar posibles puntos de transbordo en este viaje
            for (GtfsStopTimeEntity mid : times1) {
                if (mid.stopSequence <= startSeq) continue;
                // Desde mid.stopId, buscar un segundo viaje que llegue a endStop después y con espera <=10min
                List<String> midTrips = dao.getTripIdsByStop(mid.stopId);
                if (midTrips == null) continue;
                for (String t2 : midTrips) {
                    if (t2.equals(t1)) continue; // evitar el mismo
                    List<GtfsStopTimeEntity> times2 = dao.getStopTimesByTrip(t2);
                    GtfsTripEntity trip2Meta = dao.getTripById(t2);
                    if (!isServiceActiveToday(dao, trip2Meta != null ? trip2Meta.serviceId : null)) continue;
                    int midSeq2 = -1, dep2 = -1, endSeq2 = -1, arr2 = -1;
                    for (GtfsStopTimeEntity st2 : times2) {
                        if (st2.stopId.equals(mid.stopId)) { midSeq2 = st2.stopSequence; dep2 = st2.departureSeconds; }
                        if (st2.stopId.equals(endStop.stopId)) { endSeq2 = st2.stopSequence; arr2 = st2.arrivalSeconds; }
                    }
                    if (midSeq2 == -1 || endSeq2 == -1 || endSeq2 <= midSeq2) continue;
                    int wait2 = dep2 - mid.arrivalSeconds;
                    if (wait2 < 0 || wait2 > 10 * 60) continue; // espera máxima 10 min
                    if (arr2 <= depStart) continue;
                    if (arr2 < bestArr) {
                        bestArr = arr2;
                        // Construir la ruta con 2 buses
                        Route r = new Route();
                        r.setOrigin(origin);
                        r.setDestination(destination);
                        List<RouteSegment> segs = new ArrayList<>();
                        // Walk to start
                        Location startLoc = startLocBase;
                        RouteSegment w1 = new RouteSegment();
                        w1.setType(RouteSegment.SegmentType.WALKING);
                        w1.setStartLocation(origin); w1.setEndLocation(startLoc);
                        w1.setDuration(walk1MinBase); w1.setDistance(estimateDistanceMeters(origin, startLoc));
                        Date now = new Date(); w1.setStartTime(now); w1.setEndTime(new Date(now.getTime() + w1.getDuration() * 60000L));
                        segs.add(w1);
                        int wait1min = Math.max(0, (depStart - earliestBoard) / 60);
                        if (wait1min > 0) {
                            RouteSegment wait1 = new RouteSegment();
                            wait1.setType(RouteSegment.SegmentType.WAIT);
                            wait1.setStartLocation(startLoc); wait1.setEndLocation(startLoc);
                            wait1.setDuration(wait1min); wait1.setDistance(0);
                            wait1.setStartTime(w1.getEndTime()); wait1.setEndTime(new Date(w1.getEndTime().getTime() + wait1min * 60000L));
                            wait1.setInstructions("Esperar " + wait1min + " min en " + startLoc.getName());
                            segs.add(wait1);
                        }
                        // Bus1
                        GtfsTripEntity trip1 = dao.getTripById(t1);
                        GtfsRouteEntity route1 = trip1 != null ? dao.getRouteById(trip1.routeId) : null;
                        Location midLoc = new Location(mid.stopId, mid.stopId, dao.getStopById(mid.stopId).lat, dao.getStopById(mid.stopId).lon);
                        RouteSegment bus1 = new RouteSegment();
                        bus1.setType(RouteSegment.SegmentType.BUS);
                        bus1.setStartLocation(startLoc); bus1.setEndLocation(midLoc);
                        int bus1Min = Math.max(1, (mid.arrivalSeconds - depStart) / 60);
                        bus1.setDuration(bus1Min); bus1.setDistance(estimateDistanceMeters(startLoc, midLoc));
                        BusLine bl1 = new BusLine(parseIntSafe(route1 != null ? route1.shortName : null), route1 != null && route1.shortName != null ? ("Línea " + route1.shortName) : "Autobús", route1 != null ? route1.color : null);
                        bus1.setBusLine(bl1);
                        bus1.setBusStop(new BusStop(startLoc.getName(), startLoc.getLatitude(), startLoc.getLongitude()));
                        bus1.setNextStop(new BusStop(midLoc.getName(), midLoc.getLatitude(), midLoc.getLongitude()));
                        bus1.setStartTime(new Date((long) depStart * 1000L)); bus1.setEndTime(new Date((long) mid.arrivalSeconds * 1000L));
                        segs.add(bus1);
                        // Wait2
                        int wait2min = Math.max(0, wait2 / 60);
                        if (wait2min > 0) {
                            RouteSegment w2 = new RouteSegment();
                            w2.setType(RouteSegment.SegmentType.WAIT);
                            w2.setStartLocation(midLoc); w2.setEndLocation(midLoc);
                            w2.setDuration(wait2min); w2.setDistance(0);
                            w2.setStartTime(bus1.getEndTime()); w2.setEndTime(new Date(bus1.getEndTime().getTime() + wait2min * 60000L));
                            w2.setInstructions("Esperar " + wait2min + " min en " + midLoc.getName());
                            segs.add(w2);
                        }
                        // Bus2
                        GtfsTripEntity trip2 = dao.getTripById(t2);
                        GtfsRouteEntity route2 = trip2 != null ? dao.getRouteById(trip2.routeId) : null;
                        Location endLoc = new Location(endStop.name, endStop.name, endStop.lat, endStop.lon);
                        RouteSegment bus2 = new RouteSegment();
                        bus2.setType(RouteSegment.SegmentType.BUS);
                        bus2.setStartLocation(midLoc); bus2.setEndLocation(endLoc);
                        int bus2Min = Math.max(1, (arr2 - dep2) / 60);
                        bus2.setDuration(bus2Min); bus2.setDistance(estimateDistanceMeters(midLoc, endLoc));
                        BusLine bl2 = new BusLine(parseIntSafe(route2 != null ? route2.shortName : null), route2 != null && route2.shortName != null ? ("Línea " + route2.shortName) : "Autobús", route2 != null ? route2.color : null);
                        bus2.setBusLine(bl2);
                        bus2.setBusStop(new BusStop(midLoc.getName(), midLoc.getLatitude(), midLoc.getLongitude()));
                        bus2.setNextStop(new BusStop(endLoc.getName(), endLoc.getLatitude(), endLoc.getLongitude()));
                        bus2.setStartTime(new Date((long) dep2 * 1000L)); bus2.setEndTime(new Date((long) arr2 * 1000L));
                        segs.add(bus2);
                        // Walk to destination
                        RouteSegment w3 = new RouteSegment();
                        w3.setType(RouteSegment.SegmentType.WALKING);
                        w3.setStartLocation(endLoc); w3.setEndLocation(destination);
                        int w3min = estimateWalkMinutes(endLoc, destination);
                        w3.setDuration(w3min); w3.setDistance(estimateDistanceMeters(endLoc, destination));
                        w3.setStartTime(bus2.getEndTime()); w3.setEndTime(new Date(bus2.getEndTime().getTime() + w3min * 60000L));
                        w3.setInstructions("Caminar hasta destino");
                        segs.add(w3);
                        r.setSegments(segs);
                        r.calculateTotalDistance(); r.calculateTotalDuration();
                        best = r;
                    }
                }
            }
        }
        return best;
    }

    private static Route findRouteWithDoubleTransfer(Context context, Location origin, Location destination,
                                                     GtfsStopEntity startStop, GtfsStopEntity endStop) {
        AppDatabase db = AppDatabase.getInstance(context);
        GtfsDao dao = db.gtfsDao();
        List<String> startTrips = dao.getTripIdsByStop(startStop.stopId);
        if (startTrips == null) return null;
        int nowSec = currentSecondsOfDay();
        Location startLoc = new Location(startStop.name, startStop.name, startStop.lat, startStop.lon);
        int walk1Min = estimateWalkMinutes(origin, startLoc);
        int earliestBoard = nowSec + walk1Min * 60;
        int latestDep = earliestBoard + 10 * 60;
        Route best = null; int bestArr = Integer.MAX_VALUE;
        for (String t1 : startTrips) {
            List<GtfsStopTimeEntity> times1 = dao.getStopTimesByTrip(t1);
            GtfsTripEntity trip1Meta = dao.getTripById(t1);
            if (!isServiceActiveToday(dao, trip1Meta != null ? trip1Meta.serviceId : null)) continue;
            int startSeq = -1, dep1 = -1;
            for (GtfsStopTimeEntity st : times1) {
                if (st.stopId.equals(startStop.stopId)) { startSeq = st.stopSequence; dep1 = st.departureSeconds; break; }
            }
            if (dep1 < earliestBoard || dep1 > latestDep) continue;
            // Explorar posibles mid1
            for (GtfsStopTimeEntity mid1 : times1) {
                if (mid1.stopSequence <= startSeq) continue;
                // Segundo viaje desde mid1
                List<String> t2ids = dao.getTripIdsByStop(mid1.stopId);
                if (t2ids == null) continue;
                for (String t2 : t2ids) {
                    if (t2.equals(t1)) continue;
                    List<GtfsStopTimeEntity> times2 = dao.getStopTimesByTrip(t2);
                    GtfsTripEntity trip2Meta = dao.getTripById(t2);
                    if (!isServiceActiveToday(dao, trip2Meta != null ? trip2Meta.serviceId : null)) continue;
                    int mid1Seq2 = -1, dep2 = -1;
                    for (GtfsStopTimeEntity st2 : times2) {
                        if (st2.stopId.equals(mid1.stopId)) { mid1Seq2 = st2.stopSequence; dep2 = st2.departureSeconds; }
                    }
                    int wait2 = dep2 - mid1.arrivalSeconds;
                    if (mid1Seq2 == -1 || wait2 < 0 || wait2 > 10 * 60) continue;
                    // Explorar posibles mid2 en t2
                    for (GtfsStopTimeEntity mid2 : times2) {
                        if (mid2.stopSequence <= mid1Seq2) continue;
                        // Tercer viaje desde mid2 hacia endStop
                        List<String> t3ids = dao.getTripIdsByStop(mid2.stopId);
                        if (t3ids == null) continue;
                        for (String t3 : t3ids) {
                            if (t3.equals(t2)) continue;
                            List<GtfsStopTimeEntity> times3 = dao.getStopTimesByTrip(t3);
                            GtfsTripEntity trip3Meta = dao.getTripById(t3);
                            if (!isServiceActiveToday(dao, trip3Meta != null ? trip3Meta.serviceId : null)) continue;
                            int mid2Seq3 = -1, dep3 = -1, endSeq3 = -1, arr3 = -1;
                            for (GtfsStopTimeEntity st3 : times3) {
                                if (st3.stopId.equals(mid2.stopId)) { mid2Seq3 = st3.stopSequence; dep3 = st3.departureSeconds; }
                                if (st3.stopId.equals(endStop.stopId)) { endSeq3 = st3.stopSequence; arr3 = st3.arrivalSeconds; }
                            }
                            if (mid2Seq3 == -1 || endSeq3 == -1 || endSeq3 <= mid2Seq3) continue;
                            int wait3 = dep3 - mid2.arrivalSeconds;
                            if (wait3 < 0 || wait3 > 10 * 60) continue;
                            if (arr3 < dep1) continue;
                            if (arr3 < bestArr) {
                                bestArr = arr3;
                                // Construir ruta: walk1, optional wait1, bus1, wait2, bus2, wait3, bus3, walk2
                                Route r = new Route(); r.setOrigin(origin); r.setDestination(destination);
                                List<RouteSegment> segs = new ArrayList<>();
                                // Walk to start
                                RouteSegment w1 = new RouteSegment(); w1.setType(RouteSegment.SegmentType.WALKING);
                                w1.setStartLocation(origin); w1.setEndLocation(startLoc);
                                w1.setDuration(walk1Min); w1.setDistance(estimateDistanceMeters(origin, startLoc));
                                Date now = new Date(); w1.setStartTime(now); w1.setEndTime(new Date(now.getTime() + w1.getDuration() * 60000L));
                                segs.add(w1);
                                int wait1 = Math.max(0, (dep1 - earliestBoard) / 60);
                                if (wait1 > 0) {
                                    RouteSegment wseg1 = new RouteSegment(); wseg1.setType(RouteSegment.SegmentType.WAIT);
                                    wseg1.setStartLocation(startLoc); wseg1.setEndLocation(startLoc);
                                    wseg1.setDuration(wait1); wseg1.setDistance(0);
                                    wseg1.setStartTime(w1.getEndTime()); wseg1.setEndTime(new Date(w1.getEndTime().getTime() + wait1 * 60000L));
                                    wseg1.setInstructions("Esperar " + wait1 + " min en " + startLoc.getName());
                                    segs.add(wseg1);
                                }
                                // Bus1
                                GtfsRouteEntity route1 = trip1Meta != null ? dao.getRouteById(trip1Meta.routeId) : null;
                                Location mid1Loc = toLoc(dao.getStopById(mid1.stopId));
                                RouteSegment b1 = new RouteSegment(); b1.setType(RouteSegment.SegmentType.BUS);
                                b1.setStartLocation(startLoc); b1.setEndLocation(mid1Loc);
                                int b1min = Math.max(1, (mid1.arrivalSeconds - dep1) / 60);
                                b1.setDuration(b1min); b1.setDistance(estimateDistanceMeters(startLoc, mid1Loc));
                                BusLine bl1 = new BusLine(parseIntSafe(route1 != null ? route1.shortName : null), route1 != null && route1.shortName != null ? ("Línea " + route1.shortName) : "Autobús", route1 != null ? route1.color : null);
                                b1.setBusLine(bl1);
                                b1.setBusStop(new BusStop(startLoc.getName(), startLoc.getLatitude(), startLoc.getLongitude()));
                                b1.setNextStop(new BusStop(mid1Loc.getName(), mid1Loc.getLatitude(), mid1Loc.getLongitude()));
                                b1.setStartTime(new Date((long) dep1 * 1000L)); b1.setEndTime(new Date((long) mid1.arrivalSeconds * 1000L));
                                segs.add(b1);
                                // Wait2
                                int w2min = Math.max(0, (dep2 - mid1.arrivalSeconds) / 60);
                                if (w2min > 0) {
                                    RouteSegment w2 = new RouteSegment(); w2.setType(RouteSegment.SegmentType.WAIT);
                                    w2.setStartLocation(mid1Loc); w2.setEndLocation(mid1Loc);
                                    w2.setDuration(w2min); w2.setDistance(0);
                                    w2.setStartTime(b1.getEndTime()); w2.setEndTime(new Date(b1.getEndTime().getTime() + w2min * 60000L));
                                    w2.setInstructions("Esperar " + w2min + " min en " + mid1Loc.getName());
                                    segs.add(w2);
                                }
                                // Bus2
                                GtfsRouteEntity route2 = trip2Meta != null ? dao.getRouteById(trip2Meta.routeId) : null;
                                Location mid2Loc = toLoc(dao.getStopById(mid2.stopId));
                                RouteSegment b2 = new RouteSegment(); b2.setType(RouteSegment.SegmentType.BUS);
                                b2.setStartLocation(mid1Loc); b2.setEndLocation(mid2Loc);
                                int b2min = Math.max(1, (mid2.arrivalSeconds - dep2) / 60);
                                b2.setDuration(b2min); b2.setDistance(estimateDistanceMeters(mid1Loc, mid2Loc));
                                BusLine bl2 = new BusLine(parseIntSafe(route2 != null ? route2.shortName : null), route2 != null && route2.shortName != null ? ("Línea " + route2.shortName) : "Autobús", route2 != null ? route2.color : null);
                                b2.setBusLine(bl2);
                                b2.setBusStop(new BusStop(mid1Loc.getName(), mid1Loc.getLatitude(), mid1Loc.getLongitude()));
                                b2.setNextStop(new BusStop(mid2Loc.getName(), mid2Loc.getLatitude(), mid2Loc.getLongitude()));
                                b2.setStartTime(new Date((long) dep2 * 1000L)); b2.setEndTime(new Date((long) mid2.arrivalSeconds * 1000L));
                                segs.add(b2);
                                // Wait3
                                int w3min = Math.max(0, (dep3 - mid2.arrivalSeconds) / 60);
                                if (w3min > 0) {
                                    RouteSegment w3 = new RouteSegment(); w3.setType(RouteSegment.SegmentType.WAIT);
                                    w3.setStartLocation(mid2Loc); w3.setEndLocation(mid2Loc);
                                    w3.setDuration(w3min); w3.setDistance(0);
                                    w3.setStartTime(b2.getEndTime()); w3.setEndTime(new Date(b2.getEndTime().getTime() + w3min * 60000L));
                                    w3.setInstructions("Esperar " + w3min + " min en " + mid2Loc.getName());
                                    segs.add(w3);
                                }
                                // Bus3 a end
                                GtfsRouteEntity route3 = trip3Meta != null ? dao.getRouteById(trip3Meta.routeId) : null;
                                Location endLoc = new Location(endStop.name, endStop.name, endStop.lat, endStop.lon);
                                RouteSegment b3 = new RouteSegment(); b3.setType(RouteSegment.SegmentType.BUS);
                                b3.setStartLocation(mid2Loc); b3.setEndLocation(endLoc);
                                int b3min = Math.max(1, (arr3 - dep3) / 60);
                                b3.setDuration(b3min); b3.setDistance(estimateDistanceMeters(mid2Loc, endLoc));
                                BusLine bl3 = new BusLine(parseIntSafe(route3 != null ? route3.shortName : null), route3 != null && route3.shortName != null ? ("Línea " + route3.shortName) : "Autobús", route3 != null ? route3.color : null);
                                b3.setBusLine(bl3);
                                b3.setBusStop(new BusStop(mid2Loc.getName(), mid2Loc.getLatitude(), mid2Loc.getLongitude()));
                                b3.setNextStop(new BusStop(endLoc.getName(), endLoc.getLatitude(), endLoc.getLongitude()));
                                b3.setStartTime(new Date((long) dep3 * 1000L)); b3.setEndTime(new Date((long) arr3 * 1000L));
                                segs.add(b3);
                                // Walk to destination
                                RouteSegment wDest = new RouteSegment(); wDest.setType(RouteSegment.SegmentType.WALKING);
                                wDest.setStartLocation(endLoc); wDest.setEndLocation(destination);
                                int wDmin = estimateWalkMinutes(endLoc, destination);
                                wDest.setDuration(wDmin); wDest.setDistance(estimateDistanceMeters(endLoc, destination));
                                wDest.setStartTime(b3.getEndTime()); wDest.setEndTime(new Date(b3.getEndTime().getTime() + wDmin * 60000L));
                                wDest.setInstructions("Caminar hasta destino");
                                segs.add(wDest);
                                r.setSegments(segs); r.calculateTotalDistance(); r.calculateTotalDuration();
                                best = r;
                            }
                        }
                    }
                }
            }
        }
        return best;
    }

    private static Location toLoc(GtfsStopEntity e) {
        return new Location(e.name, e.name, e.lat, e.lon);
    }

    private static List<GtfsStopEntity> kNearestStops(List<GtfsStopEntity> all, Location target, int k) {
        if (target == null || all == null || all.isEmpty()) return new ArrayList<>();
        List<GtfsStopEntity> copy = new ArrayList<>(all);
        copy.sort(Comparator.comparingDouble(s -> haversineKm(s.lat, s.lon, target.getLatitude(), target.getLongitude())));
        if (k < copy.size()) return new ArrayList<>(copy.subList(0, k));
        return copy;
    }

    private static boolean isServiceActiveToday(GtfsDao dao, String serviceId) {
        if (serviceId == null) return false;
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int y = cal.get(java.util.Calendar.YEAR);
        int m = cal.get(java.util.Calendar.MONTH) + 1;
        int d = cal.get(java.util.Calendar.DAY_OF_MONTH);
        int ymd = y * 10000 + m * 100 + d;
        int dow = cal.get(java.util.Calendar.DAY_OF_WEEK); // 1=Sunday
        GtfsCalendarEntity calEnt = dao.getCalendarByService(serviceId);
        boolean base = false;
        if (calEnt != null && ymd >= calEnt.startDate && ymd <= calEnt.endDate) {
            switch (dow) {
                case java.util.Calendar.MONDAY: base = calEnt.monday == 1; break;
                case java.util.Calendar.TUESDAY: base = calEnt.tuesday == 1; break;
                case java.util.Calendar.WEDNESDAY: base = calEnt.wednesday == 1; break;
                case java.util.Calendar.THURSDAY: base = calEnt.thursday == 1; break;
                case java.util.Calendar.FRIDAY: base = calEnt.friday == 1; break;
                case java.util.Calendar.SATURDAY: base = calEnt.saturday == 1; break;
                case java.util.Calendar.SUNDAY: base = calEnt.sunday == 1; break;
            }
        }
        List<GtfsCalendarDateEntity> dates = dao.getCalendarDatesByService(serviceId);
        if (dates != null) {
            for (GtfsCalendarDateEntity cd : dates) {
                if (cd.date == ymd) {
                    if (cd.exceptionType == 1) return true; // se añade el servicio
                    if (cd.exceptionType == 2) return false; // se excluye el servicio
                }
            }
        }
        return base;
    }

    private static GtfsStopEntity nearestStop(List<GtfsStopEntity> stops, Location target) {
        if (target == null) return null;
        return Collections.min(stops, Comparator.comparingDouble(s -> haversineKm(s.lat, s.lon, target.getLatitude(), target.getLongitude())));
    }

    private static int estimateWalkMinutes(Location a, Location b) {
        double km = haversineKm(a.getLatitude(), a.getLongitude(), b.getLatitude(), b.getLongitude());
        double m = km * 1000.0;
        return (int) Math.max(1, Math.round(m / 80.0));
    }

    private static int estimateDistanceMeters(Location a, Location b) {
        double km = haversineKm(a.getLatitude(), a.getLongitude(), b.getLatitude(), b.getLongitude());
        return (int) Math.round(km * 1000.0);
    }

    private static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R * c;
    }

    private static int parseIntSafe(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return 0; }
    }

    private static int currentSecondsOfDay() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        return cal.get(java.util.Calendar.HOUR_OF_DAY) * 3600 + cal.get(java.util.Calendar.MINUTE) * 60 + cal.get(java.util.Calendar.SECOND);
    }
}


