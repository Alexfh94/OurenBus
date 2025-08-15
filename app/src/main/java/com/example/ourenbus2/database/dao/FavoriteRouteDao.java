package com.example.ourenbus2.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.ourenbus2.database.entity.FavoriteRouteEntity;

import java.util.List;

/**
 * DAO (Data Access Object) para operaciones con rutas favoritas en la base de datos.
 */
@Dao
public interface FavoriteRouteDao {

    /**
     * Inserta una nueva ruta favorita en la base de datos.
     *
     * @param favoriteRoute Ruta favorita a insertar
     * @return ID de la ruta favorita insertada
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(FavoriteRouteEntity favoriteRoute);

    /**
     * Actualiza una ruta favorita existente en la base de datos.
     *
     * @param favoriteRoute Ruta favorita a actualizar
     */
    @Update
    void update(FavoriteRouteEntity favoriteRoute);

    /**
     * Elimina una ruta favorita de la base de datos.
     *
     * @param favoriteRoute Ruta favorita a eliminar
     */
    @Delete
    void delete(FavoriteRouteEntity favoriteRoute);

    /**
     * Obtiene una ruta favorita por su ID.
     *
     * @param id ID de la ruta favorita
     * @return Ruta favorita con el ID especificado
     */
    @Query("SELECT * FROM favorite_routes WHERE id = :id")
    LiveData<FavoriteRouteEntity> getFavoriteRouteById(long id);

    /**
     * Obtiene todas las rutas favoritas de la base de datos.
     *
     * @return Lista de todas las rutas favoritas
     */
    @Query("SELECT * FROM favorite_routes WHERE userEmail = :email ORDER BY timestamp DESC")
    LiveData<List<FavoriteRouteEntity>> getAllFavoriteRoutes(String email);

    /**
     * Elimina una ruta favorita por su ID.
     *
     * @param id ID de la ruta favorita a eliminar
     */
    @Query("DELETE FROM favorite_routes WHERE id = :id")
    void deleteFavoriteRouteById(long id);

    /**
     * Elimina por usuario, nombre y datos exactos de ruta
     */
    @Query("DELETE FROM favorite_routes WHERE userEmail = :email AND name = :name AND routeData = :routeData")
    void deleteByUserNameData(String email, String name, String routeData);

    /**
     * Elimina todas las rutas favoritas.
     */
    @Query("DELETE FROM favorite_routes")
    void deleteAll();

    /**
     * Busca rutas favoritas que coincidan con el texto de búsqueda.
     *
     * @param searchText Texto de búsqueda
     * @return Lista de rutas favoritas que coinciden con el texto de búsqueda
     */
    @Query("SELECT * FROM favorite_routes WHERE userEmail = :email AND name LIKE '%' || :searchText || '%' ORDER BY timestamp DESC")
    LiveData<List<FavoriteRouteEntity>> searchFavoriteRoutes(String email, String searchText);
} 