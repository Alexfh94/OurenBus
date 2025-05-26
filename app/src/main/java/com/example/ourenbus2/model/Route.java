package com.example.ourenbus2.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * Clase que representa una ruta completa entre un origen y un destino
 */
@Entity(tableName = "routes")
public class Route {

    @PrimaryKey(autoGenerate = true)
    private long id;
    
    private String name;
    private int totalDistance;  // En metros
    private int totalDuration;  // En minutos
    private Date createdDate;
    private Date savedDate;
    private boolean isFavorite;
    
    // Relaciones con otras entidades
    @Ignore
    private Location origin;
    
    @Ignore
    private Location destination;
    
    @Ignore
    private List<RouteSegment> segments;
    
    public Route() {
        // Constructor vacío requerido por Room
        segments = new ArrayList<>();
        createdDate = new Date();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getTotalDistance() {
        return totalDistance;
    }

    public void setTotalDistance(int totalDistance) {
        this.totalDistance = totalDistance;
    }

    public int getTotalDuration() {
        return totalDuration;
    }

    public void setTotalDuration(int totalDuration) {
        this.totalDuration = totalDuration;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public Date getSavedDate() {
        return savedDate;
    }

    public void setSavedDate(Date savedDate) {
        this.savedDate = savedDate;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }

    public Location getOrigin() {
        return origin;
    }

    public void setOrigin(Location origin) {
        this.origin = origin;
    }

    public Location getDestination() {
        return destination;
    }

    public void setDestination(Location destination) {
        this.destination = destination;
    }

    public List<RouteSegment> getSegments() {
        return segments;
    }

    public void setSegments(List<RouteSegment> segments) {
        this.segments = segments;
    }
    
    /**
     * Calcula la distancia total de la ruta sumando las distancias de todos los segmentos
     */
    public void calculateTotalDistance() {
        int total = 0;
        if (segments != null) {
            for (RouteSegment segment : segments) {
                total += segment.getDistance();
            }
        }
        this.totalDistance = total;
    }
    
    /**
     * Calcula la duración total de la ruta sumando las duraciones de todos los segmentos
     */
    public void calculateTotalDuration() {
        int total = 0;
        if (segments != null) {
            for (RouteSegment segment : segments) {
                total += segment.getDuration();
            }
        }
        this.totalDuration = total;
    }
    
    /**
     * Verifica si la ruta es válida (tiene origen, destino y al menos un segmento)
     */
    public boolean isValid() {
        return origin != null && destination != null && segments != null && !segments.isEmpty();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Route route = (Route) o;
        return id == route.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @NonNull
    @Override
    public String toString() {
        String originName = origin != null ? origin.getName() : "?";
        String destName = destination != null ? destination.getName() : "?";
        return originName + " → " + destName + " (" + totalDistance + "m, " + totalDuration + "min)";
    }

    public int getEstimatedTimeInMinutes() {
        return totalDuration;
    }

    public void setEstimatedTimeInMinutes(int minutes) {
        this.totalDuration = minutes;
    }
} 