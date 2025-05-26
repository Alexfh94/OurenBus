package com.example.ourenbus2.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Objects;

/**
 * Clase que representa una ubicación geográfica
 */
@Entity(tableName = "locations")
public class Location {

    @PrimaryKey(autoGenerate = true)
    private long id;
    
    private String name;
    private String address;
    private double latitude;
    private double longitude;
    
    public Location() {
        // Constructor vacío requerido por Room
    }
    
    public Location(String name, double latitude, double longitude) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = name; // Por defecto la dirección es igual al nombre
    }
    
    public Location(String name, String address, double latitude, double longitude) {
        this.name = name;
        this.address = address;
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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
    
    /**
     * Obtiene la descripción de la ubicación (alias para getAddress)
     */
    public String getDescription() {
        return address;
    }
    
    /**
     * Establece la descripción de la ubicación (alias para setAddress)
     */
    public void setDescription(String description) {
        this.address = description;
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
        Location location = (Location) o;
        return Double.compare(location.latitude, latitude) == 0 &&
               Double.compare(location.longitude, longitude) == 0 &&
               Objects.equals(name, location.name);
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