package com.smartbite.application.puerto.entrada

import com.smartbite.application.dto.SesionVista
import com.smartbite.domain.modelo.Carrito
import com.smartbite.domain.modelo.CodigoQr
import com.smartbite.domain.modelo.Compra
import com.smartbite.domain.modelo.Ficha
import com.smartbite.domain.modelo.Fuente
import com.smartbite.domain.modelo.Opcion
import com.smartbite.domain.modelo.PedidoCreado
import com.smartbite.domain.modelo.PedidoGuardado
import com.smartbite.domain.modelo.Producto
import com.smartbite.domain.modelo.Recurso
import com.smartbite.domain.modelo.ReservaCliente
import com.smartbite.domain.modelo.RestauranteRef
import com.smartbite.domain.modelo.SucursalPublica
import com.smartbite.domain.modelo.Tablero
import com.smartbite.domain.modelo.TipoCuenta
import com.smartbite.domain.modelo.TipoOpinion

interface CuentasCasos {
    suspend fun registrar(
        tipo: TipoCuenta,
        nombre: String,
        correo: String,
        clave: String,
        repetida: String,
        codigo: String,
        telefono: String,
    ): String
    suspend fun verificarCorreo(correo: String, codigo: String): SesionVista
    suspend fun reenviarCodigo(correo: String): String
    suspend fun recuperarClave(correo: String): String
    suspend fun restablecerClave(correo: String, codigo: String, clave: String, repetida: String): String
    suspend fun trabajarCon(restaurante: RestauranteRef): SesionVista
}

interface PublicoCasos {
    suspend fun buscarRestaurantes(busqueda: String): List<SucursalPublica>
    suspend fun menuDe(sucursalId: Int): List<Producto>
    suspend fun pedirParaLlevar(sucursal: SucursalPublica, nombre: String, carrito: Carrito): PedidoCreado
    suspend fun recordarPedido(codigo: String)
    suspend fun misPedidos(): List<PedidoGuardado>
    suspend fun reservar(
        sucursalId: Int,
        fecha: String,
        hora: String,
        personas: String,
        nombre: String,
        telefono: String,
    ): ReservaCliente
    suspend fun misReservas(): List<ReservaCliente>
    suspend fun cancelarReservaCliente(codigo: String): ReservaCliente
    suspend fun opinar(tipo: TipoOpinion, mensaje: String, nombre: String, contacto: String, mesaId: Int?)
}

interface GestionCasos {
    suspend fun listar(recurso: Recurso): List<Ficha>
    suspend fun guardar(recurso: Recurso, id: Int?, valores: Map<String, String>): Ficha
    suspend fun eliminar(recurso: Recurso, id: Int)
    suspend fun ejecutar(recurso: Recurso, id: Int, accion: String, valores: Map<String, String> = emptyMap()): Ficha
    suspend fun opciones(fuente: Fuente): List<Opcion>
    suspend fun qrDeMesa(id: Int): CodigoQr
    suspend fun subirFoto(productoId: Int, bytes: ByteArray): Ficha
    suspend fun tablero(): Tablero
    suspend fun compras(): List<Compra>
    suspend fun crearCompra(proveedorId: Int?, lineas: List<Triple<Int, String, String>>): Compra
    suspend fun recibirCompra(compra: Compra): Compra
    suspend fun anularCompra(compra: Compra): Compra
}

interface CasosDeUso : OperacionCasos, CuentasCasos, PublicoCasos, GestionCasos
