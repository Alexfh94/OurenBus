# OurenBus

OurenBus es una aplicación Android para calcular rutas de transporte público en Ourense. Permite a los usuarios encontrar la mejor ruta entre dos puntos utilizando el sistema de autobuses urbanos de la ciudad.

## Características

- Búsqueda de rutas entre dos ubicaciones
- Uso de la ubicación actual como origen
- Vista de mapa con la ruta completa
- Navegación paso a paso
- Guardado de rutas favoritas
- Soporte para tema claro y oscuro

## Configuración

### Requisitos

- Android Studio Arctic Fox o superior
- SDK mínimo: Android 7.0 (API 24)
- SDK objetivo: Android 14 (API 34)
- Google Maps API Key

### Configuración de la API Key

Para utilizar la aplicación, necesitas una API Key de Google Maps. Sigue estos pasos:

1. Obtén una API Key desde la [Google Cloud Console](https://console.cloud.google.com/)
2. Habilita las APIs de Google Maps para Android y Places
3. Crea un archivo `local.properties` en la raíz del proyecto (si no existe)
4. Agrega la siguiente línea al archivo:

```
MAPS_API_KEY=TU_API_KEY_AQUI
```

## Estructura del Proyecto

El proyecto sigue el patrón de arquitectura MVVM (Model-View-ViewModel):

- **Model**: Clases de datos que representan la información del dominio (ubicaciones, rutas, segmentos)
- **View**: Actividades y fragmentos para la interfaz de usuario
- **ViewModel**: Clases que gestionan la lógica de presentación y estado de la aplicación

## Uso

La aplicación permite:

1. Ingresar origen y destino (o usar ubicación actual)
2. Ver la ruta calculada en el mapa
3. Iniciar navegación paso a paso
4. Guardar rutas en favoritos
5. Cambiar entre tema claro y oscuro

## Implementación

La aplicación es un prototipo funcional con simulación de datos. En una versión completa, se conectaría a APIs reales para:

- Geocodificación de direcciones
- Cálculo de rutas de transporte público
- Información en tiempo real de autobuses

## Licencia

Este proyecto es para uso educativo. Todos los derechos de imágenes y datos pertenecen a sus respectivos propietarios. 