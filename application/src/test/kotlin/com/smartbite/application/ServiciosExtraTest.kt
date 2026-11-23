package com.smartbite.application

import com.smartbite.application.servicio.CuentasServicio
import com.smartbite.application.servicio.GestionServicio
import com.smartbite.domain.error.CamposInvalidos
import com.smartbite.domain.error.ReglaInvalida
import com.smartbite.domain.modelo.CatalogoGestion
import com.smartbite.domain.modelo.CodigoQr
import com.smartbite.domain.modelo.Compra
import com.smartbite.domain.modelo.Ficha
import com.smartbite.domain.modelo.Opcion
import com.smartbite.domain.modelo.Recurso
import com.smartbite.domain.modelo.Registro
import com.smartbite.domain.modelo.RestauranteRef
import com.smartbite.domain.modelo.Rol
import com.smartbite.domain.modelo.Sesion
import com.smartbite.domain.modelo.Tablero
import com.smartbite.domain.modelo.TipoCuenta
import com.smartbite.domain.puerto.salida.CuentaGateway
import com.smartbite.domain.puerto.salida.GestionGateway
import com.smartbite.domain.puerto.salida.SesionStore
import com.smartbite.domain.valor.Correo
import com.smartbite.domain.valor.Dinero
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ServiciosExtraTest {

    private val llamadas = mutableListOf<String>()

    private val memoria = object : SesionStore {
        var sesion: Sesion? = Sesion("t", "Laura", Rol.ADMINISTRADOR, "A", listOf(RestauranteRef(1, "A"), RestauranteRef(2, "B")), 1)
        override suspend fun leer() = sesion
        override suspend fun guardar(sesion: Sesion) { this.sesion = sesion }
        override suspend fun borrar() { sesion = null }
    }

    private val cuentasFalsas = object : CuentaGateway {
        override suspend fun registrar(registro: Registro) = registro.correo.texto.also { llamadas += "registrar:${registro.tipo}" }
        override suspend fun verificar(correo: Correo, codigo: String) = Sesion("nuevo", "Ana", Rol.CLIENTE, null).also { llamadas += "verificar:$codigo" }
        override suspend fun reenviar(correo: Correo) = "ok"
        override suspend fun recuperar(correo: Correo) = "ok"
        override suspend fun restablecer(correo: Correo, codigo: String, clave: String, repetida: String) = "ok"
    }

    private val gestionFalsa = object : GestionGateway {
        override suspend fun listar(recurso: Recurso) = listOf(Ficha(3, mapOf("nombre" to "Centro")))
        override suspend fun crear(recurso: Recurso, valores: Map<String, Any?>) = Ficha(9, emptyMap()).also { llamadas += "crear:${valores.keys.sorted()}" }
        override suspend fun editar(recurso: Recurso, id: Int, valores: Map<String, Any?>) = Ficha(id, emptyMap()).also { llamadas += "editar:$id" }
        override suspend fun eliminar(recurso: Recurso, id: Int) { llamadas += "eliminar:$id" }
        override suspend fun accion(recurso: Recurso, id: Int, accion: String, valores: Map<String, Any?>) = Ficha(id, emptyMap()).also { llamadas += "accion:$accion:${valores["cantidad"]}" }
        override suspend fun qrMesa(id: Int) = CodigoQr("u", ByteArray(1), Dinero.CERO)
        override suspend fun subirFoto(productoId: Int, bytes: ByteArray) = Ficha(productoId, emptyMap())
        override suspend fun unidades() = listOf(Opcion("kg", "Kilogramo"))
        override suspend fun rolesEmpleado() = listOf(Opcion("2", "Mesero"))
        override suspend fun tablero(): Tablero = throw UnsupportedOperationException()
        override suspend fun compras() = emptyList<Compra>()
        override suspend fun crearCompra(proveedorId: Int, lineas: List<Triple<Int, Int, String>>) =
            Compra(1, "P", "PENDIENTE", "Pendiente", Dinero.CERO, "", emptyList()).also { llamadas += "compra:$lineas" }
        override suspend fun recibirCompra(id: Int) = Compra(id, "P", "RECIBIDA", "Recibida", Dinero.CERO, "", emptyList())
        override suspend fun anularCompra(id: Int) = Compra(id, "P", "ANULADA", "Anulada", Dinero.CERO, "", emptyList())
    }

    private val gestion = GestionServicio(gestionFalsa)
    private val cuentas = CuentasServicio(cuentasFalsas, memoria)

    @Test
    fun `un formulario incompleto no llega al servidor`() = runTest {
        try {
            gestion.guardar(CatalogoGestion.mesas, null, mapOf("numero" to "3"))
            fail("Debió marcar campos")
        } catch (invalido: CamposInvalidos) {
            assertTrue("sucursal" in invalido.errores)
        }
        assertTrue(llamadas.isEmpty())
    }

    @Test
    fun `crear y editar usan el mismo formulario`() = runTest {
        val completo = mapOf("numero" to "3", "capacidad" to "4", "sucursal" to "3", "activa" to "true")
        gestion.guardar(CatalogoGestion.mesas, null, completo)
        gestion.guardar(CatalogoGestion.mesas, 5, completo)
        assertEquals(listOf("crear:[activa, capacidad, numero, sucursal]", "editar:5"), llamadas)
    }

    @Test
    fun `el ajuste de stock valida sus propios campos`() = runTest {
        gestion.ejecutar(CatalogoGestion.items, 4, "ajustar", mapOf("tipo" to "SALIDA", "cantidad" to "3", "motivo" to "merma"))
        assertEquals(listOf("accion:ajustar:3"), llamadas)
    }

    @Test
    fun `las opciones de otro recurso salen con su nombre`() = runTest {
        val opciones = gestion.opciones(CatalogoGestion.mesas.campos.first { it.clave == "sucursal" }.fuente!!)
        assertEquals(listOf(Opcion("3", "Centro")), opciones)
    }

    @Test
    fun `los movimientos no se crean ni se borran desde la app`() = runTest {
        try {
            gestion.eliminar(CatalogoGestion.movimientos, 1)
            fail("No debía borrar")
        } catch (esperado: ReglaInvalida) {
            assertTrue(true)
        }
    }

    @Test
    fun `una compra con lineas invalidas no se envia`() = runTest {
        try {
            gestion.crearCompra(2, listOf(Triple(1, "0", "900")))
            fail("Debió rechazar")
        } catch (invalido: CamposInvalidos) {
            assertTrue("detalles" in invalido.errores)
        }
        gestion.crearCompra(2, listOf(Triple(1, "4", "$ 900")))
        assertEquals(listOf("compra:[(1, 4, 900.00)]"), llamadas)
    }

    @Test
    fun `verificar el correo abre la sesion`() = runTest {
        memoria.sesion = null
        val vista = cuentas.verificarCorreo("ana@x.co", "1234")
        assertEquals(Rol.CLIENTE, vista.rol)
        assertEquals("nuevo", memoria.sesion?.token)
    }

    @Test
    fun `solo se cambia a un restaurante propio`() = runTest {
        assertEquals(2, cuentas.trabajarCon(RestauranteRef(2, "B")).restauranteId)
        try {
            cuentas.trabajarCon(RestauranteRef(9, "Ajeno"))
            fail("No debía permitirlo")
        } catch (esperado: ReglaInvalida) {
            assertTrue(true)
        }
    }

    @Test
    fun `el registro de empleado pide el codigo`() = runTest {
        try {
            cuentas.registrar(TipoCuenta.EMPLEADO, "Ana", "ana@x.co", "clave-123", "clave-123", "", "")
            fail("Debió pedir el código")
        } catch (invalido: CamposInvalidos) {
            assertEquals(setOf("codigo"), invalido.errores.keys)
        }
        cuentas.registrar(TipoCuenta.CLIENTE, "Ana", "ana@x.co", "clave-123", "clave-123", "", "300")
        assertEquals(listOf("registrar:CLIENTE"), llamadas)
    }

}
