package com.example.ourenbus2.database;

import androidx.room.TypeConverter;

import com.example.ourenbus2.model.Location;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Clase para convertir tipos complejos a tipos que Room pueda almacenar.
 */
public class Converters {

    private static final Gson gson = new Gson();

    /**
     * Convierte una lista de ubicaciones a JSON para almacenar en la base de datos.
     *
     * @param locations Lista de ubicaciones
     * @return Cadena JSON
     */
    @TypeConverter
    public static String fromLocationList(List<Location> locations) {
        if (locations == null) {
            return null;
        }
        return gson.toJson(locations);
    }

    /**
     * Convierte una cadena JSON a una lista de ubicaciones.
     *
     * @param locationsJson Cadena JSON
     * @return Lista de ubicaciones
     */
    @TypeConverter
    public static List<Location> toLocationList(String locationsJson) {
        if (locationsJson == null) {
            return new ArrayList<>();
        }
        Type type = new TypeToken<List<Location>>() {}.getType();
        return gson.fromJson(locationsJson, type);
    }

    /**
     * Convierte una ubicación a JSON para almacenar en la base de datos.
     *
     * @param location Ubicación
     * @return Cadena JSON
     */
    @TypeConverter
    public static String fromLocation(Location location) {
        if (location == null) {
            return null;
        }
        return gson.toJson(location);
    }

    /**
     * Convierte una cadena JSON a una ubicación.
     *
     * @param locationJson Cadena JSON
     * @return Ubicación
     */
    @TypeConverter
    public static Location toLocation(String locationJson) {
        if (locationJson == null) {
            return null;
        }
        return gson.fromJson(locationJson, Location.class);
    }

    /**
     * Convierte una lista de enteros a JSON para almacenar en la base de datos.
     *
     * @param list Lista de enteros
     * @return Cadena JSON
     */
    @TypeConverter
    public static String fromIntList(List<Integer> list) {
        if (list == null) {
            return null;
        }
        return gson.toJson(list);
    }

    /**
     * Convierte una cadena JSON a una lista de enteros.
     *
     * @param listJson Cadena JSON
     * @return Lista de enteros
     */
    @TypeConverter
    public static List<Integer> toIntList(String listJson) {
        if (listJson == null) {
            return new ArrayList<>();
        }
        Type type = new TypeToken<List<Integer>>() {}.getType();
        return gson.fromJson(listJson, type);
    }
} 