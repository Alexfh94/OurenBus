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
import com.example.ourenbus2.model.RouteSegment;
import com.example.ourenbus2.ui.adapter.NavigationStepsAdapter;
import com.example.ourenbus2.util.NavigationSession;
import com.example.ourenbus2.ui.viewmodel.NavigationViewModel;

/**
 * Fragmento que muestra la lista de pasos a seguir durante la navegación en tiempo real
 */
public class NavigationStepsFragment extends Fragment {

    private NavigationViewModel viewModel;
    private RecyclerView rvSteps;
    private NavigationStepsAdapter adapter;
    private TextView tvNoSteps;
    private View loadingView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_navigation_steps, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Inicializar ViewModel
        viewModel = new ViewModelProvider(requireActivity()).get(NavigationViewModel.class);
        
        // Referencias a vistas
        rvSteps = view.findViewById(R.id.rv_steps);
        tvNoSteps = view.findViewById(R.id.tv_no_steps);
        loadingView = view.findViewById(R.id.loading_view);
        
        // Configurar RecyclerView
        setupRecyclerView();
        
        // Observar cambios del ViewModel
        observeViewModel();
    }
    
    private void setupRecyclerView() {
        adapter = new NavigationStepsAdapter(segment -> {
            // Cuando se selecciona un paso, centramos el mapa en ese segmento
            viewModel.setSelectedSegment(segment);
        });
        
        rvSteps.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvSteps.setAdapter(adapter);
    }
    
    private void observeViewModel() {
        // Observar los segmentos de ruta actualizados
        viewModel.getRouteSegments().observe(getViewLifecycleOwner(), segments -> {
            if (segments != null && !segments.isEmpty()) {
                adapter.submitList(segments);
                tvNoSteps.setVisibility(View.GONE);
                rvSteps.setVisibility(View.VISIBLE);
            } else {
                tvNoSteps.setVisibility(View.VISIBLE);
                rvSteps.setVisibility(View.GONE);
            }
        });
        
        // Observar el segmento activo (donde está el usuario actualmente)
        viewModel.getActiveSegment().observe(getViewLifecycleOwner(), activeSegment -> {
            if (activeSegment != null) {
                adapter.setActiveSegment(activeSegment);
                
                // Hacer scroll a la posición del segmento activo
                int position = adapter.getPositionForSegment(activeSegment);
                if (position >= 0) {
                    rvSteps.smoothScrollToPosition(position);
                }
            }
        });
        
        // Observar estado de recálculo de ruta
        viewModel.getIsRecalculating().observe(getViewLifecycleOwner(), isRecalculating -> {
            loadingView.setVisibility(isRecalculating ? View.VISIBLE : View.GONE);
        });
    }
} 