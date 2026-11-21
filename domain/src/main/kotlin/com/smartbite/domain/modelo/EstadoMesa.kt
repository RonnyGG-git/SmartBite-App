package com.smartbite.domain.modelo

import com.smartbite.domain.error.ReglaInvalida

enum class EstadoMesa(val etiqueta: String) {
    DISPONIBLE("Disponible"),
    OCUPADA("Ocupada"),
    RESERVADA("Reservada");

    companion object {
        fun de(texto: String?): EstadoMesa =
            entries.firstOrNull { it.name == texto } ?: throw ReglaInvalida("Estado de mesa desconocido: $texto")
    }
}
