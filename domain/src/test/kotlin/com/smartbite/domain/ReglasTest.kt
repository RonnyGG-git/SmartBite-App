package com.smartbite.domain

import com.smartbite.domain.error.ReglaInvalida
import com.smartbite.domain.modelo.Carrito
import com.smartbite.domain.modelo.EstadoOrden
import com.smartbite.domain.modelo.Orden
import com.smartbite.domain.modelo.Producto
import com.smartbite.domain.modelo.Rol
import com.smartbite.domain.valor.Cantidad
import com.smartbite.domain.valor.Correo
import com.smartbite.domain.valor.Dinero
import com.smartbite.domain.valor.Minutos
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReglasTest {

    private val bandeja = Producto(1, "Bandeja paisa", "", Dinero.de("32000.00"), "Fuertes", null)
    private val limonada = Producto(2, "Limonada", "", Dinero.de("6500"), "Bebidas", null)

    @Test
    fun `el correo se limpia y pasa a minusculas`() {
        assertEquals("mesero@labrasa.co", Correo.de("  Mesero@LaBrasa.CO ").texto)
    }

    @Test(expected = ReglaInvalida::class)
    fun `un correo sin punto en el dominio no pasa`() {
        Correo.de("mesero@labrasa")
    }

    @Test(expected = ReglaInvalida::class)
    fun `no se piden mas de 20 unidades de un platillo`() {
        Cantidad.de(21)
    }

    @Test(expected = ReglaInvalida::class)
    fun `cocina no acepta un tiempo de cero minutos`() {
        Minutos.de(0)
    }

    @Test(expected = ReglaInvalida::class)
    fun `un precio negativo no existe`() {
        Dinero.de("-1")
    }

    @Test
    fun `el carrito suma subtotales y unidades`() {
        val carrito = Carrito().agregar(bandeja).agregar(bandeja).agregar(limonada)
        assertEquals(Dinero.de("70500"), carrito.total)
        assertEquals(3, carrito.unidades)
        assertEquals(2, carrito.cantidadDe(1))
    }

    @Test
    fun `quitar la ultima unidad saca el platillo del carrito`() {
        val carrito = Carrito().agregar(limonada).quitar(limonada)
        assertTrue(carrito.vacio)
    }

    @Test(expected = ReglaInvalida::class)
    fun `un carrito vacio no se puede pedir`() {
        Carrito().validarParaPedir()
    }

    @Test
    fun `las transiciones copian las del backend`() {
        assertTrue(EstadoOrden.POR_CONFIRMAR.puedePasarA(EstadoOrden.PENDIENTE))
        assertTrue(EstadoOrden.PENDIENTE.puedePasarA(EstadoOrden.EN_PREPARACION))
        assertFalse(EstadoOrden.PENDIENTE.puedePasarA(EstadoOrden.LISTA))
        assertFalse(EstadoOrden.ENTREGADA.puedePasarA(EstadoOrden.CANCELADA))
    }

    @Test
    fun `solo se entrega una orden lista y pagada`() {
        val lista = Orden(1, EstadoOrden.LISTA, 4, null, Dinero.de("1"), pagada = false, null, emptyList())
        assertFalse(lista.puedeEntregarse)
        assertTrue(lista.copy(pagada = true).puedeEntregarse)
    }

    @Test
    fun `cada rol ve solo su modulo`() {
        assertTrue(Rol.MESERO.atiendeMesas)
        assertFalse(Rol.MESERO.cobra)
        assertTrue(Rol.ADMINISTRADOR.cocina && Rol.ADMINISTRADOR.cobra)
    }
}
