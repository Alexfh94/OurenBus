package com.example.ourenbus2.repository;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.ourenbus2.database.AppDatabase;
import com.example.ourenbus2.database.dao.FavoriteRouteDao;
import com.example.ourenbus2.database.entity.FavoriteRouteEntity;
import com.example.ourenbus2.model.Location;
import com.example.ourenbus2.model.Route;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Repositorio para gestionar las rutas favoritas
 */
public class FavoriteRouteRepository {

    private static final String PREF_NAME = "favorite_routes_preferences"; // legacy, ya no se usa para guardar rutas
    private static final String KEY_ROUTES = "favorite_routes"; // legacy

    private final SharedPreferences preferences; // solo para otras preferencias si fuese necesario
    private final Gson gson;
    private String currentUserEmail = null;
    
    private final FavoriteRouteDao favoriteRouteDao;
    private LiveData<List<FavoriteRouteEntity>> roomFavorites;
    private final androidx.lifecycle.MediatorLiveData<List<Route>> favoritesLiveData = new androidx.lifecycle.MediatorLiveData<>();

    /**
     * Constructor del repositorio.
     *
     * @param application Aplicación
     */
    public FavoriteRouteRepository(Application application) {
        preferences = application.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
        
        AppDatabase database = AppDatabase.getInstance(application);
        favoriteRouteDao = database.favoriteRouteDao();
        // Por defecto, sin usuario -> LiveData vacía
        roomFavorites = new MutableLiveData<>(new ArrayList<>());
        favoritesLiveData.addSource(roomFavorites, list -> mapAndPost(list));
    }
    
    /**
     * Carga las rutas favoritas desde SharedPreferences
     */
    private void mapAndPost(List<FavoriteRouteEntity> entities) {
        List<Route> out = new ArrayList<>();
        if (entities != null) {
            for (FavoriteRouteEntity e : entities) out.add(convertToRoute(e));
        }
        favoritesLiveData.postValue(out);
    }
    
    /**
     * Guarda las rutas favoritas en SharedPreferences
     */
    private void saveFavoriteRoutes(List<Route> routes) { /* legacy no-op */ }
    
    /**
     * Obtiene todas las rutas favoritas
     * @return LiveData con la lista de rutas favoritas
     */
    public LiveData<List<Route>> getAllFavoriteRoutes() { return favoritesLiveData; }
    
    /**
     * Guarda una ruta en favoritos
     * @param route Ruta a guardar
     */
    public void saveRoute(Route route) { /* legacy no-op */ }
    
    /**
     * Elimina una ruta de favoritos
     * @param route Ruta a eliminar
     */
    public void deleteRoute(Route route) { /* legacy no-op */ }
    
    /**
     * Actualiza una ruta existente
     * @param route Ruta actualizada
     */
    public void updateRoute(Route route) { /* legacy no-op */ }

    /**
     * Obtiene una ruta favorita por su ID.
     *
     * @param id ID de la ruta favorita
     * @return Ruta favorita con el ID especificado
     */
    public LiveData<FavoriteRouteEntity> getFavoriteRouteById(long id) {
        return favoriteRouteDao.getFavoriteRouteById(id);
    }

    /**
     * Busca rutas favoritas que coincidan con el texto de búsqueda.
     *
     * @param searchText Texto de búsqueda
     * @return Lista de rutas favoritas que coinciden con el texto de búsqueda
     */
    public LiveData<List<FavoriteRouteEntity>> searchFavoriteRoutes(String userId, String searchText) {
        return favoriteRouteDao.searchFavoriteRoutes(userId, searchText);
    }

    /**
     * Inserta una nueva ruta favorita.
     *
     * @param route Ruta a guardar como favorita
     */
    public void insert(Route route) {
        String routeJson = gson.toJson(route);
        String name = route.getOrigin().getName() + " - " + route.getDestination().getName();
        String userId = getCurrentUserEmail();
        if (userId == null || userId.isEmpty()) return;
        FavoriteRouteEntity favoriteRoute = new FavoriteRouteEntity(
                name,
                route.getOrigin(),
                route.getDestination(),
                routeJson,
                System.currentTimeMillis(),
                userId
        );
        new InsertFavoriteRouteAsyncTask(favoriteRouteDao, () -> { /* Room LiveData actualizará */ }).execute(favoriteRoute);
    }

    public void setCurrentUser(String userEmail) {
        // Establecer el email de usuario actual
        currentUserEmail = (userEmail != null && !userEmail.isEmpty()) ? userEmail : null;
        // Reasignar fuente Room para el usuario actual
        if (roomFavorites != null) {
            favoritesLiveData.removeSource(roomFavorites);
        }
        if (currentUserEmail != null) {
            roomFavorites = favoriteRouteDao.getAllFavoriteRoutes(currentUserEmail);
        } else {
            roomFavorites = new MutableLiveData<>(new ArrayList<>());
        }
        favoritesLiveData.addSource(roomFavorites, this::mapAndPost);
    }

    private String getCurrentUserEmail() { return currentUserEmail; }

