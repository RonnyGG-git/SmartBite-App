package com.smartbite.movil

import com.smartbite.domain.valor.Dinero
import com.smartbite.movil.ui.pesos
import org.junit.Assert.assertTrue
import org.junit.Test

class FormatoTest {

    @Test
    fun `los precios se muestran en pesos sin decimales`() {
        val texto = Dinero.de("32000.00").pesos()
        assertTrue(texto, texto.contains("32.000"))
        assertTrue(texto, !texto.contains(",00"))
    }
}
