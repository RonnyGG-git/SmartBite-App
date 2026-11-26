package com.smartbite.movil.ui.gestion

import com.smartbite.movil.ui.componentes.Linea
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartbite.application.puerto.entrada.GestionCasos
import com.smartbite.domain.modelo.FilaTablero
import com.smartbite.domain.modelo.PeriodoReporte
import com.smartbite.domain.modelo.ReportePdf
import com.smartbite.domain.modelo.Tablero
import com.smartbite.movil.ui.ModeloBase
import com.smartbite.movil.ui.casos
import com.smartbite.movil.ui.componentes.Mensajes
import com.smartbite.movil.ui.pesos
import com.smartbite.movil.ui.pesosDeTexto
import com.smartbite.movil.ui.tema.Brasa

class TableroModelo(private val gestion: GestionCasos) : ModeloBase() {
    var tablero by mutableStateOf<Tablero?>(null)
        private set

    init {
        cargar()
    }

    fun cargar() = trabajar {
        tablero = gestion.tablero()
    }

    var periodo by mutableStateOf(PeriodoReporte.MES)
    var reporte by mutableStateOf<ReportePdf?>(null)
        private set
    var guardado by mutableStateOf<Uri?>(null)
        private set

    fun descargarReporte() = trabajar {
        guardado = null
        reporte = gestion.reportePdf(periodo)
    }

    fun reporteGuardado(destino: Uri?, escribir: (Uri, ByteArray) -> Unit) {
        val listo = reporte ?: return
        reporte = null
        if (destino == null) return
        try {
            escribir(destino, listo.bytes)
            guardado = destino
            error = null
            aviso = "Reporte guardado: ${listo.nombre}"
        } catch (fallo: Exception) {
            error = "No se pudo guardar el PDF. Intenta de nuevo."
        }
    }
}

@Composable
fun TableroPantalla() {
    val app = casos()
    val modelo: TableroModelo = viewModel { TableroModelo(app) }
    val datos = modelo.tablero
    val contexto = LocalContext.current
    val guardar = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { destino ->
        modelo.reporteGuardado(destino) { uri, bytes ->
            contexto.contentResolver.openOutputStream(uri)?.use { it.write(bytes) } ?: error("Sin destino")
        }
    }

    LaunchedEffect(modelo.reporte) {
        modelo.reporte?.let { guardar.launch(it.nombre) }
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Mensajes(error = modelo.error, aviso = modelo.aviso) }
        item {
            OutlinedButton(onClick = modelo::cargar, enabled = !modelo.ocupado, modifier = Modifier.fillMaxWidth()) {
                Text(if (modelo.ocupado) "Cargando…" else "Actualizar")
            }
        }
        if (datos != null) {
            item { Text(datos.restaurante ?: "Tu restaurante", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Cifra("Ventas de hoy", datos.ventasHoy.pesos(), Modifier.weight(1f))
                    Cifra("Órdenes de hoy", datos.ordenesHoy.toString(), Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Cifra("Mesas ocupadas", "${datos.mesasOcupadas} / ${datos.mesas}", Modifier.weight(1f))
                    Cifra("Platillos", datos.productos.toString(), Modifier.weight(1f))
                    Cifra("Usuarios", datos.usuarios.toString(), Modifier.weight(1f))
                }
            }
            item { Bloque("Órdenes por estado", datos.ordenesPorEstado) }
            item { Bloque("Ventas por método de pago", datos.ventasPorMetodo.map { FilaTablero(it.etiqueta, pesosDeTexto(it.valor)) }) }
            item { Bloque("Inventario con stock bajo", datos.stockBajo, vacio = "Todo el inventario está sobre su mínimo.") }
        }
        item {
            ReportePdfTarjeta(
                modelo = modelo,
                alAbrir = { uri ->
                    val abrir = Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/pdf").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    runCatching { contexto.startActivity(Intent.createChooser(abrir, "Abrir reporte")) }
                },
            )
        }
    }
}

@Composable
private fun Cifra(etiqueta: String, valor: String, modifier: Modifier) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Linea),
        modifier = modifier,
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(etiqueta, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
            Text(valor, color = Brasa, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun Bloque(titulo: String, filas: List<FilaTablero>, vacio: String = "Sin datos todavía.") {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Linea),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(titulo, fontWeight = FontWeight.Bold)
            if (filas.isEmpty()) Text(vacio, color = MaterialTheme.colorScheme.onSurfaceVariant)
            filas.forEach { fila ->
                Row {
                    Text(fila.etiqueta, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(fila.valor, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun ReportePdfTarjeta(modelo: TableroModelo, alAbrir: (Uri) -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Linea),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Reporte en PDF", fontWeight = FontWeight.Bold)
            Text(
                "Ventas, propinas, ticket promedio, ventas por método y por día, órdenes por estado, platillos más vendidos y stock bajo.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PeriodoReporte.entries.forEach { opcion ->
                    FilterChip(selected = modelo.periodo == opcion, onClick = { modelo.periodo = opcion }, label = { Text(opcion.etiqueta) })
                }
            }
            Button(onClick = modelo::descargarReporte, enabled = !modelo.ocupado, modifier = Modifier.fillMaxWidth()) {
                Text(if (modelo.ocupado) "Generando…" else "Descargar PDF")
            }
            modelo.guardado?.let { uri ->
                OutlinedButton(onClick = { alAbrir(uri) }, modifier = Modifier.fillMaxWidth()) { Text("Abrir el reporte") }
            }
        }
    }
}
