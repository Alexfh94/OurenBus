package com.example.ourenbus2.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.example.ourenbus2.database.dao.FavoriteRouteDao;
import com.example.ourenbus2.database.dao.GtfsDao;
import com.example.ourenbus2.database.dao.UserDao;
import com.example.ourenbus2.database.entity.FavoriteRouteEntity;
import com.example.ourenbus2.database.entity.UserEntity;
import com.example.ourenbus2.database.entity.gtfs.GtfsRouteEntity;
import com.example.ourenbus2.database.entity.gtfs.GtfsStopEntity;
import com.example.ourenbus2.database.entity.gtfs.GtfsStopTimeEntity;
import com.example.ourenbus2.database.entity.gtfs.GtfsTripEntity;
import com.example.ourenbus2.database.entity.gtfs.GtfsCalendarEntity;
import com.example.ourenbus2.database.entity.gtfs.GtfsCalendarDateEntity;

/**
 * Clase principal de la base de datos de la aplicación.
 */
@Database(entities = {UserEntity.class, FavoriteRouteEntity.class,
        GtfsStopEntity.class, GtfsRouteEntity.class, GtfsTripEntity.class, GtfsStopTimeEntity.class,
        GtfsCalendarEntity.class, GtfsCalendarDateEntity.class}, version = 4, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {

    private static final String DATABASE_NAME = "ourenbus_db";
    private static volatile AppDatabase instance;

    /**
     * Obtiene el DAO para las rutas favoritas.
     *
     * @return DAO de rutas favoritas
     */
    public abstract FavoriteRouteDao favoriteRouteDao();

    /**
     * Obtiene el DAO para los usuarios.
     *
     * @return DAO de usuarios
     */
    public abstract UserDao userDao();

    public abstract GtfsDao gtfsDao();

    /**
     * Obtiene una instancia de la base de datos.
     *
     * @param context Contexto de la aplicación
     * @return Instancia de la base de datos
     */
    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                    context.getApplicationContext(),
                    AppDatabase.class,
                    DATABASE_NAME)
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
} 