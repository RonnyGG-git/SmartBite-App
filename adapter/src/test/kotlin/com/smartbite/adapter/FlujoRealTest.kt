package com.smartbite.adapter

import com.smartbite.adapter.http.FabricaHttp
import com.smartbite.adapter.http.SmartBiteHttp
import com.smartbite.application.servicio.SmartBiteServicio
import com.smartbite.domain.error.ReglaInvalida
import com.smartbite.domain.modelo.Carrito
import com.smartbite.domain.modelo.EstadoOrden
import com.smartbite.domain.modelo.ResultadoCobro
import com.smartbite.domain.modelo.Sesion
import com.smartbite.domain.modelo.TipoConsumo
import com.smartbite.domain.puerto.salida.SesionStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

class FlujoRealTest {

    private class Memoria : SesionStore {
        var sesion: Sesion? = null
        override suspend fun leer() = sesion
        override suspend fun guardar(sesion: Sesion) { this.sesion = sesion }
        override suspend fun borrar() { sesion = null }
    }

    private fun servicio(direccion: String): SmartBiteServicio {
        val memoria = Memoria()
        val http = SmartBiteHttp(FabricaHttp.api(direccion), FabricaHttp.llamadas(memoria) {})
        return SmartBiteServicio(http, http, http, http, http, memoria)
    }

    @Test
    fun `del QR de la mesa hasta la caja`() = runBlocking {
        val direccion = System.getProperty("api.url").orEmpty()
        val mesaQr = System.getProperty("api.mesa").orEmpty()
        assumeTrue("Se omite: pasa -PapiTestUrl y -PapiTestMesa", direccion.isNotBlank() && mesaQr.isNotBlank())

        val cliente = servicio(direccion)
        val mesa = cliente.buscarMesa("$direccion/cliente/mesa/$mesaQr/")
        val menu = cliente.verMenu(mesa.sucursalId)
        val carrito = Carrito().agregar(menu[0]).agregar(menu[0]).agregar(menu[1])
        val pedido = cliente.pedir(mesa, "Prueba app", TipoConsumo.EN_RESTAURANTE, carrito)
        assertEquals(EstadoOrden.POR_CONFIRMAR, pedido.orden.estado)
        assertEquals(carrito.total, pedido.orden.total)

        val mesero = servicio(direccion)
        mesero.iniciarSesion("mesero@movil.co", "Demo12345")
        val enMesa = mesero.verMesas().first { it.id == mesa.id }.orden!!
        assertEquals(pedido.orden.id, enMesa.id)
        mesero.confirmarPedido(enMesa.id, enMesa.estado)

        val cocina = servicio(direccion)
        cocina.iniciarSesion("cocina@movil.co", "Demo12345")
        val comanda = cocina.verComandas().first { it.id == enMesa.id }
        assertEquals(EstadoOrden.PENDIENTE, comanda.estado)
        val enFuego = cocina.empezar(comanda, 15)
        assertEquals(15, enFuego.minutosEstimados)
        assertEquals(EstadoOrden.LISTA, cocina.marcarLista(enFuego).estado)

        assertEquals(EstadoOrden.LISTA, cliente.seguirPedido(pedido.codigo).estado)

        mesero.pedirCuenta(enMesa.id, EstadoOrden.LISTA)

        val caja = servicio(direccion)
        caja.iniciarSesion(System.getProperty("api.caja").orEmpty().ifBlank { "caja@movil.co" }, "Demo12345")
        val porCobrar = caja.verPorCobrar().first { it.id == enMesa.id }
        try {
            caja.entregar(porCobrar)
            throw AssertionError("No debía entregarse sin pagar")
        } catch (esperado: ReglaInvalida) {
            assertTrue(true)
        }
        val cuenta = caja.verCuenta(enMesa.id)
        assertEquals(carrito.total, cuenta.saldoPendiente)
        val efectivo = caja.metodosDePago().first { !it.usaPasarela }
        val primer = caja.cobrar(cuenta, efectivo, "10000", "", "", "")
        assertTrue(!primer.cuenta.saldada)
        val total = primer.cuenta.saldoDisponible.valor.toPlainString()
        val pagado = caja.cobrar(primer.cuenta, efectivo, total, "2000", "200000", "app") as ResultadoCobro.Registrado
        assertTrue(pagado.cuenta.saldada)
        assertEquals(EstadoOrden.ENTREGADA, pagado.cuenta.orden.estado)
        assertTrue(pagado.cambio!!.positivo)
        val factura = caja.verFactura(pagado.cuenta.facturaId!!)
        assertEquals(carrito.total, factura.consumo)
        assertTrue(caja.verFacturas().any { it.id == factura.resumen.id })
        assertTrue(cliente.buscarMesa("$direccion/cliente/mesa/$mesaQr/").disponible)
        caja.cerrarSesion()
    }
}
