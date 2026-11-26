package com.smartbite.movil.ui.caja

import com.smartbite.movil.ui.componentes.Linea
import androidx.compose.foundation.BorderStroke
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartbite.application.puerto.entrada.CajaCasos
import com.smartbite.domain.modelo.CodigoQr
import com.smartbite.domain.modelo.Cuenta
import com.smartbite.domain.modelo.EstadoOrden
import com.smartbite.domain.modelo.Factura
import com.smartbite.domain.modelo.FacturaResumen
import com.smartbite.domain.modelo.MetodoPago
import com.smartbite.domain.modelo.Orden
import com.smartbite.domain.modelo.PagoRegistrado
import com.smartbite.domain.modelo.ResultadoCobro
import com.smartbite.movil.ui.ModeloBase
import com.smartbite.movil.ui.casos
import com.smartbite.movil.ui.componentes.Mensajes
import com.smartbite.movil.ui.componentes.Pestanas
import com.smartbite.movil.ui.componentes.TarjetaOrden
import com.smartbite.movil.ui.componentes.Vacio
import com.smartbite.movil.ui.paraEditar
import com.smartbite.movil.ui.pesos
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class PestanaCaja(val titulo: String) { COBRAR("Por cobrar"), LLEVAR("Para llevar"), FACTURAS("Facturas"), VENTAS("Ventas") }

class CajaModelo(private val caja: CajaCasos) : ModeloBase() {

    var pestana by mutableStateOf(PestanaCaja.COBRAR)
        private set
    var porCobrar by mutableStateOf<List<Orden>>(emptyList())
        private set
    var paraLlevar by mutableStateOf<List<Orden>>(emptyList())
        private set
    var facturas by mutableStateOf<List<FacturaResumen>>(emptyList())
        private set
    var ventas by mutableStateOf<List<Orden>>(emptyList())
        private set
    var metodos by mutableStateOf<List<MetodoPago>>(emptyList())
        private set
    var cuenta by mutableStateOf<Cuenta?>(null)
        private set
    var qr by mutableStateOf<CodigoQr?>(null)
        private set
    var porAnular by mutableStateOf<PagoRegistrado?>(null)
        private set
    var factura by mutableStateOf<Factura?>(null)
        private set
    var navegador by mutableStateOf<String?>(null)
        private set
    var metodo by mutableStateOf<MetodoPago?>(null)
    var monto by mutableStateOf("")
    var propina by mutableStateOf("")
    var recibido by mutableStateOf("")
    var referencia by mutableStateOf("")

    private var vigilancia: Job? = null

    init {
        cargar()
    }

    fun elegir(nueva: PestanaCaja) {
        pestana = nueva
        cargar()
    }

    fun cargar() = trabajar {
        when (pestana) {
            PestanaCaja.COBRAR -> porCobrar = caja.verPorCobrar()
            PestanaCaja.LLEVAR -> paraLlevar = caja.verParaLlevar()
            PestanaCaja.FACTURAS -> facturas = caja.verFacturas()
            PestanaCaja.VENTAS -> ventas = caja.verVentas()
        }
    }

    fun abrirCobro(orden: Orden) = trabajar {
        if (metodos.isEmpty()) metodos = caja.metodosDePago()
        val nueva = caja.verCuenta(orden.id)
        cuenta = nueva
        metodo = metodos.firstOrNull { !it.usaPasarela } ?: metodos.firstOrNull()
        monto = nueva.saldoDisponible.paraEditar()
        propina = ""
        recibido = ""
        referencia = ""
    }

    fun cobrar() = trabajar {
        val actual = cuenta ?: return@trabajar
        when (val resultado = caja.cobrar(actual, metodo, monto, propina, recibido, referencia)) {
            is ResultadoCobro.Registrado -> {
                cuenta = resultado.cuenta
                monto = resultado.cuenta.saldoDisponible.paraEditar()
                recibido = ""
                propina = ""
                aviso = buildString {
                    append(if (resultado.cuenta.saldada) "Orden #${actual.orden.id} pagada por completo." else "Pago registrado. Saldo: ${resultado.cuenta.saldoPendiente.pesos()}.")
                    resultado.cambio?.let { append(" Cambio a devolver: ${it.pesos()}.") }
                }
            }
            is ResultadoCobro.EnPasarela -> {
                cuenta = resultado.cuenta
                navegador = resultado.checkout
                aviso = "Se abrió la pasarela de pago. Esta pantalla se actualiza cuando el pago se apruebe."
                vigilar()
            }
        }
    }

