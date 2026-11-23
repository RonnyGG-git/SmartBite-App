package com.smartbite.adapter.almacen

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.smartbite.domain.puerto.salida.HistorialLocal
import kotlinx.coroutines.flow.first

class HistorialDataStore(private val datos: DataStore<Preferences>) : HistorialLocal {

    private suspend fun leer(clave: Preferences.Key<String>): List<String> =
        datos.data.first()[clave].orEmpty().split(",").filter { it.isNotBlank() }

    private suspend fun agregar(clave: Preferences.Key<String>, codigo: String) {
        val nuevos = (listOf(codigo) + leer(clave).filterNot { it == codigo }).take(MAXIMO)
        datos.edit { it[clave] = nuevos.joinToString(",") }
    }

    override suspend fun pedidos(): List<String> = leer(PEDIDOS)

    override suspend fun recordarPedido(codigo: String) = agregar(PEDIDOS, codigo)

    override suspend fun reservas(): List<String> = leer(RESERVAS)

    override suspend fun recordarReserva(codigo: String) = agregar(RESERVAS, codigo)

    companion object {
        private const val MAXIMO = 20
        private val PEDIDOS = stringPreferencesKey("historial.pedidos")
        private val RESERVAS = stringPreferencesKey("historial.reservas")
    }
}
