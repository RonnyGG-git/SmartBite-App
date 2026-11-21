package com.smartbite.movil.ui.mesero

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartbite.application.puerto.entrada.MeseroCasos
import com.smartbite.domain.modelo.EstadoOrden
import com.smartbite.domain.modelo.Mesa
import com.smartbite.domain.modelo.Orden
import com.smartbite.domain.modelo.Producto
import com.smartbite.domain.modelo.Reserva
import com.smartbite.movil.ui.ModeloBase
import com.smartbite.movil.ui.casos
import com.smartbite.movil.ui.componentes.Etiqueta
import com.smartbite.movil.ui.componentes.FotoPlatillo
import com.smartbite.movil.ui.componentes.Mensajes
import com.smartbite.movil.ui.componentes.Pestanas
import com.smartbite.movil.ui.componentes.TarjetaOrden
import com.smartbite.movil.ui.componentes.Vacio
import com.smartbite.movil.ui.pesos

enum class PestanaMesero(val titulo: String) { MESAS("Mesas"), RESERVAS("Reservas"), SIN_MESA("Sin mesa") }

class MeseroModelo(private val mesero: MeseroCasos) : ModeloBase() {

    var pestana by mutableStateOf(PestanaMesero.MESAS)
        private set
    var mesas by mutableStateOf<List<Mesa>>(emptyList())
        private set
    var reservas by mutableStateOf<List<Reserva>>(emptyList())
        private set
    var sinMesa by mutableStateOf<List<Orden>>(emptyList())
        private set
    var orden by mutableStateOf<Orden?>(null)
        private set
    var productos by mutableStateOf<List<Producto>>(emptyList())
        private set
    var mesaPorAbrir by mutableStateOf<Mesa?>(null)
        private set
    var ordenPorUbicar by mutableStateOf<Orden?>(null)
        private set
    var clienteNombre by mutableStateOf("")
    var clienteTelefono by mutableStateOf("")
    val cantidades = mutableStateMapOf<Int, Int>()

    init {
        cargar()
    }

    fun elegir(nueva: PestanaMesero) {
        pestana = nueva
        orden = null
        cargar()
    }

    fun cargar() = trabajar {
        mesas = mesero.verMesas()
        when (pestana) {
            PestanaMesero.MESAS -> Unit
            PestanaMesero.RESERVAS -> reservas = mesero.verReservas()
            PestanaMesero.SIN_MESA -> sinMesa = mesero.verPedidosSinMesa()
        }
    }

    fun pedirDatosParaAbrir(mesa: Mesa) {
        clienteNombre = ""
        clienteTelefono = ""
        mesaPorAbrir = mesa
    }

    fun cerrarDialogo() {
        mesaPorAbrir = null
        ordenPorUbicar = null
    }

    fun abrirNueva() = trabajar {
        val mesa = mesaPorAbrir ?: return@trabajar
        val nueva = mesero.abrirOrden(mesa, clienteNombre, clienteTelefono)
        mesaPorAbrir = null
        mostrar(nueva, mesa.sucursalId)
    }

    fun verOrdenDe(mesa: Mesa) = trabajar {
        val enMesa = mesa.orden ?: return@trabajar
        mostrar(mesero.verOrden(enMesa.id), mesa.sucursalId)
    }

    private suspend fun mostrar(nueva: Orden, sucursalId: Int) {
        productos = mesero.verProductos(sucursalId)
        cantidades.clear()
        orden = nueva
        mesas = mesero.verMesas()
    }

    fun cambiarCantidad(producto: Producto, delta: Int) {
        cantidades[producto.id] = ((cantidades[producto.id] ?: 1) + delta).coerceIn(1, 50)
    }

    fun agregar(producto: Producto) {
        val cuantas = cantidades[producto.id] ?: 1
        trabajar("$cuantas × ${producto.nombre} agregado.") {
            val actual = orden ?: return@trabajar
            orden = mesero.agregarPlatillo(actual, producto.id, cuantas)
            cantidades.remove(producto.id)
        }
    }

