package com.example.ourenbus2.database.entity.gtfs;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "gtfs_stops")
public class GtfsStopEntity {
    @PrimaryKey
    @NonNull
    public String stopId;
    public String name;
    public double lat;
    public double lon;
}