    fun navegadorAbierto() {
        navegador = null
    }

    fun generarQr() = trabajar {
        val actual = cuenta ?: return@trabajar
        qr = caja.generarQr(actual)
        vigilar()
    }

    private fun vigilar() {
        vigilancia?.cancel()
        val ordenId = cuenta?.orden?.id ?: return
        vigilancia = viewModelScope.launch {
            repeat(150) {
                delay(4_000)
                if (!isActive) return@launch
                val nueva = try {
                    caja.verCuenta(ordenId)
                } catch (pausa: Exception) {
                    if (!isActive) throw pausa
                    null
                }
                if (nueva != null) {
                    val pagosAntes = cuenta?.pagos?.count { it.aprobado } ?: 0
                    cuenta = nueva
                    monto = nueva.saldoDisponible.paraEditar()
                    if (nueva.pagos.count { it.aprobado } > pagosAntes) {
                        aviso = if (nueva.saldada) "Pago aprobado: la orden #$ordenId quedó pagada." else "Pago aprobado. Saldo: ${nueva.saldoPendiente.pesos()}."
                        qr = null
                    }
                    if (nueva.saldada) return@launch
                }
            }
        }
    }

    fun cerrarQr() {
        qr = null
    }

    fun pedirAnular(pago: PagoRegistrado) {
        porAnular = pago
    }

    fun cancelarAnular() {
        porAnular = null
    }

    fun anular() {
        val pago = porAnular ?: return
        porAnular = null
        trabajar("Intento de pago anulado. El saldo quedó libre para cobrar.") {
            val nueva = caja.anularIntento(pago)
            cuenta = nueva
            monto = nueva.saldoDisponible.paraEditar()
        }
    }

    fun cerrarCobro() {
        vigilancia?.cancel()
        cuenta = null
        qr = null
        cargar()
    }

    fun verFactura(facturaId: Int) = trabajar {
        factura = caja.verFactura(facturaId)
    }

    fun cerrarFactura() {
        factura = null
    }

    fun aceptar(orden: Orden) = trabajar("Orden #${orden.id} aceptada: pasó a cocina.") {
        caja.aceptarParaLlevar(orden)
        paraLlevar = caja.verParaLlevar()
    }

    fun rechazar(orden: Orden) = trabajar("Orden #${orden.id} rechazada.") {
        caja.rechazarParaLlevar(orden)
        paraLlevar = caja.verParaLlevar()
    }

    fun entregar(orden: Orden) = trabajar("Orden #${orden.id} entregada.") {
        caja.entregar(orden)
        paraLlevar = caja.verParaLlevar()
    }
}

@Composable
fun CajaPantalla() {
    val app = casos()
    val modelo: CajaModelo = viewModel { CajaModelo(app) }
    val enlaces = LocalUriHandler.current

    LaunchedEffect(modelo.navegador) {
        modelo.navegador?.let {
            enlaces.openUri(it)
            modelo.navegadorAbierto()
        }
    }

    val cuenta = modelo.cuenta
    val factura = modelo.factura

    Column(Modifier.fillMaxSize()) {
        if (cuenta == null && factura == null) {
            Pestanas(PestanaCaja.entries, modelo.pestana, { it.titulo }, modelo::elegir)
        }
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Mensajes(error = modelo.error, aviso = modelo.aviso) }
            when {
                factura != null -> detalleFactura(modelo, factura)
                cuenta != null -> panelCobro(modelo, cuenta)
                else -> listas(modelo)
            }
        }
    }

    modelo.qr?.let { codigo -> DialogoQr(modelo, codigo) }
    modelo.porAnular?.let { pago ->
        AlertDialog(
            onDismissRequest = modelo::cancelarAnular,
            title = { Text("¿Anular el intento de ${(pago.monto + pago.propina).pesos()}?") },
            text = { Text("Hazlo solo si el cliente no terminó el pago en línea. El saldo vuelve a quedar libre para cobrar.") },
            confirmButton = { TextButton(onClick = modelo::anular) { Text("Anular") } },
            dismissButton = { TextButton(onClick = modelo::cancelarAnular) { Text("No") } },
        )
    }
}

