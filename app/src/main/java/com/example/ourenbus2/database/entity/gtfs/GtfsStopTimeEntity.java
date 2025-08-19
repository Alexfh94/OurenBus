package com.example.ourenbus2.database.entity.gtfs;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "gtfs_stop_times", indices = {@Index("tripId"), @Index("stopId")})
public class GtfsStopTimeEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String tripId;
    public String stopId;
    public int arrivalSeconds;  // segundos desde medianoche
    public int departureSeconds; // segundos desde medianoche
    public int stopSequence;
}


