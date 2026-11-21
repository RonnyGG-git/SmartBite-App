package com.smartbite.movil

import android.app.Application
import com.smartbite.movil.di.Contenedor

class SmartBiteMovil : Application() {

    lateinit var contenedor: Contenedor
        private set

    override fun onCreate() {
        super.onCreate()
        contenedor = Contenedor(this)
    }
}
