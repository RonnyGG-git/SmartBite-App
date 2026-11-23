package com.smartbite.movil.ui.gestion

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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartbite.application.puerto.entrada.GestionCasos
import com.smartbite.domain.modelo.CatalogoGestion
import com.smartbite.domain.modelo.Compra
import com.smartbite.domain.modelo.Fuente
import com.smartbite.domain.modelo.Opcion
import com.smartbite.movil.ui.ModeloBase
import com.smartbite.movil.ui.casos
import com.smartbite.movil.ui.componentes.CampoTexto
import com.smartbite.movil.ui.componentes.Mensajes
import com.smartbite.movil.ui.componentes.Selector
import com.smartbite.movil.ui.componentes.Vacio
import com.smartbite.movil.ui.pesos

class LineaEditable(item: String = "", cantidad: String = "1", costo: String = "") {
    var item by mutableStateOf(item)
    var cantidad by mutableStateOf(cantidad)
    var costo by mutableStateOf(costo)
}

class ComprasModelo(private val gestion: GestionCasos) : ModeloBase() {
    var compras by mutableStateOf<List<Compra>>(emptyList())
        private set
    var nueva by mutableStateOf(false)
        private set
    var proveedores by mutableStateOf<List<Opcion>>(emptyList())
        private set
    var items by mutableStateOf<List<Opcion>>(emptyList())
        private set
    var proveedor by mutableStateOf("")
    val lineas = mutableStateListOf<LineaEditable>()

    init {
        cargar()
    }

    fun cargar() = trabajar {
        compras = gestion.compras()
    }

    fun empezar() = trabajar {
        proveedores = gestion.opciones(Fuente.DeRecurso(CatalogoGestion.proveedores.clave))
        items = gestion.opciones(Fuente.DeRecurso(CatalogoGestion.items.clave))
        proveedor = ""
        lineas.clear()
        lineas.add(LineaEditable())
        nueva = true
    }

    fun cancelar() {
        nueva = false
        limpiarMensajes()
    }

    fun crear() = trabajar("Compra registrada como pendiente.") {
        val datos = lineas.filter { it.item.isNotBlank() }.map { Triple(it.item.toInt(), it.cantidad, it.costo) }
        gestion.crearCompra(proveedor.toIntOrNull(), datos)
        nueva = false
        compras = gestion.compras()
    }

    fun recibir(compra: Compra) = trabajar("Compra #${compra.id} recibida: el stock ya subió.") {
        gestion.recibirCompra(compra)
        compras = gestion.compras()
    }

    fun anular(compra: Compra) = trabajar("Compra #${compra.id} anulada.") {
        gestion.anularCompra(compra)
        compras = gestion.compras()
    }
}

@Composable
fun ComprasPantalla() {
    val app = casos()
    val modelo: ComprasModelo = viewModel { ComprasModelo(app) }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Mensajes(error = modelo.error, aviso = modelo.aviso) }
        if (modelo.nueva) {
            item { Text("Nueva compra", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            item { Selector("Proveedor *", modelo.proveedores, modelo.proveedor, modelo.campos["proveedor"]) { modelo.proveedor = it } }
            items(modelo.lineas) { linea ->
                Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Selector("Ítem", modelo.items, linea.item, null) { linea.item = it }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column(Modifier.weight(1f)) { CampoTexto("Cantidad", linea.cantidad, null, KeyboardType.Number) { linea.cantidad = it } }
                            Column(Modifier.weight(1f)) { CampoTexto("Costo unitario", linea.costo, null, KeyboardType.Decimal) { linea.costo = it } }
                        }
                        TextButton(onClick = { modelo.lineas.remove(linea) }) { Text("Quitar línea") }
                    }
                }
            }
            item {
                modelo.campos["detalles"]?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                OutlinedButton(onClick = { modelo.lineas.add(LineaEditable()) }, modifier = Modifier.fillMaxWidth()) { Text("Agregar línea") }
            }
            item {
                Button(onClick = modelo::crear, enabled = !modelo.ocupado, modifier = Modifier.fillMaxWidth()) { Text("Registrar compra") }
                TextButton(onClick = modelo::cancelar, modifier = Modifier.fillMaxWidth()) { Text("Cancelar") }
            }
        } else {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = modelo::cargar, enabled = !modelo.ocupado, modifier = Modifier.weight(1f)) { Text("Actualizar") }
                    Button(onClick = modelo::empezar, enabled = !modelo.ocupado) { Text("Nueva compra") }
                }
            }
            if (modelo.compras.isEmpty() && !modelo.ocupado && modelo.error == null) item { Vacio("Todavía no hay compras.") }
            items(modelo.compras, key = { it.id }) { compra ->
                Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Compra #${compra.id} · ${compra.proveedor}", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text(compra.estadoNombre, color = MaterialTheme.colorScheme.primary)
                        }
                        compra.lineas.forEach { linea ->
                            Row {
                                Text("${linea.cantidad} × ${linea.item}", Modifier.weight(1f))
                                Text(linea.subtotal.pesos(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Text("Total ${compra.total.pesos()} · ${compra.fecha}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (compra.pendiente) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { modelo.recibir(compra) }, enabled = !modelo.ocupado) { Text("Recibir") }
                                OutlinedButton(onClick = { modelo.anular(compra) }, enabled = !modelo.ocupado) { Text("Anular") }
                            }
                        }
                    }
                }
            }
        }
    }
}
