package com.smartbite.movil.ui.empleado

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smartbite.application.dto.SesionVista
import com.smartbite.domain.modelo.CatalogoGestion
import com.smartbite.domain.modelo.RestauranteRef
import com.smartbite.domain.modelo.Rol
import com.smartbite.movil.ui.caja.CajaPantalla
import com.smartbite.movil.ui.casos
import com.smartbite.movil.ui.cocina.CocinaPantalla
import com.smartbite.movil.ui.gestion.ComprasPantalla
import com.smartbite.movil.ui.gestion.GestionPantalla
import com.smartbite.movil.ui.gestion.TableroPantalla
import com.smartbite.movil.ui.mensajeDe
import com.smartbite.movil.ui.mesero.MeseroPantalla
import com.smartbite.movil.ui.navegacion.Rutas
import kotlinx.coroutines.launch

enum class Modulo(val titulo: String, val permitido: (Rol) -> Boolean) {
    INICIO("Inicio", { it.administra }),
    MESAS("Mesas", { it.atiendeMesas }),
    COCINA("Cocina", { it.cocina }),
    CAJA("Caja", { it.cobra }),
    INVENTARIO("Inventario", { it.leeInventario }),
    CATALOGO("Catálogo", { it.leeCatalogo }),
    ADMINISTRACION("Admin", { it.administra }),
}

private sealed interface Seccion {
    val titulo: String
    data class DeRecurso(val clave: String, override val titulo: String) : Seccion
    data object Compras : Seccion { override val titulo = "Compras" }
}

private fun seccionesDe(modulo: Modulo, rol: Rol): List<Seccion> {
    val recursos = when (modulo) {
        Modulo.INVENTARIO -> listOf(CatalogoGestion.items, CatalogoGestion.movimientos, CatalogoGestion.proveedores, CatalogoGestion.recetas)
        Modulo.CATALOGO -> listOf(CatalogoGestion.productos, CatalogoGestion.categorias, CatalogoGestion.recetas)
        Modulo.ADMINISTRACION -> listOf(
            CatalogoGestion.usuarios, CatalogoGestion.mesas, CatalogoGestion.sucursales, CatalogoGestion.restaurantes, CatalogoGestion.opiniones,
        )
        else -> emptyList()
    }
    val lista: List<Seccion> = recursos.filter { it.lee(rol) }.map { Seccion.DeRecurso(it.clave, it.titulo) }
    return if (modulo == Modulo.INVENTARIO && rol.gestionaInventario) lista + Seccion.Compras else lista
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmpleadoPantalla(alSalir: () -> Unit, alReiniciar: () -> Unit, alAbrir: (String) -> Unit) {
    val app = casos()
    val alcance = rememberCoroutineScope()
    var sesion by remember { mutableStateOf<SesionVista?>(null) }
    var elegido by remember { mutableStateOf<Modulo?>(null) }
    var mas by remember { mutableStateOf(false) }
    var seccion by remember { mutableStateOf<Seccion?>(null) }
    var cambiando by remember { mutableStateOf(false) }
    var aviso by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val actual = app.sesionActual()
        if (actual == null) alSalir() else sesion = actual
    }

    val actual = sesion ?: return

    fun salir() {
        alcance.launch {
            app.cerrarSesion()
            alSalir()
        }
    }

    fun trabajarCon(restaurante: RestauranteRef) {
        alcance.launch {
            try {
                app.trabajarCon(restaurante)
                alReiniciar()
            } catch (fallo: Exception) {
                aviso = mensajeDe(fallo)
            }
        }
    }

    if (!actual.rol.esEmpleado) {
        InicioCliente(actual, alAbrir, ::salir)
        return
    }

    val modulos = Modulo.entries.filter { it.permitido(actual.rol) }
    val enBarra = if (modulos.size > 5) modulos.take(4) else modulos
    val visible = elegido ?: modulos.firstOrNull()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("${actual.nombre} · ${actual.rol.etiqueta}", maxLines = 1)
                        actual.restaurante?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary) }
                    }
                },
                actions = {
                    if (actual.restaurantes.size > 1) TextButton(onClick = { cambiando = true }) { Text("Cambiar") }
                    TextButton(onClick = ::salir) { Text("Salir") }
                },
            )
        },
        bottomBar = {
            if (modulos.size > 1) {
                NavigationBar {
                    enBarra.forEach { modulo ->
                        NavigationBarItem(
                            selected = !mas && modulo == visible,
                            onClick = { elegido = modulo; mas = false; seccion = null },
                            icon = { Text(modulo.titulo.take(1), fontWeight = FontWeight.Bold) },
                            label = { Text(modulo.titulo, maxLines = 1) },
                        )
                    }
                    if (modulos.size > 5) {
                        NavigationBarItem(
                            selected = mas || (visible != null && visible !in enBarra),
                            onClick = { mas = true; seccion = null },
                            icon = { Text("+", fontWeight = FontWeight.Bold) },
                            label = { Text("Más") },
                        )
                    }
                }
            }
        },
    ) { margen ->
        Box(Modifier.padding(margen).fillMaxSize()) {
            when {
                mas -> ListaOpciones(modulos.drop(4).map { it.titulo }) { indice ->
                    elegido = modulos.drop(4)[indice]
                    mas = false
                    seccion = null
                }
                visible == null -> Text(
                    "Tu rol (${actual.rol.etiqueta}) no tiene módulos en la app.",
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                )
                else -> ContenidoModulo(visible, actual.rol, seccion, { seccion = it }) { ficha ->
                    trabajarCon(RestauranteRef(ficha.id, ficha.texto("nombre")))
                }
            }
        }
    }

    if (cambiando) {
        AlertDialog(
            onDismissRequest = { cambiando = false },
            title = { Text("¿Con qué restaurante trabajas?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    actual.restaurantes.forEach { restaurante ->
                        OutlinedButton(onClick = { cambiando = false; trabajarCon(restaurante) }, modifier = Modifier.fillMaxWidth()) {
                            Text(restaurante.nombre + if (restaurante.id == actual.restauranteId) " (actual)" else "")
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { cambiando = false }) { Text("Cerrar") } },
        )
    }
    aviso?.let { texto ->
        AlertDialog(onDismissRequest = { aviso = null }, text = { Text(texto) }, confirmButton = { TextButton(onClick = { aviso = null }) { Text("Entendido") } })
    }
}

