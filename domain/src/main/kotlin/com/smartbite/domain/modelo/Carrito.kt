package com.smartbite.domain.modelo

import com.smartbite.domain.error.ReglaInvalida
import com.smartbite.domain.valor.Cantidad
import com.smartbite.domain.valor.Dinero

data class LineaCarrito(val producto: Producto, val cantidad: Cantidad) {
    val subtotal: Dinero get() = producto.precio * cantidad.valor
}

data class Carrito(val lineas: List<LineaCarrito> = emptyList()) {

    val total: Dinero get() = lineas.fold(Dinero.CERO) { suma, linea -> suma + linea.subtotal }
    val unidades: Int get() = lineas.sumOf { it.cantidad.valor }
    val vacio: Boolean get() = lineas.isEmpty()

    fun cantidadDe(productoId: Int): Int = lineas.firstOrNull { it.producto.id == productoId }?.cantidad?.valor ?: 0

    fun agregar(producto: Producto): Carrito {
        val nueva = Cantidad.de(cantidadDe(producto.id) + 1)
        val otras = lineas.filterNot { it.producto.id == producto.id }
        return Carrito(otras + LineaCarrito(producto, nueva))
    }

    fun quitar(producto: Producto): Carrito {
        val actual = cantidadDe(producto.id)
        val otras = lineas.filterNot { it.producto.id == producto.id }
        if (actual <= 1) return Carrito(otras)
        return Carrito(otras + LineaCarrito(producto, Cantidad.de(actual - 1)))
    }

    fun validarParaPedir() {
        if (vacio) throw ReglaInvalida("Agrega al menos un platillo antes de pedir.")
    }
}

data class PedidoCreado(val codigo: String, val orden: Orden)
