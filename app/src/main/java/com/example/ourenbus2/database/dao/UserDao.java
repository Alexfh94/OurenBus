package com.example.ourenbus2.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.ourenbus2.database.entity.UserEntity;

/**
 * DAO (Data Access Object) para operaciones con usuarios en la base de datos.
 */
@Dao
public interface UserDao {

    /**
     * Inserta un nuevo usuario en la base de datos.
     *
     * @param user Usuario a insertar
     * @return ID del usuario insertado
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(UserEntity user);

    /**
     * Actualiza un usuario existente en la base de datos.
     *
     * @param user Usuario a actualizar
     */
    @Update
    void update(UserEntity user);

    /**
     * Elimina un usuario de la base de datos.
     *
     * @param user Usuario a eliminar
     */
    @Delete
    void delete(UserEntity user);

    /**
     * Obtiene un usuario por su ID.
     *
     * @param id ID del usuario
     * @return Usuario con el ID especificado
     */
    @Query("SELECT * FROM users WHERE id = :id")
    LiveData<UserEntity> getUserById(String id);

    /**
     * Obtiene todos los usuarios de la base de datos.
     *
     * @return Lista de todos los usuarios
     */
    @Query("SELECT * FROM users")
    LiveData<UserEntity[]> getAllUsers();

    /**
     * Obtiene el usuario actual.
     * Asumimos que solo hay un usuario en la aplicación.
     *
     * @return Usuario actual
     */
    @Query("SELECT * FROM users LIMIT 1")
    LiveData<UserEntity> getCurrentUser();
} 