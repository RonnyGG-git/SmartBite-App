package com.smartbite.movil.ui.inicio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smartbite.movil.ui.tema.Brasa

@Composable
fun BienvenidaPantalla(
    alPedirEnMesa: () -> Unit,
    alVerRestaurantes: () -> Unit,
    alVerMisPedidos: () -> Unit,
    alEntrar: () -> Unit,
    alRegistrarse: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("SMART BITE", color = Brasa, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
        Text(
            "Del QR de tu mesa a la cocina, sin esperar al mesero.",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(28.dp))
        Button(onClick = alPedirEnMesa, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Pedir en mi mesa") }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = alVerRestaurantes, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text("Restaurantes: para llevar y reservas")
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = alVerMisPedidos, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Mis pedidos y reservas") }
        Spacer(Modifier.height(28.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = alEntrar, modifier = Modifier.weight(1f)) { Text("Entrar") }
            OutlinedButton(onClick = alRegistrarse, modifier = Modifier.weight(1f)) { Text("Crear cuenta") }
        }
        TextButton(onClick = alEntrar, modifier = Modifier.fillMaxWidth()) {
            Text("Empleados y clientes registrados entran con su correo", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
