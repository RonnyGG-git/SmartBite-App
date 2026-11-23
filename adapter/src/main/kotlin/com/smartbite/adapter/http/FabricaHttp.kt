package com.smartbite.adapter.http

import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.smartbite.domain.puerto.salida.SesionStore
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object FabricaHttp {

    val gson: Gson = GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .create()

    private fun retrofit(direccion: String, cabecera: CabeceraRestaurante): Retrofit {
        val cliente = OkHttpClient.Builder()
            .addInterceptor(cabecera)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .callTimeout(75, TimeUnit.SECONDS)
            .build()
        return Retrofit.Builder()
            .baseUrl(if (direccion.endsWith("/")) direccion else "$direccion/")
            .client(cliente)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    fun api(direccion: String, cabecera: CabeceraRestaurante = CabeceraRestaurante()): SmartBiteApi =
        retrofit(direccion, cabecera).create(SmartBiteApi::class.java)

    fun extra(direccion: String, cabecera: CabeceraRestaurante = CabeceraRestaurante()): ApiExtra =
        retrofit(direccion, cabecera).create(ApiExtra::class.java)

    fun llamadas(
        sesiones: SesionStore,
        cabecera: CabeceraRestaurante = CabeceraRestaurante(),
        alVencerSesion: () -> Unit,
    ): LlamadaApi = LlamadaApi(sesiones, gson, cabecera, alVencerSesion)
}
