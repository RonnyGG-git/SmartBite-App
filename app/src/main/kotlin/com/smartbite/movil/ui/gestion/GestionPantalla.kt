package com.smartbite.movil.ui.gestion

import com.smartbite.movil.ui.componentes.Encabezado
import com.smartbite.movil.ui.componentes.Linea
import androidx.compose.foundation.BorderStroke
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartbite.application.puerto.entrada.GestionCasos
import com.smartbite.domain.modelo.AccionRecurso
import com.smartbite.domain.modelo.Campo
import com.smartbite.domain.modelo.CodigoQr
import com.smartbite.domain.modelo.Ficha
import com.smartbite.domain.modelo.Opcion
import com.smartbite.domain.modelo.Recurso
import com.smartbite.domain.modelo.Rol
import com.smartbite.domain.modelo.TipoAccion
import com.smartbite.domain.modelo.TipoCampo
import com.smartbite.movil.ui.ModeloBase
import com.smartbite.movil.ui.casos
import com.smartbite.movil.ui.componentes.CampoTexto
import com.smartbite.movil.ui.componentes.FotoPlatillo
import com.smartbite.movil.ui.componentes.Interruptor
import com.smartbite.movil.ui.componentes.Mensajes
import com.smartbite.movil.ui.componentes.Selector
import com.smartbite.movil.ui.componentes.Vacio
import com.smartbite.movil.ui.mensajeDe
import com.smartbite.movil.ui.tema.Brasa
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class GestionModelo(private val gestion: GestionCasos, val recurso: Recurso) : ModeloBase() {

    var fichas by mutableStateOf<List<Ficha>>(emptyList())
        private set
    var busqueda by mutableStateOf("")
    var abierta by mutableStateOf<Ficha?>(null)
        private set
    var creando by mutableStateOf(false)
        private set
    var accion by mutableStateOf<AccionRecurso?>(null)
        private set
    var qr by mutableStateOf<CodigoQr?>(null)
        private set
    var porBorrar by mutableStateOf(false)
        private set
    val valores = mutableStateMapOf<String, String>()
    val valoresAccion = mutableStateMapOf<String, String>()
    val opciones = mutableStateMapOf<String, List<Opcion>>()

    val visibles: List<Ficha>
        get() {
            val texto = busqueda.trim().lowercase()
            if (texto.isEmpty()) return fichas
            return fichas.filter { (recurso.principal(it) + " " + recurso.secundario(it)).lowercase().contains(texto) }
        }

    val formularioAbierto: Boolean get() = creando || abierta != null

    init {
        cargar()
    }

    fun cargar() = trabajar {
        fichas = gestion.listar(recurso)
    }

    private fun cargarOpciones(de: List<Campo>) {
        viewModelScope.launch {
            de.forEach { campo ->
                val fuente = campo.fuente ?: return@forEach
                if (campo.clave in opciones) return@forEach
                try {
                    opciones[campo.clave] = gestion.opciones(fuente)
                } catch (cancelado: CancellationException) {
                    throw cancelado
                } catch (fallo: Exception) {
                    error = mensajeDe(fallo)
                }
            }
        }
    }

    fun nuevo() {
        limpiarMensajes()
        valores.clear()
        recurso.camposPara(true).filter { it.tipo == TipoCampo.BOOLEANO }.forEach { valores[it.clave] = "true" }
        abierta = null
        creando = true
        cargarOpciones(recurso.camposPara(true))
    }

    fun abrir(ficha: Ficha) {
        limpiarMensajes()
        valores.clear()
        recurso.camposPara(false).forEach { campo -> valores[campo.clave] = ficha.valores[campo.clave].orEmpty() }
        abierta = ficha
        creando = false
        if (recurso.editable) cargarOpciones(recurso.camposPara(false))
    }

    fun cerrar() {
        abierta = null
        creando = false
        accion = null
        porBorrar = false
        limpiarMensajes()
    }

    fun guardar() = trabajar("Guardado.") {
        val guardada = gestion.guardar(recurso, abierta?.id, valores.toMap())
        fichas = gestion.listar(recurso)
        abierta = guardada
        creando = false
        recurso.camposPara(false).forEach { campo -> valores[campo.clave] = guardada.valores[campo.clave].orEmpty() }
    }

    fun pedirBorrado(si: Boolean) {
        porBorrar = si
    }

    fun eliminar() = trabajar("Eliminado.") {
        val actual = abierta ?: return@trabajar
        porBorrar = false
        gestion.eliminar(recurso, actual.id)
        abierta = null
        fichas = gestion.listar(recurso)
    }

    fun abrirAccion(definida: AccionRecurso) {
        limpiarMensajes()
        valoresAccion.clear()
        accion = definida
        cargarOpciones(definida.campos)
    }

    fun cerrarAccion() {
        accion = null
    }

    fun ejecutar(definida: AccionRecurso) = trabajar("${definida.etiqueta}: listo.") {
        val actual = abierta ?: return@trabajar
        val resultado = gestion.ejecutar(recurso, actual.id, definida.clave, valoresAccion.toMap())
        accion = null
        abierta = resultado
        recurso.camposPara(false).forEach { campo -> valores[campo.clave] = resultado.valores[campo.clave].orEmpty() }
        fichas = gestion.listar(recurso)
    }

    fun verQr() = trabajar {
        val actual = abierta ?: return@trabajar
        qr = gestion.qrDeMesa(actual.id)
    }

    fun cerrarQr() {
        qr = null
    }

    fun subirFoto(bytes: ByteArray) = trabajar("Foto actualizada.") {
        val actual = abierta ?: return@trabajar
        abierta = gestion.subirFoto(actual.id, bytes)
        fichas = gestion.listar(recurso)
    }
}

