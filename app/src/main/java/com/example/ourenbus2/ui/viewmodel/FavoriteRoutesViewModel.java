package com.example.ourenbus2.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.ourenbus2.model.Route;
import com.example.ourenbus2.repository.FavoriteRouteRepository;

import java.util.List;

/**
 * ViewModel para gestionar las rutas favoritas
 */
public class FavoriteRoutesViewModel extends AndroidViewModel {

    private final FavoriteRouteRepository repository;
    private final LiveData<List<Route>> favoriteRoutes;
    private final MutableLiveData<Route> selectedRoute = new MutableLiveData<>();

    public FavoriteRoutesViewModel(@NonNull Application application) {
        super(application);
        repository = new FavoriteRouteRepository(application);
        favoriteRoutes = repository.getAllFavoriteRoutes();
    }

    /**
     * Obtiene todas las rutas favoritas
     */
    public LiveData<List<Route>> getFavoriteRoutes() {
        return favoriteRoutes;
    }

    /**
     * Elimina una ruta de favoritos
     */
    public void deleteRoute(Route route) {
        if (route != null) {
            repository.deleteRoute(route);
        }
    }

    /**
     * Selecciona una ruta para ser usada en la navegación
     */
    public void selectRoute(Route route) {
        selectedRoute.setValue(route);
    }

    /**
     * Obtiene la ruta seleccionada
     */
    public LiveData<Route> getSelectedRoute() {
        return selectedRoute;
    }

    /**
     * Guarda una ruta en favoritos
     */
    public void saveRoute(Route route) {
        if (route != null) {
            repository.saveRoute(route);
        }
    }

    /**
     * Renombra una ruta favorita
     */
    public void renameRoute(Route route, String newName) {
        if (route != null && newName != null && !newName.isEmpty()) {
            route.setName(newName);
            repository.updateRoute(route);
        }
    }
} 