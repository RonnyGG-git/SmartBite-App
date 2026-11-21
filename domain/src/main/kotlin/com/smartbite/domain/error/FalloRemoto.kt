package com.smartbite.domain.error

sealed class FalloRemoto(mensaje: String) : RuntimeException(mensaje) {
    class CorreoSinVerificar : FalloRemoto("Antes de entrar, confirma tu correo desde la web.")
    class SesionVencida : FalloRemoto("Tu sesión terminó. Inicia sesión de nuevo.")
    class SinPermiso(mensaje: String?) : FalloRemoto(mensaje ?: "Tu rol no tiene permiso para esta acción.")
    class NoEncontrado : FalloRemoto("No encontramos lo que buscabas. Revisa el código.")
    class Rechazado(mensaje: String?) : FalloRemoto(mensaje ?: "El servidor rechazó la operación.")
    class Caido : FalloRemoto("Smart Bite no está disponible en este momento.")
    class TiempoAgotado : FalloRemoto("El servidor tardó demasiado en responder.")
    class SinConexion : FalloRemoto("No hay conexión con Smart Bite. Revisa la red y la dirección del servidor.")
    class RespuestaInvalida : FalloRemoto("El servidor respondió algo que la app no entiende.")
}
