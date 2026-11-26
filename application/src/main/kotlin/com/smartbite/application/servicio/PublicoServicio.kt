package com.smartbite.application.servicio

import com.smartbite.application.puerto.entrada.PublicoCasos
import com.smartbite.domain.error.FalloRemoto
import com.smartbite.domain.error.ReglaInvalida
import com.smartbite.domain.modelo.Carrito
import com.smartbite.domain.modelo.NuevaReserva
import com.smartbite.domain.modelo.Opinion
import com.smartbite.domain.modelo.PagoCliente
import com.smartbite.domain.modelo.PedidoCreado
import com.smartbite.domain.modelo.PedidoGuardado
import com.smartbite.domain.modelo.Producto
import com.smartbite.domain.modelo.ReservaCliente
import com.smartbite.domain.modelo.SucursalPublica
import com.smartbite.domain.modelo.TipoOpinion
import com.smartbite.domain.puerto.salida.ClienteGateway
import com.smartbite.domain.puerto.salida.HistorialLocal
import com.smartbite.domain.puerto.salida.PublicoGateway

class PublicoServicio(
    private val publico: PublicoGateway,
    private val cliente: ClienteGateway,
    private val historial: HistorialLocal,
) : PublicoCasos {

    override suspend fun buscarRestaurantes(busqueda: String): List<SucursalPublica> = publico.sucursales(busqueda.trim())

    override suspend fun menuDe(sucursalId: Int): List<Producto> = cliente.menu(sucursalId)

    override suspend fun pedirParaLlevar(sucursal: SucursalPublica, nombre: String, carrito: Carrito): PedidoCreado {
        carrito.validarParaPedir()
        val pedido = publico.pedirParaLlevar(sucursal.id, nombre.trim(), carrito)
        historial.recordarPedido(pedido.codigo)
        return pedido
    }

    override suspend fun recordarPedido(codigo: String) = historial.recordarPedido(codigo)

    override suspend fun misPedidos(): List<PedidoGuardado> = historial.pedidos().mapNotNull { codigo ->
        try {
            PedidoGuardado(codigo, cliente.seguir(codigo))
        } catch (borrado: FalloRemoto.NoEncontrado) {
            null
        }
    }

    override suspend fun pagarPedido(pedido: PedidoGuardado): PagoCliente {
        val orden = pedido.orden
        if (!orden.pagablePorCliente) {
            throw ReglaInvalida(
                when {
                    orden.pagada -> "Este pedido ya está pagado."
                    orden.esperaConfirmacion -> "Podrás pagar cuando el restaurante confirme tu pedido."
                    else -> "Este pedido ya está cerrado."
                }
            )
        }
        return publico.pagarPedido(pedido.codigo)
    }

    override suspend fun reservar(
        sucursalId: Int,
        fecha: String,
        hora: String,
        personas: String,
        nombre: String,
        telefono: String,
    ): ReservaCliente {
        val reserva = publico.reservar(sucursalId, NuevaReserva.de(fecha, hora, personas, nombre, telefono))
        historial.recordarReserva(reserva.codigo)
        return reserva
    }

    override suspend fun misReservas(): List<ReservaCliente> = historial.reservas().mapNotNull { codigo ->
        try {
            publico.reserva(codigo)
        } catch (borrada: FalloRemoto.NoEncontrado) {
            null
        }
    }

    override suspend fun cancelarReservaCliente(codigo: String): ReservaCliente = publico.cancelarReserva(codigo)

    override suspend fun opinar(tipo: TipoOpinion, mensaje: String, nombre: String, contacto: String, mesaId: Int?) =
        publico.opinar(Opinion.de(tipo, mensaje, nombre, contacto, mesaId))
}
