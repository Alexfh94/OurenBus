package com.example.ourenbus2.service;

import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;

import com.example.ourenbus2.model.BusLine;
import com.example.ourenbus2.model.BusStop;
import com.example.ourenbus2.model.Location;
import com.example.ourenbus2.model.Route;
import com.example.ourenbus2.model.RouteSegment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Servicio HTTP para consultar la API de Google Directions y transformar la respuesta
 * en el modelo de dominio de la app. Genera segmentos de ruta (a pie y bus) y
 * aprovecha recursos localizados para construir los textos de instrucciones.
 */
public class DirectionsHttpService {

    private static final String BASE = "https://maps.googleapis.com/maps/api/directions/json";
    private final Resources resources;

    public DirectionsHttpService(Context context) {
        this.resources = context.getApplicationContext().getResources();
    }

    private static String httpGet(String urlString) throws IOException {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(15000);
            connection.connect();
            int code = connection.getResponseCode();
            InputStream is = code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream();
            if (is == null) throw new IOException("No response body");
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                return sb.toString();
            }
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    public Route getBestTransitRoute(String apiKey, Location origin, Location destination) throws IOException {
        long departureEpoch = System.currentTimeMillis() / 1000L;
        return getTransitRouteAt(apiKey, origin, destination, departureEpoch);
    }

