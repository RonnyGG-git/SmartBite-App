package com.smartbite.domain.valor

import com.smartbite.domain.error.ReglaInvalida

@JvmInline
value class Cantidad private constructor(val valor: Int) {
    companion object {
        const val MAXIMA = 20

        fun de(valor: Int): Cantidad {
            if (valor !in 1..MAXIMA) throw ReglaInvalida("La cantidad va de 1 a $MAXIMA por platillo.")
            return Cantidad(valor)
        }
    }
}
