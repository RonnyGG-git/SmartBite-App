package com.smartbite.domain.modelo

import com.smartbite.domain.valor.Dinero

data class Producto(
    val id: Int,
    val nombre: String,
    val descripcion: String,
    val precio: Dinero,
    val categoria: String,
    val imagen: String?,
)
