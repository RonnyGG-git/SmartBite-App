package com.smartbite.domain.valor

import com.smartbite.domain.error.ReglaInvalida
import java.math.BigDecimal
import java.math.RoundingMode

@JvmInline
value class Dinero private constructor(val valor: BigDecimal) {
    operator fun plus(otro: Dinero): Dinero = Dinero(valor + otro.valor)

    operator fun times(veces: Int): Dinero = Dinero(valor * BigDecimal(veces))

    companion object {
        val CERO = Dinero(BigDecimal.ZERO.setScale(2))

        fun de(texto: String?): Dinero {
            val numero = texto?.toBigDecimalOrNull() ?: throw ReglaInvalida("Precio inválido: $texto")
            if (numero < BigDecimal.ZERO) throw ReglaInvalida("Un precio no puede ser negativo.")
            return Dinero(numero.setScale(2, RoundingMode.HALF_UP))
        }
    }
}
