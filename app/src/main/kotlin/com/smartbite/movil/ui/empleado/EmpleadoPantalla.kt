package com.smartbite.movil.ui.empleado

import com.smartbite.movil.ui.componentes.TarjetaAccion
import com.smartbite.movil.ui.componentes.Portada
import com.smartbite.movil.ui.componentes.Encabezado
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Icon
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.statusBarsPadding
import com.smartbite.movil.ui.componentes.Linea
import androidx.compose.foundation.BorderStroke
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

enum class Modulo(val titulo: String, val icono: ImageVector, val permitido: (Rol) -> Boolean) {
    INICIO("Inicio", Icons.Filled.Home, { it.administra }),
    MESAS("Mesas", Icons.Filled.Place, { it.atiendeMesas }),
    COCINA("Cocina", Icons.Filled.Star, { it.cocina }),
    CAJA("Caja", Icons.Filled.ShoppingCart, { it.cobra }),
    INVENTARIO("Inventario", Icons.AutoMirrored.Filled.List, { it.leeInventario }),
    CATALOGO("Catálogo", Icons.Filled.Menu, { it.leeCatalogo }),
    ADMINISTRACION("Admin", Icons.Filled.Settings, { it.administra }),
}

private val ICONOS = mapOf(
    "productos" to Icons.Filled.Menu,
    "categorias" to Icons.AutoMirrored.Filled.List,
    "recetas" to Icons.Filled.Build,
    "items" to Icons.AutoMirrored.Filled.List,
    "movimientos" to Icons.Filled.Refresh,
    "proveedores" to Icons.Filled.Call,
    "mesas" to Icons.Filled.Place,
    "usuarios" to Icons.Filled.Person,
    "sucursales" to Icons.Filled.LocationOn,
    "restaurantes" to Icons.Filled.Home,
    "opiniones" to Icons.Filled.ThumbUp,
)

private val DETALLES = mapOf(
    "productos" to "Platillos, precios, fotos y disponibilidad",
    "categorias" to "Cómo se agrupa el menú",
    "recetas" to "Ingredientes del inventario por platillo",
    "items" to "Stock, mínimos y ajustes",
    "movimientos" to "Entradas, salidas y pérdidas",
    "proveedores" to "A quién le compras",
    "mesas" to "Capacidad, sucursal y su código QR",
    "usuarios" to "Empleados, roles y acceso",
    "sucursales" to "Direcciones y teléfonos",
    "restaurantes" to "Datos y código para tus empleados",
    "opiniones" to "Lo que dicen tus clientes",
)

private sealed interface Seccion {
    val titulo: String
    val icono: ImageVector
    val detalle: String
    data class DeRecurso(val clave: String, override val titulo: String) : Seccion {
        override val icono: ImageVector get() = ICONOS[clave] ?: Icons.Filled.Info
        override val detalle: String get() = DETALLES[clave].orEmpty()
    }
    data object Compras : Seccion {
        override val titulo = "Compras"
        override val icono: ImageVector get() = Icons.Filled.ShoppingCart
        override val detalle = "Pedidos a proveedores: recibir sube el stock"
    }
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
                            icon = { Icon(modulo.icono, contentDescription = null) },
                            label = { Text(modulo.titulo, maxLines = 1) },
                        )
                    }
                    if (modulos.size > 5) {
                        NavigationBarItem(
                            selected = mas || (visible != null && visible !in enBarra),
                            onClick = { mas = true; seccion = null },
                            icon = { Icon(Icons.Filled.MoreVert, contentDescription = null) },
                            label = { Text("Más") },
                        )
                    }
                }
            }
        },
    ) { margen ->
        Box(Modifier.padding(margen).fillMaxSize()) {
            when {
                mas -> ListaOpciones("Más módulos", modulos.drop(4).map { Triple(it.icono, it.titulo, "") }) { indice ->
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
                    ListaOpciones(modulo.titulo, secciones.map { Triple(it.icono, it.titulo, it.detalle) }) { alElegir(secciones[it]) }
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
private fun ListaOpciones(titulo: String, opciones: List<Triple<ImageVector, String, String>>, alElegir: (Int) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Encabezado(titulo) }
        items(opciones.indices.toList()) { indice ->
            val (icono, nombre, detalle) = opciones[indice]
            TarjetaAccion(icono, nombre, detalle.ifBlank { null }) { alElegir(indice) }
        }
    }
}

@Composable
private fun InicioCliente(sesion: SesionVista, alAbrir: (String) -> Unit, alSalir: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Column(Modifier.statusBarsPadding()) {
            Portada(
                etiqueta = "Smart Bite",
                titulo = "Hola, ${sesion.nombre}",
                detalle = if (sesion.rol == Rol.DOMICILIARIO) "Las entregas a domicilio se manejan desde la web por ahora." else "¿Qué se te antoja hoy?",
            )
        }
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TarjetaAccion(Icons.Filled.Place, "Pedir en mi mesa", "Escanea el QR de la mesa", destacada = true) { alAbrir(Rutas.CLIENTE) }
            TarjetaAccion(Icons.Filled.Search, "Restaurantes", "Para llevar, reservas y opiniones") { alAbrir(Rutas.RESTAURANTES) }
            TarjetaAccion(Icons.Filled.DateRange, "Mis pedidos y reservas", "Estado de lo que pediste") { alAbrir(Rutas.MIS_PEDIDOS) }
            TextButton(onClick = alSalir, modifier = Modifier.fillMaxWidth()) { Text("Cerrar sesión") }
        }
    }
}
