package com.smartbite.movil.di

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.smartbite.adapter.almacen.SesionDataStore
import com.smartbite.adapter.http.FabricaHttp
import com.smartbite.adapter.http.SmartBiteHttp
import com.smartbite.application.puerto.entrada.CasosDeUso
import com.smartbite.application.servicio.SmartBiteServicio
import com.smartbite.movil.BuildConfig
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

private val Context.almacenSesion by preferencesDataStore(name = "sesion")

class Contenedor(contexto: Context) {

    private val vencida = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sesionVencida: SharedFlow<Unit> = vencida

    private val sesiones = SesionDataStore(contexto.applicationContext.almacenSesion)

    private val llamar = FabricaHttp.llamadas(sesiones) { vencida.tryEmit(Unit) }

    private val http = SmartBiteHttp(FabricaHttp.api(BuildConfig.API_BASE_URL), llamar)

    val casos: CasosDeUso = SmartBiteServicio(
        auth = http,
        cliente = http,
        salon = http,
        cocina = http,
        caja = http,
        sesiones = sesiones,
    )
}
