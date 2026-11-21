package com.smartbite.movil.ui.caja

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartbite.application.puerto.entrada.CajaCasos
import com.smartbite.domain.modelo.Orden
import com.smartbite.movil.ui.ModeloBase
import com.smartbite.movil.ui.casos
import com.smartbite.movil.ui.componentes.Mensajes
import com.smartbite.movil.ui.componentes.TarjetaOrden

class CajaModelo(private val caja: CajaCasos) : ModeloBase() {

    var cuentas by mutableStateOf<List<Orden>>(emptyList())
        private set

    init {
        cargar()
    }

    fun cargar() = trabajar {
        cuentas = caja.verPorCobrar()
    }

    fun entregar(orden: Orden) = trabajar("Orden #${orden.id} entregada. Mesa liberada.") {
        caja.entregar(orden)
        cuentas = caja.verPorCobrar()
    }
}

@Composable
fun CajaPantalla() {
    val app = casos()
    val modelo: CajaModelo = viewModel { CajaModelo(app) }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Mensajes(error = modelo.error, aviso = modelo.aviso) }
        item {
            Text(
                "Cuentas pedidas por los meseros. El cobro (efectivo, tarjeta o QR) se registra en el módulo de caja de la web; " +
                    "cuando la orden aparece pagada, aquí se entrega.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            OutlinedButton(onClick = modelo::cargar, enabled = !modelo.ocupado, modifier = Modifier.fillMaxWidth()) {
                Text(if (modelo.ocupado) "Cargando…" else "Actualizar (${modelo.cuentas.size})")
            }
        }
        items(modelo.cuentas, key = { it.id }) { orden ->
            TarjetaOrden(orden) {
                Button(
                    onClick = { modelo.entregar(orden) },
                    enabled = !modelo.ocupado && orden.puedeEntregarse,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (orden.pagada) "Entregar y cerrar" else "Esperando pago")
                }
            }
        }
    }
}
