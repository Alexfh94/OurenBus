package com.example.ourenbus2.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Objects;

/**
 * Clase que representa un usuario de la aplicación
 */
@Entity(tableName = "users")
public class User {

    @PrimaryKey(autoGenerate = true)
    private long id;
    
    private String name;
    private String email;
    private String profileImageUrl;
    
    public User() {
        // Constructor vacío requerido por Room
    }
    
    public User(String name, String email) {
        this.name = name;
        this.email = email;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }
    
    /**
     * Método para compatibilidad con UserEntity
     * @return La URL de la imagen de perfil como ruta
     */
    public String getProfileImagePath() {
        return profileImageUrl;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return id == user.id &&
               Objects.equals(email, user.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, email);
    }

    @NonNull
    @Override
    public String toString() {
        return name + " (" + email + ")";
    }
} 