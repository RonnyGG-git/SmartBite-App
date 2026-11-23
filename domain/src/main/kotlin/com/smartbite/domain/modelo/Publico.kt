package com.smartbite.domain.modelo

import com.smartbite.domain.error.CamposInvalidos
import java.time.LocalDate
import java.time.LocalTime

data class SucursalPublica(
    val id: Int,
    val nombre: String,
    val restaurante: String,
    val direccion: String,
    val telefono: String,
    val distanciaKm: Double?,
)

data class ReservaCliente(
    val codigo: String,
    val mesa: Int?,
    val sucursal: String,
    val restaurante: String,
    val fechaHora: String,
    val personas: Int,
    val nombre: String,
    val telefono: String,
    val estado: String,
    val estadoNombre: String,
) {
    val vigente: Boolean get() = estado == "CONFIRMADA"
}

data class NuevaReserva(val fecha: String, val hora: String, val personas: Int, val nombre: String, val telefono: String) {
    companion object {
        const val MAX_PERSONAS = 30

        fun de(fecha: String, hora: String, personas: String, nombre: String, telefono: String, hoy: LocalDate = LocalDate.now()): NuevaReserva {
            val errores = mutableMapOf<String, String>()
            val dia = runCatching { LocalDate.parse(fecha.trim()) }.getOrNull()
            if (dia == null) errores["fecha"] = "Elige la fecha."
            else if (dia.isBefore(hoy)) errores["fecha"] = "La fecha ya pasó."
            val momento = runCatching { LocalTime.parse(hora.trim()) }.getOrNull()
            if (momento == null) errores["hora"] = "Escribe la hora como 19:30."
            val cuantas = personas.trim().toIntOrNull()
            if (cuantas == null || cuantas !in 1..MAX_PERSONAS) errores["num_personas"] = "Entre 1 y $MAX_PERSONAS personas."
            if (nombre.isBlank()) errores["nombre"] = "Escribe tu nombre."
            if (telefono.isBlank()) errores["telefono"] = "Escribe tu teléfono."
            if (errores.isNotEmpty()) throw CamposInvalidos(errores)
            return NuevaReserva(dia.toString(), momento.toString(), cuantas!!, nombre.trim(), telefono.trim())
        }
    }
}

enum class TipoOpinion(val etiqueta: String) { SUGERENCIA("Sugerencia"), DENUNCIA("Denuncia") }

data class Opinion(val tipo: TipoOpinion, val mensaje: String, val nombre: String, val contacto: String, val mesaId: Int?) {
    companion object {
        fun de(tipo: TipoOpinion, mensaje: String, nombre: String, contacto: String, mesaId: Int?): Opinion {
            val texto = mensaje.trim()
            if (texto.isEmpty()) throw CamposInvalidos(mapOf("mensaje" to "Cuéntanos qué pasó o qué mejorarías."))
            if (texto.length > 1000) throw CamposInvalidos(mapOf("mensaje" to "Máximo 1000 caracteres."))
            return Opinion(tipo, texto, nombre.trim(), contacto.trim(), mesaId)
        }
    }
}

data class PedidoGuardado(val codigo: String, val orden: Orden)
