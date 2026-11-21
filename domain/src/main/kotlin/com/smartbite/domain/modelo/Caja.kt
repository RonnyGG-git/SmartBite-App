package com.smartbite.domain.modelo

import com.smartbite.domain.error.ReglaInvalida
import com.smartbite.domain.valor.Dinero

data class MetodoPago(val id: Int, val nombre: String, val usaPasarela: Boolean)

data class PagoRegistrado(
    val id: Int,
    val metodo: String,
    val monto: Dinero,
    val propina: Dinero,
    val estado: String,
    val aprobado: Boolean,
)

data class Cuenta(
    val orden: Orden,
    val saldoPendiente: Dinero,
    val saldoDisponible: Dinero,
    val cobrable: Boolean,
    val pagos: List<PagoRegistrado>,
    val facturaId: Int?,
) {
    val saldada: Boolean get() = !saldoPendiente.positivo
}

data class Cobro(
    val metodo: MetodoPago,
    val monto: Dinero,
    val propina: Dinero,
    val efectivoRecibido: Dinero?,
    val referencia: String,
) {
    val cambio: Dinero? get() = efectivoRecibido?.let { it - monto - propina }?.takeIf { it.positivo }

    companion object {
        fun de(
            cuenta: Cuenta,
            metodo: MetodoPago?,
            monto: String,
            propina: String,
            recibido: String,
            referencia: String,
        ): Cobro {
            val elegido = metodo ?: throw ReglaInvalida("Elige el método de pago.")
            if (!cuenta.cobrable) throw ReglaInvalida("Esta orden no se puede cobrar en este momento.")
            val valor = Dinero.deEntrada(monto)
            if (!valor.positivo) throw ReglaInvalida("El monto debe ser mayor que cero.")
            if (valor > cuenta.saldoDisponible) throw ReglaInvalida("El monto supera el saldo por cobrar.")
            val extra = if (propina.isBlank()) Dinero.CERO else Dinero.deEntrada(propina)
            val efectivo = if (recibido.isBlank()) null else Dinero.deEntrada(recibido)
            if (efectivo != null && efectivo < valor + extra) {
                throw ReglaInvalida("El efectivo recibido no alcanza para el monto y la propina.")
            }
            return Cobro(elegido, valor, extra, efectivo, referencia.trim())
        }
    }
}

sealed interface ResultadoCobro {
    val cuenta: Cuenta

    data class Registrado(override val cuenta: Cuenta, val cambio: Dinero?) : ResultadoCobro
    data class EnPasarela(override val cuenta: Cuenta, val checkout: String) : ResultadoCobro
}

class CodigoQr(val url: String, val imagenPng: ByteArray, val saldo: Dinero)

data class FacturaResumen(val id: Int, val codigo: String, val ordenId: Int, val cliente: String, val total: Dinero, val fecha: String)

data class LineaFactura(val nombre: String, val cantidad: Int, val precioUnitario: Dinero, val subtotal: Dinero)

data class Factura(
    val resumen: FacturaResumen,
    val documento: String,
    val emisor: String,
    val nit: String,
    val base: Dinero,
    val impuesto: Dinero,
    val consumo: Dinero,
    val propina: Dinero,
    val cufe: String,
    val lineas: List<LineaFactura>,
)
