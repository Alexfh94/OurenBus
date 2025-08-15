package com.example.ourenbus2.ui.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.ourenbus2.R;
import com.example.ourenbus2.model.Location;
import com.example.ourenbus2.service.LocationService;
import com.example.ourenbus2.service.PlacesHttpService;
import com.example.ourenbus2.ui.viewmodel.RouteViewModel;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import java.util.ArrayList;
import java.util.List;

import android.widget.ArrayAdapter;

/**
 * Fragmento para ingresar origen y destino de la ruta
 */
public class RouteInputFragment extends Fragment {

    private RouteViewModel viewModel;
    private LocationService locationService;
    private PlacesHttpService placesService;
    
    private MaterialAutoCompleteTextView etOrigin;
    private MaterialAutoCompleteTextView etDestination;
    private CheckBox cbUseCurrentLocation;
    private Button btnSearchRoute;
    
    // Sugerencias
    private final List<Location> originSuggestions = new ArrayList<>();
    private final List<Location> destinationSuggestions = new ArrayList<>();
    private ArrayAdapter<String> originAdapter;
    private ArrayAdapter<String> destinationAdapter;
    
    // Autocomplete HTTP
    private String sessionToken;
    private final List<PlacesHttpService.Prediction> originPredictions = new ArrayList<>();
    private final List<PlacesHttpService.Prediction> destinationPredictions = new ArrayList<>();
    
    private boolean isSearchingOrigin = true; // true = buscando origen, false = buscando destino
    private boolean suppressTextChange = false;
    private long suppressDropdownUntilMs = 0L;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable pendingOriginQuery;
    private Runnable pendingDestinationQuery;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_route_input, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Inicializar servicios
        locationService = LocationService.getInstance(requireContext());
        placesService = new PlacesHttpService();
        sessionToken = PlacesHttpService.newSessionToken();
        
        // Inicializar ViewModel
        viewModel = new ViewModelProvider(requireActivity()).get(RouteViewModel.class);
        
        // Inicializar vistas
        etOrigin = view.findViewById(R.id.et_origin);
        etDestination = view.findViewById(R.id.et_destination);
        cbUseCurrentLocation = view.findViewById(R.id.cb_use_current_location);
        btnSearchRoute = view.findViewById(R.id.btn_search_route);
        
