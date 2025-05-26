package com.example.ourenbus2.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.ourenbus2.model.BusLine;
import com.example.ourenbus2.model.BusStop;
import com.example.ourenbus2.model.Location;
import com.example.ourenbus2.model.Route;
import com.example.ourenbus2.model.RouteSegment;
import com.example.ourenbus2.repository.FavoritesRepository;
import com.example.ourenbus2.repository.LocationRepository;
import com.example.ourenbus2.repository.RouteRepository;
import com.example.ourenbus2.util.RouteGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * ViewModel para la gestión de rutas
 */
public class RouteViewModel extends AndroidViewModel {
    
    private final Executor executor;
    private final LocationRepository locationRepository;
    private final RouteRepository routeRepository;
    private final FavoritesRepository favoritesRepository;
    
    // LiveData para la ubicación actual
    private final MutableLiveData<Location> currentLocation = new MutableLiveData<>();
    
    // LiveData para origen y destino
    private final MutableLiveData<Location> origin = new MutableLiveData<>();
    private final MutableLiveData<Location> destination = new MutableLiveData<>();
    
    // LiveData para la ruta actual
    private final MutableLiveData<Route> currentRoute = new MutableLiveData<>();
    
    // LiveData para sugerencias de ubicaciones
    private final MutableLiveData<List<Location>> locationSuggestions = new MutableLiveData<>();
    
    // LiveData para favoritos
    private final LiveData<List<Route>> favoriteRoutes;
    
    // LiveData para el estado de carga
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    
    public RouteViewModel(@NonNull Application application) {
        super(application);
        executor = Executors.newFixedThreadPool(2);
        
        // Inicializar repositorios
        locationRepository = new LocationRepository(application);
        routeRepository = new RouteRepository(application);
        favoritesRepository = new FavoritesRepository(application);
        
        // Cargar favoritos
        favoriteRoutes = favoritesRepository.getAllFavorites();
    }
    
    /**
     * Actualiza la ubicación actual
     */
    public void updateCurrentLocation(Location location) {
        currentLocation.setValue(location);
    }
    
    /**
     * Establece el origen de la ruta
     */
    public void setOrigin(Location location) {
        origin.setValue(location);
    }
    
    /**
     * Establece el destino de la ruta
     */
    public void setDestination(Location location) {
        destination.setValue(location);
    }
    
    /**
     * Busca sugerencias de ubicaciones
     */
    public void searchLocationSuggestions(String query) {
        executor.execute(() -> {
            isLoading.postValue(true);
            List<Location> suggestions = locationRepository.searchLocations(query);
            locationSuggestions.postValue(suggestions);
            isLoading.postValue(false);
        });
    }
    
    /**
     * Busca una ruta entre origen y destino
     */
    public void searchRoute(Location origin, Location destination) {
        if (origin == null || destination == null) {
            return;
        }
        
        executor.execute(() -> {
            isLoading.postValue(true);
            
            // En una app real, aquí se haría la llamada a la API
            // En este prototipo, generamos una ruta de ejemplo
            Route route = RouteGenerator.generateSampleRoute(origin, destination);
            
            currentRoute.postValue(route);
            isLoading.postValue(false);
        });
    }
    
    /**
     * Guarda una ruta en favoritos
     */
    public void saveRouteToFavorites(String name) {
        Route currentRouteValue = currentRoute.getValue();
        if (currentRouteValue != null) {
            currentRouteValue.setName(name);
            favoritesRepository.insertFavorite(currentRouteValue);
        }
    }
    
    /**
     * Elimina una ruta de favoritos
     */
    public void removeRouteFromFavorites(Route route) {
        favoritesRepository.deleteFavorite(route);
    }
    
    // Getters para los LiveData
    public LiveData<Location> getCurrentLocation() {
        return currentLocation;
    }
    
    public LiveData<Location> getOrigin() {
        return origin;
    }
    
    public LiveData<Location> getDestination() {
        return destination;
    }
    
    public LiveData<Route> getCurrentRoute() {
        return currentRoute;
    }
    
    public LiveData<List<Location>> getLocationSuggestions() {
        return locationSuggestions;
    }
    
    public LiveData<List<Route>> getFavoriteRoutes() {
        return favoriteRoutes;
    }
    
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
} 