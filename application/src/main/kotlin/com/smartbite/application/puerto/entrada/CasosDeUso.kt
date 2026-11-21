package com.smartbite.application.puerto.entrada

import com.smartbite.application.dto.SesionVista
import com.smartbite.domain.modelo.Carrito
import com.smartbite.domain.modelo.EstadoOrden
import com.smartbite.domain.modelo.Mesa
import com.smartbite.domain.modelo.MesaQr
import com.smartbite.domain.modelo.Orden
import com.smartbite.domain.modelo.PedidoCreado
import com.smartbite.domain.modelo.Producto
import com.smartbite.domain.modelo.TipoConsumo

interface SesionCasos {
    suspend fun iniciarSesion(correo: String, clave: String): SesionVista
    suspend fun sesionActual(): SesionVista?
    suspend fun cerrarSesion()
}

interface ClienteCasos {
    suspend fun buscarMesa(codigoQr: String): MesaQr
    suspend fun verMenu(sucursalId: Int): List<Producto>
    suspend fun pedir(mesa: MesaQr, nombre: String, tipo: TipoConsumo, carrito: Carrito): PedidoCreado
    suspend fun seguirPedido(codigo: String): Orden
}

interface MeseroCasos {
    suspend fun verMesas(): List<Mesa>
    suspend fun verProductos(sucursalId: Int): List<Producto>
    suspend fun verOrden(ordenId: Int): Orden
    suspend fun abrirOrden(mesa: Mesa): Orden
    suspend fun agregarPlatillo(orden: Orden, productoId: Int, cantidad: Int): Orden
    suspend fun confirmarPedido(ordenId: Int, estado: EstadoOrden): Orden
    suspend fun rechazarPedido(ordenId: Int, estado: EstadoOrden): Orden
    suspend fun pedirCuenta(ordenId: Int, estado: EstadoOrden): Orden
}

interface CocinaCasos {
    suspend fun verComandas(): List<Orden>
    suspend fun empezar(orden: Orden, minutos: Int): Orden
    suspend fun marcarLista(orden: Orden): Orden
}

interface CajaCasos {
    suspend fun verPorCobrar(): List<Orden>
    suspend fun entregar(orden: Orden): Orden
}

interface CasosDeUso : SesionCasos, ClienteCasos, MeseroCasos, CocinaCasos, CajaCasos
