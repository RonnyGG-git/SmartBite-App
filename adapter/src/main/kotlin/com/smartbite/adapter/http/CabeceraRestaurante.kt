package com.smartbite.adapter.http

import okhttp3.Interceptor
import okhttp3.Response

class CabeceraRestaurante : Interceptor {

    @Volatile
    var restauranteId: Int? = null

    override fun intercept(chain: Interceptor.Chain): Response {
        val id = restauranteId
        val peticion = chain.request()
        if (id == null || peticion.header("Authorization") == null) return chain.proceed(peticion)
        return chain.proceed(peticion.newBuilder().header("X-Restaurante", id.toString()).build())
    }
}
