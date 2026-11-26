package com.smartbite.domain

import com.smartbite.domain.error.ReglaInvalida
import com.smartbite.domain.modelo.Cobro
import com.smartbite.domain.modelo.Cuenta
import com.smartbite.domain.modelo.EstadoOrden
import com.smartbite.domain.modelo.MetodoPago
import com.smartbite.domain.modelo.Orden
import com.smartbite.domain.valor.Cantidad
import com.smartbite.domain.valor.Dinero
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CajaReglasTest {

    private val efectivo = MetodoPago(1, "Efectivo", usaPasarela = false)
    private val orden = Orden(9, EstadoOrden.LISTA, 4, null, Dinero.de("64000"), false, null, emptyList())
    private val cuenta = Cuenta(orden, Dinero.de("64000"), Dinero.de("64000"), cobrable = true, pagos = emptyList(), facturaId = null)

    @Test
    fun `los montos se escriben como en Colombia`() {
        assertEquals(Dinero.de("64000"), Dinero.deEntrada("$ 64.000"))
        assertEquals(Dinero.de("1500.5"), Dinero.deEntrada("1.500,50"))
        assertEquals(Dinero.de("40000"), Dinero.deEntrada("40000"))
    }

    @Test(expected = ReglaInvalida::class)
    fun `un monto con letras no pasa`() {
        Dinero.deEntrada("cuarenta mil")
    }

    @Test
    fun `el cambio sale de lo recibido menos monto y propina`() {
        val cobro = Cobro.de(cuenta, efectivo, "64.000", "5.000", "70.000", "")
        assertEquals(Dinero.de("1000"), cobro.cambio)
    }

    @Test
    fun `sin efectivo recibido no hay cambio`() {
        assertNull(Cobro.de(cuenta, efectivo, "64000", "", "", "").cambio)
    }

    @Test(expected = ReglaInvalida::class)
    fun `no se cobra mas del saldo`() {
        Cobro.de(cuenta, efectivo, "90.000", "", "", "")
    }

    @Test(expected = ReglaInvalida::class)
    fun `el efectivo debe alcanzar`() {
        Cobro.de(cuenta, efectivo, "64.000", "", "50.000", "")
    }

    @Test(expected = ReglaInvalida::class)
    fun `hay que elegir metodo`() {
        Cobro.de(cuenta, null, "64.000", "", "", "")
    }

    @Test
    fun `el mesero puede pedir hasta 50 y el cliente hasta 20`() {
        assertEquals(50, Cantidad.de(50, Cantidad.MAXIMA_MESERO).valor)
        assertTrue(runCatching { Cantidad.de(21) }.isFailure)
    }

    @Test
    fun `una cuenta sin saldo esta saldada`() {
        assertTrue(cuenta.copy(saldoPendiente = Dinero.CERO).saldada)
    }

    @Test
    fun `el cliente paga cuando el restaurante confirmo y aun no esta pagado`() {
        assertTrue(orden.pagablePorCliente)
        assertTrue(orden.copy(estado = EstadoOrden.EN_PREPARACION).pagablePorCliente)
        assertEquals(false, orden.copy(estado = EstadoOrden.POR_CONFIRMAR).pagablePorCliente)
        assertEquals(false, orden.copy(estado = EstadoOrden.ENTREGADA).pagablePorCliente)
        assertEquals(false, orden.copy(pagada = true).pagablePorCliente)
    }
}