@Composable
private fun ContenidoModulo(
    modulo: Modulo,
    rol: Rol,
    seccion: Seccion?,
    alElegir: (Seccion?) -> Unit,
    alElegirRestaurante: (com.smartbite.domain.modelo.Ficha) -> Unit,
) {
    when (modulo) {
        Modulo.INICIO -> TableroPantalla()
        Modulo.MESAS -> MeseroPantalla()
        Modulo.COCINA -> CocinaPantalla()
        Modulo.CAJA -> CajaPantalla()
        else -> {
            val secciones = seccionesDe(modulo, rol)
            Column(Modifier.fillMaxSize()) {
                if (seccion == null) {
                    ListaOpciones(secciones.map { it.titulo }) { alElegir(secciones[it]) }
                } else {
                    TextButton(onClick = { alElegir(null) }, modifier = Modifier.padding(horizontal = 8.dp)) { Text("‹ ${modulo.titulo}") }
                    when (seccion) {
                        Seccion.Compras -> ComprasPantalla()
                        is Seccion.DeRecurso -> GestionPantalla(CatalogoGestion.porClave(seccion.clave), rol, alElegirRestaurante)
                    }
                }
            }
        }
    }
}

@Composable
private fun ListaOpciones(titulos: List<String>, alElegir: (Int) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(titulos.indices.toList()) { indice ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().clickable { alElegir(indice) },
            ) {
                Text(titulos[indice], fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(20.dp))
            }
        }
    }
}

@Composable
private fun InicioCliente(sesion: SesionVista, alAbrir: (String) -> Unit, alSalir: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, alignment = Alignment.CenterVertically),
    ) {
        Text("Hola, ${sesion.nombre}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        if (sesion.rol == Rol.DOMICILIARIO) {
            Text("Las entregas a domicilio se manejan desde la web por ahora.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Button(onClick = { alAbrir(Rutas.CLIENTE) }, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Pedir en mi mesa") }
        OutlinedButton(onClick = { alAbrir(Rutas.RESTAURANTES) }, modifier = Modifier.fillMaxWidth()) { Text("Restaurantes: para llevar y reservas") }
        OutlinedButton(onClick = { alAbrir(Rutas.MIS_PEDIDOS) }, modifier = Modifier.fillMaxWidth()) { Text("Mis pedidos y reservas") }
        TextButton(onClick = alSalir, modifier = Modifier.fillMaxWidth()) { Text("Cerrar sesión") }
    }
}
