package com.smartbite.movil.ui.publico

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartbite.application.puerto.entrada.ClienteCasos
import com.smartbite.application.puerto.entrada.PublicoCasos
import com.smartbite.domain.error.SinDisponibilidad
import com.smartbite.domain.modelo.Carrito
import com.smartbite.domain.modelo.Orden
import com.smartbite.domain.modelo.PedidoGuardado
import com.smartbite.domain.modelo.Producto
import com.smartbite.domain.modelo.ReservaCliente
import com.smartbite.domain.modelo.SucursalPublica
import com.smartbite.domain.modelo.TipoOpinion
import com.smartbite.domain.error.ReglaInvalida
import com.smartbite.movil.ui.ModeloBase
import com.smartbite.movil.ui.casos
import com.smartbite.movil.ui.componentes.CampoTexto
import com.smartbite.movil.ui.componentes.FotoPlatillo
import com.smartbite.movil.ui.componentes.Mensajes
import com.smartbite.movil.ui.componentes.Pestanas
import com.smartbite.movil.ui.componentes.TarjetaOrden
import com.smartbite.movil.ui.componentes.Vacio
import com.smartbite.movil.ui.pesos
import java.time.LocalDate

enum class VistaRestaurante { LISTA, MENU, PEDIDO, RESERVAR, OPINAR }

class RestaurantesModelo(private val publico: PublicoCasos, private val cliente: ClienteCasos) : ModeloBase() {
    var vista by mutableStateOf(VistaRestaurante.LISTA)
        private set
    var busqueda by mutableStateOf("")
    var sucursales by mutableStateOf<List<SucursalPublica>>(emptyList())
        private set
    var elegida by mutableStateOf<SucursalPublica?>(null)
        private set
    var menu by mutableStateOf<List<Producto>>(emptyList())
        private set
    var carrito by mutableStateOf(Carrito())
        private set
    var nombre by mutableStateOf("")
    var orden by mutableStateOf<Orden?>(null)
        private set
    var codigoPedido by mutableStateOf<String?>(null)
        private set
    var fecha by mutableStateOf(LocalDate.now().plusDays(1).toString())
    var hora by mutableStateOf("19:00")
    var personas by mutableStateOf("2")
    var telefono by mutableStateOf("")
    var alternativas by mutableStateOf<List<String>>(emptyList())
        private set
    var reserva by mutableStateOf<ReservaCliente?>(null)
        private set
    var tipoOpinion by mutableStateOf(TipoOpinion.SUGERENCIA)
    var mensaje by mutableStateOf("")
    var contacto by mutableStateOf("")

    init {
        buscar()
    }

    fun buscar() = trabajar {
        sucursales = publico.buscarRestaurantes(busqueda)
    }

    fun abrir(sucursal: SucursalPublica) = trabajar {
        menu = publico.menuDe(sucursal.id)
        elegida = sucursal
        carrito = Carrito()
        vista = VistaRestaurante.MENU
    }

    fun irA(nueva: VistaRestaurante) {
        limpiarMensajes()
        alternativas = emptyList()
        vista = nueva
    }

    fun agregar(producto: Producto) {
        try {
            carrito = carrito.agregar(producto)
        } catch (regla: ReglaInvalida) {
            error = regla.message
        }
    }

    fun quitar(producto: Producto) {
        carrito = carrito.quitar(producto)
    }

    fun pedir() = trabajar {
        val sucursal = elegida ?: return@trabajar
        val pedido = publico.pedirParaLlevar(sucursal, nombre, carrito)
        codigoPedido = pedido.codigo
        orden = pedido.orden
        carrito = Carrito()
        vista = VistaRestaurante.PEDIDO
    }

    fun actualizarPedido() = trabajar {
        val codigo = codigoPedido ?: return@trabajar
        orden = cliente.seguirPedido(codigo)
    }

    fun reservar() = trabajar {
        val sucursal = elegida ?: return@trabajar
        alternativas = emptyList()
        try {
            reserva = publico.reservar(sucursal.id, fecha, hora, personas, nombre, telefono)
            aviso = "Reserva confirmada. Guarda tu código: ${reserva?.codigo}."
        } catch (sin: SinDisponibilidad) {
            alternativas = sin.alternativas
            throw sin
        }
    }

    fun usarAlternativa(texto: String) {
        fecha = texto.take(10)
        hora = texto.drop(11).take(5)
        alternativas = emptyList()
    }

