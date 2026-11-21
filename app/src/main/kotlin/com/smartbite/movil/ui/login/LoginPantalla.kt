package com.smartbite.movil.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartbite.application.puerto.entrada.SesionCasos
import com.smartbite.movil.ui.ModeloBase
import com.smartbite.movil.ui.casos
import com.smartbite.movil.ui.componentes.Mensajes

class LoginModelo(private val sesion: SesionCasos) : ModeloBase() {

    var correo by mutableStateOf("")
    var clave by mutableStateOf("")

    fun entrar(alEntrar: () -> Unit) = trabajar {
        sesion.iniciarSesion(correo, clave)
        clave = ""
        alEntrar()
    }
}

@Composable
fun LoginPantalla(vencida: Boolean, alEntrar: () -> Unit, alVolver: () -> Unit) {
    val app = casos()
    val modelo: LoginModelo = viewModel { LoginModelo(app) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .imePadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, alignment = androidx.compose.ui.Alignment.CenterVertically),
    ) {
        Text("Entrar como empleado", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        if (vencida) Mensajes(error = "Tu sesión terminó. Vuelve a entrar.", aviso = null)
        Mensajes(error = modelo.error, aviso = null)
        OutlinedTextField(
            value = modelo.correo,
            onValueChange = { modelo.correo = it },
            label = { Text("Correo") },
            singleLine = true,
            enabled = !modelo.ocupado,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = modelo.clave,
            onValueChange = { modelo.clave = it },
            label = { Text("Contraseña") },
            singleLine = true,
            enabled = !modelo.ocupado,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { modelo.entrar(alEntrar) }),
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = { modelo.entrar(alEntrar) },
            enabled = !modelo.ocupado,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            Text(if (modelo.ocupado) "Entrando…" else "Entrar")
        }
        TextButton(onClick = alVolver, modifier = Modifier.fillMaxWidth()) { Text("Volver") }
    }
}
