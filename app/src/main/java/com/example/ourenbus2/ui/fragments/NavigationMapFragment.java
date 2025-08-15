package com.example.ourenbus2.ui.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.ourenbus2.R;
import com.example.ourenbus2.model.Location;
import com.example.ourenbus2.model.RouteSegment;
import com.example.ourenbus2.ui.viewmodel.NavigationViewModel;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

/**
 * Fragmento para mostrar el mapa durante la navegación en tiempo real
 */
public class NavigationMapFragment extends Fragment implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final float ZOOM_LEVEL = 17f;
    private static final long LOCATION_UPDATE_INTERVAL = 5000; // 5 segundos

    private GoogleMap googleMap;
    private NavigationViewModel viewModel;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private FloatingActionButton fabMyLocation;
    private FloatingActionButton fabZoomIn;
    private FloatingActionButton fabZoomOut;
    private FloatingActionButton fabNavigationMode;
    private boolean followUserLocation = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_navigation_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Inicializar ViewModel
        viewModel = new ViewModelProvider(requireActivity()).get(NavigationViewModel.class);
        
        // Referencias a vistas
        fabMyLocation = view.findViewById(R.id.fab_my_location);
        fabNavigationMode = view.findViewById(R.id.fab_navigation_mode);
        fabZoomIn = view.findViewById(R.id.fab_zoom_in);
        fabZoomOut = view.findViewById(R.id.fab_zoom_out);
        
        // Configurar listeners
        setupListeners();
        
        // Obtener referencia al mapa
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.navigation_map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        
        // Configurar cliente de ubicación
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        
        // Configurar callback de ubicación
        setupLocationCallback();
    }
    
    private void setupListeners() {
        fabMyLocation.setOnClickListener(v -> {
            followUserLocation = true;
            showMyLocation();
        });
        fabNavigationMode.setOnClickListener(v -> {
            followUserLocation = true;
            enableNavigationCamera();
        });
        
        fabZoomIn.setOnClickListener(v -> {
            if (googleMap != null) {
                googleMap.animateCamera(CameraUpdateFactory.zoomIn());
            }
        });
        
        fabZoomOut.setOnClickListener(v -> {
            if (googleMap != null) {
                googleMap.animateCamera(CameraUpdateFactory.zoomOut());
            }
        });
    }
    
    private void setupLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                
                android.location.Location location = locationResult.getLastLocation();
                if (location != null) {
                    // Actualizar ubicación en el ViewModel
                    viewModel.updateUserLocation(location);
                    
                    // Centrar mapa en la ubicación si estamos siguiendo al usuario
                    if (followUserLocation) {
                        centerMapOnLocation(location);
                    }
                }
            }
        };
    }
    
    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;
        
        // Configurar mapa
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        
        // Comprobar permisos de ubicación
        if (hasLocationPermission()) {
            enableMyLocation();
        } else {
            requestLocationPermission();
        }
        
        // Configurar listener de movimiento de cámara
        googleMap.setOnCameraMoveStartedListener(reason -> {
            if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                // Si el usuario mueve el mapa manualmente, dejar de seguir la ubicación
                followUserLocation = false;
            }
        });
        
        // Observar cambios en los segmentos de ruta
        viewModel.getRouteSegments().observe(getViewLifecycleOwner(), segments -> {
            drawRoute(segments);
            // si hay segmentos, centrar a la ruta inmediatamente al abrir
            if (segments != null && !segments.isEmpty()) {
                followUserLocation = false; // evitar que se mueva a la ubicación antes de ver la ruta
            }
        });
        
        // Observar cambios en el segmento activo
        viewModel.getActiveSegment().observe(getViewLifecycleOwner(), this::highlightActiveSegment);
        
        // Observar cambios en el segmento seleccionado
        viewModel.getSelectedSegment().observe(getViewLifecycleOwner(), this::centerMapOnSegment);
        
        // Observar cambios en la ubicación del usuario
        viewModel.getCurrentUserLocation().observe(getViewLifecycleOwner(), this::updateUserMarker);
    }
    
    /**
     * Dibuja la ruta en el mapa
     */
    private void drawRoute(List<RouteSegment> segments) {
        if (googleMap == null || segments == null || segments.isEmpty()) {
            return;
        }

        googleMap.clear();

        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();

        for (RouteSegment segment : segments) {
            Location start = segment.getStartLocation();
            Location end = segment.getEndLocation();
            if (start == null || end == null) continue;

            LatLng startLatLng = new LatLng(start.getLatitude(), start.getLongitude());
            LatLng endLatLng = new LatLng(end.getLatitude(), end.getLongitude());

            int color;
            if (segment.getType() == RouteSegment.SegmentType.WALKING) {
                color = Color.GRAY;
            } else if (segment.getType() == RouteSegment.SegmentType.BUS && segment.getBusLine() != null && segment.getBusLine().getColor() != null) {
                try { color = Color.parseColor(segment.getBusLine().getColor()); }
                catch (Exception e) { color = Color.BLACK; }
            } else {
                color = Color.BLACK;
            }

            // Dibujar polilínea real si está disponible
            boolean drewPolyline = false;
            if (segment.getPolylineEncoded() != null && !segment.getPolylineEncoded().isEmpty()) {
                List<LatLng> points = decodePolyline(segment.getPolylineEncoded());
                if (points != null && points.size() > 1) {
                    googleMap.addPolyline(new PolylineOptions().addAll(points).color(color).width(10));
                    for (LatLng p : points) boundsBuilder.include(p);
                    drewPolyline = true;
                }
            }
            if (!drewPolyline) {
                googleMap.addPolyline(new PolylineOptions().add(startLatLng, endLatLng).color(color).width(10));
                boundsBuilder.include(startLatLng);
                boundsBuilder.include(endLatLng);
            }

            // Marcadores de origen/destino
            if (segment.equals(segments.get(0))) {
                googleMap.addMarker(new MarkerOptions()
                        .position(startLatLng)
                        .title(start.getName())
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
            }
            if (segment.equals(segments.get(segments.size() - 1))) {
                googleMap.addMarker(new MarkerOptions()
                        .position(endLatLng)
                        .title(end.getName())
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
            }

            // Marcador de parada de bus si existe
            if (segment.getType() == RouteSegment.SegmentType.BUS && segment.getBusStop() != null) {
                LatLng stopLatLng = new LatLng(segment.getBusStop().getLatitude(), segment.getBusStop().getLongitude());
                googleMap.addMarker(new MarkerOptions()
                        .position(stopLatLng)
                        .title(segment.getBusStop().getName())
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
            }
        }

        try {
            LatLngBounds bounds = boundsBuilder.build();
            googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
        } catch (IllegalStateException ignored) {}
    }

    private List<LatLng> decodePolyline(String encoded) {
        List<LatLng> poly = new java.util.ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;
        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            double latD = lat / 1E5;
            double lngD = lng / 1E5;
            poly.add(new LatLng(latD, lngD));
        }
        return poly;
    }
    
    /**
     * Destaca el segmento activo en el mapa
     */
    private void highlightActiveSegment(RouteSegment segment) {
        if (googleMap == null || segment == null) {
            return;
        }
        
        // En una implementación real, aquí destacaríamos visualmente el segmento activo
        // Por ejemplo, cambiando su grosor o color
        
        // Por ahora, simplemente centramos el mapa en el segmento
        centerMapOnSegment(segment);
    }
    
    /**
     * Centra el mapa en un segmento específico
     */
    private void centerMapOnSegment(RouteSegment segment) {
        if (googleMap == null || segment == null) {
            return;
        }
        
        Location start = segment.getStartLocation();
        Location end = segment.getEndLocation();
        
        if (start != null && end != null) {
            LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
            boundsBuilder.include(new LatLng(start.getLatitude(), start.getLongitude()));
            boundsBuilder.include(new LatLng(end.getLatitude(), end.getLongitude()));
            
            try {
                LatLngBounds bounds = boundsBuilder.build();
                googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
                
                // Desactivar seguimiento automático
                followUserLocation = false;
            } catch (IllegalStateException e) {
                // Error al construir los bounds
            }
        }
    }
    
    /**
     * Actualiza el marcador de posición del usuario
     */
    private void updateUserMarker(android.location.Location userLocation) {
        if (googleMap == null || userLocation == null) {
            return;
        }
        
        LatLng position = new LatLng(userLocation.getLatitude(), userLocation.getLongitude());
        
        // Limpiar marcadores anteriores y agregar el nuevo
        googleMap.addMarker(new MarkerOptions()
                .position(position)
                .title(getString(R.string.current_location))
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
        
        // Si estamos siguiendo al usuario, centrar el mapa en su posición
        if (followUserLocation) {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, ZOOM_LEVEL));
        }
    }
    
    /**
     * Centra el mapa en la ubicación actual del usuario
     */
    private void centerMapOnLocation(android.location.Location location) {
        if (googleMap != null && location != null) {
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, ZOOM_LEVEL));
        }
    }

    /**
     * Activa un modo de cámara similar a navegación: centra en usuario, aplica bearing y tilt.
     */
    private void enableNavigationCamera() {
        if (googleMap == null) return;
        try {
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location == null) return;
                LatLng pos = new LatLng(location.getLatitude(), location.getLongitude());
                float bearing = location.hasBearing() ? location.getBearing() : googleMap.getCameraPosition().bearing;
                com.google.android.gms.maps.model.CameraPosition cam = new com.google.android.gms.maps.model.CameraPosition.Builder()
                        .target(pos)
                        .zoom(Math.max(googleMap.getCameraPosition().zoom, ZOOM_LEVEL))
                        .bearing(bearing)
                        .tilt(45f)
                        .build();
                googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cam));
            });
        } catch (SecurityException ignored) { }
    }
    
    /**
     * Muestra la ubicación actual del usuario
     */
    private void showMyLocation() {
        if (!hasLocationPermission()) {
            requestLocationPermission();
            return;
        }
        
        try {
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    centerMapOnLocation(location);
                }
            });
        } catch (SecurityException e) {
            Toast.makeText(requireContext(), R.string.error_location_permission, Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Comprueba si tenemos permisos de ubicación
     */
    private boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
    
    /**
     * Solicita permisos de ubicación
     */
    private void requestLocationPermission() {
        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
    }
    
    /**
     * Habilita la ubicación del usuario en el mapa
     */
    private void enableMyLocation() {
        if (googleMap != null && hasLocationPermission()) {
            try {
                googleMap.setMyLocationEnabled(true);
                googleMap.getUiSettings().setMyLocationButtonEnabled(false); // Usamos nuestro propio botón
                
                // Iniciar actualizaciones de ubicación
                startLocationUpdates();
            } catch (SecurityException e) {
                Toast.makeText(requireContext(), R.string.error_location_permission, Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    /**
     * Inicia las actualizaciones de ubicación
     */
    private void startLocationUpdates() {
        if (!hasLocationPermission()) {
            return;
        }
        
        try {
            LocationRequest locationRequest = LocationRequest.create()
                    .setInterval(LOCATION_UPDATE_INTERVAL)
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        } catch (SecurityException e) {
            Toast.makeText(requireContext(), R.string.error_location_permission, Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Detiene las actualizaciones de ubicación
     */
    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation();
            } else {
                Toast.makeText(requireContext(), R.string.error_location_permission, Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        if (hasLocationPermission()) {
            startLocationUpdates();
        }
    }
    
    @Override
    public void onPause() {
        super.onPause();
        stopLocationUpdates();
    }
} 