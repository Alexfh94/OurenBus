package com.example.ourenbus2.database.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * Entidad que representa un usuario en la base de datos.
 */
@Entity(tableName = "users")
public class UserEntity {

    @PrimaryKey
    @NonNull
    private String id;
    private String name;
    private String email;
    private String profileImagePath;

    /**
     * Constructor por defecto.
     */
    public UserEntity() {
    }

    /**
     * Constructor con parámetros.
     *
     * @param id              Identificador único del usuario
     * @param name            Nombre del usuario
     * @param email           Correo electrónico del usuario
     * @param profileImagePath Ruta de la imagen de perfil
     */
    @Ignore
    public UserEntity(@NonNull String id, String name, String email, String profileImagePath) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.profileImagePath = profileImagePath;
    }

    /**
     * Obtiene el identificador único del usuario.
     *
     * @return Identificador del usuario
     */
    @NonNull
    public String getId() {
        return id;
    }

    /**
     * Establece el identificador único del usuario.
     *
     * @param id Identificador del usuario
     */
    public void setId(@NonNull String id) {
        this.id = id;
    }

    /**
     * Obtiene el nombre del usuario.
     *
     * @return Nombre del usuario
     */
    public String getName() {
        return name;
    }

    /**
     * Establece el nombre del usuario.
     *
     * @param name Nombre del usuario
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Obtiene el correo electrónico del usuario.
     *
     * @return Correo electrónico del usuario
     */
    public String getEmail() {
        return email;
    }

    /**
     * Establece el correo electrónico del usuario.
     *
     * @param email Correo electrónico del usuario
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Obtiene la ruta de la imagen de perfil.
     *
     * @return Ruta de la imagen de perfil
     */
    public String getProfileImagePath() {
        return profileImagePath;
    }

    /**
     * Establece la ruta de la imagen de perfil.
     *
     * @param profileImagePath Ruta de la imagen de perfil
     */
    public void setProfileImagePath(String profileImagePath) {
        this.profileImagePath = profileImagePath;
    }
} 