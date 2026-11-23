package com.smartbite.domain.error

class SinDisponibilidad(mensaje: String, val alternativas: List<String>) : RuntimeException(mensaje)
