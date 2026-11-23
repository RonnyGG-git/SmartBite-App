package com.smartbite.adapter.http

import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.smartbite.adapter.dto.ErrorDto
import com.smartbite.domain.error.CamposInvalidos
import com.smartbite.domain.error.FalloRemoto
import com.smartbite.domain.error.SinDisponibilidad
import com.smartbite.domain.puerto.salida.SesionStore
import kotlinx.coroutines.CancellationException
import retrofit2.Response
import java.io.IOException
import java.io.InterruptedIOException

class LlamadaApi(
    private val sesiones: SesionStore,
    private val gson: Gson,
    private val cabecera: CabeceraRestaurante,
    private val alVencerSesion: () -> Unit,
) {

    suspend fun <T> publica(llamada: suspend () -> Response<T>): T = ejecutar(token = null, llamada = llamada)

    suspend fun <T> conSesion(llamada: suspend (encabezado: String) -> Response<T>): T {
        val sesion = sesiones.leer() ?: run {
            alVencerSesion()
            throw FalloRemoto.SesionVencida()
        }
        cabecera.restauranteId = sesion.restauranteId
        return ejecutar(token = sesion.token) { llamada("Token ${sesion.token}") }
    }

    private suspend fun <T> ejecutar(token: String?, llamada: suspend () -> Response<T>): T {
        val respuesta = try {
            llamada()
        } catch (cancelada: CancellationException) {
            throw cancelada
        } catch (lenta: InterruptedIOException) {
            throw FalloRemoto.TiempoAgotado()
        } catch (rara: JsonParseException) {
            throw FalloRemoto.RespuestaInvalida()
        } catch (sinRed: IOException) {
            throw FalloRemoto.SinConexion()
        }

        if (respuesta.isSuccessful) {
            @Suppress("UNCHECKED_CAST")
            return respuesta.body() ?: if (respuesta.code() == 204) Unit as T else throw FalloRemoto.RespuestaInvalida()
        }
        throw fallo(respuesta.code(), leerError(respuesta), token)
    }

    private fun leerError(respuesta: Response<*>): ErrorDto? = try {
        gson.fromJson(respuesta.errorBody()?.string(), ErrorDto::class.java)
    } catch (ilegible: Exception) {
        null
    }

    private fun primerMensaje(valor: Any?): String? = when (valor) {
        is String -> valor
        is List<*> -> valor.firstNotNullOfOrNull { primerMensaje(it) }
        is Map<*, *> -> valor.values.firstNotNullOfOrNull { primerMensaje(it) }
        else -> null
    }

    private suspend fun fallo(codigo: Int, error: ErrorDto?, token: String?): RuntimeException {
        val campos = error?.errores?.mapNotNull { (clave, valor) ->
            primerMensaje(valor)?.let { (if (clave == "non_field_errors" || clave == "__all__") "general" else clave) to it }
        }?.toMap().orEmpty()
        return when {
            codigo == 401 && token != null -> {
                if (sesiones.leer()?.token == token) {
                    sesiones.borrar()
                    alVencerSesion()
                }
                FalloRemoto.SesionVencida()
            }
            codigo == 403 && error?.code == "correo_sin_verificar" -> FalloRemoto.CorreoSinVerificar()
            codigo == 403 -> FalloRemoto.SinPermiso(error?.detail)
            codigo == 404 -> FalloRemoto.NoEncontrado()
            codigo >= 500 -> FalloRemoto.Caido()
            campos.isNotEmpty() -> CamposInvalidos(campos)
            !error?.alternativas.isNullOrEmpty() -> SinDisponibilidad(error?.detail ?: "No hay disponibilidad.", error!!.alternativas!!)
            else -> FalloRemoto.Rechazado(error?.detail)
        }
    }
}
