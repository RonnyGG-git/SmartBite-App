package com.smartbite.application.servicio

import com.smartbite.application.dto.SesionVista
import com.smartbite.application.puerto.entrada.OperacionCasos
import com.smartbite.domain.error.FalloRemoto
import com.smartbite.domain.error.ReglaInvalida
import com.smartbite.domain.modelo.Carrito
import com.smartbite.domain.modelo.Cobro
import com.smartbite.domain.modelo.CodigoQr
import com.smartbite.domain.modelo.Cuenta
import com.smartbite.domain.modelo.EstadoOrden
import com.smartbite.domain.modelo.Factura
import com.smartbite.domain.modelo.FacturaResumen
import com.smartbite.domain.modelo.Mesa
import com.smartbite.domain.modelo.MesaQr
import com.smartbite.domain.modelo.MetodoPago
import com.smartbite.domain.modelo.Orden
import com.smartbite.domain.modelo.PedidoCreado
import com.smartbite.domain.modelo.Producto
import com.smartbite.domain.modelo.Reserva
import com.smartbite.domain.modelo.ResultadoCobro
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
) : OperacionCasos {

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
            ?: throw ReglaInvalida("Escanea el QR de tu mesa o escribe su código.")
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

    override suspend fun abrirOrden(mesa: Mesa, cliente: String, telefono: String): Orden {
        if (!mesa.libre) throw ReglaInvalida("La mesa ${mesa.numero} no está libre.")
        return salon.abrirOrden(mesa.id, cliente.trim(), telefono.trim())
    }

    override suspend fun agregarPlatillo(orden: Orden, productoId: Int, cantidad: Int): Orden {
        if (orden.estado != EstadoOrden.PENDIENTE) throw ReglaInvalida("La orden ya entró a cocina; no se le agregan platillos.")
        return salon.agregar(orden.id, productoId, Cantidad.de(cantidad, Cantidad.MAXIMA_MESERO))
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

    override suspend fun cancelarOrden(orden: Orden): Orden {
        exigir(orden.cancelablePorMesero, "Solo se cancelan órdenes que todavía no entran a cocina.")
        return salon.cancelar(orden.id)
    }

    override suspend fun entregarOrden(orden: Orden): Orden = entregar(orden)

    override suspend fun verPedidosSinMesa(): List<Orden> = salon.sinMesa()

    override suspend fun asignarMesa(orden: Orden, mesa: Mesa): Orden {
        exigir(orden.sinMesa, "Esta orden ya tiene mesa.")
        exigir(mesa.libre, "La mesa ${mesa.numero} no está libre.")
        return salon.asignarMesa(orden.id, mesa.id)
    }

    override suspend fun verReservas(): List<Reserva> = salon.reservas()

    override suspend fun confirmarLlegada(reserva: Reserva) = salon.confirmarReserva(reserva.id)

    override suspend fun cancelarReserva(reserva: Reserva) = salon.cancelarReserva(reserva.id)

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

    override suspend fun verParaLlevar(): List<Orden> = caja.paraLlevar()

    override suspend fun metodosDePago(): List<MetodoPago> = caja.metodos()

    override suspend fun verCuenta(ordenId: Int): Cuenta = caja.cuenta(ordenId)

    override suspend fun cobrar(
        cuenta: Cuenta,
        metodo: MetodoPago?,
        monto: String,
        propina: String,
        recibido: String,
        referencia: String,
    ): ResultadoCobro {
        val cobro = Cobro.de(cuenta, metodo, monto, propina, recibido, referencia)
        val resultado = caja.cobrar(cuenta.orden.id, cobro)
        return if (resultado is ResultadoCobro.Registrado) resultado.copy(cambio = cobro.cambio) else resultado
    }

    override suspend fun generarQr(cuenta: Cuenta): CodigoQr {
        exigir(cuenta.orden.estado.activa, "La orden ya está cerrada.")
        return caja.qr(cuenta.orden.id)
    }

    override suspend fun aceptarParaLlevar(orden: Orden): Orden {
        exigir(orden.esperaConfirmacion && orden.sinMesa, "Solo se aceptan pedidos sin mesa por confirmar.")
        return caja.aceptarParaLlevar(orden.id)
    }

    override suspend fun rechazarParaLlevar(orden: Orden): Orden {
        exigir(orden.esperaConfirmacion && orden.sinMesa, "Solo se rechazan pedidos sin mesa por confirmar.")
        return caja.rechazarParaLlevar(orden.id)
    }

    override suspend fun entregar(orden: Orden): Orden {
        exigir(orden.puedeEntregarse, "Solo se entrega una orden lista y pagada.")
        return salon.entregar(orden.id)
    }

    override suspend fun verVentas(): List<Orden> = caja.ventas()

    override suspend fun verFacturas(): List<FacturaResumen> = caja.facturas()

    override suspend fun verFactura(facturaId: Int): Factura = caja.factura(facturaId)

    private fun exigir(condicion: Boolean, mensaje: String) {
        if (!condicion) throw ReglaInvalida(mensaje)
    }

}
