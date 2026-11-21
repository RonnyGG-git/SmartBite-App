package com.smartbite.domain.puerto.salida

import com.smartbite.domain.modelo.Carrito
import com.smartbite.domain.modelo.Cobro
import com.smartbite.domain.modelo.CodigoQr
import com.smartbite.domain.modelo.Cuenta
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
import com.smartbite.domain.modelo.Sesion
import com.smartbite.domain.modelo.TipoConsumo
import com.smartbite.domain.valor.Cantidad
import com.smartbite.domain.valor.Correo
import com.smartbite.domain.valor.Minutos

interface AuthGateway {
    suspend fun entrar(correo: Correo, clave: String): Sesion
    suspend fun salir()
}

interface ClienteGateway {
    suspend fun mesa(mesaId: Int): MesaQr
    suspend fun menu(sucursalId: Int): List<Producto>
    suspend fun pedir(mesaId: Int, nombre: String, tipo: TipoConsumo, carrito: Carrito): PedidoCreado
    suspend fun seguir(codigo: String): Orden
}

interface SalonGateway {
    suspend fun mesas(): List<Mesa>
    suspend fun productos(sucursalId: Int): List<Producto>
    suspend fun orden(ordenId: Int): Orden
    suspend fun abrirOrden(mesaId: Int, cliente: String, telefono: String): Orden
    suspend fun agregar(ordenId: Int, productoId: Int, cantidad: Cantidad): Orden
    suspend fun confirmar(ordenId: Int): Orden
    suspend fun rechazar(ordenId: Int): Orden
    suspend fun pedirCuenta(ordenId: Int): Orden
    suspend fun cancelar(ordenId: Int): Orden
    suspend fun entregar(ordenId: Int): Orden
    suspend fun sinMesa(): List<Orden>
    suspend fun asignarMesa(ordenId: Int, mesaId: Int): Orden
    suspend fun reservas(): List<Reserva>
    suspend fun confirmarReserva(reservaId: Int)
    suspend fun cancelarReserva(reservaId: Int)
}

interface CocinaGateway {
    suspend fun pendientes(): List<Orden>
    suspend fun iniciar(ordenId: Int, minutos: Minutos): Orden
    suspend fun terminar(ordenId: Int): Orden
}

interface CajaGateway {
    suspend fun porCobrar(): List<Orden>
    suspend fun paraLlevar(): List<Orden>
    suspend fun metodos(): List<MetodoPago>
    suspend fun cuenta(ordenId: Int): Cuenta
    suspend fun cobrar(ordenId: Int, cobro: Cobro): ResultadoCobro
    suspend fun qr(ordenId: Int): CodigoQr
    suspend fun aceptarParaLlevar(ordenId: Int): Orden
    suspend fun rechazarParaLlevar(ordenId: Int): Orden
    suspend fun ventas(): List<Orden>
    suspend fun facturas(): List<FacturaResumen>
    suspend fun factura(facturaId: Int): Factura
}

interface SesionStore {
    suspend fun leer(): Sesion?
    suspend fun guardar(sesion: Sesion)
    suspend fun borrar()
}
