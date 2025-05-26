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

    private static final String PREF_NAME = "favorite_routes_preferences";
    private static final String KEY_ROUTES = "favorite_routes";
    
    private final SharedPreferences preferences;
    private final Gson gson;
    private final MutableLiveData<List<Route>> favoriteRoutes;
    
    private final FavoriteRouteDao favoriteRouteDao;
    private final LiveData<List<FavoriteRouteEntity>> allFavoriteRoutes;

    /**
     * Constructor del repositorio.
     *
     * @param application Aplicación
     */
    public FavoriteRouteRepository(Application application) {
        preferences = application.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
        favoriteRoutes = new MutableLiveData<>(new ArrayList<>());
        
        AppDatabase database = AppDatabase.getInstance(application);
        favoriteRouteDao = database.favoriteRouteDao();
        allFavoriteRoutes = favoriteRouteDao.getAllFavoriteRoutes();
        
        // Cargar rutas favoritas al inicializar
        loadFavoriteRoutes();
    }
    
    /**
     * Carga las rutas favoritas desde SharedPreferences
     */
    private void loadFavoriteRoutes() {
        String routesJson = preferences.getString(KEY_ROUTES, null);
        if (routesJson != null) {
            Type type = new TypeToken<List<Route>>(){}.getType();
            List<Route> routes = gson.fromJson(routesJson, type);
            favoriteRoutes.setValue(routes);
        }
    }
    
    /**
     * Guarda las rutas favoritas en SharedPreferences
     */
    private void saveFavoriteRoutes(List<Route> routes) {
        String routesJson = gson.toJson(routes);
        preferences.edit()
                .putString(KEY_ROUTES, routesJson)
                .apply();
        favoriteRoutes.setValue(routes);
    }
    
    /**
     * Obtiene todas las rutas favoritas
     * @return LiveData con la lista de rutas favoritas
     */
    public LiveData<List<Route>> getAllFavoriteRoutes() {
        return favoriteRoutes;
    }
    
    /**
     * Guarda una ruta en favoritos
     * @param route Ruta a guardar
     */
    public void saveRoute(Route route) {
        if (route != null) {
            List<Route> routes = favoriteRoutes.getValue();
            if (routes == null) {
                routes = new ArrayList<>();
            }
            
            // Verificar si la ruta ya existe
            for (int i = 0; i < routes.size(); i++) {
                if (routes.get(i).getId() == route.getId()) {
                    routes.remove(i);
                    break;
                }
            }
            
            // Marcar como favorita y establecer fecha de guardado
            route.setFavorite(true);
            route.setSavedDate(new Date());
            
            // Agregar la ruta a la lista
            routes.add(route);
            
            // Guardar la lista actualizada
            saveFavoriteRoutes(routes);
        }
    }
    
    /**
     * Elimina una ruta de favoritos
     * @param route Ruta a eliminar
     */
    public void deleteRoute(Route route) {
        if (route != null) {
            List<Route> routes = favoriteRoutes.getValue();
            if (routes != null) {
                for (int i = 0; i < routes.size(); i++) {
                    if (routes.get(i).getId() == route.getId()) {
                        routes.remove(i);
                        saveFavoriteRoutes(routes);
                        break;
                    }
                }
            }
        }
    }
    
    /**
     * Actualiza una ruta existente
     * @param route Ruta actualizada
     */
    public void updateRoute(Route route) {
        if (route != null) {
            List<Route> routes = favoriteRoutes.getValue();
            if (routes != null) {
                for (int i = 0; i < routes.size(); i++) {
                    if (routes.get(i).getId() == route.getId()) {
                        routes.set(i, route);
                        saveFavoriteRoutes(routes);
                        break;
                    }
                }
            }
        }
    }

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
    public LiveData<List<FavoriteRouteEntity>> searchFavoriteRoutes(String searchText) {
        return favoriteRouteDao.searchFavoriteRoutes(searchText);
    }

    /**
     * Inserta una nueva ruta favorita.
     *
     * @param route Ruta a guardar como favorita
     */
    public void insert(Route route) {
        String routeJson = gson.toJson(route);
        String name = route.getOrigin().getName() + " - " + route.getDestination().getName();
        FavoriteRouteEntity favoriteRoute = new FavoriteRouteEntity(
                name,
                route.getOrigin(),
                route.getDestination(),
                routeJson,
                System.currentTimeMillis()
        );
        new InsertFavoriteRouteAsyncTask(favoriteRouteDao).execute(favoriteRoute);
    }

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
        new DeleteFavoriteRouteAsyncTask(favoriteRouteDao).execute(favoriteRoute);
    }

    /**
     * Elimina una ruta favorita por su ID.
     *
     * @param id ID de la ruta favorita a eliminar
     */
    public void deleteById(long id) {
        new DeleteFavoriteRouteByIdAsyncTask(favoriteRouteDao).execute(id);
    }

    /**
     * Convierte una entidad de ruta favorita a un modelo de ruta.
     *
     * @param favoriteRouteEntity Entidad de ruta favorita
     * @return Modelo de ruta
     */
    public Route convertToRoute(FavoriteRouteEntity favoriteRouteEntity) {
        return gson.fromJson(favoriteRouteEntity.getRouteData(), Route.class);
    }

    /**
     * Tarea asíncrona para insertar una ruta favorita.
     */
    private static class InsertFavoriteRouteAsyncTask extends AsyncTask<FavoriteRouteEntity, Void, Void> {
        private final FavoriteRouteDao favoriteRouteDao;

        private InsertFavoriteRouteAsyncTask(FavoriteRouteDao favoriteRouteDao) {
            this.favoriteRouteDao = favoriteRouteDao;
        }

        @Override
        protected Void doInBackground(FavoriteRouteEntity... favoriteRouteEntities) {
            favoriteRouteDao.insert(favoriteRouteEntities[0]);
            return null;
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

        private DeleteFavoriteRouteAsyncTask(FavoriteRouteDao favoriteRouteDao) {
            this.favoriteRouteDao = favoriteRouteDao;
        }

        @Override
        protected Void doInBackground(FavoriteRouteEntity... favoriteRouteEntities) {
            favoriteRouteDao.delete(favoriteRouteEntities[0]);
            return null;
        }
    }

    /**
     * Tarea asíncrona para eliminar una ruta favorita por su ID.
     */
    private static class DeleteFavoriteRouteByIdAsyncTask extends AsyncTask<Long, Void, Void> {
        private final FavoriteRouteDao favoriteRouteDao;

        private DeleteFavoriteRouteByIdAsyncTask(FavoriteRouteDao favoriteRouteDao) {
            this.favoriteRouteDao = favoriteRouteDao;
        }

        @Override
        protected Void doInBackground(Long... ids) {
            favoriteRouteDao.deleteFavoriteRouteById(ids[0]);
            return null;
        }
    }
} 