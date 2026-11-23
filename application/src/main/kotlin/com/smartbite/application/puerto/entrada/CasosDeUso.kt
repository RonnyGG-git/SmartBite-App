package com.smartbite.application.puerto.entrada

import com.smartbite.application.dto.SesionVista
import com.smartbite.domain.modelo.Carrito
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

interface SesionCasos {
    suspend fun iniciarSesion(correo: String, clave: String): SesionVista
    suspend fun sesionActual(): SesionVista?
    suspend fun cerrarSesion()
}

interface ClienteCasos {
    suspend fun buscarMesa(codigoQr: String): MesaQr
    suspend fun verMenu(sucursalId: Int): List<Producto>
    suspend fun pedir(mesa: MesaQr, nombre: String, tipo: TipoConsumo, carrito: Carrito): PedidoCreado
    suspend fun seguirPedido(codigo: String): Orden
}

interface MeseroCasos {
    suspend fun verMesas(): List<Mesa>
    suspend fun verProductos(sucursalId: Int): List<Producto>
    suspend fun verOrden(ordenId: Int): Orden
    suspend fun abrirOrden(mesa: Mesa, cliente: String = "", telefono: String = ""): Orden
    suspend fun agregarPlatillo(orden: Orden, productoId: Int, cantidad: Int): Orden
    suspend fun confirmarPedido(ordenId: Int, estado: EstadoOrden): Orden
    suspend fun rechazarPedido(ordenId: Int, estado: EstadoOrden): Orden
    suspend fun pedirCuenta(ordenId: Int, estado: EstadoOrden): Orden
    suspend fun cancelarOrden(orden: Orden): Orden
    suspend fun entregarOrden(orden: Orden): Orden
    suspend fun verPedidosSinMesa(): List<Orden>
    suspend fun asignarMesa(orden: Orden, mesa: Mesa): Orden
    suspend fun verReservas(): List<Reserva>
    suspend fun confirmarLlegada(reserva: Reserva)
    suspend fun cancelarReserva(reserva: Reserva)
}

interface CocinaCasos {
    suspend fun verComandas(): List<Orden>
    suspend fun empezar(orden: Orden, minutos: Int): Orden
    suspend fun marcarLista(orden: Orden): Orden
}

interface CajaCasos {
    suspend fun verPorCobrar(): List<Orden>
    suspend fun verParaLlevar(): List<Orden>
    suspend fun metodosDePago(): List<MetodoPago>
    suspend fun verCuenta(ordenId: Int): Cuenta
    suspend fun cobrar(
        cuenta: Cuenta,
        metodo: MetodoPago?,
        monto: String,
        propina: String,
        recibido: String,
        referencia: String,
    ): ResultadoCobro
    suspend fun generarQr(cuenta: Cuenta): CodigoQr
    suspend fun aceptarParaLlevar(orden: Orden): Orden
    suspend fun rechazarParaLlevar(orden: Orden): Orden
    suspend fun entregar(orden: Orden): Orden
    suspend fun verVentas(): List<Orden>
    suspend fun verFacturas(): List<FacturaResumen>
    suspend fun verFactura(facturaId: Int): Factura
}

interface OperacionCasos : SesionCasos, ClienteCasos, MeseroCasos, CocinaCasos, CajaCasos
