package com.example.ourenbus2.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.ourenbus2.R;
import com.example.ourenbus2.model.Route;
import com.example.ourenbus2.model.RouteSegment;
import com.example.ourenbus2.service.LocationService;
import com.example.ourenbus2.ui.viewmodel.RouteViewModel;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Fragmento que muestra el mapa con la ruta
 */
public class MapFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap googleMap;
    private RouteViewModel viewModel;
    private LocationService locationService;
    private FloatingActionButton fabMyLocation, fabZoomIn, fabZoomOut;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Inicializar servicios
        locationService = LocationService.getInstance(requireContext());
        
        // Inicializar ViewModel
        viewModel = new ViewModelProvider(requireActivity()).get(RouteViewModel.class);
        
        // Inicializar vistas
        fabMyLocation = view.findViewById(R.id.fab_my_location);
        fabZoomIn = view.findViewById(R.id.fab_zoom_in);
        fabZoomOut = view.findViewById(R.id.fab_zoom_out);
        
        // Configurar mapa
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        
        // Configurar listeners
        setupListeners();
        
        // Observar cambios en ViewModel
        observeViewModel();
    }
    
    private void setupListeners() {
        fabMyLocation.setOnClickListener(v -> {
            if (googleMap != null) {
                locationService.getLastLocation();
            }
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
    
    private void observeViewModel() {
        // Observar la ruta actual
        viewModel.getCurrentRoute().observe(getViewLifecycleOwner(), this::drawRoute);
        
        // Observar la ubicación actual
        locationService.getLocationLiveData().observe(getViewLifecycleOwner(), location -> {
            if (googleMap != null && location != null) {
                LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        this.googleMap = map;
        
        // Configurar mapa
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        googleMap.getUiSettings().setZoomControlsEnabled(false);
        googleMap.getUiSettings().setCompassEnabled(true);
        
        // Centrar en Ourense por defecto
        LatLng ourense = new LatLng(42.3402, -7.8636);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ourense, 14));
        
        // Verificar permisos de ubicación
        requestLocationPermission();
        
        // Dibujar ruta actual si existe
        Route currentRoute = viewModel.getCurrentRoute().getValue();
        if (currentRoute != null) {
            drawRoute(currentRoute);
        }
    }
    
    private void requestLocationPermission() {
        locationService.requestLocationPermission(requireActivity());
        
        locationService.isLocationPermissionGranted().observe(getViewLifecycleOwner(), granted -> {
            if (googleMap != null) {
                try {
                    googleMap.setMyLocationEnabled(granted);
                } catch (SecurityException e) {
                    e.printStackTrace();
                }
            }
        });
    }
    
    /**
     * Dibuja la ruta en el mapa
     */
    private void drawRoute(Route route) {
        if (googleMap == null || route == null || !route.isValid()) {
            return;
        }
        
        // Limpiar mapa
        googleMap.clear();
        
        // Marcadores de origen y destino
        LatLng originLatLng = new LatLng(route.getOrigin().getLatitude(), route.getOrigin().getLongitude());
        LatLng destLatLng = new LatLng(route.getDestination().getLatitude(), route.getDestination().getLongitude());
        
        googleMap.addMarker(new MarkerOptions()
                .position(originLatLng)
                .title(route.getOrigin().getName())
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
        
        googleMap.addMarker(new MarkerOptions()
                .position(destLatLng)
                .title(route.getDestination().getName())
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        
        // Dibujar segmentos
        List<LatLng> allPoints = new ArrayList<>();
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        
        for (RouteSegment segment : route.getSegments()) {
            // Añadir puntos al bounds
            LatLng segmentStart = new LatLng(segment.getStartLocation().getLatitude(), segment.getStartLocation().getLongitude());
            LatLng segmentEnd = new LatLng(segment.getEndLocation().getLatitude(), segment.getEndLocation().getLongitude());
            
            allPoints.add(segmentStart);
            allPoints.add(segmentEnd);
            
            boundsBuilder.include(segmentStart);
            boundsBuilder.include(segmentEnd);
            
            // Dibujar línea según tipo
            int color;
            if (segment.getType() == RouteSegment.SegmentType.WALKING) {
                color = getResources().getColor(R.color.route_walk, null);
            } else if (segment.getType() == RouteSegment.SegmentType.BUS && segment.getBusLine() != null) {
                // Determinar color según línea
                int lineNumber = segment.getBusLine().getLineNumber();
                switch (lineNumber) {
                    case 1:
                        color = getResources().getColor(R.color.route_bus_1, null);
                        break;
                    case 2:
                        color = getResources().getColor(R.color.route_bus_2, null);
                        break;
                    case 3:
                        color = getResources().getColor(R.color.route_bus_3, null);
                        break;
                    case 4:
                        color = getResources().getColor(R.color.route_bus_4, null);
                        break;
                    case 5:
                        color = getResources().getColor(R.color.route_bus_5, null);
                        break;
                    default:
                        color = getResources().getColor(R.color.primary, null);
                        break;
                }
            } else {
                color = getResources().getColor(R.color.primary, null);
            }
            
            // Dibujar línea
            googleMap.addPolyline(new PolylineOptions()
                    .add(segmentStart, segmentEnd)
                    .width(10)
                    .color(color));
            
            // Añadir marcador para paradas de bus
            if (segment.getType() == RouteSegment.SegmentType.BUS && segment.getBusStop() != null) {
                LatLng busStopLatLng = new LatLng(segment.getBusStop().getLatitude(), segment.getBusStop().getLongitude());
                googleMap.addMarker(new MarkerOptions()
                        .position(busStopLatLng)
                        .title(segment.getBusStop().getName())
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
            }
        }
        
        // Ajustar cámara para mostrar toda la ruta
        if (!allPoints.isEmpty()) {
            LatLngBounds bounds = boundsBuilder.build();
            googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
        }
    }
} 