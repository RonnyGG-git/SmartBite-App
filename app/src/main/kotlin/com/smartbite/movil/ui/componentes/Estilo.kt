package com.smartbite.movil.ui.componentes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smartbite.movil.R
import com.smartbite.movil.ui.tema.Brasa
import com.smartbite.movil.ui.tema.Carbon
import com.smartbite.movil.ui.tema.Crema
import com.smartbite.movil.ui.tema.Gris

val Linea = Color(0xFF2E2823)
val BrasaSuave = Color(0x26E8843F)

fun Modifier.degradadoMarca(): Modifier = background(
    Brush.verticalGradient(listOf(Color(0xFF3A2112), Color(0xFF1E140D), Carbon)),
)

@Composable
fun Logo(lado: Int = 56) {
    Box(
        modifier = Modifier.size(lado.dp).clip(RoundedCornerShape(18.dp)).background(Carbon),
        contentAlignment = Alignment.Center,
    ) {
        Image(painter = painterResource(R.drawable.ic_launcher_foreground), contentDescription = "Smart Bite", modifier = Modifier.size((lado * 1.6).dp))
    }
}

@Composable
fun Portada(etiqueta: String, titulo: String, detalle: String?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .degradadoMarca()
            .padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Logo(48)
            Spacer(Modifier.width(12.dp))
            Text(etiqueta.uppercase(), color = Brasa, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
        }
        Text(titulo, color = Crema, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        detalle?.let { Text(it, color = Gris, style = MaterialTheme.typography.bodyLarge) }
    }
}

@Composable
fun Encabezado(titulo: String, detalle: String? = null) {
    Column(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
        Text(titulo, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Box(Modifier.padding(top = 6.dp).width(36.dp).height(3.dp).clip(RoundedCornerShape(2.dp)).background(Brasa))
        detalle?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp)) }
    }
}

@Composable
fun Insignia(icono: ImageVector, tamano: Int = 44) {
    Box(
        modifier = Modifier.size(tamano.dp).clip(RoundedCornerShape(14.dp)).background(BrasaSuave),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icono, contentDescription = null, tint = Brasa, modifier = Modifier.size((tamano / 2).dp))
    }
}

@Composable
fun TarjetaAccion(icono: ImageVector, titulo: String, detalle: String? = null, destacada: Boolean = false, alTocar: () -> Unit) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = if (destacada) Brasa else MaterialTheme.colorScheme.surface),
        border = if (destacada) null else BorderStroke(1.dp, Linea),
        modifier = Modifier.fillMaxWidth().clickable(onClick = alTocar),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (destacada) {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Color(0x33000000)), contentAlignment = Alignment.Center) {
                    Icon(icono, contentDescription = null, tint = Color(0xFF1A0E05), modifier = Modifier.size(22.dp))
                }
            } else {
                Insignia(icono)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(titulo, fontWeight = FontWeight.Bold, color = if (destacada) Color(0xFF1A0E05) else MaterialTheme.colorScheme.onSurface)
                detalle?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = if (destacada) Color(0xCC1A0E05) else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = if (destacada) Color(0xFF1A0E05) else Gris,
            )
        }
    }
}

@Composable
fun EstadoVacio(icono: ImageVector, texto: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(72.dp).clip(CircleShape).background(BrasaSuave), contentAlignment = Alignment.Center) {
            Icon(icono, contentDescription = null, tint = Brasa, modifier = Modifier.size(34.dp))
        }
        Text(texto, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
    }
}
