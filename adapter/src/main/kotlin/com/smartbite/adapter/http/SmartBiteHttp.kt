package com.smartbite.adapter.http

import com.smartbite.adapter.dto.AgregarBody
import com.smartbite.adapter.dto.CrearOrdenBody
import com.smartbite.adapter.dto.IniciarBody
import com.smartbite.adapter.dto.LineaBody
import com.smartbite.adapter.dto.LoginBody
import com.smartbite.adapter.dto.PedidoMesaBody
import com.smartbite.adapter.mapeo.aDominio
import com.smartbite.domain.modelo.Carrito
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
import com.smartbite.domain.valor.Cantidad
import com.smartbite.domain.valor.Correo
import com.smartbite.domain.valor.Minutos

class SmartBiteHttp(
    private val api: SmartBiteApi,
    private val llamar: LlamadaApi,
) : AuthGateway, ClienteGateway, SalonGateway, CocinaGateway, CajaGateway {

    override suspend fun entrar(correo: Correo, clave: String): Sesion =
        llamar.publica { api.login(LoginBody(correo.texto, clave)) }.aDominio()

    override suspend fun salir() {
        llamar.conSesion { token -> api.logout(token) }
    }

    override suspend fun mesa(mesaId: Int): MesaQr = llamar.publica { api.mesaQr(mesaId) }.aDominio()

    override suspend fun menu(sucursalId: Int): List<Producto> =
        llamar.publica { api.menu(sucursalId) }.map { it.aDominio() }

    override suspend fun pedir(mesaId: Int, nombre: String, tipo: TipoConsumo, carrito: Carrito): PedidoCreado {
        val lineas = carrito.lineas.map { LineaBody(it.producto.id, it.cantidad.valor) }
        val cuerpo = PedidoMesaBody(tipoConsumo = tipo.name, nombre = nombre, lineas = lineas)
        return llamar.publica { api.pedirEnMesa(mesaId, cuerpo) }.aDominio()
    }

    override suspend fun seguir(codigo: String): Orden = llamar.publica { api.pedido(codigo) }.aDominio()

    override suspend fun mesas(): List<Mesa> = llamar.conSesion { token -> api.mesas(token) }.map { it.aDominio() }

    override suspend fun productos(sucursalId: Int): List<Producto> =
        llamar.conSesion { token -> api.productos(token, sucursalId) }.map { it.aDominio() }

    override suspend fun orden(ordenId: Int): Orden = llamar.conSesion { token -> api.orden(token, ordenId) }.aDominio()

    override suspend fun abrirOrden(mesaId: Int): Orden =
        llamar.conSesion { token -> api.abrirOrden(token, CrearOrdenBody(mesaId)) }.aDominio()

    override suspend fun agregar(ordenId: Int, productoId: Int, cantidad: Cantidad): Orden =
        llamar.conSesion { token -> api.agregar(token, ordenId, AgregarBody(productoId, cantidad.valor)) }.aDominio()

    override suspend fun confirmar(ordenId: Int): Orden = accion(ordenId, "confirmar")

    override suspend fun rechazar(ordenId: Int): Orden = accion(ordenId, "rechazar")

    override suspend fun pedirCuenta(ordenId: Int): Orden = accion(ordenId, "solicitar-cuenta")

    override suspend fun entregar(ordenId: Int): Orden = accion(ordenId, "entregar")

    override suspend fun pendientes(): List<Orden> = llamar.conSesion { token -> api.cocina(token) }.map { it.aDominio() }

    override suspend fun iniciar(ordenId: Int, minutos: Minutos): Orden =
        llamar.conSesion { token -> api.iniciar(token, ordenId, IniciarBody(minutos.valor)) }.aDominio()

    override suspend fun terminar(ordenId: Int): Orden = llamar.conSesion { token -> api.listo(token, ordenId) }.aDominio()

    override suspend fun porCobrar(): List<Orden> = llamar.conSesion { token -> api.porCobrar(token) }.map { it.aDominio() }

    private suspend fun accion(ordenId: Int, nombre: String): Orden =
        llamar.conSesion { token -> api.accionOrden(token, ordenId, nombre) }.aDominio()
}