    public Route getTransitRouteAt(String apiKey, Location origin, Location destination, long departureEpoch) throws IOException {
        String originParam = origin.getLatitude() + "," + origin.getLongitude();
        String destParam = destination.getLatitude() + "," + destination.getLongitude();
        Uri uri = Uri.parse(BASE).buildUpon()
                .appendQueryParameter("origin", originParam)
                .appendQueryParameter("destination", destParam)
                .appendQueryParameter("mode", "transit")
                .appendQueryParameter("alternatives", "false")
                .appendQueryParameter("transit_mode", "bus")
                .appendQueryParameter("transit_routing_preference", "less_walking")
                .appendQueryParameter("region", "es")
                .appendQueryParameter("language", Locale.getDefault().getLanguage())
                .appendQueryParameter("departure_time", String.valueOf(departureEpoch))
                .appendQueryParameter("key", apiKey)
                .build();
        String body = httpGet(uri.toString());
        try {
            JSONObject root = new JSONObject(body);
            if (!"OK".equalsIgnoreCase(root.optString("status"))) {
                return null;
            }
            JSONArray routes = root.optJSONArray("routes");
            if (routes == null || routes.length() == 0) return null;
            JSONObject route0 = routes.getJSONObject(0);
            JSONArray legs = route0.optJSONArray("legs");
            if (legs == null || legs.length() == 0) return null;
            JSONObject leg0 = legs.getJSONObject(0);

            Route route = new Route();
            route.setOrigin(origin);
            route.setDestination(destination);

            List<RouteSegment> segments = new ArrayList<>();
            JSONArray steps = leg0.optJSONArray("steps");
            int totalDuration = 0;
            int totalDistance = 0;
            long cursorTimeMs = departureEpoch * 1000L;
            if (steps != null) {
                for (int i = 0; i < steps.length(); i++) {
                    JSONObject s = steps.getJSONObject(i);
                    String mode = s.optString("travel_mode", "");
                    JSONObject startLoc = s.optJSONObject("start_location");
                    JSONObject endLoc = s.optJSONObject("end_location");
                    JSONObject duration = s.optJSONObject("duration");
                    JSONObject distance = s.optJSONObject("distance");
                    String polyline = null;
                    JSONObject polyObj = s.optJSONObject("polyline");
                    if (polyObj != null) polyline = polyObj.optString("points", null);

                    int dur = duration != null ? duration.optInt("value", 0) / 60 : 0; // min
                    int dist = distance != null ? distance.optInt("value", 0) : 0; // m
                    totalDuration += dur;
                    totalDistance += dist;

                    RouteSegment seg = new RouteSegment();
                    seg.setDuration(dur);
                    seg.setDistance(dist);
                    long segStartMs = cursorTimeMs;
                    long segEndMs = cursorTimeMs + dur * 60L * 1000L;
                    if (startLoc != null) {
                        Location start = new Location(resources.getString(com.example.ourenbus2.R.string.start_point), "", startLoc.optDouble("lat", 0), startLoc.optDouble("lng", 0));
                        seg.setStartLocation(start);
                    }
                    if (endLoc != null) {
                        Location end = new Location(resources.getString(com.example.ourenbus2.R.string.end_point), "", endLoc.optDouble("lat", 0), endLoc.optDouble("lng", 0));
                        seg.setEndLocation(end);
                    }
                    if ("WALKING".equalsIgnoreCase(mode)) {
                        seg.setType(RouteSegment.SegmentType.WALKING);
                    } else if ("TRANSIT".equalsIgnoreCase(mode)) {
                        seg.setType(RouteSegment.SegmentType.BUS);
                    } else {
                        seg.setType(RouteSegment.SegmentType.OTHER);
                    }

                    if (seg.getType() == RouteSegment.SegmentType.BUS) {
                        JSONObject transit = s.optJSONObject("transit_details");
                        if (transit != null) {
                            JSONObject line = transit.optJSONObject("line");
                            String shortName = line != null ? line.optString("short_name", "") : "";
                            String lineName = line != null ? line.optString("name", resources.getString(com.example.ourenbus2.R.string.bus_default_name)) : resources.getString(com.example.ourenbus2.R.string.bus_default_name);
                            String lineColor = line != null ? line.optString("color", "") : "";
                            int numStops = transit.optInt("num_stops", 0);
                            String displayName = shortName.isEmpty() ? lineName : resources.getString(com.example.ourenbus2.R.string.line_prefix, shortName);
                            BusLine busLine = new BusLine(parseIntSafe(shortName), displayName, normalizeHexColor(lineColor, shortName));
                            seg.setBusLine(busLine);
                            JSONObject depStop = transit.optJSONObject("departure_stop");
                            JSONObject arrStop = transit.optJSONObject("arrival_stop");
                            JSONObject depTime = transit.optJSONObject("departure_time");
                            JSONObject arrTime = transit.optJSONObject("arrival_time");
                            if (depStop != null) {
                                BusStop bs = new BusStop(depStop.optString("name", resources.getString(com.example.ourenbus2.R.string.bus_stop)),
                                        startLoc != null ? startLoc.optDouble("lat", 0) : 0,
                                        startLoc != null ? startLoc.optDouble("lng", 0) : 0);
                                seg.setBusStop(bs);
                            }
                            if (arrStop != null) {
                                BusStop ns = new BusStop(arrStop.optString("name", resources.getString(com.example.ourenbus2.R.string.bus_stop)),
                                        endLoc != null ? endLoc.optDouble("lat", 0) : 0,
                                        endLoc != null ? endLoc.optDouble("lng", 0) : 0);
                                seg.setNextStop(ns);
                            }
                            if (depTime != null) {
                                long t = depTime.optLong("value", departureEpoch);
                                segStartMs = t * 1000L;
                            }
                            if (arrTime != null) {
                                long t = arrTime.optLong("value", departureEpoch);
                                segEndMs = t * 1000L;
                            }
                            String lineToken = shortName.isEmpty() ? lineName : resources.getString(com.example.ourenbus2.R.string.line_prefix, shortName);
                            String instr = resources.getString(com.example.ourenbus2.R.string.take_line_instruction, lineToken, numStops);
                            seg.setInstructions(instr);
                        } else {
                            seg.setInstructions(resources.getString(com.example.ourenbus2.R.string.bus_ride));
                        }
                    } else { // walking
                        seg.setInstructions(resources.getString(com.example.ourenbus2.R.string.walk_distance_m, dist));
                    }
                    // Guardar polyline codificada (se dibuja en el mapa)
                    if (polyline != null) seg.setPolylineEncoded(polyline);
                    // Insertar espera si el inicio del siguiente tramo es posterior al cursor
                    if (segStartMs > cursorTimeMs) {
                        int waitMin = (int) Math.max(0, Math.round((segStartMs - cursorTimeMs) / 60000.0));
                        if (waitMin > 0) {
                            RouteSegment wait = new RouteSegment();
                            wait.setType(RouteSegment.SegmentType.WAIT);
                            wait.setDuration(waitMin);
                            wait.setDistance(0);
                            wait.setStartTime(new Date(cursorTimeMs));
                            wait.setEndTime(new Date(segStartMs));
                            String stopName = seg.getStartLocation() != null ? seg.getStartLocation().getName() : resources.getString(com.example.ourenbus2.R.string.bus_stop);
                            java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
                            String at = fmt.format(new Date(segStartMs));
                            String instrWait = resources.getString(com.example.ourenbus2.R.string.wait_instruction, waitMin, stopName, at);
                            wait.setInstructions(instrWait);
                            segments.add(wait);
                            totalDuration += waitMin;
                        }
                    }
                    seg.setStartTime(new Date(segStartMs));
                    seg.setEndTime(new Date(segEndMs));
                    segments.add(seg);
                    cursorTimeMs = segEndMs;
                }
            }
            route.setSegments(segments);
            route.setEstimatedTimeInMinutes(totalDuration);
            route.setTotalDistance(totalDistance);
            route.setTotalDuration(totalDuration);
            return route;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Devuelve todas las rutas TRANSIT (solo bus) candidatas con alternatives=true para un horario.
     */
    public List<Route> getTransitRoutesAtCandidates(String apiKey, Location origin, Location destination, long departureEpoch) throws IOException {
        String originParam = origin.getLatitude() + "," + origin.getLongitude();
        String destParam = destination.getLatitude() + "," + destination.getLongitude();
        Uri uri = Uri.parse(BASE).buildUpon()
                .appendQueryParameter("origin", originParam)
                .appendQueryParameter("destination", destParam)
                .appendQueryParameter("mode", "transit")
                .appendQueryParameter("alternatives", "true")
                .appendQueryParameter("transit_mode", "bus")
                .appendQueryParameter("transit_routing_preference", "less_walking")
                .appendQueryParameter("region", "es")
                .appendQueryParameter("language", Locale.getDefault().getLanguage())
                .appendQueryParameter("departure_time", String.valueOf(departureEpoch))
                .appendQueryParameter("key", apiKey)
                .build();
        String body = httpGet(uri.toString());
        try {
            JSONObject root = new JSONObject(body);
            if (!"OK".equalsIgnoreCase(root.optString("status"))) {
                return null;
            }
            JSONArray routes = root.optJSONArray("routes");
            if (routes == null || routes.length() == 0) return null;
            List<Route> candidates = new ArrayList<>();
            for (int rIdx = 0; rIdx < routes.length(); rIdx++) {
                JSONObject routeObj = routes.getJSONObject(rIdx);
                JSONArray legs = routeObj.optJSONArray("legs");
                if (legs == null || legs.length() == 0) continue;
                JSONObject leg0 = legs.getJSONObject(0);

                Route route = new Route();
                route.setOrigin(origin);
                route.setDestination(destination);

                List<RouteSegment> segments = new ArrayList<>();
                JSONArray steps = leg0.optJSONArray("steps");
                int totalDuration = 0;
                int totalDistance = 0;
                long cursorTimeMs = departureEpoch * 1000L;
                if (steps != null) {
                    for (int i = 0; i < steps.length(); i++) {
                        JSONObject s = steps.getJSONObject(i);
                        String mode = s.optString("travel_mode", "");
                        JSONObject startLoc = s.optJSONObject("start_location");
                        JSONObject endLoc = s.optJSONObject("end_location");
                        JSONObject duration = s.optJSONObject("duration");
                        JSONObject distance = s.optJSONObject("distance");
                        String polyline = null;
                        JSONObject polyObj = s.optJSONObject("polyline");
                        if (polyObj != null) polyline = polyObj.optString("points", null);

                        int dur = duration != null ? duration.optInt("value", 0) / 60 : 0; // min
                        int dist = distance != null ? distance.optInt("value", 0) : 0; // m
                        totalDuration += dur;
                        totalDistance += dist;

                        RouteSegment seg = new RouteSegment();
                        seg.setDuration(dur);
                        seg.setDistance(dist);
                        long segStartMs = cursorTimeMs;
                        long segEndMs = cursorTimeMs + dur * 60L * 1000L;
                        if (startLoc != null) {
                            Location start = new Location(resources.getString(com.example.ourenbus2.R.string.start_point), "", startLoc.optDouble("lat", 0), startLoc.optDouble("lng", 0));
                            seg.setStartLocation(start);
                        }
                        if (endLoc != null) {
                            Location end = new Location(resources.getString(com.example.ourenbus2.R.string.end_point), "", endLoc.optDouble("lat", 0), endLoc.optDouble("lng", 0));
                            seg.setEndLocation(end);
                        }
                        if ("WALKING".equalsIgnoreCase(mode)) {
                            seg.setType(RouteSegment.SegmentType.WALKING);
                        } else if ("TRANSIT".equalsIgnoreCase(mode)) {
                            seg.setType(RouteSegment.SegmentType.BUS);
                        } else {
                            seg.setType(RouteSegment.SegmentType.OTHER);
                        }

                        if (seg.getType() == RouteSegment.SegmentType.BUS) {
                            JSONObject transit = s.optJSONObject("transit_details");
                            if (transit != null) {
                                JSONObject line = transit.optJSONObject("line");
                                String shortName = line != null ? line.optString("short_name", "") : "";
                                String lineName = line != null ? line.optString("name", resources.getString(com.example.ourenbus2.R.string.bus_default_name)) : resources.getString(com.example.ourenbus2.R.string.bus_default_name);
                                String lineColor = line != null ? line.optString("color", "") : "";
                                int numStops = transit.optInt("num_stops", 0);
                                String displayName = shortName.isEmpty() ? lineName : resources.getString(com.example.ourenbus2.R.string.line_prefix, shortName);
                                BusLine busLine = new BusLine(parseIntSafe(shortName), displayName, normalizeHexColor(lineColor, shortName));
                                seg.setBusLine(busLine);
                                JSONObject depStop = transit.optJSONObject("departure_stop");
                                JSONObject arrStop = transit.optJSONObject("arrival_stop");
                                JSONObject depTime = transit.optJSONObject("departure_time");
                                JSONObject arrTime = transit.optJSONObject("arrival_time");
                                if (depStop != null) {
                                    BusStop bs = new BusStop(depStop.optString("name", resources.getString(com.example.ourenbus2.R.string.bus_stop)),
                                            startLoc != null ? startLoc.optDouble("lat", 0) : 0,
                                            startLoc != null ? startLoc.optDouble("lng", 0) : 0);
                                    seg.setBusStop(bs);
                                }
                                if (arrStop != null) {
                                    BusStop ns = new BusStop(arrStop.optString("name", resources.getString(com.example.ourenbus2.R.string.bus_stop)),
                                            endLoc != null ? endLoc.optDouble("lat", 0) : 0,
                                            endLoc != null ? endLoc.optDouble("lng", 0) : 0);
                                    seg.setNextStop(ns);
                                }
                                if (depTime != null) {
                                    long t = depTime.optLong("value", departureEpoch);
                                    segStartMs = t * 1000L;
                                }
                                if (arrTime != null) {
                                    long t = arrTime.optLong("value", departureEpoch);
                                    segEndMs = t * 1000L;
                                }
                                String lineToken = shortName.isEmpty() ? lineName : resources.getString(com.example.ourenbus2.R.string.line_prefix, shortName);
                                String instr = resources.getString(com.example.ourenbus2.R.string.take_line_instruction, lineToken, numStops);
                                seg.setInstructions(instr);
                            } else {
                                seg.setInstructions(resources.getString(com.example.ourenbus2.R.string.bus_ride));
                            }
                        } else { // walking
                            seg.setInstructions(resources.getString(com.example.ourenbus2.R.string.walk_distance_m, dist));
                        }
                        // Guardar polyline codificada (se dibuja en el mapa)
                        if (polyline != null) seg.setPolylineEncoded(polyline);
                        // Insertar espera si el inicio del siguiente tramo es posterior al cursor
                        if (segStartMs > cursorTimeMs) {
                            int waitMin = (int) Math.max(0, Math.round((segStartMs - cursorTimeMs) / 60000.0));
                            if (waitMin > 0) {
                                RouteSegment wait = new RouteSegment();
                                wait.setType(RouteSegment.SegmentType.WAIT);
                                wait.setDuration(waitMin);
                                wait.setDistance(0);
                                wait.setStartTime(new Date(cursorTimeMs));
                                wait.setEndTime(new Date(segStartMs));
                                String stopName = seg.getStartLocation() != null ? seg.getStartLocation().getName() : resources.getString(com.example.ourenbus2.R.string.bus_stop);
                                java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
                                String at = fmt.format(new Date(segStartMs));
                                String instrWait = resources.getString(com.example.ourenbus2.R.string.wait_instruction, waitMin, stopName, at);
                                wait.setInstructions(instrWait);
                                segments.add(wait);
                                totalDuration += waitMin;
                            }
                        }
                        seg.setStartTime(new Date(segStartMs));
                        seg.setEndTime(new Date(segEndMs));
                        segments.add(seg);
                        cursorTimeMs = segEndMs;
                    }
                }
                route.setSegments(segments);
                route.setEstimatedTimeInMinutes(totalDuration);
                route.setTotalDistance(totalDistance);
                route.setTotalDuration(totalDuration);
                if (!segments.isEmpty()) {
                    candidates.add(route);
                }
            }
            return candidates;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Obtiene la mejor ruta a pie para comparar tiempos.
     */
    public Route getBestWalkingRoute(String apiKey, Location origin, Location destination) throws IOException {
        String originParam = origin.getLatitude() + "," + origin.getLongitude();
        String destParam = destination.getLatitude() + "," + destination.getLongitude();
        Uri uri = Uri.parse(BASE).buildUpon()
                .appendQueryParameter("origin", originParam)
                .appendQueryParameter("destination", destParam)
                .appendQueryParameter("mode", "walking")
                .appendQueryParameter("alternatives", "false")
                .appendQueryParameter("region", "es")
                .appendQueryParameter("language", Locale.getDefault().getLanguage())
                .appendQueryParameter("key", apiKey)
                .build();
        String body = httpGet(uri.toString());
        try {
            JSONObject root = new JSONObject(body);
            if (!"OK".equalsIgnoreCase(root.optString("status"))) return null;
            JSONArray routes = root.optJSONArray("routes");
            if (routes == null || routes.length() == 0) return null;
            JSONObject route0 = routes.getJSONObject(0);
            JSONArray legs = route0.optJSONArray("legs");
            if (legs == null || legs.length() == 0) return null;
            JSONObject leg0 = legs.getJSONObject(0);
            int dur = leg0.optJSONObject("duration") != null ? leg0.optJSONObject("duration").optInt("value", 0) / 60 : 0;
            int dist = leg0.optJSONObject("distance") != null ? leg0.optJSONObject("distance").optInt("value", 0) : 0;
            Route r = new Route();
            r.setOrigin(origin); r.setDestination(destination);
            r.setEstimatedTimeInMinutes(dur);
            r.setTotalDistance(dist);
            return r;
        } catch (Exception e) { return null; }
    }

    private int parseIntSafe(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return 0; }
    }

    private String normalizeHexColor(String color, String seed) {
        if (color == null || color.trim().isEmpty()) {
            // Generar color estable a partir de la línea (seed)
            int hash = seed != null ? seed.hashCode() : 0x336699;
            int r = 0x30 | (hash >> 16 & 0x7F);
            int g = 0x30 | (hash >> 8 & 0x7F);
            int b = 0x30 | (hash & 0x7F);
            return String.format("#%02X%02X%02X", r, g, b);
        }
        String c = color.trim();
        if (!c.startsWith("#")) {
            c = "#" + c;
        }
        // Asegurar formato #RRGGBB (algunos proveedores devuelven #AARRGGBB)
        if (c.length() == 9) { // #AARRGGBB -> quitar alpha
            c = "#" + c.substring(3);
        }
        return c;
    }
} 