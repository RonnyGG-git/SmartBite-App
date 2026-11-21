package com.smartbite.movil.ui.navegacion

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.smartbite.movil.ui.casos
import com.smartbite.movil.ui.cliente.ClientePantalla
import com.smartbite.movil.ui.empleado.EmpleadoPantalla
import com.smartbite.movil.ui.inicio.BienvenidaPantalla
import com.smartbite.movil.ui.login.LoginPantalla
import kotlinx.coroutines.flow.SharedFlow

object Rutas {
    const val BIENVENIDA = "bienvenida"
    const val CLIENTE = "cliente"
    const val LOGIN = "login?vencida={vencida}"
    const val EMPLEADO = "empleado"

    fun login(vencida: Boolean) = "login?vencida=$vencida"
}

private fun NavHostController.irA(ruta: String) {
    navigate(ruta) {
        popUpTo(0) { inclusive = true }
        launchSingleTop = true
    }
}

@Composable
fun Navegacion(sesionVencida: SharedFlow<Unit>) {
    val app = casos()
    var inicio by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        inicio = if (app.sesionActual() != null) Rutas.EMPLEADO else Rutas.BIENVENIDA
    }

    val destinoInicial = inicio
    if (destinoInicial == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    val nav = rememberNavController()

    LaunchedEffect(nav) {
        sesionVencida.collect { nav.irA(Rutas.login(vencida = true)) }
    }

    NavHost(navController = nav, startDestination = destinoInicial) {
        composable(Rutas.BIENVENIDA) {
            BienvenidaPantalla(
                alPedir = { nav.navigate(Rutas.CLIENTE) },
                alEntrarEmpleado = { nav.navigate(Rutas.login(vencida = false)) },
            )
        }
        composable(Rutas.CLIENTE) {
            ClientePantalla(alVolver = { nav.popBackStack() })
        }
        composable(
            route = Rutas.LOGIN,
            arguments = listOf(navArgument("vencida") { type = NavType.BoolType; defaultValue = false }),
        ) { entrada ->
            LoginPantalla(
                vencida = entrada.arguments?.getBoolean("vencida") == true,
                alEntrar = { nav.irA(Rutas.EMPLEADO) },
                alVolver = { nav.irA(Rutas.BIENVENIDA) },
            )
        }
        composable(Rutas.EMPLEADO) {
            EmpleadoPantalla(alSalir = { nav.irA(Rutas.BIENVENIDA) })
        }
    }
}
