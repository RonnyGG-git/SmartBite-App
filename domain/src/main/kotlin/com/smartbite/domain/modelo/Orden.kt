package com.smartbite.domain.modelo

import com.smartbite.domain.valor.Dinero

data class LineaOrden(val nombre: String, val cantidad: Int, val subtotal: Dinero)

data class Orden(
    val id: Int,
    val estado: EstadoOrden,
    val mesa: Int?,
    val cliente: String?,
    val total: Dinero,
    val pagada: Boolean,
    val minutosEstimados: Int?,
    val lineas: List<LineaOrden>,
    val faltantes: List<String> = emptyList(),
) {
    val esperaConfirmacion: Boolean get() = estado == EstadoOrden.POR_CONFIRMAR
    val puedeEntregarse: Boolean get() = estado == EstadoOrden.LISTA && pagada
}
