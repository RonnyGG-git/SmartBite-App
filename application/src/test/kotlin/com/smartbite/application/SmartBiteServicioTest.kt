package com.smartbite.application

import com.smartbite.application.servicio.SmartBiteServicio
import com.smartbite.domain.error.FalloRemoto
import com.smartbite.domain.error.ReglaInvalida
import com.smartbite.domain.modelo.Carrito
import com.smartbite.domain.modelo.EstadoMesa
import com.smartbite.domain.modelo.EstadoOrden
import com.smartbite.domain.modelo.Mesa
import com.smartbite.domain.modelo.MesaQr
import com.smartbite.domain.modelo.Orden
import com.smartbite.domain.modelo.OrdenEnMesa
import com.smartbite.domain.modelo.PedidoCreado
import com.smartbite.domain.modelo.Producto
import com.smartbite.domain.modelo.Rol
import com.smartbite.domain.modelo.Sesion
import com.smartbite.domain.modelo.TipoConsumo
import com.smartbite.domain.puerto.salida.AuthGateway
import com.smartbite.domain.puerto.salida.CajaGateway
import com.smartbite.domain.puerto.salida.ClienteGateway
import com.smartbite.domain.puerto.salida.CocinaGateway
import com.smartbite.domain.puerto.salida.SalonGateway
import com.smartbite.domain.puerto.salida.SesionStore
import com.smartbite.domain.valor.Cantidad
import com.smartbite.domain.valor.Correo
import com.smartbite.domain.valor.Dinero
import com.smartbite.domain.valor.Minutos
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class SmartBiteServicioTest {

    private val llamadas = mutableListOf<String>()

    private fun orden(estado: EstadoOrden, pagada: Boolean = false) =
        Orden(7, estado, 4, null, Dinero.de("32000"), pagada, null, emptyList())

    private val falso = object : AuthGateway, ClienteGateway, SalonGateway, CocinaGateway, CajaGateway {
        override suspend fun entrar(correo: Correo, clave: String) = Sesion("tok", "Ana", Rol.MESERO, "La Brasa").also { llamadas += "entrar:${correo.texto}" }
        override suspend fun salir() { llamadas += "salir"; throw FalloRemoto.SinConexion() }
        override suspend fun mesa(mesaId: Int) = MesaQr(mesaId, 4, mesaId != 99, 1, "Centro", "La Brasa").also { llamadas += "mesa:$mesaId" }
        override suspend fun menu(sucursalId: Int) = emptyList<Producto>()
        override suspend fun pedir(mesaId: Int, nombre: String, tipo: TipoConsumo, carrito: Carrito) =
            PedidoCreado("abc", orden(EstadoOrden.POR_CONFIRMAR)).also { llamadas += "pedir:$nombre" }
        override suspend fun seguir(codigo: String) = orden(EstadoOrden.PENDIENTE)
        override suspend fun mesas() = emptyList<Mesa>()
        override suspend fun productos(sucursalId: Int) = emptyList<Producto>()
        override suspend fun orden(ordenId: Int) = orden(EstadoOrden.PENDIENTE)
        override suspend fun abrirOrden(mesaId: Int) = orden(EstadoOrden.PENDIENTE).also { llamadas += "abrir" }
        override suspend fun agregar(ordenId: Int, productoId: Int, cantidad: Cantidad) = orden(EstadoOrden.PENDIENTE).also { llamadas += "agregar" }
        override suspend fun confirmar(ordenId: Int) = orden(EstadoOrden.PENDIENTE).also { llamadas += "confirmar" }
        override suspend fun rechazar(ordenId: Int) = orden(EstadoOrden.CANCELADA)
        override suspend fun pedirCuenta(ordenId: Int) = orden(EstadoOrden.LISTA)
        override suspend fun entregar(ordenId: Int) = orden(EstadoOrden.ENTREGADA).also { llamadas += "entregar" }
        override suspend fun pendientes() = emptyList<Orden>()
        override suspend fun iniciar(ordenId: Int, minutos: Minutos) = orden(EstadoOrden.EN_PREPARACION).also { llamadas += "iniciar:${minutos.valor}" }
        override suspend fun terminar(ordenId: Int) = orden(EstadoOrden.LISTA)
        override suspend fun porCobrar() = emptyList<Orden>()
    }

    private val memoria = object : SesionStore {
        var sesion: Sesion? = null
        override suspend fun leer() = sesion
        override suspend fun guardar(sesion: Sesion) { this.sesion = sesion }
        override suspend fun borrar() { sesion = null }
    }

    private val servicio = SmartBiteServicio(falso, falso, falso, falso, falso, memoria)

    private suspend fun esperarRegla(bloque: suspend () -> Unit) {
        try {
            bloque()
            fail("Debió lanzar ReglaInvalida")
        } catch (esperado: ReglaInvalida) {
            assertTrue(true)
        }
    }

    @Test
    fun `el login guarda la sesion y no expone el token`() = runTest {
        val vista = servicio.iniciarSesion(" Ana@LaBrasa.co ", "clave")
        assertEquals("tok", memoria.sesion?.token)
        assertEquals(Rol.MESERO, vista.rol)
        assertEquals("entrar:ana@labrasa.co", llamadas.single())
    }

    @Test
    fun `cerrar sesion borra la sesion aunque falle la red`() = runTest {
        memoria.sesion = Sesion("tok", "Ana", Rol.MESERO, null)
        servicio.cerrarSesion()
        assertNull(memoria.sesion)
    }

    @Test
    fun `el codigo del QR acepta el numero o el enlace completo`() = runTest {
        servicio.buscarMesa("12")
        servicio.buscarMesa("https://smartbite.onrender.com/cliente/mesa/15/")
        assertEquals(listOf("mesa:12", "mesa:15"), llamadas)
    }

    @Test
    fun `una mesa ocupada no deja pedir`() = runTest {
        esperarRegla { servicio.buscarMesa("99") }
    }

    @Test
    fun `un carrito vacio no llega al servidor`() = runTest {
        val mesa = MesaQr(1, 4, true, 1, "Centro", "La Brasa")
        esperarRegla { servicio.pedir(mesa, "Luis", TipoConsumo.EN_RESTAURANTE, Carrito()) }
        assertTrue(llamadas.isEmpty())
    }

    @Test
    fun `cocina no empieza una orden con tiempo invalido`() = runTest {
        esperarRegla { servicio.empezar(orden(EstadoOrden.PENDIENTE), 0) }
        servicio.empezar(orden(EstadoOrden.PENDIENTE), 15)
        assertEquals(listOf("iniciar:15"), llamadas)
    }

    @Test
    fun `caja no entrega una orden sin pagar`() = runTest {
        esperarRegla { servicio.entregar(orden(EstadoOrden.LISTA, pagada = false)) }
        servicio.entregar(orden(EstadoOrden.LISTA, pagada = true))
        assertEquals(listOf("entregar"), llamadas)
    }

    @Test
    fun `solo se confirma un pedido que espera confirmacion`() = runTest {
        esperarRegla { servicio.confirmarPedido(7, EstadoOrden.PENDIENTE) }
        servicio.confirmarPedido(7, EstadoOrden.POR_CONFIRMAR)
        assertEquals(listOf("confirmar"), llamadas)
    }

    @Test
    fun `no se abre una orden en una mesa ocupada`() = runTest {
        val ocupada = Mesa(1, 4, EstadoMesa.OCUPADA, 1, OrdenEnMesa(7, EstadoOrden.PENDIENTE, Dinero.CERO, false))
        esperarRegla { servicio.abrirOrden(ocupada) }
        assertTrue(llamadas.isEmpty())
    }
}
