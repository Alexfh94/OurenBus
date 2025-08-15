package com.example.ourenbus2.service;

import android.net.Uri;

import com.example.ourenbus2.model.Location;

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
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Servicio HTTP ligero para integrar con Google Places (Autocomplete, Details y Find Place).
 * Se usa en UI para sugerencias de lugares y resolver texto a coordenadas.
 */
public class PlacesHttpService {

    private static final String BASE = "https://maps.googleapis.com/maps/api/place";

    public static class Prediction {
        public final String primaryText;
        public final String secondaryText;
        public final String placeId;
        public Prediction(String primaryText, String secondaryText, String placeId) {
            this.primaryText = primaryText;
            this.secondaryText = secondaryText;
            this.placeId = placeId;
        }
        @Override
        public String toString() {
            return secondaryText == null || secondaryText.isEmpty()
                    ? primaryText
                    : primaryText + " – " + secondaryText;
        }
    }

    public static String newSessionToken() {
        return UUID.randomUUID().toString();
    }

    private static String httpGet(String urlString) throws IOException {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(8000);
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

    public List<Prediction> autocomplete(String apiKey, String input, double lat, double lng, String sessionToken) throws IOException {
        if (input == null || input.trim().isEmpty()) return new ArrayList<>();
        Uri uri = Uri.parse(BASE + "/autocomplete/json").buildUpon()
                .appendQueryParameter("input", input)
                .appendQueryParameter("key", apiKey)
                .appendQueryParameter("language", Locale.getDefault().getLanguage())
                .appendQueryParameter("components", "country:es")
                .appendQueryParameter("region", "es")
                .appendQueryParameter("location", lat + "," + lng)
                .appendQueryParameter("radius", "50000")
                .appendQueryParameter("sessiontoken", sessionToken)
                .build();

        String body = httpGet(uri.toString());
        try {
            JSONObject root = new JSONObject(body);
            String status = root.optString("status", "");
            JSONArray preds = root.optJSONArray("predictions");
            List<Prediction> result = new ArrayList<>();
            if ("OK".equalsIgnoreCase(status) && preds != null) {
                for (int i = 0; i < preds.length(); i++) {
                    JSONObject p = preds.getJSONObject(i);
                    String placeId = p.optString("place_id");
                    String description = p.optString("description");
                    String primary = description;
                    String secondary = "";
                    JSONArray terms = p.optJSONArray("terms");
                    if (terms != null && terms.length() > 1) {
                        primary = terms.getJSONObject(0).optString("value");
                        StringBuilder sb = new StringBuilder();
                        for (int t = 1; t < terms.length(); t++) {
                            if (sb.length() > 0) sb.append(", ");
                            sb.append(terms.getJSONObject(t).optString("value"));
                        }
                        secondary = sb.toString();
                    }
                    result.add(new Prediction(primary, secondary, placeId));
                }
            }
            return result;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public Location fetchPlaceDetails(String apiKey, String placeId) throws IOException {
        Uri uri = Uri.parse(BASE + "/details/json").buildUpon()
                .appendQueryParameter("place_id", placeId)
                .appendQueryParameter("key", apiKey)
                .appendQueryParameter("language", Locale.getDefault().getLanguage())
                .appendQueryParameter("fields", "geometry/location,name,formatted_address")
                .build();
        String body = httpGet(uri.toString());
        try {
            JSONObject root = new JSONObject(body);
            JSONObject result = root.optJSONObject("result");
            if (result == null) return null;
            JSONObject geometry = result.optJSONObject("geometry");
            JSONObject loc = geometry != null ? geometry.optJSONObject("location") : null;
            double lat = loc != null ? loc.optDouble("lat", 0) : 0;
            double lng = loc != null ? loc.optDouble("lng", 0) : 0;
            String name = result.optString("name", "");
            String address = result.optString("formatted_address", "");
            if (lat == 0 && lng == 0) return null;
            return new Location(name, address, lat, lng);
        } catch (Exception e) {
            return null;
        }
    }

    public Location findPlaceFromText(String apiKey, String query, double lat, double lng) throws IOException {
        if (query == null || query.trim().isEmpty()) return null;
        Uri uri = Uri.parse(BASE + "/findplacefromtext/json").buildUpon()
                .appendQueryParameter("input", query)
                .appendQueryParameter("inputtype", "textquery")
                .appendQueryParameter("language", Locale.getDefault().getLanguage())
                .appendQueryParameter("fields", "geometry/location,name,formatted_address,place_id")
                .appendQueryParameter("locationbias", "circle:50000@" + lat + "," + lng)
                .appendQueryParameter("key", apiKey)
                .build();
        String body = httpGet(uri.toString());
        try {
            JSONObject root = new JSONObject(body);
            JSONArray candidates = root.optJSONArray("candidates");
            if (candidates == null || candidates.length() == 0) return null;
            JSONObject item = candidates.getJSONObject(0);
            JSONObject geometry = item.optJSONObject("geometry");
            JSONObject loc = geometry != null ? geometry.optJSONObject("location") : null;
            double plat = loc != null ? loc.optDouble("lat", 0) : 0;
            double plng = loc != null ? loc.optDouble("lng", 0) : 0;
            String name = item.optString("name", query);
            String address = item.optString("formatted_address", query);
            if (plat == 0 && plng == 0) return null;
            return new Location(name, address, plat, plng);
        } catch (Exception e) {
            return null;
        }
    }
} 