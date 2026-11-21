package com.smartbite.movil.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.smartbite.domain.modelo.EstadoOrden
import com.smartbite.domain.modelo.Orden
import com.smartbite.movil.ui.pesos
import com.smartbite.movil.ui.tema.Brasa
import com.smartbite.movil.ui.tema.colorDe

@Composable
fun Etiqueta(estado: EstadoOrden) {
    val color = colorDe(estado)
    Text(
        text = estado.etiqueta,
        color = color,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 3.dp),
    )
}

@Composable
fun Mensajes(error: String?, aviso: String?) {
    error?.let { Franja(it, MaterialTheme.colorScheme.error) }
    aviso?.let { Franja(it, Color(0xFF7BD88F)) }
}

@Composable
private fun Franja(texto: String, color: Color) {
    Text(
        text = texto,
        color = color,
        modifier = Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .padding(12.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
    )
}

@Composable
fun FotoPlatillo(url: String?, nombre: String, lado: Dp = 64.dp) {
    val forma = RoundedCornerShape(12.dp)
    if (url == null) {
        Box(
            modifier = Modifier.size(lado).clip(forma).background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(nombre.take(1).uppercase(), color = Brasa, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        }
    } else {
        AsyncImage(
            model = url,
            contentDescription = nombre,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(lado).clip(forma).background(MaterialTheme.colorScheme.surfaceVariant),
        )
    }
}

@Composable
fun <T> Pestanas(opciones: List<T>, elegida: T, titulo: (T) -> String, alElegir: (T) -> Unit) {
    TabRow(selectedTabIndex = opciones.indexOf(elegida).coerceAtLeast(0)) {
        opciones.forEach { opcion ->
            Tab(selected = opcion == elegida, onClick = { alElegir(opcion) }, text = { Text(titulo(opcion), maxLines = 1) })
        }
    }
}

@Composable
fun Vacio(texto: String) {
    Text(
        texto,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
    )
}

@Composable
fun TarjetaOrden(orden: Orden, acciones: @Composable () -> Unit = {}) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Orden #${orden.id}" + (orden.mesa?.let { " · Mesa $it" } ?: if (orden.paraLlevar) " · Para llevar" else " · Sin mesa"),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Etiqueta(orden.estado)
            }
            orden.cliente?.let { Text("Cliente: $it", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            orden.lineas.forEach { linea ->
                Row {
                    Text("${linea.cantidad} × ${linea.nombre}", Modifier.weight(1f))
                    Text(linea.subtotal.pesos(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (orden.faltantes.isNotEmpty()) {
                Text("Faltan insumos: ${orden.faltantes.joinToString()}", color = MaterialTheme.colorScheme.error)
            }
            orden.minutosEstimados?.let { Text("Tiempo estimado: $it min", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Total ${orden.total.pesos()}", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(
                    when {
                        orden.pagada -> "Pagada"
                        orden.cuentaSolicitada -> "Cuenta pedida"
                        else -> "Sin pagar"
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            acciones()
        }
    }
}
