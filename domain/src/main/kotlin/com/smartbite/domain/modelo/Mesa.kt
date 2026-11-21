package com.smartbite.domain.modelo

import com.smartbite.domain.valor.Dinero

data class OrdenEnMesa(
    val id: Int,
    val estado: EstadoOrden,
    val total: Dinero,
    val cuentaSolicitada: Boolean,
)

data class Mesa(
    val id: Int,
    val numero: Int,
    val estado: EstadoMesa,
    val sucursalId: Int,
    val orden: OrdenEnMesa?,
) {
    val libre: Boolean get() = estado == EstadoMesa.DISPONIBLE && orden == null
}

data class MesaQr(
    val id: Int,
    val numero: Int,
    val disponible: Boolean,
    val sucursalId: Int,
    val sucursal: String,
    val restaurante: String,
)
