package com.example.ourenbus2.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ourenbus2.R;
import com.example.ourenbus2.model.Location;

import java.util.ArrayList;
import java.util.List;

/**
 * Adaptador para mostrar sugerencias de ubicaciones
 */
public class LocationSuggestionsAdapter extends RecyclerView.Adapter<LocationSuggestionsAdapter.LocationViewHolder> {
    
    private final Context context;
    private final List<Location> locations;
    private final OnLocationSelectedListener listener;
    
    public LocationSuggestionsAdapter(Context context, OnLocationSelectedListener listener) {
        this.context = context;
        this.listener = listener;
        this.locations = new ArrayList<>();
    }
    
    @NonNull
    @Override
    public LocationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_location_suggestion, parent, false);
        return new LocationViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull LocationViewHolder holder, int position) {
        Location location = locations.get(position);
        holder.bind(location);
    }
    
    @Override
    public int getItemCount() {
        return locations.size();
    }
    
    /**
     * Actualiza la lista de ubicaciones
     */
    public void updateLocations(List<Location> newLocations) {
        this.locations.clear();
        if (newLocations != null) {
            this.locations.addAll(newLocations);
        }
        notifyDataSetChanged();
    }
    
    /**
     * ViewHolder para una ubicación
     */
    class LocationViewHolder extends RecyclerView.ViewHolder {
        
        private final ImageView ivLocationIcon;
        private final TextView tvLocationName;
        private final TextView tvLocationDescription;
        
        public LocationViewHolder(@NonNull View itemView) {
            super(itemView);
            ivLocationIcon = itemView.findViewById(R.id.iv_location_icon);
            tvLocationName = itemView.findViewById(R.id.tv_location_name);
            tvLocationDescription = itemView.findViewById(R.id.tv_location_description);
            
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onLocationSelected(locations.get(position));
                }
            });
        }
        
        public void bind(Location location) {
            tvLocationName.setText(location.getName());
            tvLocationDescription.setText(location.getDescription());
        }
    }
    
    /**
     * Interfaz para manejar la selección de ubicaciones
     */
    public interface OnLocationSelectedListener {
        void onLocationSelected(Location location);
    }
} 