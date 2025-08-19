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
        
        fabZoomIn.setOnClickListener(v -> { if (googleMap != null) googleMap.animateCamera(CameraUpdateFactory.zoomIn()); });
        fabZoomOut.setOnClickListener(v -> { if (googleMap != null) googleMap.animateCamera(CameraUpdateFactory.zoomOut()); });
    }
    
    private void observeViewModel() {
        viewModel.getCurrentRoute().observe(getViewLifecycleOwner(), this::drawRoute);
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
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        googleMap.getUiSettings().setZoomControlsEnabled(false);
        googleMap.getUiSettings().setCompassEnabled(true);
        LatLng ourense = new LatLng(42.3402, -7.8636);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ourense, 14));
        requestLocationPermission();
        Route currentRoute = viewModel.getCurrentRoute().getValue();
        if (currentRoute != null) drawRoute(currentRoute);
    }
    
    private void requestLocationPermission() {
        locationService.requestLocationPermission(requireActivity());
        locationService.isLocationPermissionGranted().observe(getViewLifecycleOwner(), granted -> {
            if (googleMap != null) {
                try { googleMap.setMyLocationEnabled(granted); } catch (SecurityException ignored) {}
            }
        });
    }
    
    private void drawRoute(Route route) {
        if (googleMap == null || route == null || !route.isValid()) return;
        googleMap.clear();
        LatLng originLatLng = new LatLng(route.getOrigin().getLatitude(), route.getOrigin().getLongitude());
        LatLng destLatLng = new LatLng(route.getDestination().getLatitude(), route.getDestination().getLongitude());
        googleMap.addMarker(new MarkerOptions().position(originLatLng).title(route.getOrigin().getName()).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
        googleMap.addMarker(new MarkerOptions().position(destLatLng).title(route.getDestination().getName()).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        List<LatLng> allPoints = new ArrayList<>();
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        for (RouteSegment segment : route.getSegments()) {
            // No dibujar líneas para segmentos de espera
            if (segment.getType() == RouteSegment.SegmentType.WAIT) {
                continue;
            }
            int color;
            if (segment.getType() == RouteSegment.SegmentType.WALKING) {
                color = getResources().getColor(R.color.route_walk, null);
            } else if (segment.getType() == RouteSegment.SegmentType.BUS && segment.getBusLine() != null && segment.getBusLine().getColor() != null) {
                try { color = android.graphics.Color.parseColor(segment.getBusLine().getColor()); }
                catch (Exception e) { color = getResources().getColor(R.color.primary, null); }
            } else {
                color = getResources().getColor(R.color.primary, null);
            }
            List<LatLng> polyPoints = null;
            if (segment.getPolylineEncoded() != null && !segment.getPolylineEncoded().isEmpty()) {
                polyPoints = decodePolyline(segment.getPolylineEncoded());
            }
            if (polyPoints != null && polyPoints.size() > 1) {
                googleMap.addPolyline(new PolylineOptions().addAll(polyPoints).width(10).color(color));
                for (LatLng p : polyPoints) boundsBuilder.include(p);
                allPoints.addAll(polyPoints);
            } else if (segment.getStartLocation() != null && segment.getEndLocation() != null) {
                LatLng start = new LatLng(segment.getStartLocation().getLatitude(), segment.getStartLocation().getLongitude());
                LatLng end = new LatLng(segment.getEndLocation().getLatitude(), segment.getEndLocation().getLongitude());
                googleMap.addPolyline(new PolylineOptions().add(start, end).width(10).color(color));
                allPoints.add(start); allPoints.add(end);
                boundsBuilder.include(start); boundsBuilder.include(end);
            }
            if (segment.getType() == RouteSegment.SegmentType.BUS && segment.getBusStop() != null) {
                LatLng busStopLatLng = new LatLng(segment.getBusStop().getLatitude(), segment.getBusStop().getLongitude());
                googleMap.addMarker(new MarkerOptions().position(busStopLatLng).title(segment.getBusStop().getName()).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
            }
        }
        if (!allPoints.isEmpty()) {
            LatLngBounds bounds = boundsBuilder.build();
            googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
        }
    }

    // Decodificador local de polilíneas codificadas (formato Google)
    private List<LatLng> decodePolyline(String encoded) {
        List<LatLng> poly = new ArrayList<>();
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
} 