package com.smartbite.domain.puerto.salida

import com.smartbite.domain.modelo.Carrito
import com.smartbite.domain.modelo.CodigoQr
import com.smartbite.domain.modelo.Compra
import com.smartbite.domain.modelo.Ficha
import com.smartbite.domain.modelo.NuevaReserva
import com.smartbite.domain.modelo.Opcion
import com.smartbite.domain.modelo.Opinion
import com.smartbite.domain.modelo.PedidoCreado
import com.smartbite.domain.modelo.Recurso
import com.smartbite.domain.modelo.Registro
import com.smartbite.domain.modelo.ReservaCliente
import com.smartbite.domain.modelo.Sesion
import com.smartbite.domain.modelo.SucursalPublica
import com.smartbite.domain.modelo.Tablero
import com.smartbite.domain.valor.Correo

interface CuentaGateway {
    suspend fun registrar(registro: Registro): String
    suspend fun verificar(correo: Correo, codigo: String): Sesion
    suspend fun reenviar(correo: Correo): String
    suspend fun recuperar(correo: Correo): String
    suspend fun restablecer(correo: Correo, codigo: String, clave: String, repetida: String): String
}

interface PublicoGateway {
    suspend fun sucursales(busqueda: String): List<SucursalPublica>
    suspend fun pedirParaLlevar(sucursalId: Int, nombre: String, carrito: Carrito): PedidoCreado
    suspend fun reservar(sucursalId: Int, reserva: NuevaReserva): ReservaCliente
    suspend fun reserva(codigo: String): ReservaCliente
    suspend fun cancelarReserva(codigo: String): ReservaCliente
    suspend fun opinar(opinion: Opinion)
}

interface GestionGateway {
    suspend fun listar(recurso: Recurso): List<Ficha>
    suspend fun crear(recurso: Recurso, valores: Map<String, Any?>): Ficha
    suspend fun editar(recurso: Recurso, id: Int, valores: Map<String, Any?>): Ficha
    suspend fun eliminar(recurso: Recurso, id: Int)
    suspend fun accion(recurso: Recurso, id: Int, accion: String, valores: Map<String, Any?>): Ficha
    suspend fun qrMesa(id: Int): CodigoQr
    suspend fun subirFoto(productoId: Int, bytes: ByteArray): Ficha
    suspend fun unidades(): List<Opcion>
    suspend fun rolesEmpleado(): List<Opcion>
    suspend fun tablero(): Tablero
    suspend fun compras(): List<Compra>
    suspend fun crearCompra(proveedorId: Int, lineas: List<Triple<Int, Int, String>>): Compra
    suspend fun recibirCompra(id: Int): Compra
    suspend fun anularCompra(id: Int): Compra
}

interface HistorialLocal {
    suspend fun pedidos(): List<String>
    suspend fun recordarPedido(codigo: String)
    suspend fun reservas(): List<String>
    suspend fun recordarReserva(codigo: String)
}
