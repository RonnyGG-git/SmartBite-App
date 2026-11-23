package com.smartbite.movil.ui.cliente

import androidx.compose.ui.text.style.TextOverflow
import com.smartbite.movil.ui.componentes.Linea
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.smartbite.domain.modelo.Producto
import com.smartbite.domain.modelo.TipoConsumo
import com.smartbite.movil.ui.casos
import com.smartbite.movil.ui.componentes.FotoPlatillo
import com.smartbite.movil.ui.componentes.Mensajes
import com.smartbite.movil.ui.componentes.TarjetaOrden
import com.smartbite.movil.ui.pesos

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientePantalla(alVolver: () -> Unit) {
    val app = casos()
    val modelo: ClienteModelo = viewModel { ClienteModelo(app, app) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(modelo.mesa?.let { "Mesa ${it.numero} · ${it.restaurante}" } ?: "Pedir en mi mesa") },
                navigationIcon = { TextButton(onClick = alVolver) { Text("Salir") } },
            )
        },
    ) { margen ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(margen),
        ) {
            item { Mensajes(error = modelo.error, aviso = modelo.aviso) }
            when (modelo.paso) {
                Paso.MESA -> item { BuscarMesa(modelo) }
                Paso.MENU -> {
                    items(modelo.menu, key = { it.id }) { producto -> FilaMenu(modelo, producto) }
                    item { ResumenPedido(modelo) }
                }
                Paso.SEGUIMIENTO -> item { Seguimiento(modelo) }
            }
        }
    }
}

@Composable
private fun BuscarMesa(modelo: ClienteModelo) {
    val contexto = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Escanea el código QR que está en tu mesa.", style = MaterialTheme.typography.titleMedium)
        Button(
            onClick = {
                GmsBarcodeScanning.getClient(contexto).startScan()
                    .addOnSuccessListener { leido -> leido.rawValue?.let(modelo::leerQr) }
            },
            enabled = !modelo.ocupado,
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            Text(if (modelo.ocupado) "Buscando tu mesa…" else "Escanear QR de la mesa")
        }
        Text("¿No funciona la cámara? Escribe el código o pega el enlace del QR.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value = modelo.codigoQr,
            onValueChange = { modelo.codigoQr = it },
            label = { Text("Código o enlace del QR") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedButton(onClick = modelo::buscarMesa, enabled = !modelo.ocupado, modifier = Modifier.fillMaxWidth()) {
            Text("Ver el menú")
        }
    }
}

@Composable
private fun FilaMenu(modelo: ClienteModelo, producto: Producto) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Linea),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            FotoPlatillo(producto.imagen, producto.nombre, 76.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(producto.nombre, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (producto.descripcion.isNotBlank()) {
                    Text(producto.descripcion, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(producto.precio.pesos(), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    val cantidad = modelo.carrito.cantidadDe(producto.id)
                    if (cantidad > 0) {
                        TextButton(onClick = { modelo.quitar(producto) }) { Text("−") }
                        Text("$cantidad", fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = { modelo.agregar(producto) }) { Text("+") }
                }
            }
        }
    }
}

@Composable
private fun ResumenPedido(modelo: ClienteModelo) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        modelo.limite?.let { Mensajes(error = it, aviso = null) }
        OutlinedTextField(
            value = modelo.nombre,
            onValueChange = { modelo.nombre = it },
            label = { Text("Tu nombre (opcional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TipoConsumo.entries.forEach { opcion ->
                FilterChip(
                    selected = modelo.tipo == opcion,
                    onClick = { modelo.tipo = opcion },
                    label = { Text(opcion.etiqueta) },
                )
            }
        }
        Button(
            onClick = modelo::pedir,
            enabled = !modelo.ocupado && !modelo.carrito.vacio,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            Text("Pedir ${modelo.carrito.unidades} · ${modelo.carrito.total.pesos()}")
        }
    }
}

@Composable
private fun Seguimiento(modelo: ClienteModelo) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Tu pedido va así", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            "El mesero lo confirma y luego pasa a cocina. Esta pantalla se actualiza sola cada 10 segundos.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        modelo.orden?.let { TarjetaOrden(it) }
        OutlinedButton(onClick = modelo::actualizar, enabled = !modelo.ocupado, modifier = Modifier.fillMaxWidth()) {
            Text("Actualizar ahora")
        }
        if (modelo.opinando) {
            OutlinedTextField(
                value = modelo.mensajeOpinion,
                onValueChange = { modelo.mensajeOpinion = it },
                label = { Text("¿Qué te pareció?") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(onClick = modelo::opinar, enabled = !modelo.ocupado, modifier = Modifier.fillMaxWidth()) { Text("Enviar opinión") }
        } else {
            OutlinedButton(onClick = { modelo.abrirOpinion(true) }, modifier = Modifier.fillMaxWidth()) { Text("Opinar sobre el servicio") }
        }
        TextButton(onClick = modelo::volverAMesa, modifier = Modifier.fillMaxWidth()) { Text("Hacer otro pedido") }
    }
}
