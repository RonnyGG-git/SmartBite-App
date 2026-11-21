package com.smartbite.adapter.mapeo

import com.smartbite.adapter.dto.CuentaDto
import com.smartbite.adapter.dto.FacturaDto
import com.smartbite.adapter.dto.FacturaResumenDto
import com.smartbite.adapter.dto.MesaDto
import com.smartbite.adapter.dto.MesaQrDto
import com.smartbite.adapter.dto.MetodoPagoDto
import com.smartbite.adapter.dto.OrdenDto
import com.smartbite.adapter.dto.PedidoCreadoDto
import com.smartbite.adapter.dto.ProductoDto
import com.smartbite.adapter.dto.QrDto
import com.smartbite.adapter.dto.ReservaDto
import com.smartbite.adapter.dto.SesionDto
import com.smartbite.domain.error.FalloRemoto
import com.smartbite.domain.error.ReglaInvalida
import com.smartbite.domain.modelo.CodigoQr
import com.smartbite.domain.modelo.Cuenta
import com.smartbite.domain.modelo.EstadoMesa
import com.smartbite.domain.modelo.EstadoOrden
import com.smartbite.domain.modelo.Factura
import com.smartbite.domain.modelo.FacturaResumen
import com.smartbite.domain.modelo.LineaFactura
import com.smartbite.domain.modelo.LineaOrden
import com.smartbite.domain.modelo.Mesa
import com.smartbite.domain.modelo.MesaQr
import com.smartbite.domain.modelo.MetodoPago
import com.smartbite.domain.modelo.Orden
import com.smartbite.domain.modelo.OrdenEnMesa
import com.smartbite.domain.modelo.PagoRegistrado
import com.smartbite.domain.modelo.PedidoCreado
import com.smartbite.domain.modelo.Producto
import com.smartbite.domain.modelo.Reserva
import com.smartbite.domain.modelo.Rol
import com.smartbite.domain.modelo.Sesion
import com.smartbite.domain.valor.Dinero
import java.util.Base64

private fun <T> traducir(bloque: () -> T): T = try {
    bloque()
} catch (dato: ReglaInvalida) {
    throw FalloRemoto.RespuestaInvalida()
} catch (vacio: NullPointerException) {
    throw FalloRemoto.RespuestaInvalida()
} catch (mal: IllegalArgumentException) {
    throw FalloRemoto.RespuestaInvalida()
}

fun SesionDto.aDominio(): Sesion {
    val perfil = usuario ?: throw FalloRemoto.RespuestaInvalida()
    val clave = token?.takeIf { it.isNotBlank() } ?: throw FalloRemoto.RespuestaInvalida()
    return Sesion(
        token = clave,
        nombre = perfil.nombre.orEmpty(),
        rol = Rol.de(perfil.rol),
        restaurante = perfil.restauranteActual?.nombre,
    )
}

fun ProductoDto.aDominio(): Producto = traducir {
    Producto(
        id = id!!,
        nombre = nombre!!,
        descripcion = descripcion.orEmpty(),
        precio = Dinero.de(precio),
        categoria = categoria?.nombre ?: "Otros",
        imagen = urlImagen?.takeIf { it.isNotBlank() },
    )
}

fun OrdenDto.aDominio(): Orden = traducir {
    Orden(
        id = id!!,
        estado = EstadoOrden.de(estado),
        mesa = mesa?.numero,
        cliente = cliente,
        total = Dinero.de(total),
        pagada = pagada == true,
        minutosEstimados = tiempoEstimadoMin,
        lineas = detalles.orEmpty().map { LineaOrden(it.nombre.orEmpty(), it.cantidad ?: 0, Dinero.de(it.subtotal)) },
        faltantes = faltantes.orEmpty(),
        cuentaSolicitada = !cuentaSolicitadaEn.isNullOrBlank(),
        paraLlevar = tipoConsumo == "PARA_LLEVAR",
    )
}

fun MesaDto.aDominio(): Mesa = traducir {
    Mesa(
        id = id!!,
        numero = numero!!,
        estado = EstadoMesa.de(estado),
        sucursalId = sucursal!!,
        orden = ordenActiva?.let {
            OrdenEnMesa(it.id!!, EstadoOrden.de(it.estado), Dinero.de(it.total), it.cuentaSolicitada == true)
        },
    )
}

fun MesaQrDto.aDominio(): MesaQr = traducir {
    MesaQr(
        id = id!!,
        numero = numero!!,
        disponible = disponible == true,
        sucursalId = sucursal!!.id!!,
        sucursal = sucursal.nombre.orEmpty(),
        restaurante = sucursal.restaurante.orEmpty(),
    )
}

fun PedidoCreadoDto.aDominio(): PedidoCreado = traducir {
    PedidoCreado(codigo = codigo!!, orden = orden!!.aDominio())
}

fun ReservaDto.aDominio(): Reserva = traducir {
    Reserva(
        id = id!!,
        codigo = codigo.orEmpty(),
        mesa = mesa?.numero,
        fechaHora = fechaHora.orEmpty(),
        personas = numPersonas ?: 0,
        nombre = nombre.orEmpty(),
        telefono = telefono.orEmpty(),
    )
}

fun MetodoPagoDto.aDominio(): MetodoPago = traducir {
    MetodoPago(id = id!!, nombre = nombre!!, usaPasarela = usaPasarela == true)
}

fun CuentaDto.aDominio(): Cuenta = traducir {
    Cuenta(
        orden = orden!!.aDominio(),
        saldoPendiente = Dinero.de(saldoPendiente),
        saldoDisponible = Dinero.de(saldoDisponible),
        cobrable = cobrable == true,
        pagos = pagos.orEmpty().map {
            PagoRegistrado(
                id = it.id!!,
                metodo = it.metodo.orEmpty(),
                monto = Dinero.de(it.monto),
                propina = Dinero.de(it.propina ?: "0"),
                estado = it.estadoNombre ?: it.estado.orEmpty(),
                aprobado = it.estado == "APROBADO",
            )
        },
        facturaId = factura,
    )
}

fun QrDto.aDominio(): CodigoQr = traducir {
    CodigoQr(url = url!!, imagenPng = Base64.getDecoder().decode(qrPngBase64!!), saldo = Dinero.de(saldoPendiente))
}

fun FacturaResumenDto.aDominio(): FacturaResumen = traducir {
    FacturaResumen(
        id = id!!,
        codigo = codigo.orEmpty(),
        ordenId = orden ?: 0,
        cliente = clienteNombre.orEmpty(),
        total = Dinero.de(totalPagado),
        fecha = creadoEn.orEmpty().take(16).replace("T", " "),
    )
}

fun FacturaDto.aDominio(): Factura = traducir {
    Factura(
        resumen = FacturaResumenDto(id, codigo, orden, clienteNombre, totalPagado, creadoEn).aDominio(),
        documento = clienteDocumento.orEmpty(),
        emisor = detalle?.emisor?.nombre.orEmpty(),
        nit = detalle?.emisor?.nit.orEmpty(),
        base = Dinero.de(baseGravable),
        impuesto = Dinero.de(inc),
        consumo = Dinero.de(totalConsumo),
        propina = Dinero.de(propina ?: "0"),
        cufe = cufe.orEmpty(),
        lineas = detalle?.lineas.orEmpty().map {
            LineaFactura(it.nombre.orEmpty(), it.cantidad ?: 0, Dinero.de(it.precioUnitario), Dinero.de(it.subtotal))
        },
    )
}
