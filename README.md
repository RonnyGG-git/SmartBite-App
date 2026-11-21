# Smart Bite — app Android hexagonal

Cliente, mesero, cocina y caja de Smart Bite en Kotlin + Jetpack Compose, con la arquitectura
de puertos y adaptadores: `:domain` y `:application` (Kotlin puro), `:adapter` (Retrofit, DataStore)
y `:app` (Compose, inyección manual en `Contenedor`).

Flujo: el cliente pide por el QR de la mesa → el mesero confirma → cocina prepara → el mesero pide la cuenta →
caja entrega cuando la orden está pagada (el cobro se registra en la web).

## Ejecutar

1. Backend en la rama `feature/api-confirmar-pedido` del repo `smartbite`, con una base SQLite de demo
   (ver `demo-backend/cargar_demo_movil.py`) y `runserver 0.0.0.0:8000`.
2. Abrir esta carpeta en Android Studio y ejecutar `app`. La API por defecto es `http://10.0.2.2:8000/`
   (`apiBaseUrl` en `gradle.properties`).
3. Empleados de la demo: `admin@movil.co`, `mesero@movil.co`, `cocina@movil.co`, `caja@movil.co` — clave `Demo12345`.

## Pruebas

```
./gradlew :domain:test :application:test :adapter:testDebugUnitTest :app:testDebugUnitTest
./gradlew :adapter:testDebugUnitTest --tests "*FlujoRealTest*" -PapiTestUrl=http://localhost:8000 -PapiTestMesa=1
```