private fun LazyListScope.listas(modelo: CajaModelo) {
    item {
        OutlinedButton(onClick = modelo::cargar, enabled = !modelo.ocupado, modifier = Modifier.fillMaxWidth()) {
            Text(if (modelo.ocupado) "Cargando…" else "Actualizar")
        }
    }
    when (modelo.pestana) {
        PestanaCaja.COBRAR -> {
            if (modelo.porCobrar.isEmpty() && !modelo.ocupado && modelo.error == null) item { Vacio("No hay cuentas pedidas por los meseros.") }
            items(modelo.porCobrar, key = { it.id }) { orden ->
                TarjetaOrden(orden) {
                    Button(onClick = { modelo.abrirCobro(orden) }, enabled = !modelo.ocupado, modifier = Modifier.fillMaxWidth()) { Text("Cobrar") }
                }
            }
        }
        PestanaCaja.LLEVAR -> {
            if (modelo.paraLlevar.isEmpty() && !modelo.ocupado && modelo.error == null) item { Vacio("No hay pedidos para llevar activos.") }
            items(modelo.paraLlevar, key = { it.id }) { orden ->
                TarjetaOrden(orden) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (orden.estado == EstadoOrden.POR_CONFIRMAR) {
                            Button(onClick = { modelo.aceptar(orden) }, enabled = !modelo.ocupado) { Text("Aceptar") }
                            OutlinedButton(onClick = { modelo.rechazar(orden) }, enabled = !modelo.ocupado) { Text("Rechazar") }
                        }
                        if (!orden.pagada && orden.estado.activa) {
                            OutlinedButton(onClick = { modelo.abrirCobro(orden) }, enabled = !modelo.ocupado) { Text("Cobrar") }
                        }
                        if (orden.puedeEntregarse) {
                            Button(onClick = { modelo.entregar(orden) }, enabled = !modelo.ocupado) { Text("Entregar") }
                        }
                    }
                }
            }
        }
        PestanaCaja.FACTURAS -> {
            if (modelo.facturas.isEmpty() && !modelo.ocupado && modelo.error == null) item { Vacio("Todavía no hay facturas.") }
            items(modelo.facturas, key = { it.id }) { resumen ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Linea),
                    modifier = Modifier.fillMaxWidth().clickable { modelo.verFactura(resumen.id) },
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(resumen.codigo, fontWeight = FontWeight.Bold)
                            Text("${resumen.cliente} · Orden #${resumen.ordenId}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(resumen.fecha, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(resumen.total.pesos(), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        PestanaCaja.VENTAS -> {
            if (modelo.ventas.isEmpty() && !modelo.ocupado && modelo.error == null) item { Vacio("Todavía no hay ventas.") }
            items(modelo.ventas, key = { it.id }) { orden -> TarjetaOrden(orden) }
        }
    }
}

private fun LazyListScope.panelCobro(modelo: CajaModelo, cuenta: Cuenta) {
    item { TarjetaOrden(cuenta.orden) }
    item {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Linea),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Fila("Total", cuenta.orden.total.pesos())
                Fila("Pagado", (cuenta.orden.total - cuenta.saldoPendiente).pesos())
                Fila("Saldo por cobrar", cuenta.saldoPendiente.pesos(), fuerte = true)
                cuenta.pagos.forEach { pago ->
                    Text(
                        "· ${pago.metodo} ${pago.monto.pesos()}" + (if (pago.propina.positivo) " + propina ${pago.propina.pesos()}" else "") + " (${pago.estado})",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (pago.anulable) {
                        OutlinedButton(onClick = { modelo.pedirAnular(pago) }, enabled = !modelo.ocupado) {
                            Text("Anular intento")
                        }
                    }
                }
            }
        }
    }
    if (cuenta.saldada) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("La orden quedó pagada.", fontWeight = FontWeight.Bold)
                cuenta.facturaId?.let { id ->
                    Button(onClick = { modelo.verFactura(id) }, modifier = Modifier.fillMaxWidth()) { Text("Ver factura") }
                }
                OutlinedButton(onClick = modelo::cerrarCobro, modifier = Modifier.fillMaxWidth()) { Text("Volver a caja") }
            }
        }
        return
    }
    item {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Método de pago", fontWeight = FontWeight.Bold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                modelo.metodos.forEach { opcion ->
                    FilterChip(
                        selected = modelo.metodo == opcion,
                        onClick = { modelo.metodo = opcion },
                        label = { Text(opcion.nombre + if (opcion.usaPasarela) " (pasarela)" else "") },
                    )
                }
            }
            Campo("Monto a abonar", modelo.monto) { modelo.monto = it }
            Campo("Propina (opcional)", modelo.propina) { modelo.propina = it }
            if (modelo.metodo?.usaPasarela != true) {
                Campo("Efectivo recibido (para el cambio)", modelo.recibido) { modelo.recibido = it }
            }
            OutlinedTextField(
                value = modelo.referencia,
                onValueChange = { modelo.referencia = it },
                label = { Text("Referencia (opcional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(onClick = modelo::cobrar, enabled = !modelo.ocupado, modifier = Modifier.fillMaxWidth()) {
                Text(if (modelo.ocupado) "Procesando…" else if (modelo.metodo?.usaPasarela == true) "Ir a la pasarela" else "Registrar pago")
            }
            OutlinedButton(onClick = modelo::generarQr, enabled = !modelo.ocupado, modifier = Modifier.fillMaxWidth()) {
                Text("Mostrar QR para que el cliente pague")
            }
            TextButton(onClick = modelo::cerrarCobro, modifier = Modifier.fillMaxWidth()) { Text("Volver") }
        }
    }
}

