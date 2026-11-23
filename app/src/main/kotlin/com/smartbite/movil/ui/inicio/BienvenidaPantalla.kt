package com.smartbite.movil.ui.inicio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smartbite.movil.ui.componentes.Portada
import com.smartbite.movil.ui.componentes.TarjetaAccion

@Composable
fun BienvenidaPantalla(
    alPedirEnMesa: () -> Unit,
    alVerRestaurantes: () -> Unit,
    alVerMisPedidos: () -> Unit,
    alEntrar: () -> Unit,
    alRegistrarse: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding(),
    ) {
        Column(Modifier.statusBarsPadding()) {
            Portada(
                etiqueta = "Smart Bite",
                titulo = "Del QR de tu mesa a la cocina, sin esperar al mesero.",
                detalle = "Pide, sigue tu pedido y paga desde tu teléfono.",
            )
        }
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TarjetaAccion(Icons.Filled.Place, "Pedir en mi mesa", "Escanea el QR que está en la mesa", destacada = true, alTocar = alPedirEnMesa)
            TarjetaAccion(Icons.Filled.Search, "Restaurantes", "Menú con fotos, pedidos para llevar y reservas", alTocar = alVerRestaurantes)
            TarjetaAccion(Icons.Filled.DateRange, "Mis pedidos y reservas", "Sigue lo que pediste desde este teléfono", alTocar = alVerMisPedidos)
            Text(
                "¿Trabajas en un restaurante o ya tienes cuenta?",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 12.dp),
            )
            TarjetaAccion(Icons.Filled.Person, "Entrar", "Meseros, cocina, caja, inventario, administradores y clientes", alTocar = alEntrar)
            TarjetaAccion(Icons.Filled.AddCircle, "Crear cuenta", "Cliente, empleado con código o dueño de restaurante", alTocar = alRegistrarse)
        }
    }
}
