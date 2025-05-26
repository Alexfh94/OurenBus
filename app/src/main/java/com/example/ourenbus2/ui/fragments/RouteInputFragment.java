package com.example.ourenbus2.ui.fragments;

import android.os.Bundle;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ourenbus2.R;
import com.example.ourenbus2.model.Location;
import com.example.ourenbus2.service.LocationService;
import com.example.ourenbus2.ui.adapter.LocationSuggestionsAdapter;
import com.example.ourenbus2.ui.viewmodel.RouteViewModel;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

/**
 * Fragmento para ingresar origen y destino de la ruta
 */
public class RouteInputFragment extends Fragment implements LocationSuggestionsAdapter.OnLocationSelectedListener {

    private RouteViewModel viewModel;
    private LocationService locationService;
    
    private TextInputEditText etOrigin;
    private TextInputEditText etDestination;
    private CheckBox cbUseCurrentLocation;
    private Button btnSearchRoute;
    private RecyclerView rvLocationSuggestions;
    
    private LocationSuggestionsAdapter suggestionsAdapter;
    
    private boolean isSearchingOrigin = true; // true = buscando origen, false = buscando destino
    
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
        
        // Inicializar ViewModel
        viewModel = new ViewModelProvider(requireActivity()).get(RouteViewModel.class);
        
        // Inicializar vistas
        etOrigin = view.findViewById(R.id.et_origin);
        etDestination = view.findViewById(R.id.et_destination);
        cbUseCurrentLocation = view.findViewById(R.id.cb_use_current_location);
        btnSearchRoute = view.findViewById(R.id.btn_search_route);
        rvLocationSuggestions = view.findViewById(R.id.rv_location_suggestions);
        
        // Configurar recycler view
        rvLocationSuggestions.setLayoutManager(new LinearLayoutManager(requireContext()));
        suggestionsAdapter = new LocationSuggestionsAdapter(requireContext(), this);
        rvLocationSuggestions.setAdapter(suggestionsAdapter);
        
        // Configurar listeners
        setupListeners();
        
        // Observar ViewModel
        observeViewModel();
    }
    
    private void setupListeners() {
        // TextWatcher para origen
        etOrigin.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No se requiere implementación
            }
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // No se requiere implementación
            }
            
            @Override
            public void afterTextChanged(Editable s) {
                if (!cbUseCurrentLocation.isChecked()) {
                    isSearchingOrigin = true;
                    if (s.length() > 2) {
                        viewModel.searchLocationSuggestions(s.toString());
                        rvLocationSuggestions.setVisibility(View.VISIBLE);
                    } else {
                        rvLocationSuggestions.setVisibility(View.GONE);
                    }
                }
            }
        });
        
        // TextWatcher para destino
        etDestination.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No se requiere implementación
            }
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // No se requiere implementación
            }
            
            @Override
            public void afterTextChanged(Editable s) {
                isSearchingOrigin = false;
                if (s.length() > 2) {
                    viewModel.searchLocationSuggestions(s.toString());
                    rvLocationSuggestions.setVisibility(View.VISIBLE);
                } else {
                    rvLocationSuggestions.setVisibility(View.GONE);
                }
            }
        });
        
        // Checkbox para usar ubicación actual
        cbUseCurrentLocation.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                etOrigin.setText(getString(R.string.current_location));
                etOrigin.setEnabled(false);
                locationService.getLastLocation();
                
                // Si tenemos la ubicación actual, la usamos como origen
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
            } else {
                etOrigin.setText("");
                etOrigin.setEnabled(true);
                viewModel.setOrigin(null);
            }
        });
        
        // Botón para buscar ruta
        btnSearchRoute.setOnClickListener(v -> {
            String originText = etOrigin.getText().toString().trim();
            String destText = etDestination.getText().toString().trim();
            
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
                        Toast.makeText(requireContext(), getString(R.string.error_select_destination), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(requireContext(), getString(R.string.error_location_not_available), Toast.LENGTH_SHORT).show();
                }
            } else {
                Location origin = viewModel.getOrigin().getValue();
                Location destination = viewModel.getDestination().getValue();
                
                if (origin == null) {
                    Toast.makeText(requireContext(), getString(R.string.error_select_origin), Toast.LENGTH_SHORT).show();
                    return;
                }
                
                if (destination == null) {
                    Toast.makeText(requireContext(), getString(R.string.error_select_destination), Toast.LENGTH_SHORT).show();
                    return;
                }
                
                viewModel.searchRoute(origin, destination);
                Toast.makeText(requireContext(), getString(R.string.searching_route), Toast.LENGTH_SHORT).show();
            }
            
            // Ocultar sugerencias
            rvLocationSuggestions.setVisibility(View.GONE);
        });
        
        // Perder foco al hacer clic fuera de los campos de texto
        View rootView = getView();
        if (rootView != null) {
            rootView.setOnClickListener(v -> {
                etOrigin.clearFocus();
                etDestination.clearFocus();
                rvLocationSuggestions.setVisibility(View.GONE);
            });
        }
    }
    
    private void observeViewModel() {
        // Observar sugerencias
        viewModel.getLocationSuggestions().observe(getViewLifecycleOwner(), this::updateSuggestions);
        
        // Observar origen y destino
        viewModel.getOrigin().observe(getViewLifecycleOwner(), origin -> {
            if (origin != null && !cbUseCurrentLocation.isChecked()) {
                etOrigin.setText(origin.getName());
            }
        });
        
        viewModel.getDestination().observe(getViewLifecycleOwner(), destination -> {
            if (destination != null) {
                etDestination.setText(destination.getName());
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
                // La ruta se ha encontrado
                Toast.makeText(requireContext(), 
                        getString(R.string.route_found, route.getEstimatedTimeInMinutes()), 
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * Actualiza las sugerencias de ubicación
     */
    private void updateSuggestions(List<Location> suggestions) {
        if (suggestions != null && !suggestions.isEmpty()) {
            suggestionsAdapter.updateLocations(suggestions);
            rvLocationSuggestions.setVisibility(View.VISIBLE);
        } else {
            rvLocationSuggestions.setVisibility(View.GONE);
        }
    }
    
    @Override
    public void onLocationSelected(Location location) {
        if (isSearchingOrigin) {
            viewModel.setOrigin(location);
            etOrigin.clearFocus();
        } else {
            viewModel.setDestination(location);
            etDestination.clearFocus();
        }
        rvLocationSuggestions.setVisibility(View.GONE);
    }
} 