package com.smartbite.adapter

import com.smartbite.adapter.http.ApiExtra
import com.smartbite.adapter.http.CabeceraRestaurante
import com.smartbite.adapter.http.FabricaHttp
import com.smartbite.adapter.http.GestionHttp
import com.smartbite.adapter.http.PublicoHttp
import com.smartbite.domain.error.CamposInvalidos
import com.smartbite.domain.error.SinDisponibilidad
import com.smartbite.domain.modelo.CatalogoGestion
import com.smartbite.domain.modelo.NuevaReserva
import com.smartbite.domain.modelo.RestauranteRef
import com.smartbite.domain.modelo.Rol
import com.smartbite.domain.modelo.Sesion
import com.smartbite.domain.puerto.salida.SesionStore
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class GestionHttpTest {

    private class Memoria(var sesion: Sesion?) : SesionStore {
        override suspend fun leer() = sesion
        override suspend fun guardar(sesion: Sesion) { this.sesion = sesion }
        override suspend fun borrar() { sesion = null }
    }

    private lateinit var servidor: MockWebServer
    private lateinit var gestion: GestionHttp
    private lateinit var publico: PublicoHttp

    @Before
    fun preparar() {
        servidor = MockWebServer().apply { start() }
        val memoria = Memoria(Sesion("tok", "Laura", Rol.ADMINISTRADOR, "B", listOf(RestauranteRef(1, "A"), RestauranteRef(2, "B")), 2))
        val cabecera = CabeceraRestaurante()
        val cliente = okhttp3.OkHttpClient.Builder().addInterceptor(cabecera).build()
        val api = Retrofit.Builder()
            .baseUrl(servidor.url("/"))
            .client(cliente)
            .addConverterFactory(GsonConverterFactory.create(FabricaHttp.gson))
            .build()
            .create(ApiExtra::class.java)
        val llamar = FabricaHttp.llamadas(memoria, cabecera) {}
        gestion = GestionHttp(api, llamar)
        publico = PublicoHttp(api, llamar)
    }

    @After
    fun cerrar() = servidor.shutdown()

    @Test
    fun `la lista paginada se vuelve fichas y lleva el restaurante elegido`() = runTest {
        servidor.enqueue(MockResponse().setBody("""{"count":1,"results":[{"id":4,"nombre":"Sal","stock_bajo":true,"sucursal":null}]}"""))
        val fichas = gestion.listar(CatalogoGestion.items)
        val peticion = servidor.takeRequest()
        assertEquals("/api/v1/gestion/items/", peticion.path)
        assertEquals("2", peticion.getHeader("X-Restaurante"))
        assertEquals("Sal", fichas.single().texto("nombre"))
        assertTrue(fichas.single().si("stock_bajo"))
    }

    @Test
    fun `los errores del formulario web llegan por campo`() = runTest {
        servidor.enqueue(MockResponse().setResponseCode(400).setBody(
            """{"detail":"Los datos enviados no son válidos.","code":"invalid","errores":{"sucursal":["Escoja una opción válida."],"non_field_errors":["Algo general"]}}""",
        ))
        try {
            gestion.crear(CatalogoGestion.items, mapOf("nombre" to "X"))
            fail("Debió fallar")
        } catch (invalido: CamposInvalidos) {
            assertEquals("Escoja una opción válida.", invalido.errores["sucursal"])
            assertEquals("Algo general", invalido.errores["general"])
        }
    }

    @Test
    fun `editar usa PUT y la accion va a su ruta`() = runTest {
        servidor.enqueue(MockResponse().setBody("""{"id":7,"nombre":"Mesa"}"""))
        servidor.enqueue(MockResponse().setBody("""{"id":7,"stock_actual":3}"""))
        gestion.editar(CatalogoGestion.mesas, 7, mapOf("numero" to 3))
        gestion.accion(CatalogoGestion.items, 7, "ajustar", mapOf("tipo" to "SALIDA"))
        val primera = servidor.takeRequest()
        assertEquals("PUT", primera.method)
        assertEquals("/api/v1/gestion/mesas/7/", primera.path)
        assertEquals("/api/v1/gestion/items/7/ajustar/", servidor.takeRequest().path)
    }

    @Test
    fun `una reserva sin mesas trae horarios alternativos`() = runTest {
        servidor.enqueue(MockResponse().setResponseCode(400).setBody(
            """{"detail":"No hay mesas libres a esa hora.","code":"sin_mesa","alternativas":["2026-11-23T20:00:00-05:00"]}""",
        ))
        try {
            publico.reservar(1, NuevaReserva("2026-11-23", "19:00", 2, "Luis", "300"))
            fail("Debió fallar")
        } catch (sin: SinDisponibilidad) {
            assertEquals(1, sin.alternativas.size)
            assertEquals("No hay mesas libres a esa hora.", sin.message)
        }
        assertTrue(servidor.takeRequest().body.readUtf8().contains("\"num_personas\":2"))
    }
}