    fun opinar() = trabajar("Gracias. El restaurante revisará tu mensaje.") {
        publico.opinar(tipoOpinion, mensaje, nombre, contacto, null)
        mensaje = ""
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestaurantesPantalla(alVolver: () -> Unit) {
    val app = casos()
    val modelo: RestaurantesModelo = viewModel { RestaurantesModelo(app, app) }
    val sucursal = modelo.elegida

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (modelo.vista == VistaRestaurante.LISTA || sucursal == null) "Restaurantes" else sucursal.restaurante) },
                navigationIcon = {
                    TextButton(onClick = { if (modelo.vista == VistaRestaurante.LISTA) alVolver() else modelo.irA(VistaRestaurante.LISTA) }) {
                        Text(if (modelo.vista == VistaRestaurante.LISTA) "Salir" else "Atrás")
                    }
                },
            )
        },
    ) { margen ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(margen),
        ) {
            item { Mensajes(error = modelo.error, aviso = modelo.aviso) }
            when (modelo.vista) {
                VistaRestaurante.LISTA -> lista(modelo)
                VistaRestaurante.MENU -> menu(modelo)
                VistaRestaurante.PEDIDO -> item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Tu pedido para llevar", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            "La caja lo confirma y pasa a cocina. Lo encuentras también en «Mis pedidos». Código: ${modelo.codigoPedido?.take(12)}…",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        modelo.orden?.let { TarjetaOrden(it) }
                        OutlinedButton(onClick = modelo::actualizarPedido, enabled = !modelo.ocupado, modifier = Modifier.fillMaxWidth()) { Text("Actualizar estado") }
                    }
                }
                VistaRestaurante.RESERVAR -> item { Reservar(modelo) }
                VistaRestaurante.OPINAR -> item { Opinar(modelo) }
            }
        }
    }
}

private fun LazyListScope.lista(modelo: RestaurantesModelo) {
    item {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) { CampoTexto("Buscar restaurante o sucursal", modelo.busqueda, null) { modelo.busqueda = it } }
            Button(onClick = modelo::buscar, enabled = !modelo.ocupado) { Text("Buscar") }
        }
    }
    if (modelo.sucursales.isEmpty() && !modelo.ocupado && modelo.error == null) item { Vacio("No encontramos restaurantes con ese nombre.") }
    items(modelo.sucursales, key = { it.id }) { sucursal ->
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().clickable { modelo.abrir(sucursal) },
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(sucursal.restaurante, fontWeight = FontWeight.Bold)
                Text("${sucursal.nombre} · ${sucursal.direccion}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (sucursal.telefono.isNotBlank()) Text(sucursal.telefono, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun LazyListScope.menu(modelo: RestaurantesModelo) {
    item {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { modelo.irA(VistaRestaurante.RESERVAR) }) { Text("Reservar mesa") }
            OutlinedButton(onClick = { modelo.irA(VistaRestaurante.OPINAR) }) { Text("Opinar") }
        }
    }
    items(modelo.menu, key = { it.id }) { producto ->
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                FotoPlatillo(producto.imagen, producto.nombre, 72.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(producto.nombre, fontWeight = FontWeight.SemiBold)
                    if (producto.descripcion.isNotBlank()) Text(producto.descripcion, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                    Text(producto.precio.pesos(), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
                val cantidad = modelo.carrito.cantidadDe(producto.id)
                if (cantidad > 0) {
                    TextButton(onClick = { modelo.quitar(producto) }) { Text("−") }
                    Text("$cantidad", fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = { modelo.agregar(producto) }) { Text("+") }
            }
        }
    }
    item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CampoTexto("Tu nombre (para llamarte)", modelo.nombre, null) { modelo.nombre = it }
            Button(
                onClick = modelo::pedir,
                enabled = !modelo.ocupado && !modelo.carrito.vacio,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Text("Pedir para llevar · ${modelo.carrito.unidades} · ${modelo.carrito.total.pesos()}")
            }
        }
    }
}

@Composable
private fun Reservar(modelo: RestaurantesModelo) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Reservar en ${modelo.elegida?.nombre}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        val hecha = modelo.reserva
        if (hecha != null) {
            Text("Código ${hecha.codigo} · Mesa ${hecha.mesa ?: "—"} · ${hecha.fechaHora.take(16).replace("T", " ")} · ${hecha.estadoNombre}")
            Text("La encuentras en «Mis pedidos y reservas».", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        CampoTexto("Fecha (AAAA-MM-DD)", modelo.fecha, modelo.campos["fecha"]) { modelo.fecha = it }
        CampoTexto("Hora (HH:MM)", modelo.hora, modelo.campos["hora"]) { modelo.hora = it }
        CampoTexto("Personas", modelo.personas, modelo.campos["num_personas"], KeyboardType.Number) { modelo.personas = it }
        CampoTexto("Tu nombre", modelo.nombre, modelo.campos["nombre"]) { modelo.nombre = it }
        CampoTexto("Teléfono", modelo.telefono, modelo.campos["telefono"], KeyboardType.Phone) { modelo.telefono = it }
        if (modelo.alternativas.isNotEmpty()) {
            Text("Horarios disponibles cerca:", fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                modelo.alternativas.forEach { opcion ->
                    OutlinedButton(onClick = { modelo.usarAlternativa(opcion) }) { Text(opcion.take(16).replace("T", " ")) }
                }
            }
        }
        Button(onClick = modelo::reservar, enabled = !modelo.ocupado, modifier = Modifier.fillMaxWidth()) { Text("Reservar") }
    }
}

