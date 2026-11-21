package com.smartbite.movil.ui.cocina

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartbite.application.puerto.entrada.CocinaCasos
import com.smartbite.domain.modelo.EstadoOrden
import com.smartbite.domain.modelo.Orden
import com.smartbite.movil.ui.ModeloBase
import com.smartbite.movil.ui.casos
import com.smartbite.movil.ui.componentes.Mensajes
import com.smartbite.movil.ui.componentes.TarjetaOrden

class CocinaModelo(private val cocina: CocinaCasos) : ModeloBase() {

    var comandas by mutableStateOf<List<Orden>>(emptyList())
        private set

    init {
        cargar()
    }

    fun cargar() = trabajar {
        comandas = cocina.verComandas()
    }

    fun empezar(orden: Orden, minutos: Int) = trabajar("Orden #${orden.id} en preparación: $minutos min.") {
        cocina.empezar(orden, minutos)
        comandas = cocina.verComandas()
    }

    fun terminar(orden: Orden) = trabajar("Orden #${orden.id} lista para servir.") {
        cocina.marcarLista(orden)
        comandas = cocina.verComandas()
    }
}

private val TIEMPOS = listOf(10, 15, 20, 30)

@Composable
fun CocinaPantalla() {
    val app = casos()
    val modelo: CocinaModelo = viewModel { CocinaModelo(app) }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Mensajes(error = modelo.error, aviso = modelo.aviso) }
        item {
            OutlinedButton(onClick = modelo::cargar, enabled = !modelo.ocupado, modifier = Modifier.fillMaxWidth()) {
                Text(if (modelo.ocupado) "Cargando…" else "Actualizar comandas (${modelo.comandas.size})")
            }
        }
        items(modelo.comandas, key = { it.id }) { orden ->
            TarjetaOrden(orden) {
                if (orden.estado == EstadoOrden.PENDIENTE) {
                    Text("¿En cuánto sale?")
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TIEMPOS.forEach { minutos ->
                            OutlinedButton(onClick = { modelo.empezar(orden, minutos) }, enabled = !modelo.ocupado) {
                                Text("$minutos")
                            }
                        }
                    }
                } else {
                    Button(onClick = { modelo.terminar(orden) }, enabled = !modelo.ocupado, modifier = Modifier.fillMaxWidth()) {
                        Text("Marcar lista")
                    }
                }
            }
        }
    }
}
