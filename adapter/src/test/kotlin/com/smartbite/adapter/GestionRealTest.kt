package com.smartbite.adapter

import com.smartbite.adapter.http.CabeceraRestaurante
import com.smartbite.adapter.http.FabricaHttp
import com.smartbite.adapter.http.GestionHttp
import com.smartbite.adapter.http.PublicoHttp
import com.smartbite.adapter.http.SmartBiteHttp
import com.smartbite.application.servicio.GestionServicio
import com.smartbite.application.servicio.PublicoServicio
import com.smartbite.application.servicio.SmartBiteServicio
import com.smartbite.domain.error.CamposInvalidos
import com.smartbite.domain.modelo.CatalogoGestion
import com.smartbite.domain.modelo.Fuente
import com.smartbite.domain.modelo.Sesion
import com.smartbite.domain.modelo.TipoOpinion
import com.smartbite.domain.puerto.salida.HistorialLocal
import com.smartbite.domain.puerto.salida.SesionStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.time.LocalDate

class GestionRealTest {

    private class Memoria : SesionStore {
        var sesion: Sesion? = null
        override suspend fun leer() = sesion
        override suspend fun guardar(sesion: Sesion) { this.sesion = sesion }
        override suspend fun borrar() { sesion = null }
    }

    private class Historial : HistorialLocal {
        val pedidos = mutableListOf<String>()
        val reservas = mutableListOf<String>()
        override suspend fun pedidos() = pedidos
        override suspend fun recordarPedido(codigo: String) { pedidos += codigo }
        override suspend fun reservas() = reservas
        override suspend fun recordarReserva(codigo: String) { reservas += codigo }
    }

    @Test
    fun `administracion inventario compras catalogo y cliente publico contra la API real`() = runBlocking {
        val direccion = System.getProperty("api.url").orEmpty()
        assumeTrue("Se omite: pasa -PapiTestUrl", direccion.isNotBlank())

        val memoria = Memoria()
        val cabecera = CabeceraRestaurante()
        val llamar = FabricaHttp.llamadas(memoria, cabecera) {}
        val http = SmartBiteHttp(FabricaHttp.api(direccion, cabecera), llamar)
        val extra = FabricaHttp.extra(direccion, cabecera)
        val operacion = SmartBiteServicio(http, http, http, http, http, memoria)
        val gestion = GestionServicio(GestionHttp(extra, llamar))
        val historial = Historial()
        val publico = PublicoServicio(PublicoHttp(extra, llamar), http, historial)

        operacion.iniciarSesion("admin@movil.co", "Demo12345")
        assertTrue(memoria.sesion!!.restauranteId != null)

        val tablero = gestion.tablero()
        assertEquals(3, tablero.mesas)

        val sucursal = gestion.opciones(Fuente.DeRecurso("sucursales")).first().valor
        try {
            gestion.guardar(CatalogoGestion.items, null, mapOf("nombre" to "Papa"))
            throw AssertionError("Debió marcar campos")
        } catch (invalido: CamposInvalidos) {
            assertTrue("sucursal" in invalido.errores)
        }
        val papa = gestion.guardar(CatalogoGestion.items, null, mapOf(
            "nombre" to "Papa criolla", "unidad_medida" to "kg", "stock_actual" to "2", "stock_minimo" to "5",
            "costo_unitario" to "3500", "sucursal" to sucursal,
        ))
        assertTrue(papa.si("stock_bajo"))
        val ajustada = gestion.ejecutar(CatalogoGestion.items, papa.id, "ajustar", mapOf("tipo" to "ENTRADA", "cantidad" to "10", "motivo" to "Mercado"))
        assertEquals("12", ajustada.texto("stock_actual"))

        val proveedor = gestion.guardar(CatalogoGestion.proveedores, null, mapOf("nombre" to "Fruver La 33", "activo" to "true"))
        val compra = gestion.crearCompra(proveedor.id, listOf(Triple(papa.id, "5", "3.400")))
        assertTrue(compra.pendiente)
        assertEquals("RECIBIDA", gestion.recibirCompra(compra).estado)
        assertEquals("17", gestion.listar(CatalogoGestion.items).first { it.id == papa.id }.texto("stock_actual"))
        assertTrue(gestion.listar(CatalogoGestion.movimientos).size >= 2)

        val categoria = gestion.guardar(CatalogoGestion.categorias, null, mapOf("nombre" to "Postres"))
        val postre = gestion.guardar(CatalogoGestion.productos, null, mapOf(
            "nombre" to "Arroz con leche", "precio" to "8000", "categoria" to categoria.id.toString(), "sucursal" to sucursal,
            "disponible" to "true",
        ))
        assertTrue(!gestion.ejecutar(CatalogoGestion.productos, postre.id, "disponibilidad").si("disponible"))
        gestion.ejecutar(CatalogoGestion.productos, postre.id, "disponibilidad")
        val receta = gestion.guardar(CatalogoGestion.recetas, null, mapOf(
            "producto" to postre.id.toString(), "item_inventario" to papa.id.toString(), "cantidad_requerida" to "0.2",
            "unidad_medida" to "kg", "activo" to "true",
        ))
        assertEquals("Arroz con leche", receta.texto("producto_nombre"))

        val mesa = gestion.listar(CatalogoGestion.mesas).first()
        assertTrue(gestion.qrDeMesa(mesa.id).url.contains("/cliente/mesa/${mesa.id}/"))
        val roles = gestion.opciones(Fuente.RolesEmpleado)
        assertTrue(roles.none { it.etiqueta == "Administrador" })
        val usuario = gestion.guardar(CatalogoGestion.usuarios, null, mapOf(
            "nombre" to "Pedro Ruiz", "email" to "pedro@movil.co", "rol" to roles.first().valor, "sucursal" to sucursal,
            "password1" to "Clave-Segura-2026", "password2" to "Clave-Segura-2026",
        ))
        assertTrue(!gestion.ejecutar(CatalogoGestion.usuarios, usuario.id, "activo").si("is_active"))
        assertTrue(gestion.listar(CatalogoGestion.restaurantes).first().texto("codigo_acceso").isNotBlank())

        val restaurante = publico.buscarRestaurantes("").first()
        val reserva = publico.reservar(restaurante.id, LocalDate.now().plusDays(3).toString(), "20:00", "2", "Luis", "3001112233")
        assertEquals("CONFIRMADA", reserva.estado)
        assertEquals(reserva.codigo, publico.misReservas().single().codigo)
        assertEquals("CANCELADA", publico.cancelarReservaCliente(reserva.codigo).estado)
        val menu = publico.menuDe(restaurante.id)
        val pedido = publico.pedirParaLlevar(restaurante, "Ana", com.smartbite.domain.modelo.Carrito().agregar(menu.first()))
        assertEquals(pedido.codigo, publico.misPedidos().single().codigo)
        publico.opinar(TipoOpinion.SUGERENCIA, "Muy rico todo", "Ana", "", mesa.id)
        assertTrue(gestion.listar(CatalogoGestion.opiniones).any { it.texto("mensaje") == "Muy rico todo" })
        operacion.cerrarSesion()
    }
}
