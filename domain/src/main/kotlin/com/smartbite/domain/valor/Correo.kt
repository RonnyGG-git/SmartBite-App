package com.smartbite.domain.valor

import com.smartbite.domain.error.ReglaInvalida

@JvmInline
value class Correo private constructor(val texto: String) {
    companion object {
        private val FORMATO = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

        fun de(crudo: String): Correo {
            val limpio = crudo.trim().lowercase()
            if (!FORMATO.matches(limpio)) throw ReglaInvalida("Escribe un correo válido.")
            return Correo(limpio)
        }
    }
}
