package com.smartbite.movil.ui.cliente

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.smartbite.application.puerto.entrada.ClienteCasos
import com.smartbite.domain.error.ReglaInvalida
import com.smartbite.domain.modelo.Carrito
import com.smartbite.domain.modelo.MesaQr
import com.smartbite.domain.modelo.Orden
import com.smartbite.domain.modelo.Producto
import com.smartbite.domain.modelo.TipoConsumo
import com.smartbite.movil.ui.ModeloBase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class Paso { MESA, MENU, SEGUIMIENTO }

class ClienteModelo(private val cliente: ClienteCasos) : ModeloBase() {

    var paso by mutableStateOf(Paso.MESA)
        private set
    var codigoQr by mutableStateOf("")
    var nombre by mutableStateOf("")
    var tipo by mutableStateOf(TipoConsumo.EN_RESTAURANTE)
    var mesa by mutableStateOf<MesaQr?>(null)
        private set
    var menu by mutableStateOf<List<Producto>>(emptyList())
        private set
    var carrito by mutableStateOf(Carrito())
        private set
    var orden by mutableStateOf<Orden?>(null)
        private set
    var limite by mutableStateOf<String?>(null)
        private set

    private var codigoPedido: String? = null
    private var seguimiento: Job? = null

    fun leerQr(texto: String) {
        codigoQr = texto
        buscarMesa()
    }

    fun buscarMesa() = trabajar {
        val encontrada = cliente.buscarMesa(codigoQr)
        menu = cliente.verMenu(encontrada.sucursalId)
        mesa = encontrada
        paso = Paso.MENU
    }

    fun agregar(producto: Producto) {
        try {
            carrito = carrito.agregar(producto)
            limite = null
        } catch (regla: ReglaInvalida) {
            limite = regla.message
        }
    }

    fun quitar(producto: Producto) {
        carrito = carrito.quitar(producto)
        limite = null
    }

    fun pedir() = trabajar {
        val actual = mesa ?: return@trabajar
        val pedido = cliente.pedir(actual, nombre, tipo, carrito)
        codigoPedido = pedido.codigo
        orden = pedido.orden
        carrito = Carrito()
        paso = Paso.SEGUIMIENTO
        seguir()
    }

    fun actualizar() = trabajar {
        val codigo = codigoPedido ?: return@trabajar
        orden = cliente.seguirPedido(codigo)
    }

    private fun seguir() {
        seguimiento?.cancel()
        seguimiento = viewModelScope.launch {
            while (isActive && orden?.estado?.activa == true) {
                delay(10_000)
                val codigo = codigoPedido ?: break
                try {
                    orden = cliente.seguirPedido(codigo)
                } catch (pausa: Exception) {
                    if (!isActive) throw pausa
                }
            }
        }
    }

    fun volverAMesa() {
        seguimiento?.cancel()
        paso = Paso.MESA
        mesa = null
        menu = emptyList()
        carrito = Carrito()
        orden = null
        codigoPedido = null
    }
}
