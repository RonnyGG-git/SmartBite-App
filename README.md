# Smart Bite — app Android

App de Smart Bite en Kotlin + Jetpack Compose con arquitectura de puertos y adaptadores:
`:domain` y `:application` (Kotlin puro), `:adapter` (Retrofit, DataStore) y `:app` (Compose,
inyección manual en `Contenedor`). Usa la misma API y la misma base (Render + Neon) que la web.

## Qué hace

| Quién | Módulos |
|---|---|
| Cliente sin cuenta | Pedir en la mesa con el QR, restaurantes y menú con fotos, pedido para llevar, reservar, opinar, mis pedidos y reservas |
| Cliente registrado | Lo mismo, con su cuenta |
| Mesero | Mesas, confirmar pedidos por QR, abrir órdenes, cantidades, reservas, pedidos sin mesa, pedir cuenta, entregar |
| Cocina | Comandas, tiempo estimado, lista; catálogo, categorías y recetas |
| Caja | Por cobrar, cobro en efectivo, transferencia o pasarela, QR de pago, para llevar, facturas, ventas |
| Inventario | Ítems y ajustes, movimientos, proveedores, compras (recibir y anular), recetas |
| Administrador | Todo lo anterior, más el tablero, usuarios, mesas y su QR, sucursales, restaurantes y código de acceso, opiniones; cambiar de restaurante |

El domiciliario sigue en la web.

## Ejecutar

Abrir en Android Studio y ejecutar `app`. Apunta a `https://smartbite-7z8m.onrender.com/`.
Necesita que el backend tenga la rama `feature/api-app-completa` desplegada.

Contra un Django local: `./gradlew :app:assembleDebug -PapiBaseUrl=http://10.0.2.2:8000/`
(datos de ejemplo en `demo-backend/cargar_demo_movil.py`).

APK firmado: `./gradlew :app:assembleRelease` (requiere `keystore.properties` y `firma/`, que no van al repo).

## Pruebas

```
./gradlew :domain:test :application:test :adapter:testDebugUnitTest :app:testDebugUnitTest
./gradlew :adapter:testDebugUnitTest --tests "*RealTest*" -PapiTestUrl=http://localhost:8000 -PapiTestMesa=1
```