@Composable
private fun Opinar(modelo: RestaurantesModelo) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Cuéntanos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TipoOpinion.entries.forEach { opcion ->
                FilterChip(selected = modelo.tipoOpinion == opcion, onClick = { modelo.tipoOpinion = opcion }, label = { Text(opcion.etiqueta) })
            }
        }
        CampoTexto("Mensaje", modelo.mensaje, modelo.campos["mensaje"], lineas = 4) { modelo.mensaje = it }
        CampoTexto("Tu nombre (opcional)", modelo.nombre, null) { modelo.nombre = it }
        CampoTexto("Contacto (opcional)", modelo.contacto, null) { modelo.contacto = it }
        Button(onClick = modelo::opinar, enabled = !modelo.ocupado, modifier = Modifier.fillMaxWidth()) { Text("Enviar") }
    }
}

enum class PestanaMios(val titulo: String) { PEDIDOS("Pedidos"), RESERVAS("Reservas") }

class MisPedidosModelo(private val publico: PublicoCasos) : ModeloBase() {
    var pestana by mutableStateOf(PestanaMios.PEDIDOS)
        private set
    var pedidos by mutableStateOf<List<PedidoGuardado>>(emptyList())
        private set
    var reservas by mutableStateOf<List<ReservaCliente>>(emptyList())
        private set

    init {
        cargar()
    }

    fun elegir(nueva: PestanaMios) {
        pestana = nueva
        cargar()
    }

    fun cargar() = trabajar {
        if (pestana == PestanaMios.PEDIDOS) pedidos = publico.misPedidos() else reservas = publico.misReservas()
    }

    fun cancelar(reserva: ReservaCliente) = trabajar("Reserva cancelada.") {
        publico.cancelarReservaCliente(reserva.codigo)
        reservas = publico.misReservas()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MisPedidosPantalla(alVolver: () -> Unit) {
    val app = casos()
    val modelo: MisPedidosModelo = viewModel { MisPedidosModelo(app) }

    Scaffold(topBar = { TopAppBar(title = { Text("Mis pedidos y reservas") }, navigationIcon = { TextButton(onClick = alVolver) { Text("Atrás") } }) }) { margen ->
        Column(Modifier.padding(margen)) {
            Pestanas(PestanaMios.entries, modelo.pestana, { it.titulo }, modelo::elegir)
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { Mensajes(error = modelo.error, aviso = modelo.aviso) }
                item {
                    OutlinedButton(onClick = modelo::cargar, enabled = !modelo.ocupado, modifier = Modifier.fillMaxWidth()) {
                        Text(if (modelo.ocupado) "Cargando…" else "Actualizar")
                    }
                }
                if (modelo.pestana == PestanaMios.PEDIDOS) {
                    if (modelo.pedidos.isEmpty() && !modelo.ocupado && modelo.error == null) item { Vacio("Los pedidos que hagas desde este teléfono aparecen aquí.") }
                    items(modelo.pedidos, key = { it.codigo }) { guardado -> TarjetaOrden(guardado.orden) }
                } else {
                    if (modelo.reservas.isEmpty() && !modelo.ocupado && modelo.error == null) item { Vacio("Tus reservas aparecen aquí.") }
                    items(modelo.reservas, key = { it.codigo }) { reserva ->
                        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("${reserva.restaurante} · ${reserva.sucursal}", fontWeight = FontWeight.Bold)
                                Text("${reserva.fechaHora.take(16).replace("T", " ")} · ${reserva.personas} personas · Mesa ${reserva.mesa ?: "—"}")
                                Text("Código ${reserva.codigo} · ${reserva.estadoNombre}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (reserva.vigente) {
                                    OutlinedButton(onClick = { modelo.cancelar(reserva) }, enabled = !modelo.ocupado) { Text("Cancelar reserva") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
