package com.smartbite.movil.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartbite.application.puerto.entrada.CasosDeUso
import com.smartbite.domain.error.FalloRemoto
import com.smartbite.domain.error.ReglaInvalida
import com.smartbite.domain.valor.Dinero
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

val LocalCasos = staticCompositionLocalOf<CasosDeUso> {
    error("Falta entregar los casos de uso en MainActivity.")
}

@Composable
fun casos(): CasosDeUso = LocalCasos.current

fun mensajeDe(fallo: Throwable): String = when (fallo) {
    is ReglaInvalida, is FalloRemoto -> fallo.message ?: "No se pudo completar la acción."
    else -> "Ocurrió un error inesperado."
}

private val formatoPesos = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO")).apply {
    maximumFractionDigits = 0
}

fun Dinero.pesos(): String = formatoPesos.format(valor)

fun Dinero.paraEditar(): String = valor.stripTrailingZeros().toPlainString()

abstract class ModeloBase : ViewModel() {

    var ocupado by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        protected set
    var aviso by mutableStateOf<String?>(null)
        protected set

    protected fun trabajar(exito: String? = null, bloque: suspend () -> Unit) {
        if (ocupado) return
        ocupado = true
        error = null
        aviso = null
        viewModelScope.launch {
            try {
                bloque()
                if (exito != null) aviso = exito
            } catch (cancelado: CancellationException) {
                throw cancelado
            } catch (fallo: Exception) {
                error = mensajeDe(fallo)
            } finally {
                ocupado = false
            }
        }
    }
}
