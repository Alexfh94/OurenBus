package com.example.ourenbus2.database.entity.gtfs;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "gtfs_trips")
public class GtfsTripEntity {
    @PrimaryKey
    @NonNull
    public String tripId;
    public String routeId;
    public String serviceId;
    public String tripHeadsign;
}


