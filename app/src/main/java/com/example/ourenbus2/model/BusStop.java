package com.example.ourenbus2.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Objects;

/**
 * Clase que representa una parada de autobús
 */
@Entity(tableName = "bus_stops")
public class BusStop {

    @PrimaryKey(autoGenerate = true)
    private long id;
    
    private String name;
    private double latitude;
    private double longitude;
    
    public BusStop() {
        // Constructor vacío requerido por Room
    }
    
    public BusStop(String name, double latitude, double longitude) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
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

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BusStop busStop = (BusStop) o;
        return Double.compare(busStop.latitude, latitude) == 0 &&
               Double.compare(busStop.longitude, longitude) == 0 &&
               Objects.equals(name, busStop.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, latitude, longitude);
    }

    @NonNull
    @Override
    public String toString() {
        return name + " (" + latitude + ", " + longitude + ")";
    }
} 