package com.smartbite.application.servicio

import com.smartbite.application.dto.SesionVista
import com.smartbite.application.puerto.entrada.CuentasCasos
import com.smartbite.domain.error.ReglaInvalida
import com.smartbite.domain.modelo.Registro
import com.smartbite.domain.modelo.RestauranteRef
import com.smartbite.domain.modelo.Sesion
import com.smartbite.domain.modelo.TipoCuenta
import com.smartbite.domain.modelo.validarCodigo
import com.smartbite.domain.puerto.salida.CuentaGateway
import com.smartbite.domain.puerto.salida.SesionStore
import com.smartbite.domain.valor.Correo

class CuentasServicio(
    private val cuentas: CuentaGateway,
    private val sesiones: SesionStore,
) : CuentasCasos {

    override suspend fun registrar(
        tipo: TipoCuenta,
        nombre: String,
        correo: String,
        clave: String,
        repetida: String,
        codigo: String,
        telefono: String,
    ): String = cuentas.registrar(Registro.de(tipo, nombre, correo, clave, repetida, codigo, telefono))

    override suspend fun verificarCorreo(correo: String, codigo: String): SesionVista {
        val sesion = cuentas.verificar(Correo.de(correo), validarCodigo(codigo))
        sesiones.guardar(sesion)
        return sesion.vista()
    }

    override suspend fun reenviarCodigo(correo: String): String = cuentas.reenviar(Correo.de(correo))

    override suspend fun recuperarClave(correo: String): String = cuentas.recuperar(Correo.de(correo))

    override suspend fun restablecerClave(correo: String, codigo: String, clave: String, repetida: String): String {
        if (clave != repetida) throw ReglaInvalida("Las contraseñas no coinciden.")
        return cuentas.restablecer(Correo.de(correo), validarCodigo(codigo), clave, repetida)
    }

    override suspend fun trabajarCon(restaurante: RestauranteRef): SesionVista {
        val actual = sesiones.leer() ?: throw ReglaInvalida("Inicia sesión de nuevo.")
        if (actual.restaurantes.none { it.id == restaurante.id }) throw ReglaInvalida("Ese restaurante no es tuyo.")
        val nueva = actual.conRestaurante(restaurante)
        sesiones.guardar(nueva)
        return nueva.vista()
    }
}

fun Sesion.vista() = SesionVista(
    nombre = nombre,
    rol = rol,
    restaurante = restaurante,
    restaurantes = restaurantes,
    restauranteId = restauranteId,
)
