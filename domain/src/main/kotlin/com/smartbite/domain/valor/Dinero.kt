package com.smartbite.domain.valor

import com.smartbite.domain.error.ReglaInvalida
import java.math.BigDecimal
import java.math.RoundingMode

@JvmInline
value class Dinero private constructor(val valor: BigDecimal) : Comparable<Dinero> {
    operator fun plus(otro: Dinero): Dinero = Dinero(valor + otro.valor)

    operator fun minus(otro: Dinero): Dinero = Dinero(valor - otro.valor)

    operator fun times(veces: Int): Dinero = Dinero(valor * BigDecimal(veces))

    override fun compareTo(other: Dinero): Int = valor.compareTo(other.valor)

    val positivo: Boolean get() = valor > BigDecimal.ZERO

    companion object {
        val CERO = Dinero(BigDecimal.ZERO.setScale(2))
        private val MILES = Regex("\\d{1,3}(\\.\\d{3})+")

        fun de(texto: String?): Dinero {
            val numero = texto?.toBigDecimalOrNull() ?: throw ReglaInvalida("Valor inválido: $texto")
            if (numero < BigDecimal.ZERO) throw ReglaInvalida("Un valor de dinero no puede ser negativo.")
            return Dinero(numero.setScale(2, RoundingMode.HALF_UP))
        }

        fun deEntrada(texto: String): Dinero {
            val limpio = texto.replace("$", "").replace(" ", "").trim()
            if (limpio.isEmpty()) throw ReglaInvalida("Escribe un valor.")
            val normal = when {
                "," in limpio -> limpio.replace(".", "").replace(",", ".")
                MILES.matches(limpio) -> limpio.replace(".", "")
                else -> limpio
            }
            if (normal.toBigDecimalOrNull() == null) throw ReglaInvalida("«$texto» no es un valor válido.")
            return de(normal)
        }
    }
}
