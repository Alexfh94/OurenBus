package com.example.ourenbus2.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Objects;

/**
 * Clase que representa una línea de autobús
 */
@Entity(tableName = "bus_lines")
public class BusLine {

    @PrimaryKey
    private int lineNumber;
    
    private String name;
    private String color;
    
    public BusLine() {
        // Constructor vacío requerido por Room
    }
    
    public BusLine(int lineNumber, String name, String color) {
        this.lineNumber = lineNumber;
        this.name = name;
        this.color = color;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BusLine busLine = (BusLine) o;
        return lineNumber == busLine.lineNumber;
    }

    @Override
    public int hashCode() {
        return Objects.hash(lineNumber);
    }

    @NonNull
    @Override
    public String toString() {
        return name + " (Línea " + lineNumber + ")";
    }
} 