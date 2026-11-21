package com.smartbite.domain.modelo

data class Reserva(
    val id: Int,
    val codigo: String,
    val mesa: Int?,
    val fechaHora: String,
    val personas: Int,
    val nombre: String,
    val telefono: String,
)
