package com.smartbite.application.servicio

import com.smartbite.application.dto.SesionVista
import com.smartbite.application.puerto.entrada.CasosDeUso
import com.smartbite.domain.error.FalloRemoto
import com.smartbite.domain.error.ReglaInvalida
import com.smartbite.domain.modelo.Carrito
import com.smartbite.domain.modelo.EstadoOrden
import com.smartbite.domain.modelo.Mesa
import com.smartbite.domain.modelo.MesaQr
import com.smartbite.domain.modelo.Orden
import com.smartbite.domain.modelo.PedidoCreado
import com.smartbite.domain.modelo.Producto
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
import com.smartbite.domain.valor.Minutos

class SmartBiteServicio(
    private val auth: AuthGateway,
    private val cliente: ClienteGateway,
    private val salon: SalonGateway,
    private val cocina: CocinaGateway,
    private val caja: CajaGateway,
    private val sesiones: SesionStore,
) : CasosDeUso {

    override suspend fun iniciarSesion(correo: String, clave: String): SesionVista {
        val correoValido = Correo.de(correo)
        if (clave.isBlank()) throw ReglaInvalida("Escribe tu contraseña.")
        val sesion = auth.entrar(correoValido, clave)
        sesiones.guardar(sesion)
        return sesion.vista()
    }

    override suspend fun sesionActual(): SesionVista? = sesiones.leer()?.vista()

    override suspend fun cerrarSesion() {
        try {
            auth.salir()
        } catch (sinRed: FalloRemoto) {
            sesiones.borrar()
            return
        }
        sesiones.borrar()
    }

    override suspend fun buscarMesa(codigoQr: String): MesaQr {
        val numero = Regex("(\\d+)/?$").find(codigoQr.trim())?.groupValues?.get(1)?.toIntOrNull()
            ?: throw ReglaInvalida("Escribe el código del QR de tu mesa o pega su enlace.")
        val mesa = cliente.mesa(numero)
        if (!mesa.disponible) throw ReglaInvalida("La mesa ${mesa.numero} ya tiene un pedido en curso. Pide ayuda a tu mesero.")
        return mesa
    }

    override suspend fun verMenu(sucursalId: Int): List<Producto> = cliente.menu(sucursalId)

    override suspend fun pedir(mesa: MesaQr, nombre: String, tipo: TipoConsumo, carrito: Carrito): PedidoCreado {
        carrito.validarParaPedir()
        return cliente.pedir(mesa.id, nombre.trim(), tipo, carrito)
    }

    override suspend fun seguirPedido(codigo: String): Orden = cliente.seguir(codigo)

    override suspend fun verMesas(): List<Mesa> = salon.mesas()

    override suspend fun verProductos(sucursalId: Int): List<Producto> = salon.productos(sucursalId)

    override suspend fun verOrden(ordenId: Int): Orden = salon.orden(ordenId)

    override suspend fun abrirOrden(mesa: Mesa): Orden {
        if (!mesa.libre) throw ReglaInvalida("La mesa ${mesa.numero} no está libre.")
        return salon.abrirOrden(mesa.id)
    }

    override suspend fun agregarPlatillo(orden: Orden, productoId: Int, cantidad: Int): Orden {
        if (orden.estado != EstadoOrden.PENDIENTE) throw ReglaInvalida("La orden ya entró a cocina; no se le agregan platillos.")
        return salon.agregar(orden.id, productoId, Cantidad.de(cantidad))
    }

    override suspend fun confirmarPedido(ordenId: Int, estado: EstadoOrden): Orden {
        exigir(estado == EstadoOrden.POR_CONFIRMAR, "Este pedido ya no está esperando confirmación.")
        return salon.confirmar(ordenId)
    }

    override suspend fun rechazarPedido(ordenId: Int, estado: EstadoOrden): Orden {
        exigir(estado == EstadoOrden.POR_CONFIRMAR, "Este pedido ya no está esperando confirmación.")
        return salon.rechazar(ordenId)
    }

    override suspend fun pedirCuenta(ordenId: Int, estado: EstadoOrden): Orden {
        exigir(estado == EstadoOrden.LISTA, "Solo se pide la cuenta de una orden lista y servida.")
        return salon.pedirCuenta(ordenId)
    }

    override suspend fun verComandas(): List<Orden> = cocina.pendientes()

    override suspend fun empezar(orden: Orden, minutos: Int): Orden {
        exigir(orden.estado.puedePasarA(EstadoOrden.EN_PREPARACION), "Esta orden ya no está pendiente.")
        return cocina.iniciar(orden.id, Minutos.de(minutos))
    }

    override suspend fun marcarLista(orden: Orden): Orden {
        exigir(orden.estado == EstadoOrden.EN_PREPARACION, "Primero hay que empezar a prepararla.")
        return cocina.terminar(orden.id)
    }

    override suspend fun verPorCobrar(): List<Orden> = caja.porCobrar()

    override suspend fun entregar(orden: Orden): Orden {
        exigir(orden.puedeEntregarse, "Solo se entrega una orden lista y pagada.")
        return salon.entregar(orden.id)
    }

    private fun exigir(condicion: Boolean, mensaje: String) {
        if (!condicion) throw ReglaInvalida(mensaje)
    }

    private fun Sesion.vista() = SesionVista(nombre = nombre, rol = rol, restaurante = restaurante)
}
