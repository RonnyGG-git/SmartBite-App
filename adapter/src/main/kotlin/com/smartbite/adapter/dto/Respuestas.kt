package com.smartbite.adapter.dto

data class SesionDto(val token: String?, val usuario: PerfilDto?)

data class PerfilDto(val nombre: String?, val rol: String?, val restauranteActual: NombreDto?)

data class NombreDto(val id: Int?, val nombre: String?)

data class CategoriaDto(val nombre: String?)

data class ProductoDto(
    val id: Int?,
    val nombre: String?,
    val descripcion: String?,
    val precio: String?,
    val urlImagen: String?,
    val categoria: CategoriaDto?,
)

data class OrdenActivaDto(val id: Int?, val estado: String?, val total: String?, val cuentaSolicitada: Boolean?)

data class MesaDto(
    val id: Int?,
    val numero: Int?,
    val estado: String?,
    val sucursal: Int?,
    val ordenActiva: OrdenActivaDto?,
)

data class SucursalQrDto(val id: Int?, val nombre: String?, val restaurante: String?)

data class MesaQrDto(val id: Int?, val numero: Int?, val disponible: Boolean?, val sucursal: SucursalQrDto?)

data class MesaResumenDto(val numero: Int?)

data class DetalleDto(val nombre: String?, val cantidad: Int?, val subtotal: String?)

data class OrdenDto(
    val id: Int?,
    val estado: String?,
    val mesa: MesaResumenDto?,
    val cliente: String?,
    val total: String?,
    val pagada: Boolean?,
    val tiempoEstimadoMin: Int?,
    val detalles: List<DetalleDto>?,
    val faltantes: List<String>?,
)

data class PedidoCreadoDto(val codigo: String?, val orden: OrdenDto?)

data class ErrorDto(val detail: String?, val code: String?)
