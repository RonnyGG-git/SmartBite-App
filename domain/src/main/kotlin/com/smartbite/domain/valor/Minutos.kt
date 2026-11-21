package com.smartbite.domain.valor

import com.smartbite.domain.error.ReglaInvalida

@JvmInline
value class Minutos private constructor(val valor: Int) {
    companion object {
        fun de(valor: Int): Minutos {
            if (valor !in 1..240) throw ReglaInvalida("El tiempo estimado va de 1 a 240 minutos.")
            return Minutos(valor)
        }
    }
}
