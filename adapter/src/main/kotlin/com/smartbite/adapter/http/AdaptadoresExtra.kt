package com.smartbite.adapter.http

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.smartbite.adapter.dto.CompraDto
import com.smartbite.adapter.dto.CorreoBody
import com.smartbite.adapter.dto.LineaBody
import com.smartbite.adapter.dto.OpinionBody
import com.smartbite.adapter.dto.PedidoPublicoBody
import com.smartbite.adapter.dto.RegistroBody
import com.smartbite.adapter.dto.ReservaBody
import com.smartbite.adapter.dto.ReservaPublicaDto
import com.smartbite.adapter.dto.RestablecerBody
import com.smartbite.adapter.dto.VerificarBody
import com.smartbite.adapter.mapeo.aDominio
import com.smartbite.domain.error.FalloRemoto
import com.smartbite.domain.modelo.Carrito
import com.smartbite.domain.modelo.CodigoQr
import com.smartbite.domain.modelo.Compra
import com.smartbite.domain.modelo.Ficha
import com.smartbite.domain.modelo.FilaTablero
import com.smartbite.domain.modelo.LineaCompra
import com.smartbite.domain.modelo.NuevaReserva
import com.smartbite.domain.modelo.Opcion
import com.smartbite.domain.modelo.Opinion
import com.smartbite.domain.modelo.PedidoCreado
import com.smartbite.domain.modelo.Recurso
import com.smartbite.domain.modelo.Registro
import com.smartbite.domain.modelo.ReservaCliente
import com.smartbite.domain.modelo.Sesion
import com.smartbite.domain.modelo.SucursalPublica
import com.smartbite.domain.modelo.Tablero
import com.smartbite.domain.modelo.TipoCuenta
import com.smartbite.domain.puerto.salida.CuentaGateway
import com.smartbite.domain.puerto.salida.GestionGateway
import com.smartbite.domain.puerto.salida.PublicoGateway
import com.smartbite.domain.valor.Correo
import com.smartbite.domain.valor.Dinero
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.Base64

class CuentaHttp(private val api: ApiExtra, private val llamar: LlamadaApi) : CuentaGateway {

    override suspend fun registrar(registro: Registro): String {
        val tipo = when (registro.tipo) {
            TipoCuenta.CLIENTE -> "cliente"
            TipoCuenta.EMPLEADO -> "empleado"
            TipoCuenta.ADMINISTRADOR -> "admin"
        }
        val cuerpo = RegistroBody(
            nombre = registro.nombre,
            email = registro.correo.texto,
            password1 = registro.clave,
            password2 = registro.clave,
            codigo = registro.codigo.takeIf { registro.tipo == TipoCuenta.EMPLEADO },
            telefono = registro.telefono.takeIf { registro.tipo == TipoCuenta.CLIENTE },
        )
        return llamar.publica { api.registrar(tipo, cuerpo) }.detail ?: "Te enviamos un código de 4 dígitos."
    }

    override suspend fun verificar(correo: Correo, codigo: String): Sesion =
        llamar.publica { api.verificar(VerificarBody(correo.texto, codigo)) }.aDominio()

    override suspend fun reenviar(correo: Correo): String =
        llamar.publica { api.reenviar(CorreoBody(correo.texto)) }.detail.orEmpty()

    override suspend fun recuperar(correo: Correo): String =
        llamar.publica { api.recuperar(CorreoBody(correo.texto)) }.detail.orEmpty()

    override suspend fun restablecer(correo: Correo, codigo: String, clave: String, repetida: String): String =
        llamar.publica { api.restablecer(RestablecerBody(correo.texto, codigo, clave, repetida)) }.detail.orEmpty()
}

class PublicoHttp(private val api: ApiExtra, private val llamar: LlamadaApi) : PublicoGateway {

    override suspend fun sucursales(busqueda: String): List<SucursalPublica> =
        llamar.publica { api.sucursales(busqueda.ifBlank { null }) }.mapNotNull { dto ->
            val id = dto.id ?: return@mapNotNull null
            SucursalPublica(id, dto.nombre.orEmpty(), dto.restaurante.orEmpty(), dto.direccion.orEmpty(), dto.telefono.orEmpty(), dto.distanciaKm)
        }

