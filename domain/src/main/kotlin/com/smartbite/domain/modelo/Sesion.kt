package com.smartbite.domain.modelo

data class Sesion(
    val token: String,
    val nombre: String,
    val rol: Rol,
    val restaurante: String?,
) {
    override fun toString(): String = "Sesion(token=****, nombre=$nombre, rol=$rol, restaurante=$restaurante)"
}
