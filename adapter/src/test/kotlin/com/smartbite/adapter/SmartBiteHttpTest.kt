package com.smartbite.adapter

import com.smartbite.adapter.http.FabricaHttp
import com.smartbite.adapter.http.SmartBiteHttp
import com.smartbite.domain.error.FalloRemoto
import com.smartbite.domain.modelo.Carrito
import com.smartbite.domain.modelo.EstadoOrden
import com.smartbite.domain.modelo.Producto
import com.smartbite.domain.modelo.Rol
import com.smartbite.domain.modelo.Sesion
import com.smartbite.domain.modelo.TipoConsumo
import com.smartbite.domain.puerto.salida.SesionStore
import com.smartbite.domain.valor.Correo
import com.smartbite.domain.valor.Dinero
import com.smartbite.domain.valor.Minutos
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class SmartBiteHttpTest {

    private class Memoria(var sesion: Sesion? = null) : SesionStore {
        override suspend fun leer() = sesion
        override suspend fun guardar(sesion: Sesion) { this.sesion = sesion }
        override suspend fun borrar() { sesion = null }
    }

    private lateinit var servidor: MockWebServer
    private lateinit var memoria: Memoria
    private lateinit var http: SmartBiteHttp
    private var avisos = 0

    private val ordenJson = """{"id":7,"estado":"PENDIENTE","mesa":{"id":1,"numero":4},"cliente":"Luis",
        "total":"64000.00","pagada":false,"tiempo_estimado_min":null,
        "detalles":[{"nombre":"Bandeja paisa","cantidad":2,"subtotal":"64000.00"}]}"""

    @Before
    fun preparar() {
        servidor = MockWebServer().apply { start() }
        memoria = Memoria(Sesion("tok-1", "Ana", Rol.MESERO, "La Brasa"))
        avisos = 0
        val llamar = FabricaHttp.llamadas(memoria) { avisos++ }
        http = SmartBiteHttp(FabricaHttp.api(servidor.url("/").toString()), llamar)
    }

    @After
    fun cerrar() = servidor.shutdown()

    private suspend fun esperar(tipo: Class<out FalloRemoto>, bloque: suspend () -> Unit) {
        try {
            bloque()
            fail("Debió fallar con ${tipo.simpleName}")
        } catch (fallo: FalloRemoto) {
            assertEquals(tipo, fallo.javaClass)
        }
    }

    @Test
    fun `las llamadas de empleado llevan el encabezado Token`() = runTest {
        servidor.enqueue(MockResponse().setBody("[]"))
        http.mesas()
        assertEquals("Token tok-1", servidor.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun `el login lee el rol y el restaurante del perfil`() = runTest {
        servidor.enqueue(MockResponse().setBody(
            """{"token":"abc","usuario":{"nombre":"Ana","rol":"JEFE_COCINA","restaurante_actual":{"id":1,"nombre":"La Brasa"}}}""",
        ))
        val sesion = http.entrar(Correo.de("ana@labrasa.co"), "clave")
        assertEquals(Rol.JEFE_COCINA, sesion.rol)
        assertEquals("La Brasa", sesion.restaurante)
        assertNull(servidor.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun `un correo sin verificar se reconoce por su code`() = runTest {
        servidor.enqueue(MockResponse().setResponseCode(403).setBody("""{"detail":"Antes de entrar, confirma tu correo.","code":"correo_sin_verificar"}"""))
        esperar(FalloRemoto.CorreoSinVerificar::class.java) { http.entrar(Correo.de("ana@labrasa.co"), "clave") }
    }

    @Test
    fun `un 401 borra la sesion y avisa`() = runTest {
        servidor.enqueue(MockResponse().setResponseCode(401).setBody("""{"detail":"Token inválido.","code":"authentication_failed"}"""))
        esperar(FalloRemoto.SesionVencida::class.java) { http.mesas() }
        assertNull(memoria.sesion)
        assertEquals(1, avisos)
    }

    @Test
    fun `un 403 de rol conserva la sesion y muestra el mensaje del servidor`() = runTest {
        servidor.enqueue(MockResponse().setResponseCode(403).setBody("""{"detail":"Tu rol no tiene permiso para esta acción.","code":"permission_denied"}"""))
        try {
            http.pendientes()
            fail("Debió fallar")
        } catch (fallo: FalloRemoto.SinPermiso) {
            assertEquals("Tu rol no tiene permiso para esta acción.", fallo.message)
        }
        assertEquals("tok-1", memoria.sesion?.token)
        assertEquals(0, avisos)
    }

    @Test
    fun `un 400 trae el mensaje de la regla que rompio`() = runTest {
        servidor.enqueue(MockResponse().setResponseCode(400).setBody("""{"detail":"Esta mesa ya tiene un pedido en curso.","code":"mesa_ocupada"}"""))
        try {
            http.abrirOrden(1, "", "")
            fail("Debió fallar")
        } catch (fallo: FalloRemoto.Rechazado) {
            assertEquals("Esta mesa ya tiene un pedido en curso.", fallo.message)
        }
    }

    @Test
    fun `los nombres con guion bajo llegan al dominio`() = runTest {
        servidor.enqueue(MockResponse().setBody(
            """[{"id":3,"numero":4,"estado":"OCUPADA","sucursal":1,
            "orden_activa":{"id":7,"estado":"POR_CONFIRMAR","total":"64000.00","cuenta_solicitada":true}}]""",
        ))
        val mesa = http.mesas().single()
        assertEquals(EstadoOrden.POR_CONFIRMAR, mesa.orden?.estado)
        assertTrue(mesa.orden!!.cuentaSolicitada)
    }

    @Test
    fun `el pedido del QR viaja con tipo de consumo y lineas`() = runTest {
        servidor.enqueue(MockResponse().setResponseCode(201).setBody("""{"codigo":"xyz","orden":$ordenJson}"""))
        val bandeja = Producto(5, "Bandeja paisa", "", Dinero.de("32000"), "Fuertes", null)
        val pedido = http.pedir(3, "Luis", TipoConsumo.PARA_LLEVAR, Carrito().agregar(bandeja).agregar(bandeja))
        val cuerpo = servidor.takeRequest().body.readUtf8()
        assertTrue(cuerpo.contains("\"tipo_consumo\":\"PARA_LLEVAR\""))
        assertTrue(cuerpo.contains("{\"producto\":5,\"cantidad\":2}"))
        assertEquals("xyz", pedido.codigo)
        assertEquals(Dinero.de("64000.00"), pedido.orden.total)
    }

    @Test
    fun `cocina envia el tiempo estimado con su nombre del backend`() = runTest {
        servidor.enqueue(MockResponse().setBody(ordenJson.replace("PENDIENTE", "EN_PREPARACION")))
        http.iniciar(7, Minutos.de(15))
        val peticion = servidor.takeRequest()
        assertEquals("/api/v1/cocina/ordenes/7/iniciar/", peticion.path)
        assertEquals("{\"tiempo_estimado\":15}", peticion.body.readUtf8())
    }

    @Test
    fun `confirmar usa la accion nueva de la API`() = runTest {
        servidor.enqueue(MockResponse().setBody(ordenJson))
        http.confirmar(7)
        assertEquals("/api/v1/ordenes/7/confirmar/", servidor.takeRequest().path)
    }

    @Test
    fun `un estado que el dominio no conoce es una respuesta invalida`() = runTest {
        servidor.enqueue(MockResponse().setBody(ordenJson.replace("PENDIENTE", "VOLANDO")))
        esperar(FalloRemoto.RespuestaInvalida::class.java) { http.orden(7) }
    }

    @Test
    fun `sin servidor el fallo es de conexion`() = runTest {
        servidor.shutdown()
        esperar(FalloRemoto.SinConexion::class.java) { http.mesas() }
    }
}
