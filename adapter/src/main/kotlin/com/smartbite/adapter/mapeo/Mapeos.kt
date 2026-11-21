package com.smartbite.adapter.mapeo

import com.smartbite.adapter.dto.MesaDto
import com.smartbite.adapter.dto.MesaQrDto
import com.smartbite.adapter.dto.OrdenDto
import com.smartbite.adapter.dto.PedidoCreadoDto
import com.smartbite.adapter.dto.ProductoDto
import com.smartbite.adapter.dto.SesionDto
import com.smartbite.domain.error.FalloRemoto
import com.smartbite.domain.error.ReglaInvalida
import com.smartbite.domain.modelo.EstadoMesa
import com.smartbite.domain.modelo.EstadoOrden
import com.smartbite.domain.modelo.LineaOrden
import com.smartbite.domain.modelo.Mesa
import com.smartbite.domain.modelo.MesaQr
import com.smartbite.domain.modelo.Orden
import com.smartbite.domain.modelo.OrdenEnMesa
import com.smartbite.domain.modelo.PedidoCreado
import com.smartbite.domain.modelo.Producto
import com.smartbite.domain.modelo.Rol
import com.smartbite.domain.modelo.Sesion
import com.smartbite.domain.valor.Dinero

private fun <T> traducir(bloque: () -> T): T = try {
    bloque()
} catch (dato: ReglaInvalida) {
    throw FalloRemoto.RespuestaInvalida()
} catch (vacio: NullPointerException) {
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