    private void mergeAndPost() { /* legacy no-op */ }

    private String buildKey(String email, Route r) { return ""; }

    /**
     * Actualiza una ruta favorita existente.
     *
     * @param favoriteRoute Ruta favorita a actualizar
     */
    public void update(FavoriteRouteEntity favoriteRoute) {
        new UpdateFavoriteRouteAsyncTask(favoriteRouteDao).execute(favoriteRoute);
    }

    /**
     * Elimina una ruta favorita.
     *
     * @param favoriteRoute Ruta favorita a eliminar
     */
    public void delete(FavoriteRouteEntity favoriteRoute) {
        new DeleteFavoriteRouteAsyncTask(favoriteRouteDao, this::mergeAndPost).execute(favoriteRoute);
    }

    /**
     * Elimina una ruta favorita por su ID.
     *
     * @param id ID de la ruta favorita a eliminar
     */
    public void deleteById(long id) {
        new DeleteFavoriteRouteByIdAsyncTask(favoriteRouteDao, this::mergeAndPost).execute(id);
    }

    /**
     * Elimina una ruta favorita del usuario actual por nombre y datos de ruta (JSON), útil cuando no tenemos el ID.
     */
    public void deleteByUserAndContent(Route route) {
        if (route == null) return;
        String email = getCurrentUserEmail();
        if (email == null || email.isEmpty()) return;
        String json = gson.toJson(route);
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                favoriteRouteDao.deleteByUserNameData(email, route.getName(), json);
                return null;
            }
        }.execute();
    }

    /**
     * Convierte una entidad de ruta favorita a un modelo de ruta.
     *
     * @param favoriteRouteEntity Entidad de ruta favorita
     * @return Modelo de ruta
     */
    public Route convertToRoute(FavoriteRouteEntity favoriteRouteEntity) {
        Route r = gson.fromJson(favoriteRouteEntity.getRouteData(), Route.class);
        if (r != null) {
            r.setId(favoriteRouteEntity.getId());
            if (r.getSavedDate() == null) {
                java.util.Date d = new java.util.Date(favoriteRouteEntity.getTimestamp());
                r.setSavedDate(d);
            }
        }
        return r;
    }

    /**
     * Tarea asíncrona para insertar una ruta favorita.
     */
    private static class InsertFavoriteRouteAsyncTask extends AsyncTask<FavoriteRouteEntity, Void, Void> {
        private final FavoriteRouteDao favoriteRouteDao;
        private final Runnable onDone;

        private InsertFavoriteRouteAsyncTask(FavoriteRouteDao favoriteRouteDao, Runnable onDone) {
            this.favoriteRouteDao = favoriteRouteDao;
            this.onDone = onDone;
        }

        @Override
        protected Void doInBackground(FavoriteRouteEntity... favoriteRouteEntities) {
            favoriteRouteDao.insert(favoriteRouteEntities[0]);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (onDone != null) onDone.run();
        }
    }

    /**
     * Tarea asíncrona para actualizar una ruta favorita.
     */
    private static class UpdateFavoriteRouteAsyncTask extends AsyncTask<FavoriteRouteEntity, Void, Void> {
        private final FavoriteRouteDao favoriteRouteDao;

        private UpdateFavoriteRouteAsyncTask(FavoriteRouteDao favoriteRouteDao) {
            this.favoriteRouteDao = favoriteRouteDao;
        }

        @Override
        protected Void doInBackground(FavoriteRouteEntity... favoriteRouteEntities) {
            favoriteRouteDao.update(favoriteRouteEntities[0]);
            return null;
        }
    }

    /**
     * Tarea asíncrona para eliminar una ruta favorita.
     */
    private static class DeleteFavoriteRouteAsyncTask extends AsyncTask<FavoriteRouteEntity, Void, Void> {
        private final FavoriteRouteDao favoriteRouteDao;
        private final Runnable onDone;

        private DeleteFavoriteRouteAsyncTask(FavoriteRouteDao favoriteRouteDao, Runnable onDone) {
            this.favoriteRouteDao = favoriteRouteDao;
            this.onDone = onDone;
        }

        @Override
        protected Void doInBackground(FavoriteRouteEntity... favoriteRouteEntities) {
            favoriteRouteDao.delete(favoriteRouteEntities[0]);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (onDone != null) onDone.run();
        }
    }

    /**
     * Tarea asíncrona para eliminar una ruta favorita por su ID.
     */
    private static class DeleteFavoriteRouteByIdAsyncTask extends AsyncTask<Long, Void, Void> {
        private final FavoriteRouteDao favoriteRouteDao;
        private final Runnable onDone;

        private DeleteFavoriteRouteByIdAsyncTask(FavoriteRouteDao favoriteRouteDao, Runnable onDone) {
            this.favoriteRouteDao = favoriteRouteDao;
            this.onDone = onDone;
        }

        @Override
        protected Void doInBackground(Long... ids) {
            favoriteRouteDao.deleteFavoriteRouteById(ids[0]);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (onDone != null) onDone.run();
        }
    }
} 