    override suspend fun pedirParaLlevar(sucursalId: Int, nombre: String, carrito: Carrito): PedidoCreado {
        val lineas = carrito.lineas.map { LineaBody(it.producto.id, it.cantidad.valor) }
        return llamar.publica { api.pedirParaLlevar(PedidoPublicoBody(sucursalId, "PARA_LLEVAR", nombre, lineas)) }.aDominio()
    }

    override suspend fun reservar(sucursalId: Int, reserva: NuevaReserva): ReservaCliente =
        llamar.publica {
            api.reservar(sucursalId, ReservaBody(reserva.fecha, reserva.hora, reserva.personas, reserva.nombre, reserva.telefono))
        }.aDominio()

    override suspend fun reserva(codigo: String): ReservaCliente = llamar.publica { api.reserva(codigo) }.aDominio()

    override suspend fun cancelarReserva(codigo: String): ReservaCliente = llamar.publica { api.cancelarReserva(codigo) }.aDominio()

    override suspend fun opinar(opinion: Opinion) {
        llamar.publica { api.opinar(OpinionBody(opinion.tipo.name, opinion.mensaje, opinion.nombre, opinion.contacto, opinion.mesaId)) }
    }

    private fun ReservaPublicaDto.aDominio(): ReservaCliente {
        val clave = codigo ?: throw FalloRemoto.RespuestaInvalida()
        return ReservaCliente(
            codigo = clave,
            mesa = mesaNumero,
            sucursal = sucursal.orEmpty(),
            restaurante = restaurante.orEmpty(),
            fechaHora = fechaHora.orEmpty(),
            personas = numPersonas ?: 0,
            nombre = nombre.orEmpty(),
            telefono = telefono.orEmpty(),
            estado = estado.orEmpty(),
            estadoNombre = estadoNombre ?: estado.orEmpty(),
        )
    }
}

class GestionHttp(private val api: ApiExtra, private val llamar: LlamadaApi) : GestionGateway {

    private fun ruta(recurso: Recurso, id: Int? = null, accion: String? = null) =
        "api/v1/" + recurso.ruta + (id?.let { "$it/" } ?: "") + (accion?.let { "$it/" } ?: "")

    private fun lista(json: JsonElement): List<JsonObject> {
        val arreglo = when {
            json.isJsonArray -> json.asJsonArray
            json.isJsonObject && json.asJsonObject.has("results") -> json.asJsonObject.getAsJsonArray("results")
            else -> throw FalloRemoto.RespuestaInvalida()
        }
        return arreglo.filter { it.isJsonObject }.map { it.asJsonObject }
    }

    private fun ficha(json: JsonElement): Ficha {
        if (!json.isJsonObject) throw FalloRemoto.RespuestaInvalida()
        val objeto = json.asJsonObject
        val id = objeto.get("id")?.takeIf { it.isJsonPrimitive }?.asInt ?: throw FalloRemoto.RespuestaInvalida()
        val valores = objeto.entrySet().associate { (clave, valor) ->
            clave to when {
                valor.isJsonNull -> null
                valor.isJsonPrimitive -> valor.asString
                else -> valor.toString()
            }
        }
        return Ficha(id, valores)
    }

    override suspend fun listar(recurso: Recurso): List<Ficha> =
        lista(llamar.conSesion { token -> api.leer(token, ruta(recurso)) }).map(::ficha)

    override suspend fun crear(recurso: Recurso, valores: Map<String, Any?>): Ficha =
        ficha(llamar.conSesion { token -> api.enviar(token, ruta(recurso), valores) })

    override suspend fun editar(recurso: Recurso, id: Int, valores: Map<String, Any?>): Ficha =
        ficha(llamar.conSesion { token -> api.reemplazar(token, ruta(recurso, id), valores) })

    override suspend fun eliminar(recurso: Recurso, id: Int) {
        llamar.conSesion { token -> api.borrar(token, ruta(recurso, id)) }
    }

    override suspend fun accion(recurso: Recurso, id: Int, accion: String, valores: Map<String, Any?>): Ficha =
        ficha(llamar.conSesion { token -> api.enviar(token, ruta(recurso, id, accion), valores) })

