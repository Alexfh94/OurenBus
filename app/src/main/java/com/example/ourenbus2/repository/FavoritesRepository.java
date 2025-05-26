package com.example.ourenbus2.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.ourenbus2.database.AppDatabase;
import com.example.ourenbus2.database.dao.FavoriteRouteDao;
import com.example.ourenbus2.database.entity.FavoriteRouteEntity;
import com.example.ourenbus2.model.Route;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Repositorio para gestionar operaciones relacionadas con rutas favoritas
 */
public class FavoritesRepository {
    
    private final Context context;
    private final Executor executor;
    private final FavoriteRouteDao favoriteRouteDao;
    private final MutableLiveData<List<Route>> allFavorites = new MutableLiveData<>(new ArrayList<>());
    
    public FavoritesRepository(Context context) {
        this.context = context;
        this.executor = Executors.newSingleThreadExecutor();
        
        // En una app real, obtendríamos el DAO de la base de datos
        // AppDatabase db = AppDatabase.getInstance(context);
        // this.favoriteRouteDao = db.favoriteRouteDao();
        
        // Por ahora, lo establecemos como null ya que es solo un prototipo
        this.favoriteRouteDao = null;
        
        // Cargar datos de prueba
        loadFakeData();
    }
    
    /**
     * Obtiene todas las rutas favoritas
     */
    public LiveData<List<Route>> getAllFavorites() {
        return allFavorites;
    }
    
    /**
     * Inserta una ruta en favoritos
     */
    public void insertFavorite(Route route) {
        executor.execute(() -> {
            // En una app real, convertiríamos la ruta a una entidad y la guardaríamos
            // FavoriteRouteEntity entity = convertToEntity(route);
            // favoriteRouteDao.insert(entity);
            
            // Por ahora, simplemente actualizamos nuestro LiveData con datos falsos
            List<Route> currentList = allFavorites.getValue();
            if (currentList != null) {
                currentList.add(route);
                allFavorites.postValue(new ArrayList<>(currentList));
            }
        });
    }
    
    /**
     * Elimina una ruta de favoritos
     */
    public void deleteFavorite(Route route) {
        executor.execute(() -> {
            // En una app real, eliminaríamos la entidad de la base de datos
            // favoriteRouteDao.deleteById(route.getId());
            
            // Por ahora, simplemente actualizamos nuestro LiveData con datos falsos
            List<Route> currentList = allFavorites.getValue();
            if (currentList != null) {
                currentList.remove(route);
                allFavorites.postValue(new ArrayList<>(currentList));
            }
        });
    }
    
    /**
     * Carga datos falsos para el prototipo
     */
    private void loadFakeData() {
        executor.execute(() -> {
            List<Route> fakeRoutes = new ArrayList<>();
            
            // Crear algunas rutas favoritas de ejemplo
            Route route1 = new Route();
            route1.setName("Casa al trabajo");
            route1.setOrigin(new com.example.ourenbus2.model.Location("Mi Casa", "Calle del Paseo, 15", 42.3350, -7.8640));
            route1.setDestination(new com.example.ourenbus2.model.Location("Oficina", "Polígono Industrial", 42.3320, -7.8750));
            
            Route route2 = new Route();
            route2.setName("Casa a la universidad");
            route2.setOrigin(new com.example.ourenbus2.model.Location("Mi Casa", "Calle del Paseo, 15", 42.3350, -7.8640));
            route2.setDestination(new com.example.ourenbus2.model.Location("Campus Universitario", "Campus As Lagoas", 42.3450, -7.8500));
            
            fakeRoutes.add(route1);
            fakeRoutes.add(route2);
            
            allFavorites.postValue(fakeRoutes);
        });
    }
} 