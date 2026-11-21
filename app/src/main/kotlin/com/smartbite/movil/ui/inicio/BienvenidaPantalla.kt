package com.smartbite.movil.ui.inicio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smartbite.movil.ui.tema.Brasa

@Composable
fun BienvenidaPantalla(alPedir: () -> Unit, alEntrarEmpleado: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("SMART BITE", color = Brasa, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
        Text(
            "Del QR de tu mesa a la cocina, sin esperar al mesero.",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(32.dp))
        Button(onClick = alPedir, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text("Pedir en mi mesa")
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = alEntrarEmpleado, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text("Soy empleado")
        }
    }
}
