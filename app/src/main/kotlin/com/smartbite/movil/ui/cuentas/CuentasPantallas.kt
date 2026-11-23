package com.smartbite.movil.ui.cuentas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartbite.application.puerto.entrada.CuentasCasos
import com.smartbite.domain.modelo.TipoCuenta
import com.smartbite.movil.ui.ModeloBase
import com.smartbite.movil.ui.casos
import com.smartbite.movil.ui.componentes.CampoTexto
import com.smartbite.movil.ui.componentes.Mensajes

class RegistroModelo(private val cuentas: CuentasCasos, correoPendiente: String) : ModeloBase() {
    var tipo by mutableStateOf(TipoCuenta.CLIENTE)
    var nombre by mutableStateOf("")
    var correo by mutableStateOf(correoPendiente)
    var clave by mutableStateOf("")
    var repetida by mutableStateOf("")
    var codigoEmpresa by mutableStateOf("")
    var telefono by mutableStateOf("")
    var codigo by mutableStateOf("")
    var verificando by mutableStateOf(correoPendiente.isNotBlank())
        private set

    fun registrar() = trabajar {
        aviso = cuentas.registrar(tipo, nombre, correo, clave, repetida, codigoEmpresa, telefono)
        clave = ""
        repetida = ""
        verificando = true
    }

    fun verificar(alEntrar: () -> Unit) = trabajar {
        cuentas.verificarCorreo(correo, codigo)
        alEntrar()
    }

    fun reenviar() = trabajar {
        aviso = cuentas.reenviarCodigo(correo)
    }

    fun corregir() {
        verificando = false
        limpiarMensajes()
    }
}

@Composable
fun RegistroPantalla(correoPendiente: String, alEntrar: () -> Unit, alVolver: () -> Unit) {
    val app = casos()
    val modelo: RegistroModelo = viewModel { RegistroModelo(app, correoPendiente) }

    Column(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(if (modelo.verificando) "Confirma tu correo" else "Crear cuenta", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Mensajes(error = modelo.error, aviso = modelo.aviso)
        if (modelo.verificando) {
            Text("Escribe el código de 4 dígitos que llegó a ${modelo.correo}.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (modelo.correo.isBlank()) CampoTexto("Correo", modelo.correo, modelo.campos["email"], KeyboardType.Email) { modelo.correo = it }
            CampoTexto("Código", modelo.codigo, modelo.campos["codigo"], KeyboardType.Number) { modelo.codigo = it.take(4) }
            Button(onClick = { modelo.verificar(alEntrar) }, enabled = !modelo.ocupado, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text(if (modelo.ocupado) "Verificando…" else "Confirmar y entrar")
            }
            OutlinedButton(onClick = modelo::reenviar, enabled = !modelo.ocupado, modifier = Modifier.fillMaxWidth()) { Text("Enviar otro código") }
            TextButton(onClick = modelo::corregir, modifier = Modifier.fillMaxWidth()) { Text("Corregir mis datos") }
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TipoCuenta.entries.forEach { opcion ->
                    FilterChip(selected = modelo.tipo == opcion, onClick = { modelo.tipo = opcion }, label = { Text(opcion.etiqueta) })
                }
            }
            CampoTexto("Nombre completo", modelo.nombre, modelo.campos["nombre"]) { modelo.nombre = it }
            CampoTexto("Correo", modelo.correo, modelo.campos["email"], KeyboardType.Email) { modelo.correo = it }
            if (modelo.tipo == TipoCuenta.CLIENTE) {
                CampoTexto("Teléfono (opcional)", modelo.telefono, modelo.campos["telefono"], KeyboardType.Phone) { modelo.telefono = it }
            }
            if (modelo.tipo == TipoCuenta.EMPLEADO) {
                CampoTexto("Código de acceso del restaurante", modelo.codigoEmpresa, modelo.campos["codigo"]) { modelo.codigoEmpresa = it }
            }
            CampoTexto("Contraseña", modelo.clave, modelo.campos["password1"], KeyboardType.Password, oculto = true) { modelo.clave = it }
            CampoTexto("Repite la contraseña", modelo.repetida, modelo.campos["password2"], KeyboardType.Password, oculto = true) { modelo.repetida = it }
            Button(onClick = modelo::registrar, enabled = !modelo.ocupado, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text(if (modelo.ocupado) "Creando…" else "Crear cuenta")
            }
        }
        TextButton(onClick = alVolver, modifier = Modifier.fillMaxWidth()) { Text("Volver") }
    }
}

class RecuperarModelo(private val cuentas: CuentasCasos) : ModeloBase() {
    var correo by mutableStateOf("")
    var codigo by mutableStateOf("")
    var clave by mutableStateOf("")
    var repetida by mutableStateOf("")
    var enviado by mutableStateOf(false)
        private set

    fun pedirCodigo() = trabajar {
        aviso = cuentas.recuperarClave(correo)
        enviado = true
    }

    fun cambiar(alTerminar: () -> Unit) = trabajar {
        cuentas.restablecerClave(correo, codigo, clave, repetida)
        alTerminar()
    }
}

@Composable
fun RecuperarPantalla(alTerminar: () -> Unit, alVolver: () -> Unit) {
    val app = casos()
    val modelo: RecuperarModelo = viewModel { RecuperarModelo(app) }

    Column(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Recuperar contraseña", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Mensajes(error = modelo.error, aviso = modelo.aviso)
        CampoTexto("Correo", modelo.correo, modelo.campos["email"], KeyboardType.Email, habilitado = !modelo.enviado) { modelo.correo = it }
        if (!modelo.enviado) {
            Button(onClick = modelo::pedirCodigo, enabled = !modelo.ocupado, modifier = Modifier.fillMaxWidth()) { Text("Enviarme un código") }
        } else {
            CampoTexto("Código de 4 dígitos", modelo.codigo, modelo.campos["codigo"], KeyboardType.Number) { modelo.codigo = it.take(4) }
            CampoTexto("Nueva contraseña", modelo.clave, modelo.campos["password1"], KeyboardType.Password, oculto = true) { modelo.clave = it }
            CampoTexto("Repite la contraseña", modelo.repetida, modelo.campos["password2"], KeyboardType.Password, oculto = true) { modelo.repetida = it }
            Button(onClick = { modelo.cambiar(alTerminar) }, enabled = !modelo.ocupado, modifier = Modifier.fillMaxWidth()) { Text("Cambiar contraseña") }
            TextButton(onClick = modelo::pedirCodigo, enabled = !modelo.ocupado, modifier = Modifier.fillMaxWidth()) { Text("Enviar otro código") }
        }
        TextButton(onClick = alVolver, modifier = Modifier.fillMaxWidth()) { Text("Volver") }
    }
}
