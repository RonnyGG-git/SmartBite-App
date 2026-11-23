package com.smartbite.application.dto

import com.smartbite.domain.modelo.RestauranteRef
import com.smartbite.domain.modelo.Rol

data class SesionVista(
    val nombre: String,
    val rol: Rol,
    val restaurante: String?,
    val restaurantes: List<RestauranteRef> = emptyList(),
    val restauranteId: Int? = null,
)
