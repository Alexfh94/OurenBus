package com.example.ourenbus2.util;

import com.example.ourenbus2.model.Route;

/**
 * Sesión de navegación en memoria para compartir la ruta actual entre actividades/fragmentos.
 * Evita serializar la ruta completa en intents. Se limpia al finalizar la navegación.
 */
public final class NavigationSession {
    private static Route currentRoute;

    private NavigationSession() {}

    public static synchronized void setCurrentRoute(Route route) {
        currentRoute = route;
    }

    public static synchronized Route getCurrentRoute() {
        return currentRoute;
    }

    public static synchronized void clear() {
        currentRoute = null;
    }
} 