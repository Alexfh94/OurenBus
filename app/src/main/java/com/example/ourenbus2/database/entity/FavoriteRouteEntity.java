package com.example.ourenbus2.database.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.example.ourenbus2.model.Location;

/**
 * Entidad que representa una ruta favorita en la base de datos.
 */
@Entity(tableName = "favorite_routes")
public class FavoriteRouteEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;
    private String name;
    private Location origin;
    private Location destination;
    private String routeData;  // Datos de la ruta en formato JSON
    private long timestamp;    // Fecha de guardado
    private String userEmail;  // Email del usuario propietario

    /**
     * Constructor por defecto.
     */
    public FavoriteRouteEntity() {
    }

    /**
     * Constructor con parámetros.
     *
     * @param name        Nombre de la ruta
     * @param origin      Ubicación de origen
     * @param destination Ubicación de destino
     * @param routeData   Datos de la ruta en formato JSON
     * @param timestamp   Fecha de guardado
     */
    @Ignore
    public FavoriteRouteEntity(String name, Location origin, Location destination, String routeData, long timestamp, String userEmail) {
        this.name = name;
        this.origin = origin;
        this.destination = destination;
        this.routeData = routeData;
        this.timestamp = timestamp;
        this.userEmail = userEmail;
    }

    /**
     * Obtiene el identificador único de la ruta favorita.
     *
     * @return Identificador de la ruta favorita
     */
    public long getId() {
        return id;
    }

    /**
     * Establece el identificador único de la ruta favorita.
     *
     * @param id Identificador de la ruta favorita
     */
    public void setId(long id) {
        this.id = id;
    }

    /**
     * Obtiene el nombre de la ruta.
     *
     * @return Nombre de la ruta
     */
    public String getName() {
        return name;
    }

    /**
     * Establece el nombre de la ruta.
     *
     * @param name Nombre de la ruta
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Obtiene la ubicación de origen.
     *
     * @return Ubicación de origen
     */
    public Location getOrigin() {
        return origin;
    }

    /**
     * Establece la ubicación de origen.
     *
     * @param origin Ubicación de origen
     */
    public void setOrigin(Location origin) {
        this.origin = origin;
    }

    /**
     * Obtiene la ubicación de destino.
     *
     * @return Ubicación de destino
     */
    public Location getDestination() {
        return destination;
    }

    /**
     * Establece la ubicación de destino.
     *
     * @param destination Ubicación de destino
     */
    public void setDestination(Location destination) {
        this.destination = destination;
    }

    /**
     * Obtiene los datos de la ruta en formato JSON.
     *
     * @return Datos de la ruta
     */
    public String getRouteData() {
        return routeData;
    }

    /**
     * Establece los datos de la ruta en formato JSON.
     *
     * @param routeData Datos de la ruta
     */
    public void setRouteData(String routeData) {
        this.routeData = routeData;
    }

    /**
     * Obtiene la fecha de guardado.
     *
     * @return Fecha de guardado
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Establece la fecha de guardado.
     *
     * @param timestamp Fecha de guardado
     */
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getUserEmail() { return userEmail; }

    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
} 