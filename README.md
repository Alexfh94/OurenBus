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

## Cómo Probar la Aplicación

### 1. Clonar el Repositorio

```bash
git clone [URL_DEL_REPOSITORIO]
cd OurenBus2
```

### 2. Abrir en Android Studio

1. Abre Android Studio
2. Selecciona "Open an existing Android Studio project"
3. Navega hasta la carpeta `OurenBus2` y selecciónala
4. Espera a que Android Studio sincronice el proyecto y descargue las dependencias

### 3. Configurar el Emulador o Dispositivo

#### Opción A: Usar Emulador
1. Ve a **Tools > AVD Manager**
2. Haz clic en **Create Virtual Device**
3. Selecciona un dispositivo (recomendado: Pixel 6 o similar)
4. Selecciona una imagen del sistema (recomendado: API 30 o superior)
5. Finaliza la creación del AVD

#### Opción B: Usar Dispositivo Físico
1. Habilita las **Opciones de desarrollador** en tu dispositivo Android
2. Activa la **Depuración USB**
3. Conecta tu dispositivo por USB
4. Autoriza la depuración cuando aparezca el mensaje

### 4. Ejecutar la Aplicación

1. Asegúrate de que tu emulador esté ejecutándose o tu dispositivo esté conectado
2. Haz clic en el botón **Run** (▶️) en la barra de herramientas
3. Selecciona tu dispositivo/emulador de destino
4. Espera a que la aplicación se compile e instale

### 5. Probar las Funcionalidades

Una vez que la aplicación esté ejecutándose, puedes probar:

- **Pantalla de inicio**: Verifica que se muestre el logo y las opciones de navegación
- **Búsqueda de rutas**: 
  - Toca en el campo de origen y escribe una dirección
  - Toca en el campo de destino y escribe otra dirección
  - Presiona "Buscar Ruta"
- **Vista de mapa**: Verifica que se muestre el mapa con la ruta
- **Navegación**: Toca en "Iniciar Navegación" para ver los pasos
- **Favoritos**: Guarda una ruta y verifica que aparezca en la pestaña de favoritos
- **Perfil**: Accede a la configuración y cambia entre temas

### 6. Solución de Problemas Comunes

#### Error de API Key
- Verifica que el archivo `local.properties` contenga tu API Key válida
- Asegúrate de que las APIs de Google Maps estén habilitadas en tu proyecto de Google Cloud

#### Error de Compilación
- Ejecuta **Build > Clean Project**
- Ejecuta **Build > Rebuild Project**
- Sincroniza el proyecto con **File > Sync Project with Gradle Files**

#### Error de Emulador
- Verifica que tengas suficiente RAM disponible (mínimo 4GB recomendado)
- Cierra otras aplicaciones que consuman muchos recursos
- Reinicia el emulador si es necesario

### 7. Debugging

Para depurar la aplicación:

1. Establece puntos de interrupción en el código haciendo clic en el margen izquierdo del editor
2. Ejecuta la aplicación en modo debug (botón 🐛)
3. Usa la ventana **Debug** para inspeccionar variables y el flujo de ejecución
4. Revisa la ventana **Logcat** para ver los logs de la aplicación

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