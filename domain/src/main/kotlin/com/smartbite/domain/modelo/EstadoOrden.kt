package com.smartbite.domain.modelo

import com.smartbite.domain.error.ReglaInvalida

enum class EstadoOrden(val etiqueta: String) {
    POR_CONFIRMAR("Por confirmar"),
    PENDIENTE("Pendiente"),
    EN_PREPARACION("En preparación"),
    LISTA("Lista"),
    ENTREGADA("Entregada"),
    CANCELADA("Cancelada");

    val activa: Boolean get() = this != ENTREGADA && this != CANCELADA

    fun puedePasarA(destino: EstadoOrden): Boolean = destino in siguientes()

    private fun siguientes(): Set<EstadoOrden> = when (this) {
        POR_CONFIRMAR -> setOf(PENDIENTE, CANCELADA)
        PENDIENTE -> setOf(EN_PREPARACION, CANCELADA)
        EN_PREPARACION -> setOf(LISTA, CANCELADA)
        LISTA -> setOf(ENTREGADA, CANCELADA)
        ENTREGADA, CANCELADA -> emptySet()
    }

    companion object {
        fun de(texto: String?): EstadoOrden =
            entries.firstOrNull { it.name == texto } ?: throw ReglaInvalida("Estado de orden desconocido: $texto")
    }
}
