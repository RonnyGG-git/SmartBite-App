package com.smartbite.movil.ui.empleado

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.smartbite.application.dto.SesionVista
import com.smartbite.domain.modelo.Rol
import com.smartbite.movil.ui.caja.CajaPantalla
import com.smartbite.movil.ui.casos
import com.smartbite.movil.ui.cocina.CocinaPantalla
import com.smartbite.movil.ui.mesero.MeseroPantalla
import kotlinx.coroutines.launch

enum class Modulo(val titulo: String, val permitido: (Rol) -> Boolean) {
    MESAS("Mesas", { it.atiendeMesas }),
    COCINA("Cocina", { it.cocina }),
    CAJA("Caja", { it.cobra }),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmpleadoPantalla(alSalir: () -> Unit) {
    val app = casos()
    val alcance = rememberCoroutineScope()
    var sesion by remember { mutableStateOf<SesionVista?>(null) }
    var elegido by remember { mutableStateOf<Modulo?>(null) }

    LaunchedEffect(Unit) {
        val actual = app.sesionActual()
        if (actual == null) alSalir() else sesion = actual
    }

    val actual = sesion ?: return
    val modulos = Modulo.entries.filter { it.permitido(actual.rol) }
    val visible = elegido ?: modulos.firstOrNull()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${actual.nombre} · ${actual.rol.etiqueta}") },
                actions = {
                    TextButton(onClick = { alcance.launch { app.cerrarSesion(); alSalir() } }) { Text("Salir") }
                },
            )
        },
        bottomBar = {
            if (modulos.size > 1) {
                NavigationBar {
                    modulos.forEach { modulo ->
                        NavigationBarItem(
                            selected = modulo == visible,
                            onClick = { elegido = modulo },
                            icon = { Text(modulo.titulo.take(1)) },
                            label = { Text(modulo.titulo) },
                        )
                    }
                }
            }
        },
    ) { margen ->
        Box(Modifier.padding(margen).fillMaxSize()) {
            when (visible) {
                Modulo.MESAS -> MeseroPantalla()
                Modulo.COCINA -> CocinaPantalla()
                Modulo.CAJA -> CajaPantalla()
                null -> Text(
                    "Tu rol (${actual.rol.etiqueta}) trabaja desde la web. Esta app es para salón, cocina y caja.",
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                )
            }
        }
    }
}
