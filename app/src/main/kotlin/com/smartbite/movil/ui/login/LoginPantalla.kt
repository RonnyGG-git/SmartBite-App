package com.smartbite.movil.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.smartbite.application.puerto.entrada.SesionCasos
import com.smartbite.domain.error.FalloRemoto
import com.smartbite.movil.ui.ModeloBase
import com.smartbite.movil.ui.casos
import com.smartbite.movil.ui.componentes.CampoTexto
import com.smartbite.movil.ui.componentes.Mensajes

class LoginModelo(private val sesion: SesionCasos) : ModeloBase() {

    var correo by mutableStateOf("")
    var clave by mutableStateOf("")
    var sinVerificar by mutableStateOf(false)
        private set

    fun entrar(alEntrar: () -> Unit) = trabajar {
        sinVerificar = false
        try {
            sesion.iniciarSesion(correo, clave)
        } catch (pendiente: FalloRemoto.CorreoSinVerificar) {
            sinVerificar = true
            throw pendiente
        }
        clave = ""
        alEntrar()
    }
}

@Composable
fun LoginPantalla(
    vencida: Boolean,
    alEntrar: () -> Unit,
    alVolver: () -> Unit,
    alRegistrarse: (String) -> Unit,
    alRecuperar: () -> Unit,
) {
    val app = casos()
    val modelo: LoginModelo = viewModel { LoginModelo(app) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, alignment = Alignment.CenterVertically),
    ) {
        Text("Entrar a Smart Bite", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        if (vencida) Mensajes(error = "Tu sesión terminó. Vuelve a entrar.", aviso = null)
        Mensajes(error = modelo.error, aviso = null)
        if (modelo.sinVerificar) {
            Button(onClick = { alRegistrarse(modelo.correo) }, modifier = Modifier.fillMaxWidth()) {
                Text("Escribir el código que llegó a mi correo")
            }
        }
        CampoTexto("Correo", modelo.correo, null, KeyboardType.Email, habilitado = !modelo.ocupado) { modelo.correo = it }
        CampoTexto("Contraseña", modelo.clave, null, KeyboardType.Password, oculto = true, habilitado = !modelo.ocupado) { modelo.clave = it }
        Button(
            onClick = { modelo.entrar(alEntrar) },
            enabled = !modelo.ocupado,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            Text(if (modelo.ocupado) "Entrando…" else "Entrar")
        }
        TextButton(onClick = alRecuperar, modifier = Modifier.fillMaxWidth()) { Text("Olvidé mi contraseña") }
        TextButton(onClick = { alRegistrarse("") }, modifier = Modifier.fillMaxWidth()) { Text("Crear una cuenta") }
        TextButton(onClick = alVolver, modifier = Modifier.fillMaxWidth()) { Text("Volver") }
    }
}
