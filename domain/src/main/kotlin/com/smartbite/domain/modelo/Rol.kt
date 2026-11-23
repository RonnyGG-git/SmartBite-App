package com.smartbite.domain.modelo

import com.smartbite.domain.error.ReglaInvalida

enum class Rol(val etiqueta: String) {
    ADMINISTRADOR("Administrador"),
    MESERO("Mesero"),
    JEFE_COCINA("Jefe de cocina"),
    CAJERO("Cajero"),
    JEFE_INVENTARIO("Jefe de inventario"),
    CLIENTE("Cliente"),
    DOMICILIARIO("Domiciliario");

    val administra: Boolean get() = this == ADMINISTRADOR
    val atiendeMesas: Boolean get() = this == ADMINISTRADOR || this == MESERO
    val cocina: Boolean get() = this == ADMINISTRADOR || this == JEFE_COCINA
    val cobra: Boolean get() = this == ADMINISTRADOR || this == CAJERO
    val gestionaInventario: Boolean get() = this == ADMINISTRADOR || this == JEFE_INVENTARIO
    val leeInventario: Boolean get() = gestionaInventario || this == JEFE_COCINA
    val gestionaCatalogo: Boolean get() = this == ADMINISTRADOR || this == JEFE_COCINA
    val leeCatalogo: Boolean get() = gestionaCatalogo || this == JEFE_INVENTARIO
    val esCliente: Boolean get() = this == CLIENTE
    val esEmpleado: Boolean get() = this != CLIENTE && this != DOMICILIARIO

    companion object {
        fun de(texto: String?): Rol =
            entries.firstOrNull { it.name == texto } ?: throw ReglaInvalida("Tu cuenta todavía no tiene un rol asignado.")
    }
}
