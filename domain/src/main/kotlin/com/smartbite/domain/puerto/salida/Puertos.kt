package com.smartbite.domain.puerto.salida

import com.smartbite.domain.modelo.Carrito
import com.smartbite.domain.modelo.Mesa
import com.smartbite.domain.modelo.MesaQr
import com.smartbite.domain.modelo.Orden
import com.smartbite.domain.modelo.PedidoCreado
import com.smartbite.domain.modelo.Producto
import com.smartbite.domain.modelo.Sesion
import com.smartbite.domain.modelo.TipoConsumo
import com.smartbite.domain.valor.Cantidad
import com.smartbite.domain.valor.Correo
import com.smartbite.domain.valor.Minutos

interface AuthGateway {
    suspend fun entrar(correo: Correo, clave: String): Sesion
    suspend fun salir()
}

interface ClienteGateway {
    suspend fun mesa(mesaId: Int): MesaQr
    suspend fun menu(sucursalId: Int): List<Producto>
    suspend fun pedir(mesaId: Int, nombre: String, tipo: TipoConsumo, carrito: Carrito): PedidoCreado
    suspend fun seguir(codigo: String): Orden
}

interface SalonGateway {
    suspend fun mesas(): List<Mesa>
    suspend fun productos(sucursalId: Int): List<Producto>
    suspend fun orden(ordenId: Int): Orden
    suspend fun abrirOrden(mesaId: Int): Orden
    suspend fun agregar(ordenId: Int, productoId: Int, cantidad: Cantidad): Orden
    suspend fun confirmar(ordenId: Int): Orden
    suspend fun rechazar(ordenId: Int): Orden
    suspend fun pedirCuenta(ordenId: Int): Orden
    suspend fun entregar(ordenId: Int): Orden
}

interface CocinaGateway {
    suspend fun pendientes(): List<Orden>
    suspend fun iniciar(ordenId: Int, minutos: Minutos): Orden
    suspend fun terminar(ordenId: Int): Orden
}

interface CajaGateway {
    suspend fun porCobrar(): List<Orden>
}

interface SesionStore {
    suspend fun leer(): Sesion?
    suspend fun guardar(sesion: Sesion)
    suspend fun borrar()
}
