package com.example.ourenbus2.util.gtfs;

import android.content.Context;
import android.content.res.AssetManager;

import com.example.ourenbus2.database.AppDatabase;
import com.example.ourenbus2.database.dao.GtfsDao;
import com.example.ourenbus2.database.entity.gtfs.GtfsRouteEntity;
import com.example.ourenbus2.database.entity.gtfs.GtfsStopEntity;
import com.example.ourenbus2.database.entity.gtfs.GtfsStopTimeEntity;
import com.example.ourenbus2.database.entity.gtfs.GtfsTripEntity;
import com.example.ourenbus2.database.entity.gtfs.GtfsCalendarEntity;
import com.example.ourenbus2.database.entity.gtfs.GtfsCalendarDateEntity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Carga un feed GTFS (stops, routes, trips, stop_times) desde assets/ourense_gtfs.zip si la BD está vacía.
 * El feed debe incluir los ficheros estándar: stops.txt, routes.txt, trips.txt, stop_times.txt
 */
public class GtfsImporter {

    public static void importIfEmpty(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        GtfsDao dao = db.gtfsDao();
        // Si ya hay paradas, asumimos importado
        if (dao.getAllStops() != null && !dao.getAllStops().isEmpty()) return;
        try {
            importFromAssetsZip(context, "ourense_gtfs.zip", dao);
        } catch (IOException ignored) {
            // Si falla, dejamos las tablas vacías y el enrutado local no se activará
        }
    }

    private static void importFromAssetsZip(Context context, String zipName, GtfsDao dao) throws IOException {
        AssetManager assets = context.getAssets();
        try (InputStream is = assets.open(zipName); ZipInputStream zis = new ZipInputStream(is)) {
            Map<String, List<String[]>> files = new HashMap<>();
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                if (!name.endsWith(".txt")) continue;
                List<String[]> rows = new ArrayList<>();
                BufferedReader br = new BufferedReader(new InputStreamReader(zis, StandardCharsets.UTF_8));
                String header = br.readLine();
                if (header == null) continue;
                String[] headers = splitCsv(header);
                String line;
                while ((line = br.readLine()) != null) {
                    String[] cols = splitCsv(line);
                    // Normalizar tamaño a headers
                    if (cols.length < headers.length) {
                        String[] fixed = new String[headers.length];
                        System.arraycopy(cols, 0, fixed, 0, cols.length);
                        cols = fixed;
                    }
                    rows.add(mapByHeader(headers, cols));
                }
                files.put(name.substring(name.lastIndexOf('/') + 1).toLowerCase(), rows);
            }
            // Insertar en BD
            List<String[]> stops = files.get("stops.txt");
            if (stops != null) {
                List<GtfsStopEntity> list = new ArrayList<>();
                for (String[] r : stops) {
                    GtfsStopEntity e = new GtfsStopEntity();
                    e.stopId = val(r, "stop_id");
                    e.name = val(r, "stop_name");
                    e.lat = parseDouble(val(r, "stop_lat"));
                    e.lon = parseDouble(val(r, "stop_lon"));
                    list.add(e);
                }
                dao.insertStops(list);
            }
            List<String[]> routes = files.get("routes.txt");
            if (routes != null) {
                List<GtfsRouteEntity> list = new ArrayList<>();
                for (String[] r : routes) {
                    GtfsRouteEntity e = new GtfsRouteEntity();
                    e.routeId = val(r, "route_id");
                    e.shortName = val(r, "route_short_name");
                    e.longName = val(r, "route_long_name");
                    e.color = val(r, "route_color");
                    list.add(e);
                }
                dao.insertRoutes(list);
            }
            List<String[]> trips = files.get("trips.txt");
            if (trips != null) {
                List<GtfsTripEntity> list = new ArrayList<>();
                for (String[] r : trips) {
                    GtfsTripEntity e = new GtfsTripEntity();
                    e.tripId = val(r, "trip_id");
                    e.routeId = val(r, "route_id");
                    e.serviceId = val(r, "service_id");
                    e.tripHeadsign = val(r, "trip_headsign");
                    list.add(e);
                }
                dao.insertTrips(list);
            }
            List<String[]> stopTimes = files.get("stop_times.txt");
            if (stopTimes != null) {
                List<GtfsStopTimeEntity> list = new ArrayList<>();
                for (String[] r : stopTimes) {
                    GtfsStopTimeEntity e = new GtfsStopTimeEntity();
                    e.tripId = val(r, "trip_id");
                    e.stopId = val(r, "stop_id");
                    e.arrivalSeconds = parseHmsToSeconds(val(r, "arrival_time"));
                    e.departureSeconds = parseHmsToSeconds(val(r, "departure_time"));
                    e.stopSequence = parseInt(val(r, "stop_sequence"));
                    list.add(e);
                }
                dao.insertStopTimes(list);
            }
            List<String[]> calendars = files.get("calendar.txt");
            if (calendars != null) {
                List<GtfsCalendarEntity> list = new ArrayList<>();
                for (String[] r : calendars) {
                    GtfsCalendarEntity e = new GtfsCalendarEntity();
                    e.serviceId = val(r, "service_id");
                    e.monday = parseInt(val(r, "monday"));
                    e.tuesday = parseInt(val(r, "tuesday"));
                    e.wednesday = parseInt(val(r, "wednesday"));
                    e.thursday = parseInt(val(r, "thursday"));
                    e.friday = parseInt(val(r, "friday"));
                    e.saturday = parseInt(val(r, "saturday"));
                    e.sunday = parseInt(val(r, "sunday"));
                    e.startDate = parseInt(val(r, "start_date"));
                    e.endDate = parseInt(val(r, "end_date"));
                    list.add(e);
                }
                dao.insertCalendars(list);
            }
            List<String[]> calendarDates = files.get("calendar_dates.txt");
            if (calendarDates != null) {
                List<GtfsCalendarDateEntity> list = new ArrayList<>();
                for (String[] r : calendarDates) {
                    GtfsCalendarDateEntity e = new GtfsCalendarDateEntity();
                    e.serviceId = val(r, "service_id");
                    e.date = parseInt(val(r, "date"));
                    e.exceptionType = parseInt(val(r, "exception_type"));
                    list.add(e);
                }
                dao.insertCalendarDates(list);
            }
        }
    }

    private static String[] mapByHeader(String[] headers, String[] cols) {
        // Representamos cada fila como array de pares key=value compacto: [k0,v0,k1,v1,...]
        String[] mapped = new String[headers.length * 2];
        for (int i = 0; i < headers.length; i++) {
            mapped[i * 2] = headers[i];
            mapped[i * 2 + 1] = i < cols.length ? cols[i] : null;
        }
        return mapped;
    }

    private static String val(String[] mapped, String key) {
        for (int i = 0; i < mapped.length; i += 2) {
            if (key.equals(mapped[i])) return mapped[i + 1];
        }
        return null;
    }

    private static String[] splitCsv(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                out.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        out.add(sb.toString());
        return out.toArray(new String[0]);
    }

    private static int parseHmsToSeconds(String hms) {
        if (hms == null || hms.isEmpty()) return 0;
        String[] parts = hms.split(":");
        if (parts.length < 2) return 0;
        int h = parseInt(parts[0]);
        int m = parseInt(parts[1]);
        int s = parts.length > 2 ? parseInt(parts[2]) : 0;
        return h * 3600 + m * 60 + s;
    }

    private static int parseInt(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return 0; }
    }

    private static double parseDouble(String s) {
        try { return Double.parseDouble(s); } catch (Exception e) { return 0; }
    }
}