    fun cancelarOrden() = trabajar("Orden cancelada.") {
        val actual = orden ?: return@trabajar
        orden = mesero.cancelarOrden(actual)
        mesas = mesero.verMesas()
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

    fun entregar(orden: Orden) = trabajar("Orden #${orden.id} entregada.") {
        mesero.entregarOrden(orden)
        sinMesa = mesero.verPedidosSinMesa()
    }

    fun elegirMesaPara(orden: Orden) {
        ordenPorUbicar = orden
    }

    fun ubicar(mesa: Mesa) = trabajar("Pedido ubicado en la mesa ${mesa.numero}.") {
        val pendiente = ordenPorUbicar ?: return@trabajar
        mesero.asignarMesa(pendiente, mesa)
        ordenPorUbicar = null
        sinMesa = mesero.verPedidosSinMesa()
        mesas = mesero.verMesas()
    }

    fun llego(reserva: Reserva) = trabajar("${reserva.nombre} llegó: la mesa quedó asignada.") {
        mesero.confirmarLlegada(reserva)
        reservas = mesero.verReservas()
        mesas = mesero.verMesas()
    }

    fun cancelar(reserva: Reserva) = trabajar("Reserva de ${reserva.nombre} cancelada.") {
        mesero.cancelarReserva(reserva)
        reservas = mesero.verReservas()
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

    Column(Modifier.fillMaxSize()) {
        if (abierta == null) {
            Pestanas(PestanaMesero.entries, modelo.pestana, { it.titulo }, modelo::elegir)
        }
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Mensajes(error = modelo.error, aviso = modelo.aviso) }
            if (abierta != null) {
                detalleOrden(modelo, abierta)
            } else {
                item {
                    OutlinedButton(onClick = modelo::cargar, enabled = !modelo.ocupado, modifier = Modifier.fillMaxWidth()) {
                        Text(if (modelo.ocupado) "Cargando…" else "Actualizar")
                    }
                }
                when (modelo.pestana) {
                    PestanaMesero.MESAS -> items(modelo.mesas, key = { it.id }) { mesa -> TarjetaMesa(modelo, mesa) }
                    PestanaMesero.RESERVAS -> {
                        if (modelo.reservas.isEmpty() && !modelo.ocupado) item { Vacio("No hay reservas próximas.") }
                        items(modelo.reservas, key = { it.id }) { reserva -> TarjetaReserva(modelo, reserva) }
                    }
                    PestanaMesero.SIN_MESA -> {
                        if (modelo.sinMesa.isEmpty() && !modelo.ocupado) item { Vacio("No hay pedidos sin mesa activos.") }
                        items(modelo.sinMesa, key = { it.id }) { orden ->
                            TarjetaOrden(orden) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (orden.estado.activa) {
                                        OutlinedButton(onClick = { modelo.elegirMesaPara(orden) }, enabled = !modelo.ocupado) { Text("Asignar mesa") }
                                    }
                                    if (orden.puedeEntregarse) {
                                        Button(onClick = { modelo.entregar(orden) }, enabled = !modelo.ocupado) { Text("Entregar") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    modelo.mesaPorAbrir?.let { mesa ->
        AlertDialog(
            onDismissRequest = modelo::cerrarDialogo,
            title = { Text("Abrir orden en la mesa ${mesa.numero}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Los datos del cliente son opcionales.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(modelo.clienteNombre, { modelo.clienteNombre = it }, label = { Text("Nombre") }, singleLine = true)
                    OutlinedTextField(
                        modelo.clienteTelefono,
                        { modelo.clienteTelefono = it },
                        label = { Text("Teléfono") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    )
                }
            },
            confirmButton = { Button(onClick = modelo::abrirNueva, enabled = !modelo.ocupado) { Text("Abrir") } },
            dismissButton = { TextButton(onClick = modelo::cerrarDialogo) { Text("Cancelar") } },
        )
    }

    modelo.ordenPorUbicar?.let { orden ->
        val libres = modelo.mesas.filter { it.libre }
        AlertDialog(
            onDismissRequest = modelo::cerrarDialogo,
            title = { Text("¿En qué mesa va la orden #${orden.id}?") },
            text = {
                if (libres.isEmpty()) {
                    Text("No hay mesas libres en este momento.")
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        libres.forEach { mesa ->
                            OutlinedButton(onClick = { modelo.ubicar(mesa) }, enabled = !modelo.ocupado) { Text("Mesa ${mesa.numero}") }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = modelo::cerrarDialogo) { Text("Cerrar") } },
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.detalleOrden(modelo: MeseroModelo, abierta: Orden) {
    item {
        TarjetaOrden(abierta) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = modelo::cerrarOrden) { Text("Volver") }
                if (abierta.cancelablePorMesero) {
                    OutlinedButton(onClick = modelo::cancelarOrden, enabled = !modelo.ocupado) { Text("Cancelar orden") }
                }
            }
        }
    }
    if (abierta.estado == EstadoOrden.PENDIENTE) {
        item { Text("Agregar platillos", fontWeight = FontWeight.Bold) }
        items(modelo.productos, key = { it.id }) { producto ->
            val cuantas = modelo.cantidades[producto.id] ?: 1
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    FotoPlatillo(producto.imagen, producto.nombre, 52.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(producto.nombre, fontWeight = FontWeight.SemiBold)
                        Text(producto.precio.pesos(), color = MaterialTheme.colorScheme.primary)
                    }
                    TextButton(onClick = { modelo.cambiarCantidad(producto, -1) }) { Text("−") }
                    Text("$cuantas", fontWeight = FontWeight.Bold)
                    TextButton(onClick = { modelo.cambiarCantidad(producto, 1) }) { Text("+") }
                    Button(onClick = { modelo.agregar(producto) }, enabled = !modelo.ocupado) { Text("Agregar") }
                }
            }
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
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when {
                    enMesa?.estado == EstadoOrden.POR_CONFIRMAR -> {
                        Button(onClick = { modelo.confirmar(mesa) }, enabled = !modelo.ocupado) { Text("Confirmar") }
                        OutlinedButton(onClick = { modelo.rechazar(mesa) }, enabled = !modelo.ocupado) { Text("Rechazar") }
                    }
                    enMesa?.estado == EstadoOrden.LISTA && !enMesa.cuentaSolicitada ->
                        Button(onClick = { modelo.pedirCuenta(mesa) }, enabled = !modelo.ocupado) { Text("Pedir cuenta") }
                    mesa.libre ->
                        Button(onClick = { modelo.pedirDatosParaAbrir(mesa) }, enabled = !modelo.ocupado) { Text("Abrir orden") }
                }
                if (enMesa != null) {
                    TextButton(onClick = { modelo.verOrdenDe(mesa) }, enabled = !modelo.ocupado) { Text("Ver orden") }
                }
            }
        }
    }
}

@Composable
private fun TarjetaReserva(modelo: MeseroModelo, reserva: Reserva) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("${reserva.nombre} · ${reserva.personas} personas", fontWeight = FontWeight.Bold)
            Text(
                "Mesa ${reserva.mesa ?: "—"} · ${reserva.fechaHora.take(16).replace("T", " ")} · ${reserva.telefono}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text("Código ${reserva.codigo}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { modelo.llego(reserva) }, enabled = !modelo.ocupado) { Text("Llegó") }
                OutlinedButton(onClick = { modelo.cancelar(reserva) }, enabled = !modelo.ocupado) { Text("Cancelar") }
            }
        }
    }
}
