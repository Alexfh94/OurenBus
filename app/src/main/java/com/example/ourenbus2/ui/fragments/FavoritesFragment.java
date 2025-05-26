package com.example.ourenbus2.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ourenbus2.R;
import com.example.ourenbus2.model.Route;
import com.example.ourenbus2.ui.adapter.FavoriteRoutesAdapter;
import com.example.ourenbus2.ui.viewmodel.FavoriteRoutesViewModel;

import java.util.List;

/**
 * Fragmento para mostrar y gestionar las rutas favoritas
 */
public class FavoritesFragment extends Fragment implements FavoriteRoutesAdapter.OnRouteClickListener {

    private FavoriteRoutesViewModel viewModel;
    private RecyclerView rvFavorites;
    private TextView tvNoFavorites;
    private FavoriteRoutesAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_favorites, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Referencias a vistas
        rvFavorites = view.findViewById(R.id.rv_favorites);
        tvNoFavorites = view.findViewById(R.id.tv_no_favorites);
        
        // Inicializar ViewModel
        viewModel = new ViewModelProvider(requireActivity()).get(FavoriteRoutesViewModel.class);
        
        // Configurar RecyclerView
        setupRecyclerView();
        
        // Observar cambios en las rutas favoritas
        viewModel.getFavoriteRoutes().observe(getViewLifecycleOwner(), this::updateUI);
    }
    
    private void setupRecyclerView() {
        adapter = new FavoriteRoutesAdapter(this);
        rvFavorites.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvFavorites.setAdapter(adapter);
    }
    
    private void updateUI(List<Route> routes) {
        if (routes != null && !routes.isEmpty()) {
            adapter.submitList(routes);
            tvNoFavorites.setVisibility(View.GONE);
            rvFavorites.setVisibility(View.VISIBLE);
        } else {
            tvNoFavorites.setVisibility(View.VISIBLE);
            rvFavorites.setVisibility(View.GONE);
        }
    }
    
    @Override
    public void onRouteClicked(Route route) {
        // Navegar a la actividad principal con esta ruta seleccionada
        viewModel.selectRoute(route);
        requireActivity().getSupportFragmentManager().popBackStack();
    }
    
    @Override
    public void onRouteDeleteClicked(Route route) {
        // Eliminar ruta de favoritos
        viewModel.deleteRoute(route);
    }
} 