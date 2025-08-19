package com.example.ourenbus2.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ourenbus2.R;
import com.example.ourenbus2.model.RouteSegment;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Adaptador para mostrar los pasos de navegación
 */
public class NavigationStepsAdapter extends ListAdapter<RouteSegment, NavigationStepsAdapter.StepViewHolder> {

    private final OnStepClickListener listener;
    private RouteSegment activeSegment;
    private final SimpleDateFormat timeFormat;

    public NavigationStepsAdapter(OnStepClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
        this.timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    }

    @NonNull
    @Override
    public StepViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_navigation_step, parent, false);
        return new StepViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull StepViewHolder holder, int position) {
        RouteSegment current = getItem(position);
        holder.bind(current, current.equals(activeSegment), listener);
    }

    /**
     * Establece el segmento activo y notifica el cambio
     */
    public void setActiveSegment(RouteSegment segment) {
        RouteSegment oldActive = this.activeSegment;
        this.activeSegment = segment;
        
        // Notificar cambio para el segmento antiguo
        if (oldActive != null) {
            int oldIndex = getPositionForSegment(oldActive);
            if (oldIndex != -1) {
                notifyItemChanged(oldIndex);
            }
        }
        
        // Notificar cambio para el nuevo segmento
        int newIndex = getPositionForSegment(segment);
        if (newIndex != -1) {
            notifyItemChanged(newIndex);
        }
    }
    
    /**
     * Obtiene la posición de un segmento en la lista
     */
    public int getPositionForSegment(RouteSegment segment) {
        if (segment == null) {
            return -1;
        }
        
        for (int i = 0; i < getItemCount(); i++) {
            if (segment.equals(getItem(i))) {
                return i;
            }
        }
        
        return -1;
    }

    static class StepViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivIcon;
        private final TextView tvInstructions;
        private final TextView tvTime;
        private final TextView tvDistance;
        private final View cardContainer;

        StepViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_step_icon);
            tvInstructions = itemView.findViewById(R.id.tv_step_instructions);
            tvTime = itemView.findViewById(R.id.tv_step_time);
            tvDistance = itemView.findViewById(R.id.tv_step_distance);
            cardContainer = itemView.findViewById(R.id.card_container);
        }

        void bind(final RouteSegment segment, boolean isActive, final OnStepClickListener listener) {
            // Configurar icono según tipo de segmento
            switch (segment.getType()) {
                case BUS:
                    ivIcon.setImageResource(R.drawable.ic_bus);
                    break;
                case WALKING:
                    ivIcon.setImageResource(R.drawable.ic_walk);
                    break;
                case WAIT:
                    ivIcon.setImageResource(R.drawable.ic_directions);
                    break;
                default:
                    ivIcon.setImageResource(R.drawable.ic_directions);
                    break;
            }
            
            // Establecer instrucciones
            tvInstructions.setText(segment.getInstructions());
            
            // Formatear y establecer hora
            if (segment.getStartTime() != null && segment.getEndTime() != null) {
                // Crear una instancia local del formateador
                SimpleDateFormat localTimeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                String time = localTimeFormat.format(segment.getStartTime()) + " - " + 
                        localTimeFormat.format(segment.getEndTime());
                tvTime.setText(time);
            } else {
                tvTime.setText("");
            }
            
            // Formatear y establecer distancia
            String distance = segment.getDistance() + " m";
            tvDistance.setText(distance);
            
            // Destacar paso activo
            if (isActive) {
                cardContainer.setBackgroundResource(R.drawable.bg_active_step);
                tvInstructions.setTextAppearance(R.style.TextAppearance_App_StepActive);
            } else {
                cardContainer.setBackgroundResource(R.drawable.bg_inactive_step);
                tvInstructions.setTextAppearance(R.style.TextAppearance_App_StepInactive);
            }
            
            // Configurar clic en el elemento
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onStepClick(segment);
                }
            });
        }
    }

    public interface OnStepClickListener {
        void onStepClick(RouteSegment segment);
    }

    private static final DiffUtil.ItemCallback<RouteSegment> DIFF_CALLBACK = new DiffUtil.ItemCallback<RouteSegment>() {
        @Override
        public boolean areItemsTheSame(@NonNull RouteSegment oldItem, @NonNull RouteSegment newItem) {
            // Compara los segmentos por su tipo y ubicaciones
            return oldItem.getStartLocation() != null && newItem.getStartLocation() != null &&
                   oldItem.getEndLocation() != null && newItem.getEndLocation() != null &&
                   oldItem.getStartLocation().equals(newItem.getStartLocation()) &&
                   oldItem.getEndLocation().equals(newItem.getEndLocation()) &&
                   oldItem.getType() == newItem.getType();
        }

        @Override
        public boolean areContentsTheSame(@NonNull RouteSegment oldItem, @NonNull RouteSegment newItem) {
            return oldItem.equals(newItem);
        }
    };
} 