package com.example.ourenbus2.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.Relation;

import java.util.Date;
import java.util.Objects;

/**
 * Clase que representa un segmento de una ruta (a pie, en bus, etc.)
 */
@Entity(tableName = "route_segments")
public class RouteSegment {

    public enum SegmentType {
        WALKING,
        BUS,
        WAIT,
        BIKE,
        OTHER
    }

    @PrimaryKey(autoGenerate = true)
    private long id;
    
    private long routeId;
    private SegmentType type;
    private int distance;  // En metros
    private int duration;  // En minutos
    private Date startTime;
    private Date endTime;
    private String instructions;

    // Polilínea codificada (cuando viene de Directions)
    @Ignore
    private String polylineEncoded;
    
    // Relaciones con otras entidades
    @Ignore
    private Location startLocation;
    
    @Ignore
    private Location endLocation;
    
    @Ignore
    private BusLine busLine;
    
    @Ignore
    private BusStop busStop;
    
    @Ignore
    private BusStop nextStop;
    
    public RouteSegment() {
        // Constructor vacío requerido por Room
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getRouteId() {
        return routeId;
    }

    public void setRouteId(long routeId) {
        this.routeId = routeId;
    }

    public SegmentType getType() {
        return type;
    }

    public void setType(SegmentType type) {
        this.type = type;
    }

    public int getDistance() {
        return distance;
    }

    public void setDistance(int distance) {
        this.distance = distance;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public int getTimeInMinutes() {
        return duration;
    }

    public void setTimeInMinutes(int minutes) {
        this.duration = minutes;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public String getPolylineEncoded() {
        return polylineEncoded;
    }

    public void setPolylineEncoded(String polylineEncoded) {
        this.polylineEncoded = polylineEncoded;
    }

    public Location getStartLocation() {
        return startLocation;
    }

    public void setStartLocation(Location startLocation) {
        this.startLocation = startLocation;
    }

    public Location getEndLocation() {
        return endLocation;
    }

    public void setEndLocation(Location endLocation) {
        this.endLocation = endLocation;
    }

    public BusLine getBusLine() {
        return busLine;
    }

    public void setBusLine(BusLine busLine) {
        this.busLine = busLine;
    }

    public BusStop getBusStop() {
        return busStop;
    }

    public void setBusStop(BusStop busStop) {
        this.busStop = busStop;
    }

    public BusStop getNextStop() {
        return nextStop;
    }

    public void setNextStop(BusStop nextStop) {
        this.nextStop = nextStop;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RouteSegment segment = (RouteSegment) o;
        return id == segment.id &&
               routeId == segment.routeId &&
               type == segment.type &&
               Objects.equals(startTime, segment.startTime) &&
               Objects.equals(endTime, segment.endTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, routeId, type, startTime, endTime);
    }

    @NonNull
    @Override
    public String toString() {
        String typeStr = type != null ? type.name() : "UNKNOWN";
        return typeStr + ": " + (startLocation != null ? startLocation.getName() : "?") + 
               " → " + (endLocation != null ? endLocation.getName() : "?");
    }
} 