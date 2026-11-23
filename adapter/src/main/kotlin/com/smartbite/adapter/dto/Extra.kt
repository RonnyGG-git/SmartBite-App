package com.smartbite.adapter.dto

data class RegistroBody(
    val nombre: String,
    val email: String,
    val password1: String,
    val password2: String,
    val codigo: String?,
    val telefono: String?,
)

data class RegistroDto(val email: String?, val detail: String?)

data class CorreoBody(val email: String)

data class VerificarBody(val email: String, val codigo: String)

data class RestablecerBody(val email: String, val codigo: String, val password1: String, val password2: String)

data class MensajeDto(val detail: String?)

data class SucursalPublicaDto(
    val id: Int?,
    val nombre: String?,
    val restaurante: String?,
    val direccion: String?,
    val telefono: String?,
    val distanciaKm: Double?,
)

data class PedidoPublicoBody(val sucursal: Int, val tipoConsumo: String, val nombre: String, val lineas: List<LineaBody>)

data class ReservaBody(val fecha: String, val hora: String, val numPersonas: Int, val nombre: String, val telefono: String)

data class ReservaPublicaDto(
    val codigo: String?,
    val mesaNumero: Int?,
    val sucursal: String?,
    val restaurante: String?,
    val fechaHora: String?,
    val numPersonas: Int?,
    val nombre: String?,
    val telefono: String?,
    val estado: String?,
    val estadoNombre: String?,
)

data class OpinionBody(val tipo: String, val mensaje: String, val nombre: String, val contacto: String, val mesa: Int?)

data class StockDto(val nombre: String?, val stock: Int?, val minimo: Int?)

data class EstadoCantidadDto(val nombre: String?, val cantidad: Int?)

data class MetodoTotalDto(val metodo: String?, val total: String?)

data class TableroDto(
    val restaurante: String?,
    val totalUsuarios: Int?,
    val totalProductos: Int?,
    val totalMesas: Int?,
    val mesasOcupadas: Int?,
    val ordenesHoy: Int?,
    val ventasHoy: String?,
    val stockBajo: List<StockDto>?,
    val ordenesPorEstado: List<EstadoCantidadDto>?,
    val ventasPorMetodo: List<MetodoTotalDto>?,
)

data class DetalleCompraDto(val item: Int?, val itemNombre: String?, val cantidad: Int?, val costoUnitario: String?)

data class CompraDto(
    val id: Int?,
    val proveedorNombre: String?,
    val estado: String?,
    val estadoNombre: String?,
    val total: String?,
    val creadoEn: String?,
    val detalles: List<DetalleCompraDto>?,
)
