package com.smartbite.domain.modelo

import com.smartbite.domain.error.CamposInvalidos
import com.smartbite.domain.valor.Correo

enum class TipoCuenta(val etiqueta: String) {
    CLIENTE("Soy cliente"),
    EMPLEADO("Trabajo en un restaurante"),
    ADMINISTRADOR("Tengo un restaurante"),
}

data class Registro(
    val tipo: TipoCuenta,
    val nombre: String,
    val correo: Correo,
    val clave: String,
    val codigo: String,
    val telefono: String,
) {
    companion object {
        fun de(tipo: TipoCuenta, nombre: String, correo: String, clave: String, repetida: String, codigo: String, telefono: String): Registro {
            val errores = mutableMapOf<String, String>()
            if (nombre.isBlank()) errores["nombre"] = "Escribe tu nombre."
            val valido = runCatching { Correo.de(correo) }.getOrNull()
            if (valido == null) errores["email"] = "Escribe un correo válido."
            if (clave.length < 8) errores["password1"] = "La contraseña debe tener al menos 8 caracteres."
            if (clave != repetida) errores["password2"] = "Las contraseñas no coinciden."
            if (tipo == TipoCuenta.EMPLEADO && codigo.isBlank()) errores["codigo"] = "Pide el código de acceso a tu administrador."
            if (errores.isNotEmpty()) throw CamposInvalidos(errores)
            return Registro(tipo, nombre.trim(), valido!!, clave, codigo.trim().uppercase(), telefono.trim())
        }
    }
}

fun validarCodigo(codigo: String): String {
    val limpio = codigo.trim()
    if (!Regex("^\\d{4}$").matches(limpio)) throw CamposInvalidos(mapOf("codigo" to "Escribe los 4 números del código."))
    return limpio
}
