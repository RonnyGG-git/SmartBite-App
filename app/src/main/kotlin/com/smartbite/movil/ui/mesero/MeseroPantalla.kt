package com.smartbite.movil.ui.mesero

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartbite.application.puerto.entrada.MeseroCasos
import com.smartbite.domain.modelo.EstadoOrden
import com.smartbite.domain.modelo.Mesa
import com.smartbite.domain.modelo.Orden
import com.smartbite.domain.modelo.Producto
import com.smartbite.movil.ui.ModeloBase
import com.smartbite.movil.ui.casos
import com.smartbite.movil.ui.componentes.Etiqueta
import com.smartbite.movil.ui.componentes.Mensajes
import com.smartbite.movil.ui.componentes.TarjetaOrden
import com.smartbite.movil.ui.pesos

class MeseroModelo(private val mesero: MeseroCasos) : ModeloBase() {

    var mesas by mutableStateOf<List<Mesa>>(emptyList())
        private set
    var orden by mutableStateOf<Orden?>(null)
        private set
    var productos by mutableStateOf<List<Producto>>(emptyList())
        private set

    init {
        cargar()
    }

    fun cargar() = trabajar {
        mesas = mesero.verMesas()
    }

    fun abrir(mesa: Mesa) = trabajar {
        val elegida = mesa.orden?.let { mesero.verOrden(it.id) } ?: mesero.abrirOrden(mesa)
        productos = mesero.verProductos(mesa.sucursalId)
        orden = elegida
        mesas = mesero.verMesas()
    }

    fun agregar(producto: Producto) = trabajar("${producto.nombre} agregado.") {
        val actual = orden ?: return@trabajar
        orden = mesero.agregarPlatillo(actual, producto.id, 1)
    }

    fun confirmar(mesa: Mesa) = trabajar("Pedido de la mesa ${mesa.numero} enviado a cocina.") {
        val enMesa = mesa.orden ?: return@trabajar
        mesero.confirmarPedido(enMesa.id, enMesa.estado)
        mesas = mesero.verMesas()
    }

    fun rechazar(mesa: Mesa) = trabajar("Pedido de la mesa ${mesa.numero} rechazado.") {
        val enMesa = mesa.orden ?: return@trabajar
        mesero.rechazarPedido(enMesa.id, enMesa.estado)
        mesas = mesero.verMesas()
    }

    fun pedirCuenta(mesa: Mesa) = trabajar("La caja ya ve la cuenta de la mesa ${mesa.numero}.") {
        val enMesa = mesa.orden ?: return@trabajar
        mesero.pedirCuenta(enMesa.id, enMesa.estado)
        mesas = mesero.verMesas()
    }

    fun cerrarOrden() {
        orden = null
        cargar()
    }
}

@Composable
fun MeseroPantalla() {
    val app = casos()
    val modelo: MeseroModelo = viewModel { MeseroModelo(app) }
    val abierta = modelo.orden

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Mensajes(error = modelo.error, aviso = modelo.aviso) }
        if (abierta != null) {
            item { TarjetaOrden(abierta) { TextButton(onClick = modelo::cerrarOrden) { Text("Volver a las mesas") } } }
            if (abierta.estado == EstadoOrden.PENDIENTE) {
                item { Text("Agregar platillos", fontWeight = FontWeight.Bold) }
                items(modelo.productos, key = { it.id }) { producto ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${producto.nombre} · ${producto.precio.pesos()}", Modifier.weight(1f))
                        TextButton(onClick = { modelo.agregar(producto) }, enabled = !modelo.ocupado) { Text("Agregar") }
                    }
                }
            }
        } else {
            item {
                OutlinedButton(onClick = modelo::cargar, enabled = !modelo.ocupado, modifier = Modifier.fillMaxWidth()) {
                    Text(if (modelo.ocupado) "Cargando…" else "Actualizar mesas")
                }
            }
            items(modelo.mesas, key = { it.id }) { mesa -> TarjetaMesa(modelo, mesa) }
        }
    }
}

@Composable
private fun TarjetaMesa(modelo: MeseroModelo, mesa: Mesa) {
    val enMesa = mesa.orden
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Mesa ${mesa.numero}", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (enMesa != null) Etiqueta(enMesa.estado) else Text(mesa.estado.etiqueta)
            }
            enMesa?.let { Text("Orden #${it.id} · ${it.total.pesos()}" + if (it.cuentaSolicitada) " · cuenta pedida" else "") }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when {
                    enMesa?.estado == EstadoOrden.POR_CONFIRMAR -> {
                        Button(onClick = { modelo.confirmar(mesa) }, enabled = !modelo.ocupado) { Text("Confirmar") }
                        OutlinedButton(onClick = { modelo.rechazar(mesa) }, enabled = !modelo.ocupado) { Text("Rechazar") }
                    }
                    enMesa?.estado == EstadoOrden.LISTA && !enMesa.cuentaSolicitada ->
                        Button(onClick = { modelo.pedirCuenta(mesa) }, enabled = !modelo.ocupado) { Text("Pedir cuenta") }
                    mesa.libre ->
                        Button(onClick = { modelo.abrir(mesa) }, enabled = !modelo.ocupado) { Text("Abrir orden") }
                }
                if (enMesa != null) {
                    TextButton(onClick = { modelo.abrir(mesa) }, enabled = !modelo.ocupado) { Text("Ver orden") }
                }
            }
        }
    }
}
