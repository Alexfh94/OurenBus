package com.example.ourenbus2.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ourenbus2.R;
import com.example.ourenbus2.model.Route;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Adaptador para mostrar rutas favoritas en un RecyclerView
 */
public class FavoriteRoutesAdapter extends ListAdapter<Route, FavoriteRoutesAdapter.RouteViewHolder> {

    private final OnRouteClickListener listener;
    private final SimpleDateFormat dateFormat;

    public FavoriteRoutesAdapter(OnRouteClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
    }

    @NonNull
    @Override
    public RouteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_favorite_route, parent, false);
        return new RouteViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull RouteViewHolder holder, int position) {
        Route current = getItem(position);
        holder.bind(current, listener);
    }

    static class RouteViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvRouteName;
        private final TextView tvRouteOrigin;
        private final TextView tvRouteDestination;
        private final TextView tvRouteSavedDate;
        private final ImageButton btnDelete;

        RouteViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRouteName = itemView.findViewById(R.id.tv_route_name);
            tvRouteOrigin = itemView.findViewById(R.id.tv_route_origin);
            tvRouteDestination = itemView.findViewById(R.id.tv_route_destination);
            tvRouteSavedDate = itemView.findViewById(R.id.tv_route_saved_date);
            btnDelete = itemView.findViewById(R.id.btn_delete_route);
        }

        void bind(final Route route, final OnRouteClickListener listener) {
            // Nombre de la ruta (si tiene uno personalizado)
            String name = route.getName();
            if (name != null && !name.isEmpty()) {
                tvRouteName.setText(name);
            } else {
                // Si no tiene nombre personalizado, usamos origen -> destino
                tvRouteName.setText(route.getOrigin().getName() + " → " + route.getDestination().getName());
            }
            
            // Origen y destino
            tvRouteOrigin.setText(route.getOrigin().getName());
            tvRouteDestination.setText(route.getDestination().getName());
            
            // Fecha de guardado
            Date savedDate = route.getSavedDate();
            if (savedDate != null) {
                // Formatear fecha de guardado
                SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                String formattedDate = fmt.format(savedDate);
                tvRouteSavedDate.setText(formattedDate);
            } else {
                tvRouteSavedDate.setText("");
            }
            
            // Click en el elemento
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRouteClicked(route);
                }
            });
            
            // Click en el botón eliminar
            btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRouteDeleteClicked(route);
                }
            });
        }
    }
    
    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        // Guardar el formateador de fecha en el tag del RecyclerView para evitar recrearlo
        recyclerView.setTag(dateFormat);
    }

    public interface OnRouteClickListener {
        void onRouteClicked(Route route);
        void onRouteDeleteClicked(Route route);
    }

    private static final DiffUtil.ItemCallback<Route> DIFF_CALLBACK = new DiffUtil.ItemCallback<Route>() {
        @Override
        public boolean areItemsTheSame(@NonNull Route oldItem, @NonNull Route newItem) {
            // Comparamos por combinación estable de origen/destino (coordenadas) y nombre
            String oldKey = buildKey(oldItem);
            String newKey = buildKey(newItem);
            return oldKey.equals(newKey);
        }

        @Override
        public boolean areContentsTheSame(@NonNull Route oldItem, @NonNull Route newItem) {
            // Comparamos el contenido completo
            return oldItem.equals(newItem);
        }
    };

    private static String buildKey(Route r) {
        if (r == null || r.getOrigin() == null || r.getDestination() == null) return String.valueOf(r != null ? r.getId() : 0);
        return String.format(java.util.Locale.US, "%s|%.6f,%.6f|%.6f,%.6f",
                r.getName() != null ? r.getName() : "",
                r.getOrigin().getLatitude(), r.getOrigin().getLongitude(),
                r.getDestination().getLatitude(), r.getDestination().getLongitude());
    }
} 