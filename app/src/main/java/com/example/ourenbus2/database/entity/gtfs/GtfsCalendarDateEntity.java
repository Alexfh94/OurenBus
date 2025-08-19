package com.example.ourenbus2.database.entity.gtfs;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "gtfs_calendar_dates", indices = {@Index("serviceId")})
public class GtfsCalendarDateEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String serviceId;
    public int date; // yyyymmdd
    public int exceptionType; // 1=add service, 2=remove service
}


