package com.smartbite.movil.ui.tema

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.smartbite.domain.modelo.EstadoOrden

val Carbon = Color(0xFF0E0C0A)
val Superficie = Color(0xFF1C1916)
val Crema = Color(0xFFF3EDE6)
val Gris = Color(0xFFA8A096)
val Brasa = Color(0xFFE8843F)

private val colores = darkColorScheme(
    primary = Brasa,
    onPrimary = Color(0xFF1A0E05),
    background = Carbon,
    onBackground = Crema,
    surface = Superficie,
    onSurface = Crema,
    surfaceVariant = Color(0xFF2A2521),
    onSurfaceVariant = Gris,
    error = Color(0xFFFF8A80),
)

@Composable
fun TemaSmartBite(contenido: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colores, content = contenido)
}

fun colorDe(estado: EstadoOrden): Color = when (estado) {
    EstadoOrden.POR_CONFIRMAR -> Color(0xFFFFC857)
    EstadoOrden.PENDIENTE -> Color(0xFF9AB8FF)
    EstadoOrden.EN_PREPARACION -> Brasa
    EstadoOrden.LISTA -> Color(0xFF7BD88F)
    EstadoOrden.ENTREGADA -> Gris
    EstadoOrden.CANCELADA -> Color(0xFFFF8A80)
}
