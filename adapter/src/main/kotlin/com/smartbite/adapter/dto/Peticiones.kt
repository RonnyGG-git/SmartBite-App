package com.smartbite.adapter.dto

data class LoginBody(val email: String, val password: String)

data class CrearOrdenBody(val mesa: Int, val clienteNombre: String, val clienteTelefono: String)

data class AgregarBody(val producto: Int, val cantidad: Int)

data class AsignarMesaBody(val mesa: Int)

data class IniciarBody(val tiempoEstimado: Int)

data class LineaBody(val producto: Int, val cantidad: Int)

data class PedidoMesaBody(val tipoConsumo: String, val nombre: String, val lineas: List<LineaBody>)

data class CobrarBody(
    val metodo: Int,
    val monto: String,
    val propina: String,
    val referencia: String,
    val efectivoRecibido: String?,
)
