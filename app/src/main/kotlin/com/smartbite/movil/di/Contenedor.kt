package com.smartbite.movil.di

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.smartbite.adapter.almacen.HistorialDataStore
import com.smartbite.adapter.almacen.SesionDataStore
import com.smartbite.adapter.http.CabeceraRestaurante
import com.smartbite.adapter.http.CuentaHttp
import com.smartbite.adapter.http.FabricaHttp
import com.smartbite.adapter.http.GestionHttp
import com.smartbite.adapter.http.PublicoHttp
import com.smartbite.adapter.http.SmartBiteHttp
import com.smartbite.application.puerto.entrada.CasosDeUso
import com.smartbite.application.servicio.CuentasServicio
import com.smartbite.application.servicio.Fachada
import com.smartbite.application.servicio.GestionServicio
import com.smartbite.application.servicio.PublicoServicio
import com.smartbite.application.servicio.SmartBiteServicio
import com.smartbite.movil.BuildConfig
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

private val Context.almacenSesion by preferencesDataStore(name = "sesion")
private val Context.almacenHistorial by preferencesDataStore(name = "historial")

class Contenedor(contexto: Context) {

    private val vencida = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sesionVencida: SharedFlow<Unit> = vencida

    private val sesiones = SesionDataStore(contexto.applicationContext.almacenSesion)
    private val historial = HistorialDataStore(contexto.applicationContext.almacenHistorial)

    private val cabecera = CabeceraRestaurante()
    private val llamar = FabricaHttp.llamadas(sesiones, cabecera) { vencida.tryEmit(Unit) }
    private val http = SmartBiteHttp(FabricaHttp.api(BuildConfig.API_BASE_URL, cabecera), llamar)
    private val extra = FabricaHttp.extra(BuildConfig.API_BASE_URL, cabecera)

    val casos: CasosDeUso = Fachada(
        operacion = SmartBiteServicio(auth = http, cliente = http, salon = http, cocina = http, caja = http, sesiones = sesiones),
        cuentas = CuentasServicio(CuentaHttp(extra, llamar), sesiones),
        publico = PublicoServicio(PublicoHttp(extra, llamar), http, historial),
        gestion = GestionServicio(GestionHttp(extra, llamar)),
    )
}
