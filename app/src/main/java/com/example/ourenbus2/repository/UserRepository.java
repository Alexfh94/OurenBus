package com.example.ourenbus2.repository;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import androidx.lifecycle.LiveData;

import com.example.ourenbus2.database.AppDatabase;
import com.example.ourenbus2.database.dao.UserDao;
import com.example.ourenbus2.database.entity.UserEntity;
import com.example.ourenbus2.model.User;
import com.google.gson.Gson;

import java.util.UUID;

/**
 * Repositorio para gestionar los datos de usuario
 */
public class UserRepository {

    private static final String PREF_NAME = "user_preferences";
    private static final String KEY_USER = "current_user";
    private static final String KEY_LAST_EMAIL = "last_email";
    
    private final SharedPreferences preferences;
    private final Gson gson;
    
    private final UserDao userDao;
    private final LiveData<UserEntity> currentUser;

    /**
     * Constructor del repositorio.
     *
     * @param application Aplicación
     */
    public UserRepository(Application application) {
        AppDatabase database = AppDatabase.getInstance(application);
        userDao = database.userDao();
        currentUser = userDao.getCurrentUser();
        preferences = application.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    /**
     * Obtiene el usuario actual
     * @param callback Callback para recibir el usuario
     */
    public void getCurrentUser(UserCallback callback) {
        String userJson = preferences.getString(KEY_USER, null);
        if (userJson != null) {
            User user = gson.fromJson(userJson, User.class);
            callback.onUserLoaded(user);
        } else {
            callback.onUserLoaded(null);
        }
    }
    
    /**
     * Guarda un usuario
     * @param user Usuario a guardar
     */
    public void saveUser(User user) {
        if (user != null) {
            String userJson = gson.toJson(user);
            preferences.edit()
                    .putString(KEY_USER, userJson)
                    .putString(KEY_LAST_EMAIL, user.getEmail())
                    .apply();
        }
    }
    
    /**
     * Elimina el usuario actual
     */
    public void deleteUser() {
        preferences.edit()
                .remove(KEY_USER)
                .remove(KEY_LAST_EMAIL)
                .apply();
    }

    public String getLastEmail() {
        return preferences.getString(KEY_LAST_EMAIL, "");
    }

    public boolean hasUserSaved() {
        return preferences.contains(KEY_USER);
    }
    
    /**
     * Interfaz para recibir el usuario cargado
     */
    public interface UserCallback {
        void onUserLoaded(User user);
    }

    /**
     * Obtiene un usuario por su ID.
     *
     * @param id ID del usuario
     * @return Usuario con el ID especificado
     */
    public LiveData<UserEntity> getUserById(String id) {
        return userDao.getUserById(id);
    }

    /**
     * Inserta un nuevo usuario en la base de datos.
     *
     * @param user Usuario a insertar
     */
    public void insert(User user) {
        String id = UUID.randomUUID().toString();
        UserEntity userEntity = new UserEntity(id, user.getName(), user.getEmail(), user.getProfileImagePath());
        new InsertUserAsyncTask(userDao).execute(userEntity);
    }

    /**
     * Comprueba si existe un usuario con el email dado (sincrónico en hilo de fondo).
     */
    public boolean existsByEmail(String email) {
        try {
            UserEntity e = userDao.getUserByEmailSync(email);
            return e != null;
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * Actualiza un usuario existente en la base de datos.
     *
     * @param userEntity Usuario a actualizar
     */
    public void update(UserEntity userEntity) {
        new UpdateUserAsyncTask(userDao).execute(userEntity);
    }

    /**
     * Elimina un usuario de la base de datos.
     *
     * @param userEntity Usuario a eliminar
     */
    public void delete(UserEntity userEntity) {
        new DeleteUserAsyncTask(userDao).execute(userEntity);
    }

    /**
     * Tarea asíncrona para insertar un usuario.
     */
    private static class InsertUserAsyncTask extends AsyncTask<UserEntity, Void, Void> {
        private final UserDao userDao;

        private InsertUserAsyncTask(UserDao userDao) {
            this.userDao = userDao;
        }

        @Override
        protected Void doInBackground(UserEntity... userEntities) {
            userDao.insert(userEntities[0]);
            return null;
        }
    }

    /**
     * Tarea asíncrona para actualizar un usuario.
     */
    private static class UpdateUserAsyncTask extends AsyncTask<UserEntity, Void, Void> {
        private final UserDao userDao;

        private UpdateUserAsyncTask(UserDao userDao) {
            this.userDao = userDao;
        }

        @Override
        protected Void doInBackground(UserEntity... userEntities) {
            userDao.update(userEntities[0]);
            return null;
        }
    }

    /**
     * Tarea asíncrona para eliminar un usuario.
     */
    private static class DeleteUserAsyncTask extends AsyncTask<UserEntity, Void, Void> {
        private final UserDao userDao;

        private DeleteUserAsyncTask(UserDao userDao) {
            this.userDao = userDao;
        }

        @Override
        protected Void doInBackground(UserEntity... userEntities) {
            userDao.delete(userEntities[0]);
            return null;
        }
    }
} 