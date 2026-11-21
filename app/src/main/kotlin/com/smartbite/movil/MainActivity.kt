package com.smartbite.movil

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import com.smartbite.movil.ui.LocalCasos
import com.smartbite.movil.ui.navegacion.Navegacion
import com.smartbite.movil.ui.tema.TemaSmartBite

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val contenedor = (application as SmartBiteMovil).contenedor
        setContent {
            TemaSmartBite {
                CompositionLocalProvider(LocalCasos provides contenedor.casos) {
                    Navegacion(sesionVencida = contenedor.sesionVencida)
                }
            }
        }
    }
}
