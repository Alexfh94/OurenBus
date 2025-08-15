package com.example.ourenbus2.ui.viewmodel;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.ourenbus2.model.Location;
import com.example.ourenbus2.model.Route;
import com.example.ourenbus2.repository.LocationRepository;
import com.example.ourenbus2.repository.RouteRepository;
import com.example.ourenbus2.repository.FavoriteRouteRepository;
import com.example.ourenbus2.service.DirectionsHttpService;
import com.example.ourenbus2.util.RouteGenerator;
import com.example.ourenbus2.repository.UserRepository;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class RouteViewModel extends AndroidViewModel {

    private final Executor executor;
    private final LocationRepository locationRepository;
    private final RouteRepository routeRepository;
    private final FavoriteRouteRepository favoritesRepository;
    private final UserRepository userRepository;
    private final DirectionsHttpService directionsService;

    private final MutableLiveData<Location> currentLocation = new MutableLiveData<>();
    private final MutableLiveData<Location> origin = new MutableLiveData<>();
    private final MutableLiveData<Location> destination = new MutableLiveData<>();
    private final MutableLiveData<Route> currentRoute = new MutableLiveData<>();
    private final MutableLiveData<List<Location>> locationSuggestions = new MutableLiveData<>();
    private final LiveData<List<Route>> favoriteRoutes;
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>(null);

    private Location lastOrigin;
    private Location lastDestination;

    public RouteViewModel(@NonNull Application application) {
        super(application);
        executor = Executors.newFixedThreadPool(2);
        locationRepository = new LocationRepository(application);
        routeRepository = new RouteRepository(application);
        favoritesRepository = new FavoriteRouteRepository(application);
        userRepository = new UserRepository(application);
        directionsService = new DirectionsHttpService();
        favoriteRoutes = favoritesRepository.getAllFavoriteRoutes();
    }

    public void updateCurrentLocation(Location location) { currentLocation.setValue(location); }
    public void setOrigin(Location location) { origin.setValue(location); }
    public void setDestination(Location location) { destination.setValue(location); }

    public void searchLocationSuggestions(String query) {
        executor.execute(() -> {
            isLoading.postValue(true);
            List<Location> suggestions = locationRepository.searchLocations(query);
            locationSuggestions.postValue(suggestions);
            isLoading.postValue(false);
        });
    }

    public void searchRoute(Location origin, Location destination) {
        if (origin == null || destination == null) return;
        if (lastOrigin != null && lastDestination != null &&
                equalsCoord(lastOrigin, origin) && equalsCoord(lastDestination, destination) &&
                currentRoute.getValue() != null) {
            return;
        }
        lastOrigin = origin;
        lastDestination = destination;

        executor.execute(() -> {
            isLoading.postValue(true);
            errorMessage.postValue(null);
            String apiKey = getApiKey();
            if (apiKey == null || apiKey.isEmpty()) {
                isLoading.postValue(false);
                errorMessage.postValue("Falta MAPS_API_KEY");
                return;
            }
            Route route = null;
            try {
                // Preferir siempre transporte público; minimizar caminatas con transit_routing_preference=less_walking
                Route transit = directionsService.getBestTransitRoute(apiKey, origin, destination);
                if (transit != null && transit.isValid()) {
                    route = transit;
                } else {
                    // Si no hay transit disponible, caer a caminar
                    route = directionsService.getBestWalkingRoute(apiKey, origin, destination);
                }
            } catch (Exception e) {
                errorMessage.postValue("Error Directions: " + e.getMessage());
            }
            // Fallback si Directions no devuelve ruta válida
            if (route == null || !route.isValid()) {
                route = RouteGenerator.generateSampleRoute(origin, destination);
            }
            if (route != null && route.isValid()) {
                currentRoute.postValue(route);
            } else {
                errorMessage.postValue("No se pudo calcular la ruta");
            }
            isLoading.postValue(false);
        });
    }

    private String getApiKey() {
        try {
            ApplicationInfo ai = getApplication().getPackageManager().getApplicationInfo(getApplication().getPackageName(), PackageManager.GET_META_DATA);
            Object v = ai.metaData != null ? ai.metaData.get("com.google.android.geo.API_KEY") : null;
            return v != null ? String.valueOf(v) : null;
        } catch (Exception e) { return null; }
    }

    private boolean equalsCoord(Location a, Location b) {
        if (a == null || b == null) return false;
        return Math.abs(a.getLatitude() - b.getLatitude()) < 1e-6 && Math.abs(a.getLongitude() - b.getLongitude()) < 1e-6;
    }

    public void saveRouteToFavorites(String name) {
        Route route = currentRoute.getValue();
        if (route != null) {
            route.setName(name != null && !name.isEmpty() ? name : route.toString());
            // Asegurar que el repositorio conoce el email del usuario actual antes de insertar
            String email = userRepository.getLastEmail();
            favoritesRepository.setCurrentUser(email);
            favoritesRepository.insert(route);
        }
    }

    public void removeRouteFromFavorites(Route route) { favoritesRepository.deleteRoute(route); }

    public LiveData<Location> getCurrentLocation() { return currentLocation; }
    public LiveData<Location> getOrigin() { return origin; }
    public LiveData<Location> getDestination() { return destination; }
    public LiveData<Route> getCurrentRoute() { return currentRoute; }
    public LiveData<List<Location>> getLocationSuggestions() { return locationSuggestions; }
    public LiveData<List<Route>> getFavoriteRoutes() { return favoriteRoutes; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
} 