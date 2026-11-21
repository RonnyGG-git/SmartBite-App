package com.smartbite.domain.valor

import com.smartbite.domain.error.ReglaInvalida

@JvmInline
value class Cantidad private constructor(val valor: Int) {
    companion object {
        const val MAXIMA = 20
        const val MAXIMA_MESERO = 50

        fun de(valor: Int, maxima: Int = MAXIMA): Cantidad {
            if (valor !in 1..maxima) throw ReglaInvalida("La cantidad va de 1 a $maxima por platillo.")
            return Cantidad(valor)
        }
    }
}
