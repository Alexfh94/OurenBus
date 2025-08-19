package com.example.ourenbus2.database.entity.gtfs;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "gtfs_calendar")
public class GtfsCalendarEntity {
    @PrimaryKey
    @NonNull
    public String serviceId;
    public int monday;
    public int tuesday;
    public int wednesday;
    public int thursday;
    public int friday;
    public int saturday;
    public int sunday;
    public int startDate; // yyyymmdd
    public int endDate;   // yyyymmdd
}


