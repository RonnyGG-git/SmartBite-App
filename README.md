# Smart Bite — app Android hexagonal

Cliente, mesero, cocina y caja de Smart Bite en Kotlin + Jetpack Compose, con la arquitectura
de puertos y adaptadores: `:domain` y `:application` (Kotlin puro), `:adapter` (Retrofit, DataStore)
y `:app` (Compose, inyección manual en `Contenedor`).

Flujo: el cliente pide por el QR de la mesa → el mesero confirma → cocina prepara → el mesero pide la cuenta →
caja entrega cuando la orden está pagada (el cobro se registra en la web).

## Ejecutar

La app usa la API de producción en Render (`https://smartbite-7z8m.onrender.com/`, base de datos en Neon).
No hay que levantar nada: abrir la carpeta en Android Studio y ejecutar `app`.

- Empleados de La Brasa Criolla: `carlos@labrasa.co` (mesero), `jorge@labrasa.co` (cocina),
  `diana@labrasa.co` (caja), `admin@labrasa.co` (todo). La clave la tiene el equipo.
- El plan gratuito de Render se duerme: la primera petición puede tardar hasta un minuto.
- Para probar contra un Django local: `./gradlew :app:assembleDebug -PapiBaseUrl=http://10.0.2.2:8000/`
  (datos de ejemplo en `demo-backend/cargar_demo_movil.py`).

## Pruebas

```
./gradlew :domain:test :application:test :adapter:testDebugUnitTest :app:testDebugUnitTest
./gradlew :adapter:testDebugUnitTest --tests "*FlujoRealTest*" -PapiTestUrl=http://localhost:8000 -PapiTestMesa=1
```