    override suspend fun qrMesa(id: Int): CodigoQr {
        val json = llamar.conSesion { token -> api.leer(token, "api/v1/gestion/mesas/$id/qr/") }.asJsonObject
        val url = json.get("url")?.asString ?: throw FalloRemoto.RespuestaInvalida()
        val png = json.get("qr_png_base64")?.asString ?: throw FalloRemoto.RespuestaInvalida()
        return CodigoQr(url, Base64.getDecoder().decode(png), Dinero.CERO)
    }

    override suspend fun subirFoto(productoId: Int, bytes: ByteArray): Ficha {
        val parte = MultipartBody.Part.createFormData("archivo", "foto.jpg", bytes.toRequestBody("image/jpeg".toMediaType()))
        return ficha(llamar.conSesion { token -> api.subir(token, "api/v1/gestion/productos/$productoId/foto/", parte) })
    }

    private suspend fun opcionesDe(clave: String): List<Opcion> {
        val json = llamar.conSesion { token -> api.leer(token, "api/v1/gestion/opciones/") }.asJsonObject
        return json.getAsJsonArray(clave).map { Opcion(it.asJsonObject.get("valor").asString, it.asJsonObject.get("etiqueta").asString) }
    }

    override suspend fun unidades(): List<Opcion> = opcionesDe("unidades")

    override suspend fun rolesEmpleado(): List<Opcion> =
        lista(llamar.conSesion { token -> api.leer(token, "api/v1/gestion/usuarios/roles/") }).map {
            Opcion(it.get("id").asString, it.get("etiqueta").asString)
        }

    override suspend fun tablero(): Tablero {
        val dto = llamar.conSesion { token -> api.tablero(token) }
        return Tablero(
            restaurante = dto.restaurante,
            usuarios = dto.totalUsuarios ?: 0,
            productos = dto.totalProductos ?: 0,
            mesas = dto.totalMesas ?: 0,
            mesasOcupadas = dto.mesasOcupadas ?: 0,
            ordenesHoy = dto.ordenesHoy ?: 0,
            ventasHoy = Dinero.de(dto.ventasHoy ?: "0"),
            stockBajo = dto.stockBajo.orEmpty().map { FilaTablero(it.nombre.orEmpty(), "${it.stock ?: 0} / mín ${it.minimo ?: 0}") },
            ordenesPorEstado = dto.ordenesPorEstado.orEmpty().map { FilaTablero(it.nombre.orEmpty(), (it.cantidad ?: 0).toString()) },
            ventasPorMetodo = dto.ventasPorMetodo.orEmpty().map { FilaTablero(it.metodo.orEmpty(), it.total ?: "0") },
        )
    }

    private fun CompraDto.aDominio(): Compra = Compra(
        id = id ?: throw FalloRemoto.RespuestaInvalida(),
        proveedor = proveedorNombre.orEmpty(),
        estado = estado.orEmpty(),
        estadoNombre = estadoNombre.orEmpty(),
        total = Dinero.de(total ?: "0"),
        fecha = creadoEn.orEmpty().take(16).replace("T", " "),
        lineas = detalles.orEmpty().map { LineaCompra(it.item ?: 0, it.itemNombre.orEmpty(), it.cantidad ?: 0, Dinero.de(it.costoUnitario ?: "0")) },
    )

    override suspend fun compras(): List<Compra> =
        llamar.conSesion { token -> api.compras(token) }.results.orEmpty().map { it.aDominio() }

    override suspend fun crearCompra(proveedorId: Int, lineas: List<Triple<Int, Int, String>>): Compra {
        val cuerpo = mapOf(
            "proveedor" to proveedorId,
            "detalles" to lineas.map { (item, cantidad, costo) -> mapOf("item" to item, "cantidad" to cantidad, "costo_unitario" to costo) },
        )
        return llamar.conSesion { token -> api.crearCompra(token, cuerpo) }.aDominio()
    }

    override suspend fun recibirCompra(id: Int): Compra = llamar.conSesion { token -> api.accionCompra(token, id, "recibir") }.aDominio()

    override suspend fun anularCompra(id: Int): Compra = llamar.conSesion { token -> api.accionCompra(token, id, "anular") }.aDominio()
}
