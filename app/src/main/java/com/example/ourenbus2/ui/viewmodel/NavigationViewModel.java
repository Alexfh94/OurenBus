package com.example.ourenbus2.ui.viewmodel;

import android.app.Application;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.ourenbus2.model.Route;
import com.example.ourenbus2.model.RouteSegment;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel para la pantalla de navegación
 */
public class NavigationViewModel extends AndroidViewModel {
    
    // Ruta actual en navegación
    private final MutableLiveData<Route> currentRoute = new MutableLiveData<>();
    
    // Segmentos de la ruta
    private final MutableLiveData<List<RouteSegment>> routeSegments = new MutableLiveData<>(new ArrayList<>());
    
    // Segmento activo actual en navegación
    private final MutableLiveData<RouteSegment> activeSegment = new MutableLiveData<>();
    
    // Segmento seleccionado por el usuario
    private final MutableLiveData<RouteSegment> selectedSegment = new MutableLiveData<>();
    
    // Ubicación actual del usuario
    private final MutableLiveData<Location> currentUserLocation = new MutableLiveData<>();
    
    // Próximo segmento en navegación
    private final MutableLiveData<RouteSegment> nextSegment = new MutableLiveData<>();
    
    // Indicador de recalculando ruta
    private final MutableLiveData<Boolean> isRecalculating = new MutableLiveData<>(false);
    
    // Progreso de la navegación (0-100%)
    private final MutableLiveData<Integer> navigationProgress = new MutableLiveData<>(0);
    
    // Tiempo estimado restante en minutos
    private final MutableLiveData<Integer> remainingTime = new MutableLiveData<>(0);
    
    public NavigationViewModel(@NonNull Application application) {
        super(application);
    }
    
    /**
     * Inicia la navegación con una ruta
     */
    public void startNavigation(Route route) {
        currentRoute.setValue(route);
        
        // Inicializar segmentos
        List<RouteSegment> segments = route.getSegments();
        routeSegments.setValue(segments);
        
        if (segments != null && !segments.isEmpty()) {
            // Primer segmento como actual
            activeSegment.setValue(segments.get(0));
            selectedSegment.setValue(segments.get(0));
            
            // Segundo segmento como próximo (si existe)
            if (segments.size() > 1) {
                nextSegment.setValue(segments.get(1));
            } else {
                nextSegment.setValue(null);
            }
        }
        
        // Inicializar tiempo restante
        remainingTime.setValue(route.getEstimatedTimeInMinutes());
        
        // Inicializar progreso
        navigationProgress.setValue(0);
    }
    
    /**
     * Actualiza la ubicación actual del usuario
     */
    public void updateUserLocation(Location location) {
        currentUserLocation.setValue(location);
        
        // Aquí se implementaría la lógica para actualizar el progreso de la navegación
        // basado en la ubicación actual
        updateNavigationProgress(location);
    }
    
    /**
     * Establece el segmento seleccionado por el usuario
     */
    public void setSelectedSegment(RouteSegment segment) {
        selectedSegment.setValue(segment);
    }
    
    /**
     * Avanza al siguiente segmento de la ruta
     */
    public void advanceToNextSegment() {
        RouteSegment current = activeSegment.getValue();
        RouteSegment next = nextSegment.getValue();
        
        if (next != null) {
            // Avanzar al siguiente segmento
            activeSegment.setValue(next);
            
            // Buscar el siguiente segmento en la lista
            List<RouteSegment> segments = currentRoute.getValue().getSegments();
            int currentIndex = segments.indexOf(next);
            
            if (currentIndex >= 0 && currentIndex < segments.size() - 1) {
                nextSegment.setValue(segments.get(currentIndex + 1));
            } else {
                // No hay más segmentos
                nextSegment.setValue(null);
            }
        }
    }
    
    /**
     * Recalcula la ruta (simulado)
     */
    public void recalculateRoute() {
        isRecalculating.setValue(true);
        
        // Simulación de recálculo
        new Thread(() -> {
            try {
                Thread.sleep(2000); // Simular 2 segundos de recálculo
                isRecalculating.postValue(false);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
    
    /**
     * Actualiza el progreso de la navegación basado en la ubicación actual
     */
    private void updateNavigationProgress(Location location) {
        Route route = currentRoute.getValue();
        RouteSegment segment = activeSegment.getValue();
        
        if (route == null || segment == null || location == null) {
            return;
        }
        
        // Simular progreso basado en la cercanía al destino final
        com.example.ourenbus2.model.Location destinationLocation = route.getDestination();
        float[] results = new float[1];
        
        Location.distanceBetween(
                location.getLatitude(), location.getLongitude(),
                destinationLocation.getLatitude(), destinationLocation.getLongitude(),
                results
        );
        
        float distanceToDestination = results[0]; // metros
        
        // Calcular un progreso aproximado
        // En una app real, esto sería mucho más sofisticado
        double totalRouteDistance = 0;
        for (RouteSegment seg : route.getSegments()) {
            totalRouteDistance += seg.getDistance();
        }
        
        // Progreso entre 0 y 100
        int progress = (int) (100 - (distanceToDestination / totalRouteDistance * 100));
        progress = Math.max(0, Math.min(100, progress)); // Limitar entre 0 y 100
        
        navigationProgress.setValue(progress);
        
        // Actualizar tiempo restante estimado
        int totalTime = route.getEstimatedTimeInMinutes();
        int timeRemaining = (int) (totalTime * (1 - (progress / 100.0)));
        remainingTime.setValue(timeRemaining);
    }
    
    // Getters para los LiveData
    public LiveData<Route> getCurrentRoute() {
        return currentRoute;
    }
    
    public LiveData<RouteSegment> getActiveSegment() {
        return activeSegment;
    }
    
    public LiveData<RouteSegment> getNextSegment() {
        return nextSegment;
    }
    
    public LiveData<Location> getCurrentUserLocation() {
        return currentUserLocation;
    }
    
    public LiveData<Boolean> getIsRecalculating() {
        return isRecalculating;
    }
    
    public LiveData<Integer> getNavigationProgress() {
        return navigationProgress;
    }
    
    public LiveData<Integer> getRemainingTime() {
        return remainingTime;
    }
    
    public LiveData<List<RouteSegment>> getRouteSegments() {
        return routeSegments;
    }
    
    public LiveData<RouteSegment> getSelectedSegment() {
        return selectedSegment;
    }
} 