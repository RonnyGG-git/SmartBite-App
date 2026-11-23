package com.smartbite.domain.modelo

import com.smartbite.domain.error.CamposInvalidos

enum class TipoCampo { TEXTO, TEXTO_LARGO, ENTERO, DECIMAL, BOOLEANO, OPCION, CORREO, TELEFONO, CLAVE }

data class Opcion(val valor: String, val etiqueta: String)

sealed interface Fuente {
    data class Fija(val opciones: List<Opcion>) : Fuente
    data class DeRecurso(val clave: String, val etiqueta: String = "nombre") : Fuente
    data object Unidades : Fuente
    data object RolesEmpleado : Fuente
}

data class Campo(
    val clave: String,
    val etiqueta: String,
    val tipo: TipoCampo,
    val obligatorio: Boolean = false,
    val fuente: Fuente? = null,
    val soloAlCrear: Boolean = false,
    val soloAlEditar: Boolean = false,
)

enum class TipoAccion { ALTERNAR, FORMULARIO, QR, FOTO, ELEGIR }

data class AccionRecurso(
    val clave: String,
    val etiqueta: String,
    val tipo: TipoAccion,
    val campos: List<Campo> = emptyList(),
    val soloGestion: Boolean = true,
)

data class Ficha(val id: Int, val valores: Map<String, String?>) {
    fun texto(clave: String): String = valores[clave].orEmpty()
    fun si(clave: String): Boolean = valores[clave] == "true"
}

class Recurso(
    val clave: String,
    val ruta: String,
    val titulo: String,
    val singular: String,
    val campos: List<Campo>,
    val lee: (Rol) -> Boolean,
    val gestiona: (Rol) -> Boolean,
    val principal: (Ficha) -> String,
    val secundario: (Ficha) -> String = { "" },
    val imagen: (Ficha) -> String? = { null },
    val alerta: (Ficha) -> Boolean = { false },
    val acciones: List<AccionRecurso> = emptyList(),
    val creable: Boolean = true,
    val editable: Boolean = true,
    val borrable: Boolean = true,
) {
    fun camposPara(creando: Boolean): List<Campo> =
        campos.filter { (creando || !it.soloAlCrear) && (!creando || !it.soloAlEditar) }

    fun preparar(valores: Map<String, String>, creando: Boolean, de: List<Campo> = camposPara(creando)): Map<String, Any?> {
        val errores = mutableMapOf<String, String>()
        val salida = linkedMapOf<String, Any?>()
        de.forEach { campo ->
            val crudo = valores[campo.clave].orEmpty().trim()
            if (campo.obligatorio && crudo.isEmpty() && campo.tipo != TipoCampo.BOOLEANO) {
                errores[campo.clave] = "Este campo es obligatorio."
                return@forEach
            }
            salida[campo.clave] = when (campo.tipo) {
                TipoCampo.BOOLEANO -> crudo == "true"
                TipoCampo.ENTERO -> if (crudo.isEmpty()) "" else crudo.toIntOrNull() ?: run {
                    errores[campo.clave] = "Escribe un número entero."
                    null
                }
                TipoCampo.DECIMAL -> if (crudo.isEmpty()) "" else crudo.replace(",", ".").toBigDecimalOrNull()?.toPlainString() ?: run {
                    errores[campo.clave] = "Escribe un número."
                    null
                }
                TipoCampo.CORREO -> if (crudo.isNotEmpty() && !crudo.contains("@")) {
                    errores[campo.clave] = "Escribe un correo válido."
                    null
                } else crudo.lowercase()
                TipoCampo.CLAVE -> valores[campo.clave].orEmpty()
                else -> crudo
            }
        }
        if (errores.isNotEmpty()) throw CamposInvalidos(errores)
        return salida
    }
}

data class FilaTablero(val etiqueta: String, val valor: String)

data class Tablero(
    val restaurante: String?,
    val usuarios: Int,
    val productos: Int,
    val mesas: Int,
    val mesasOcupadas: Int,
    val ordenesHoy: Int,
    val ventasHoy: com.smartbite.domain.valor.Dinero,
    val stockBajo: List<FilaTablero>,
    val ordenesPorEstado: List<FilaTablero>,
    val ventasPorMetodo: List<FilaTablero>,
)

data class LineaCompra(val itemId: Int, val item: String, val cantidad: Int, val costo: com.smartbite.domain.valor.Dinero) {
    val subtotal get() = costo * cantidad
}

data class Compra(
    val id: Int,
    val proveedor: String,
    val estado: String,
    val estadoNombre: String,
    val total: com.smartbite.domain.valor.Dinero,
    val fecha: String,
    val lineas: List<LineaCompra>,
) {
    val pendiente: Boolean get() = estado == "PENDIENTE"
}
