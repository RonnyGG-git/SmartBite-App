package com.smartbite.domain.modelo

data class RestauranteRef(val id: Int, val nombre: String)

data class Sesion(
    val token: String,
    val nombre: String,
    val rol: Rol,
    val restaurante: String?,
    val restaurantes: List<RestauranteRef> = emptyList(),
    val restauranteId: Int? = null,
) {
    fun conRestaurante(elegido: RestauranteRef): Sesion = copy(restaurante = elegido.nombre, restauranteId = elegido.id)

    override fun toString(): String = "Sesion(token=****, nombre=$nombre, rol=$rol, restaurante=$restaurante)"
}
