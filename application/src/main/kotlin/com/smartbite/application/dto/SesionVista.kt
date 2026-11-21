package com.smartbite.application.dto

import com.smartbite.domain.modelo.Rol

data class SesionVista(val nombre: String, val rol: Rol, val restaurante: String?)
