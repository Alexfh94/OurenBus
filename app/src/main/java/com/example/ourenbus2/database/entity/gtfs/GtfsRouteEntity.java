package com.example.ourenbus2.database.entity.gtfs;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "gtfs_routes")
public class GtfsRouteEntity {
    @PrimaryKey
    @NonNull
    public String routeId;
    public String shortName;
    public String longName;
    public String color;
}


