package com.smartbite.domain

import com.smartbite.domain.error.CamposInvalidos
import com.smartbite.domain.modelo.CatalogoGestion
import com.smartbite.domain.modelo.NuevaReserva
import com.smartbite.domain.modelo.Opinion
import com.smartbite.domain.modelo.Registro
import com.smartbite.domain.modelo.Rol
import com.smartbite.domain.modelo.TipoCuenta
import com.smartbite.domain.modelo.TipoOpinion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.time.LocalDate

class GestionReglasTest {

    private fun errores(bloque: () -> Unit): Map<String, String> {
        try {
            bloque()
        } catch (invalido: CamposInvalidos) {
            return invalido.errores
        }
        fail("Debió marcar campos inválidos")
        return emptyMap()
    }

    @Test
    fun `los obligatorios se marcan campo por campo`() {
        val marcados = errores { CatalogoGestion.items.preparar(mapOf("nombre" to "Papa"), creando = true) }
        assertTrue(marcados.keys.containsAll(listOf("unidad_medida", "stock_actual", "sucursal")))
        assertFalse("nombre" in marcados)
    }

    @Test
    fun `los tipos se convierten para la API`() {
        val datos = CatalogoGestion.mesas.preparar(mapOf("numero" to "7", "capacidad" to "4", "sucursal" to "2", "activa" to "true"), creando = true)
        assertEquals(7, datos["numero"])
        assertEquals(true, datos["activa"])
        assertEquals("2", datos["sucursal"])
        assertEquals("1500.5", CatalogoGestion.productos.preparar(
            mapOf("nombre" to "X", "precio" to "1500,5", "categoria" to "1", "sucursal" to "1"), creando = true)["precio"])
    }

    @Test
    fun `la clave solo se pide al crear un usuario`() {
        assertTrue(CatalogoGestion.usuarios.camposPara(true).any { it.clave == "password1" })
        assertFalse(CatalogoGestion.usuarios.camposPara(false).any { it.clave == "password1" })
        assertTrue(CatalogoGestion.usuarios.camposPara(false).any { it.clave == "is_active" })
    }

    @Test
    fun `los permisos copian los roles de la web`() {
        assertTrue(CatalogoGestion.productos.gestiona(Rol.JEFE_COCINA))
        assertFalse(CatalogoGestion.productos.gestiona(Rol.JEFE_INVENTARIO))
        assertTrue(CatalogoGestion.productos.lee(Rol.JEFE_INVENTARIO))
        assertFalse(CatalogoGestion.usuarios.lee(Rol.CAJERO))
        assertTrue(CatalogoGestion.mesas.lee(Rol.MESERO))
        assertFalse(CatalogoGestion.mesas.gestiona(Rol.MESERO))
    }

    @Test
    fun `el registro revisa claves y codigo de empleado`() {
        val marcados = errores { Registro.de(TipoCuenta.EMPLEADO, "Ana", "ana@x.co", "clave-123", "otra-1234", "", "") }
        assertEquals(setOf("password2", "codigo"), marcados.keys)
        assertEquals("ABC123", Registro.de(TipoCuenta.EMPLEADO, "Ana", "ana@x.co", "clave-123", "clave-123", " abc123 ", "").codigo)
    }

    @Test
    fun `la reserva no acepta fechas pasadas ni horas mal escritas`() {
        val hoy = LocalDate.of(2026, 11, 22)
        val marcados = errores { NuevaReserva.de("2026-11-20", "7pm", "2", "Luis", "300", hoy) }
        assertEquals(setOf("fecha", "hora"), marcados.keys)
        assertEquals("19:30", NuevaReserva.de("2026-11-23", "19:30", "4", "Luis", "300", hoy).hora)
    }

    @Test
    fun `una opinion vacia no se envia`() {
        assertEquals(setOf("mensaje"), errores { Opinion.de(TipoOpinion.SUGERENCIA, "  ", "", "", null) }.keys)
    }
}
