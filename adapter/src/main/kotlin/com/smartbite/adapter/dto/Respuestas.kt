package com.smartbite.adapter.dto

data class SesionDto(val token: String?, val usuario: PerfilDto?)

data class PerfilDto(
    val nombre: String?,
    val rol: String?,
    val restauranteActual: NombreDto?,
    val restaurantes: List<NombreDto>?,
)

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
    val tipoConsumo: String?,
    val cuentaSolicitadaEn: String?,
    val pagaraEfectivo: Boolean? = null,
)

data class PedidoCreadoDto(val codigo: String?, val orden: OrdenDto?)

data class ReservaDto(
    val id: Int?,
    val codigo: String?,
    val mesa: MesaResumenDto?,
    val fechaHora: String?,
    val numPersonas: Int?,
    val nombre: String?,
    val telefono: String?,
)

data class Pagina<T>(val count: Int?, val results: List<T>?)

data class MetodoPagoDto(val id: Int?, val nombre: String?, val usaPasarela: Boolean?)

data class PagoDto(
    val id: Int?,
    val metodo: String?,
    val monto: String?,
    val propina: String?,
    val estado: String?,
    val estadoNombre: String?,
    val anulable: Boolean? = null,
)

data class CuentaDto(
    val orden: OrdenDto?,
    val saldoPendiente: String?,
    val saldoDisponible: String?,
    val cobrable: Boolean?,
    val pagos: List<PagoDto>?,
    val factura: Int?,
    val checkout: String?,
    val cambio: String?,
)

data class QrDto(val url: String?, val qrPngBase64: String?, val saldoPendiente: String?)

data class FacturaResumenDto(
    val id: Int?,
    val codigo: String?,
    val orden: Int?,
    val clienteNombre: String?,
    val totalPagado: String?,
    val creadoEn: String?,
)

data class EmisorDto(val nombre: String?, val nit: String?)

data class LineaFacturaDto(val nombre: String?, val cantidad: Int?, val precioUnitario: String?, val subtotal: String?)

data class DetalleFacturaDto(val emisor: EmisorDto?, val lineas: List<LineaFacturaDto>?)

data class FacturaDto(
    val id: Int?,
    val codigo: String?,
    val orden: Int?,
    val clienteNombre: String?,
    val totalPagado: String?,
    val creadoEn: String?,
    val clienteDocumento: String?,
    val baseGravable: String?,
    val inc: String?,
    val totalConsumo: String?,
    val propina: String?,
    val cufe: String?,
    val detalle: DetalleFacturaDto?,
)

data class ErrorDto(
    val detail: String?,
    val code: String?,
    val errores: Map<String, Any?>?,
    val alternativas: List<String>?,
)