private fun LazyListScope.detalleFactura(modelo: CajaModelo, factura: Factura) {
    item {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val tinta = Color(0xFF1A1A1A)
                Text(factura.emisor, color = tinta, fontWeight = FontWeight.Bold)
                Text("NIT ${factura.nit}", color = tinta)
                Text("Factura ${factura.resumen.codigo} · ${factura.resumen.fecha}", color = tinta)
                Text("Cliente: ${factura.resumen.cliente} · ${factura.documento}", color = tinta)
                HorizontalDivider()
                factura.lineas.forEach { linea ->
                    Row {
                        Text("${linea.cantidad} × ${linea.nombre}", color = tinta, modifier = Modifier.weight(1f))
                        Text(linea.subtotal.pesos(), color = tinta)
                    }
                }
                HorizontalDivider()
                FilaTinta("Base", factura.base.pesos())
                FilaTinta("Impuesto al consumo", factura.impuesto.pesos())
                FilaTinta("Consumo", factura.consumo.pesos())
                FilaTinta("Propina", factura.propina.pesos())
                FilaTinta("Total pagado", factura.resumen.total.pesos())
                Text("CUFE (simulado): ${factura.cufe.take(24)}…", color = Color(0xFF555555))
            }
        }
    }
    item { OutlinedButton(onClick = modelo::cerrarFactura, modifier = Modifier.fillMaxWidth()) { Text("Volver") } }
}

@Composable
private fun Fila(etiqueta: String, valor: String, fuerte: Boolean = false) {
    Row {
        Text(etiqueta, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor, fontWeight = if (fuerte) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun FilaTinta(etiqueta: String, valor: String) {
    Row {
        Text(etiqueta, modifier = Modifier.weight(1f), color = Color(0xFF333333))
        Text(valor, color = Color(0xFF1A1A1A), fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Campo(etiqueta: String, valor: String, alCambiar: (String) -> Unit) {
    OutlinedTextField(
        value = valor,
        onValueChange = alCambiar,
        label = { Text(etiqueta) },
        singleLine = true,
        prefix = { Text("$ ") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun DialogoQr(modelo: CajaModelo, codigo: CodigoQr) {
    val imagen = remember(codigo) { BitmapFactory.decodeByteArray(codigo.imagenPng, 0, codigo.imagenPng.size)?.asImageBitmap() }
    var verEnlace by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = modelo::cerrarQr,
        title = { Text("Escanea para pagar ${codigo.saldo.pesos()}") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                imagen?.let {
                    Image(
                        bitmap = it,
                        contentDescription = "Código QR de pago",
                        modifier = Modifier.size(240.dp).background(Color.White, RoundedCornerShape(12.dp)).padding(8.dp),
                    )
                }
                Text("La pantalla se actualiza sola cuando el pago se aprueba.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = { verEnlace = !verEnlace }) { Text(if (verEnlace) "Ocultar enlace" else "Ver enlace") }
                if (verEnlace) Text(codigo.url, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { TextButton(onClick = modelo::cerrarQr) { Text("Cerrar") } },
    )
}