@Composable
fun GestionPantalla(recurso: Recurso, rol: Rol, alElegirRestaurante: (Ficha) -> Unit = {}) {
    val app = casos()
    val modelo: GestionModelo = viewModel(key = "gestion-${recurso.clave}") { GestionModelo(app, recurso) }
    val gestiona = recurso.gestiona(rol)
    val contexto = LocalContext.current
    val elegirFoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { elegida ->
            contexto.contentResolver.openInputStream(elegida)?.use { it.readBytes() }?.let(modelo::subirFoto)
        }
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Mensajes(error = modelo.error, aviso = modelo.aviso) }
        if (modelo.formularioAbierto) {
            item {
                Formulario(
                    modelo = modelo,
                    gestiona = gestiona,
                    alAccion = { definida ->
                        when (definida.tipo) {
                            TipoAccion.ALTERNAR -> modelo.ejecutar(definida)
                            TipoAccion.FORMULARIO -> modelo.abrirAccion(definida)
                            TipoAccion.QR -> modelo.verQr()
                            TipoAccion.FOTO -> elegirFoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            TipoAccion.ELEGIR -> modelo.abierta?.let(alElegirRestaurante)
                        }
                    },
                )
            }
        } else {
            item { Encabezado(recurso.titulo, "${modelo.fichas.size} en total" + if (gestiona) "" else " · solo lectura") }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        CampoTexto("Buscar ${recurso.titulo.lowercase()}", modelo.busqueda, null) { modelo.busqueda = it }
                    }
                    if (gestiona && recurso.creable) {
                        Button(onClick = modelo::nuevo, enabled = !modelo.ocupado) { Text("Nuevo") }
                    }
                }
            }
            item {
                OutlinedButton(onClick = modelo::cargar, enabled = !modelo.ocupado, modifier = Modifier.fillMaxWidth()) {
                    Text(if (modelo.ocupado) "Cargando…" else "Actualizar (${modelo.visibles.size})")
                }
            }
            if (modelo.visibles.isEmpty() && !modelo.ocupado && modelo.error == null) item { Vacio("No hay ${recurso.titulo.lowercase()} todavía.") }
            items(modelo.visibles, key = { it.id }) { ficha -> FilaFicha(recurso, ficha) { modelo.abrir(ficha) } }
        }
    }

    modelo.accion?.let { definida -> DialogoAccion(modelo, definida) }
    modelo.qr?.let { codigo -> DialogoQrMesa(codigo, modelo::cerrarQr) }
    if (modelo.porBorrar) {
        AlertDialog(
            onDismissRequest = { modelo.pedirBorrado(false) },
            title = { Text("¿Eliminar ${recurso.singular}?") },
            text = { Text("Esta acción no se puede deshacer.") },
            confirmButton = { TextButton(onClick = modelo::eliminar) { Text("Eliminar", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { modelo.pedirBorrado(false) }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun FilaFicha(recurso: Recurso, ficha: Ficha, alAbrir: () -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Linea),
        modifier = Modifier.fillMaxWidth().clickable(onClick = alAbrir),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (recurso.clave == "productos") {
                FotoPlatillo(recurso.imagen(ficha), recurso.principal(ficha), 52.dp)
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(recurso.principal(ficha), fontWeight = FontWeight.SemiBold, maxLines = 2)
                val detalle = recurso.secundario(ficha)
                if (detalle.isNotBlank()) Text(detalle, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            }
            if (recurso.alerta(ficha)) {
                Spacer(Modifier.width(8.dp))
                Text("●", color = Brasa)
            }
        }
    }
}

@Composable
private fun Formulario(modelo: GestionModelo, gestiona: Boolean, alAccion: (AccionRecurso) -> Unit) {
    val recurso = modelo.recurso
    val ficha = modelo.abierta
    val editable = gestiona && (modelo.creando || recurso.editable)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            when {
                modelo.creando -> "Nuevo ${recurso.singular}"
                ficha != null -> recurso.principal(ficha)
                else -> ""
            },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        if (ficha != null) {
            val detalle = recurso.secundario(ficha)
            if (detalle.isNotBlank()) Text(detalle, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (recurso.clave == "productos") FotoPlatillo(recurso.imagen(ficha), recurso.principal(ficha), 120.dp)
        }
        recurso.camposPara(modelo.creando).forEach { campo -> CampoDe(modelo, campo, editable) }
        if (editable && (modelo.creando || recurso.editable) && recurso.campos.isNotEmpty()) {
            Button(onClick = modelo::guardar, enabled = !modelo.ocupado, modifier = Modifier.fillMaxWidth()) {
                Text(if (modelo.ocupado) "Guardando…" else "Guardar")
            }
        }
        if (ficha != null) {
            val acciones = recurso.acciones.filter { gestiona || !it.soloGestion }
            if (acciones.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    acciones.forEach { definida ->
                        OutlinedButton(onClick = { alAccion(definida) }, enabled = !modelo.ocupado) { Text(definida.etiqueta) }
                    }
                }
            }
            if (gestiona && recurso.borrable) {
                TextButton(onClick = { modelo.pedirBorrado(true) }, enabled = !modelo.ocupado) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            }
        }
        TextButton(onClick = modelo::cerrar, modifier = Modifier.fillMaxWidth()) { Text("Volver a la lista") }
    }
}

@Composable
private fun CampoDe(modelo: GestionModelo, campo: Campo, editable: Boolean) {
    val valor = modelo.valores[campo.clave].orEmpty()
    val error = modelo.campos[campo.clave]
    val etiqueta = campo.etiqueta + if (campo.obligatorio && campo.tipo != TipoCampo.BOOLEANO) " *" else ""
    when (campo.tipo) {
        TipoCampo.BOOLEANO -> Interruptor(etiqueta, valor == "true", editable) { modelo.valores[campo.clave] = it.toString() }
        TipoCampo.OPCION -> Selector(etiqueta, modelo.opciones[campo.clave].orEmpty(), valor, error, editable) { modelo.valores[campo.clave] = it }
        else -> CampoTexto(
            etiqueta = etiqueta,
            valor = valor,
            error = error,
            teclado = when (campo.tipo) {
                TipoCampo.ENTERO -> KeyboardType.Number
                TipoCampo.DECIMAL -> KeyboardType.Decimal
                TipoCampo.CORREO -> KeyboardType.Email
                TipoCampo.TELEFONO -> KeyboardType.Phone
                TipoCampo.CLAVE -> KeyboardType.Password
                else -> KeyboardType.Text
            },
            oculto = campo.tipo == TipoCampo.CLAVE,
            lineas = if (campo.tipo == TipoCampo.TEXTO_LARGO) 3 else 1,
            habilitado = editable,
        ) { modelo.valores[campo.clave] = it }
    }
}

@Composable
private fun DialogoAccion(modelo: GestionModelo, definida: AccionRecurso) {
    AlertDialog(
        onDismissRequest = modelo::cerrarAccion,
        title = { Text(definida.etiqueta) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                modelo.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                definida.campos.forEach { campo ->
                    val valor = modelo.valoresAccion[campo.clave].orEmpty()
                    val error = modelo.campos[campo.clave]
                    if (campo.tipo == TipoCampo.OPCION) {
                        Selector(campo.etiqueta, modelo.opciones[campo.clave].orEmpty(), valor, error) { modelo.valoresAccion[campo.clave] = it }
                    } else {
                        CampoTexto(
                            campo.etiqueta, valor, error,
                            teclado = if (campo.tipo == TipoCampo.ENTERO) KeyboardType.Number else KeyboardType.Text,
                        ) { modelo.valoresAccion[campo.clave] = it }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { modelo.ejecutar(definida) }, enabled = !modelo.ocupado) { Text("Aplicar") } },
        dismissButton = { TextButton(onClick = modelo::cerrarAccion) { Text("Cancelar") } },
    )
}

@Composable
fun DialogoQrMesa(codigo: CodigoQr, alCerrar: () -> Unit) {
    val imagen = remember(codigo) { BitmapFactory.decodeByteArray(codigo.imagenPng, 0, codigo.imagenPng.size)?.asImageBitmap() }
    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text("QR de la mesa") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                imagen?.let {
                    Image(
                        bitmap = it,
                        contentDescription = "Código QR de la mesa",
                        modifier = Modifier.size(240.dp).background(Color.White, RoundedCornerShape(12.dp)).padding(8.dp),
                    )
                }
                Text("Imprímelo y pégalo en la mesa. Al escanearlo, el cliente ve el menú y pide.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(codigo.url, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { TextButton(onClick = alCerrar) { Text("Cerrar") } },
    )
}
