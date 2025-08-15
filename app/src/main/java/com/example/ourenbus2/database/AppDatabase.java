package com.example.ourenbus2.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.example.ourenbus2.database.dao.FavoriteRouteDao;
import com.example.ourenbus2.database.dao.UserDao;
import com.example.ourenbus2.database.entity.FavoriteRouteEntity;
import com.example.ourenbus2.database.entity.UserEntity;

/**
 * Clase principal de la base de datos de la aplicación.
 */
@Database(entities = {UserEntity.class, FavoriteRouteEntity.class}, version = 2, exportSchema = false)
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