        // Configurar adapters de dropdown
        originAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, new ArrayList<>());
        destinationAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, new ArrayList<>());
        etOrigin.setAdapter(originAdapter);
        etDestination.setAdapter(destinationAdapter);
        etOrigin.setThreshold(1);
        etDestination.setThreshold(1);
        
        // Abrir dropdown al enfocar si ya hay datos
        // Evitar abrir automáticamente el desplegable al enfocar para que no se reprograme tras seleccionar
        etOrigin.setOnFocusChangeListener((v, hasFocus) -> { /* no-op */ });
        etDestination.setOnFocusChangeListener((v, hasFocus) -> { /* no-op */ });
        
        // Configurar listeners
        setupListeners();
        
        // Observar ViewModel
        observeViewModel();
    }
    
    private String getApiKey() {
        try {
            android.content.pm.ApplicationInfo ai = requireContext().getPackageManager().getApplicationInfo(requireContext().getPackageName(), android.content.pm.PackageManager.GET_META_DATA);
            Object value = ai.metaData != null ? ai.metaData.get("com.google.android.geo.API_KEY") : null;
            return value != null ? value.toString() : null;
        } catch (Exception e) { return null; }
    }
    
    private void setupListeners() {
        // TextWatcher para origen
        etOrigin.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override
            public void afterTextChanged(Editable s) {
                if (suppressTextChange || cbUseCurrentLocation.isChecked()) return;
                if (System.currentTimeMillis() < suppressDropdownUntilMs) return; // no re-lanzar aún
                isSearchingOrigin = true;
                if (pendingOriginQuery != null) handler.removeCallbacks(pendingOriginQuery);
                String q = s != null ? s.toString() : "";
                pendingOriginQuery = () -> performAutocompleteHttp(q, true);
                handler.postDelayed(pendingOriginQuery, 250);
            }
        });
        
        // TextWatcher para destino
        etDestination.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override
            public void afterTextChanged(Editable s) {
                if (suppressTextChange) return;
                if (System.currentTimeMillis() < suppressDropdownUntilMs) return;
                isSearchingOrigin = false;
                if (pendingDestinationQuery != null) handler.removeCallbacks(pendingDestinationQuery);
                String q = s != null ? s.toString() : "";
                pendingDestinationQuery = () -> performAutocompleteHttp(q, false);
                handler.postDelayed(pendingDestinationQuery, 250);
            }
        });
        
        // Selección en dropdown de origen
        etOrigin.setOnItemClickListener((parent, v, position, id) -> {
            suppressTextChange = true;
            suppressDropdownUntilMs = System.currentTimeMillis() + 600; // evitar relanzar dropdown tras seleccionar
            if (position >= 0 && position < originPredictions.size()) {
                etOrigin.dismissDropDown();
                fetchPlaceAndSetHttp(originPredictions.get(position).placeId, true);
            } else if (position >= 0 && position < originSuggestions.size()) {
                etOrigin.dismissDropDown();
                viewModel.setOrigin(originSuggestions.get(position));
            }
            handler.postDelayed(() -> suppressTextChange = false, 150);
        });
        
        // Selección en dropdown de destino
        etDestination.setOnItemClickListener((parent, v, position, id) -> {
            suppressTextChange = true;
            suppressDropdownUntilMs = System.currentTimeMillis() + 600;
            if (position >= 0 && position < destinationPredictions.size()) {
                etDestination.dismissDropDown();
                fetchPlaceAndSetHttp(destinationPredictions.get(position).placeId, false);
            } else if (position >= 0 && position < destinationSuggestions.size()) {
                etDestination.dismissDropDown();
                viewModel.setDestination(destinationSuggestions.get(position));
            }
            handler.postDelayed(() -> suppressTextChange = false, 150);
        });
        
        // Checkbox para usar ubicación actual
        cbUseCurrentLocation.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                suppressTextChange = true;
                etOrigin.setText(getString(R.string.current_location));
                etOrigin.setEnabled(false);
                etOrigin.dismissDropDown();
                locationService.getLastLocation();
                
                android.location.Location currentLoc = locationService.getLastKnownLocation();
                if (currentLoc != null) {
                    Location origin = new Location(
                            getString(R.string.current_location),
                            getString(R.string.current_location),
                            currentLoc.getLatitude(),
                            currentLoc.getLongitude()
                    );
                    viewModel.setOrigin(origin);
                }
                handler.postDelayed(() -> suppressTextChange = false, 100);
            } else {
                etOrigin.setText("");
                etOrigin.setEnabled(true);
                viewModel.setOrigin(null);
            }
        });
        
        // Botón para buscar ruta
        btnSearchRoute.setOnClickListener(v -> {
            String originText = etOrigin.getText() != null ? etOrigin.getText().toString().trim() : "";
            String destText = etDestination.getText() != null ? etDestination.getText().toString().trim() : "";
            
            if (originText.isEmpty()) {
                Toast.makeText(requireContext(), getString(R.string.error_origin_empty), Toast.LENGTH_SHORT).show();
                return;
            }
            if (destText.isEmpty()) {
                Toast.makeText(requireContext(), getString(R.string.error_destination_empty), Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (cbUseCurrentLocation.isChecked()) {
                android.location.Location currentLoc = locationService.getLastKnownLocation();
                if (currentLoc != null) {
                    Location origin = new Location(
                            getString(R.string.current_location),
                            getString(R.string.current_location),
                            currentLoc.getLatitude(),
                            currentLoc.getLongitude()
                    );
                    viewModel.setOrigin(origin);
                    
                    Location destination = viewModel.getDestination().getValue();
                    if (destination != null) {
                        viewModel.searchRoute(origin, destination);
                        Toast.makeText(requireContext(), getString(R.string.searching_route), Toast.LENGTH_SHORT).show();
                    } else {
                        // Intentar resolver el destino por texto con Places (Find Place)
                        resolveAndSearchByText(origin, null, destText);
                    }
                } else {
                    Toast.makeText(requireContext(), getString(R.string.error_location_not_available), Toast.LENGTH_SHORT).show();
                }
            } else {
                Location origin = viewModel.getOrigin().getValue();
                Location destination = viewModel.getDestination().getValue();
                if (origin != null && destination != null) {
                    viewModel.searchRoute(origin, destination);
                    Toast.makeText(requireContext(), getString(R.string.searching_route), Toast.LENGTH_SHORT).show();
                } else {
                    // Resolver cualquiera que falte mediante Places Find Place
                    resolveAndSearchByText(null, originText, destText);
                }
            }
        });
    }

    private void resolveAndSearchByText(@Nullable Location knownOrigin, @Nullable String originText, @NonNull String destText) {
        String apiKey = getApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.error_select_destination), Toast.LENGTH_SHORT).show();
            return;
        }
        android.location.Location bias = locationService.getLastKnownLocation();
        double lat = 42.3402, lng = -7.8636;
        if (bias != null) { lat = bias.getLatitude(); lng = bias.getLongitude(); }
        final double latF = lat;
        final double lngF = lng;
        new Thread(() -> {
            try {
                Location finalOrigin = knownOrigin;
                if (finalOrigin == null && originText != null && !originText.trim().isEmpty() && !cbUseCurrentLocation.isChecked()) {
                    Location loc = placesService.findPlaceFromText(apiKey, originText, latF, lngF);
                    finalOrigin = loc;
                }
                Location finalDestination = viewModel.getDestination().getValue();
                if (finalDestination == null && destText != null && !destText.trim().isEmpty()) {
                    Location loc = placesService.findPlaceFromText(apiKey, destText, latF, lngF);
                    finalDestination = loc;
                }
                Location originResolved = finalOrigin;
                Location destResolved = finalDestination;
                requireActivity().runOnUiThread(() -> {
                    if (originResolved != null) viewModel.setOrigin(originResolved);
                    if (destResolved != null) viewModel.setDestination(destResolved);
                    if (originResolved != null && destResolved != null) {
                        viewModel.searchRoute(originResolved, destResolved);
                        Toast.makeText(requireContext(), getString(R.string.searching_route), Toast.LENGTH_SHORT).show();
                    } else {
                        if (originResolved == null) Toast.makeText(requireContext(), getString(R.string.error_select_origin), Toast.LENGTH_SHORT).show();
                        if (destResolved == null) Toast.makeText(requireContext(), getString(R.string.error_select_destination), Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception ignored) {}
        }).start();
    }
    
    private void performAutocompleteHttp(String query, boolean forOrigin) {
        if (query == null || query.trim().isEmpty()) {
            if (forOrigin) { originPredictions.clear(); originAdapter.clear(); }
            else { destinationPredictions.clear(); destinationAdapter.clear(); }
            return;
        }
        String apiKey = getApiKey();
        if (apiKey == null || apiKey.isEmpty()) { viewModel.searchLocationSuggestions(query); return; }
        
        // Usar ubicación actual si está disponible, si no, centro de Ourense
        android.location.Location loc = locationService.getLastKnownLocation();
        double lat = 42.3402, lng = -7.8636;
        if (loc != null) { lat = loc.getLatitude(); lng = loc.getLongitude(); }
        final double latF = lat;
        final double lngF = lng;
        
        new Thread(() -> {
            try {
                List<PlacesHttpService.Prediction> preds = placesService.autocomplete(apiKey, query, latF, lngF, sessionToken);
                List<String> display = new ArrayList<>();
                if (forOrigin) {
                    originPredictions.clear();
                    originPredictions.addAll(preds);
                    for (PlacesHttpService.Prediction p : originPredictions) display.add(p.toString());
                    requireActivity().runOnUiThread(() -> {
                        if (display.isEmpty()) {
                            // Fallback inmediato si no hay predicciones
                            viewModel.searchLocationSuggestions(query);
                        } else {
                            originAdapter.clear(); originAdapter.addAll(display); etOrigin.showDropDown();
                        }
                    });
                } else {
                    destinationPredictions.clear();
                    destinationPredictions.addAll(preds);
                    for (PlacesHttpService.Prediction p : destinationPredictions) display.add(p.toString());
                    requireActivity().runOnUiThread(() -> {
                        if (display.isEmpty()) {
                            viewModel.searchLocationSuggestions(query);
                        } else {
                            destinationAdapter.clear(); destinationAdapter.addAll(display); etDestination.showDropDown();
                        }
                    });
                }
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> viewModel.searchLocationSuggestions(query));
            }
        }).start();
    }
    
    private void fetchPlaceAndSetHttp(String placeId, boolean forOrigin) {
        String apiKey = getApiKey(); if (apiKey == null || apiKey.isEmpty()) return;
        new Thread(() -> {
            Location loc = null;
            try { loc = placesService.fetchPlaceDetails(apiKey, placeId); } catch (Exception ignored) {}
            Location finalLoc = loc;
            requireActivity().runOnUiThread(() -> {
                if (finalLoc != null) {
                    if (forOrigin) viewModel.setOrigin(finalLoc); else viewModel.setDestination(finalLoc);
                }
            });
        }).start();
    }
    
    private void observeViewModel() {
        // Fallback local
        viewModel.getLocationSuggestions().observe(getViewLifecycleOwner(), suggestions -> {
            List<String> displayList = new ArrayList<>();
            if (suggestions != null) {
                for (Location loc : suggestions) {
                    String title = loc.getName() != null ? loc.getName() : "";
                    String desc = loc.getDescription() != null ? loc.getDescription() : "";
                    displayList.add(desc.isEmpty() ? title : (title + " – " + desc));
                }
            }
            boolean allowDropdown = System.currentTimeMillis() >= suppressDropdownUntilMs;
            if (isSearchingOrigin) {
                originSuggestions.clear();
                if (suggestions != null) originSuggestions.addAll(suggestions);
                originAdapter.clear(); originAdapter.addAll(displayList);
                if (allowDropdown && etOrigin.hasFocus()) etOrigin.post(etOrigin::showDropDown);
            } else {
                destinationSuggestions.clear();
                if (suggestions != null) destinationSuggestions.addAll(suggestions);
                destinationAdapter.clear(); destinationAdapter.addAll(displayList);
                if (allowDropdown && etDestination.hasFocus()) etDestination.post(etDestination::showDropDown);
            }
        });
        
        // Observar origen y destino
        viewModel.getOrigin().observe(getViewLifecycleOwner(), origin -> {
            if (origin != null && !cbUseCurrentLocation.isChecked()) {
                suppressTextChange = true;
                etOrigin.setText(origin.getName());
                suppressTextChange = false;
            }
        });
        
        viewModel.getDestination().observe(getViewLifecycleOwner(), destination -> {
            if (destination != null) {
                suppressTextChange = true;
                etDestination.setText(destination.getName());
                suppressTextChange = false;
            }
        });
        
        // Observar ubicación actual
        locationService.getLocationLiveData().observe(getViewLifecycleOwner(), location -> {
            if (location != null && cbUseCurrentLocation.isChecked()) {
                Location currentLocation = new Location(
                        getString(R.string.current_location),
                        getString(R.string.current_location),
                        location.getLatitude(),
                        location.getLongitude()
                );
                viewModel.updateCurrentLocation(currentLocation);
                viewModel.setOrigin(currentLocation);
            }
        });
        
        // Observar estado de carga de ruta
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            btnSearchRoute.setEnabled(!isLoading);
            btnSearchRoute.setText(isLoading ? R.string.searching : R.string.search_route);
        });
        
        // Observar ruta actual
        viewModel.getCurrentRoute().observe(getViewLifecycleOwner(), route -> {
            if (route != null) {
                Toast.makeText(requireContext(), 
                        getString(R.string.route_found, route.getEstimatedTimeInMinutes()), 
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
} 