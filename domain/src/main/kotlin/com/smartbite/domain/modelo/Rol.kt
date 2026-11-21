package com.smartbite.domain.modelo

import com.smartbite.domain.error.ReglaInvalida

enum class Rol(val etiqueta: String) {
    ADMINISTRADOR("Administrador"),
    MESERO("Mesero"),
    JEFE_COCINA("Jefe de cocina"),
    CAJERO("Cajero"),
    JEFE_INVENTARIO("Jefe de inventario");

    val atiendeMesas: Boolean get() = this == ADMINISTRADOR || this == MESERO
    val cocina: Boolean get() = this == ADMINISTRADOR || this == JEFE_COCINA
    val cobra: Boolean get() = this == ADMINISTRADOR || this == CAJERO

    companion object {
        fun de(texto: String?): Rol =
            entries.firstOrNull { it.name == texto } ?: throw ReglaInvalida("Tu cuenta no tiene un rol de empleado.")
    }
}
