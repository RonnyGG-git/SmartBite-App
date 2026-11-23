package com.smartbite.application.servicio

import com.smartbite.application.puerto.entrada.GestionCasos
import com.smartbite.domain.error.CamposInvalidos
import com.smartbite.domain.error.ReglaInvalida
import com.smartbite.domain.modelo.CatalogoGestion
import com.smartbite.domain.modelo.CodigoQr
import com.smartbite.domain.modelo.Compra
import com.smartbite.domain.modelo.Ficha
import com.smartbite.domain.modelo.Fuente
import com.smartbite.domain.modelo.Opcion
import com.smartbite.domain.modelo.Recurso
import com.smartbite.domain.modelo.Tablero
import com.smartbite.domain.puerto.salida.GestionGateway
import com.smartbite.domain.valor.Dinero

class GestionServicio(private val gestion: GestionGateway) : GestionCasos {

    override suspend fun listar(recurso: Recurso): List<Ficha> = gestion.listar(recurso)

    override suspend fun guardar(recurso: Recurso, id: Int?, valores: Map<String, String>): Ficha {
        val creando = id == null
        if (creando && !recurso.creable) throw ReglaInvalida("Aquí no se crean ${recurso.titulo.lowercase()}.")
        val datos = recurso.preparar(valores, creando)
        return if (id == null) gestion.crear(recurso, datos) else gestion.editar(recurso, id, datos)
    }

    override suspend fun eliminar(recurso: Recurso, id: Int) {
        if (!recurso.borrable) throw ReglaInvalida("Esto no se puede eliminar.")
        gestion.eliminar(recurso, id)
    }

    override suspend fun ejecutar(recurso: Recurso, id: Int, accion: String, valores: Map<String, String>): Ficha {
        val definida = recurso.acciones.firstOrNull { it.clave == accion } ?: throw ReglaInvalida("Acción desconocida.")
        val datos = recurso.preparar(valores, creando = true, de = definida.campos)
        return gestion.accion(recurso, id, accion, datos)
    }

    override suspend fun opciones(fuente: Fuente): List<Opcion> = when (fuente) {
        is Fuente.Fija -> fuente.opciones
        Fuente.Unidades -> gestion.unidades()
        Fuente.RolesEmpleado -> gestion.rolesEmpleado()
        is Fuente.DeRecurso -> gestion.listar(CatalogoGestion.porClave(fuente.clave)).map { ficha ->
            Opcion(ficha.id.toString(), ficha.texto(fuente.etiqueta).ifBlank { "#${ficha.id}" })
        }
    }

    override suspend fun qrDeMesa(id: Int): CodigoQr = gestion.qrMesa(id)

    override suspend fun subirFoto(productoId: Int, bytes: ByteArray): Ficha {
        if (bytes.isEmpty()) throw ReglaInvalida("No se pudo leer la foto.")
        if (bytes.size > 8 * 1024 * 1024) throw ReglaInvalida("La foto pesa más de 8 MB. Elige una más liviana.")
        return gestion.subirFoto(productoId, bytes)
    }

    override suspend fun tablero(): Tablero = gestion.tablero()

    override suspend fun compras(): List<Compra> = gestion.compras()

    override suspend fun crearCompra(proveedorId: Int?, lineas: List<Triple<Int, String, String>>): Compra {
        val errores = mutableMapOf<String, String>()
        if (proveedorId == null) errores["proveedor"] = "Elige el proveedor."
        if (lineas.isEmpty()) errores["detalles"] = "Agrega al menos un ítem."
        val limpias = lineas.mapNotNull { (item, cantidad, costo) ->
            val unidades = cantidad.trim().toIntOrNull()
            val valor = runCatching { Dinero.deEntrada(costo) }.getOrNull()
            if (unidades == null || unidades < 1 || valor == null) {
                errores["detalles"] = "Cada línea necesita cantidad (1 o más) y costo."
                null
            } else Triple(item, unidades, valor.valor.toPlainString())
        }
        if (errores.isNotEmpty()) throw CamposInvalidos(errores)
        return gestion.crearCompra(proveedorId!!, limpias)
    }

    override suspend fun recibirCompra(compra: Compra): Compra {
        if (!compra.pendiente) throw ReglaInvalida("Solo se recibe una compra pendiente.")
        return gestion.recibirCompra(compra.id)
    }

    override suspend fun anularCompra(compra: Compra): Compra {
        if (!compra.pendiente) throw ReglaInvalida("Solo se anula una compra pendiente.")
        return gestion.anularCompra(compra.id)
    }
}
