package com.example.ourenbus2.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.ourenbus2.database.entity.gtfs.GtfsRouteEntity;
import com.example.ourenbus2.database.entity.gtfs.GtfsStopEntity;
import com.example.ourenbus2.database.entity.gtfs.GtfsStopTimeEntity;
import com.example.ourenbus2.database.entity.gtfs.GtfsTripEntity;
import com.example.ourenbus2.database.entity.gtfs.GtfsCalendarEntity;
import com.example.ourenbus2.database.entity.gtfs.GtfsCalendarDateEntity;

import java.util.List;

@Dao
public interface GtfsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertStops(List<GtfsStopEntity> stops);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertRoutes(List<GtfsRouteEntity> routes);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertTrips(List<GtfsTripEntity> trips);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertStopTimes(List<GtfsStopTimeEntity> stopTimes);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertCalendars(List<GtfsCalendarEntity> calendars);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertCalendarDates(List<GtfsCalendarDateEntity> calendarDates);

    @Query("SELECT * FROM gtfs_stops")
    List<GtfsStopEntity> getAllStops();

    @Query("SELECT * FROM gtfs_routes")
    List<GtfsRouteEntity> getAllRoutes();

    @Query("SELECT * FROM gtfs_trips WHERE routeId = :routeId")
    List<GtfsTripEntity> getTripsByRoute(String routeId);

    @Query("SELECT * FROM gtfs_stop_times WHERE tripId = :tripId ORDER BY stopSequence ASC")
    List<GtfsStopTimeEntity> getStopTimesByTrip(String tripId);

    @Query("SELECT DISTINCT tripId FROM gtfs_stop_times WHERE stopId = :stopId")
    List<String> getTripIdsByStop(String stopId);

    @Query("SELECT * FROM gtfs_stops WHERE stopId = :stopId LIMIT 1")
    GtfsStopEntity getStopById(String stopId);

    @Query("SELECT * FROM gtfs_trips WHERE tripId = :tripId LIMIT 1")
    GtfsTripEntity getTripById(String tripId);

    @Query("SELECT * FROM gtfs_routes WHERE routeId = :routeId LIMIT 1")
    GtfsRouteEntity getRouteById(String routeId);

    @Query("SELECT * FROM gtfs_calendar WHERE serviceId = :serviceId LIMIT 1")
    GtfsCalendarEntity getCalendarByService(String serviceId);

    @Query("SELECT * FROM gtfs_calendar_dates WHERE serviceId = :serviceId")
    List<GtfsCalendarDateEntity> getCalendarDatesByService(String serviceId);
}


