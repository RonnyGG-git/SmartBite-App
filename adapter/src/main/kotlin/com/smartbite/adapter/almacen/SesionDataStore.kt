package com.smartbite.adapter.almacen

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.gson.Gson
import com.smartbite.domain.modelo.Rol
import com.smartbite.domain.modelo.Sesion
import com.smartbite.domain.puerto.salida.SesionStore
import kotlinx.coroutines.flow.first

class SesionDataStore(
    private val datos: DataStore<Preferences>,
    private val gson: Gson = Gson(),
) : SesionStore {

    private data class Guardada(val token: String?, val nombre: String?, val rol: String?, val restaurante: String?)

    override suspend fun leer(): Sesion? {
        val texto = datos.data.first()[CLAVE] ?: return null
        val guardada = try {
            gson.fromJson(texto, Guardada::class.java)
        } catch (danada: Exception) {
            null
        }
        val rol = Rol.entries.firstOrNull { it.name == guardada?.rol }
        if (guardada == null || guardada.token.isNullOrBlank() || rol == null) {
            borrar()
            return null
        }
        return Sesion(guardada.token, guardada.nombre.orEmpty(), rol, guardada.restaurante)
    }

    override suspend fun guardar(sesion: Sesion) {
        val texto = gson.toJson(Guardada(sesion.token, sesion.nombre, sesion.rol.name, sesion.restaurante))
        datos.edit { it[CLAVE] = texto }
    }

    override suspend fun borrar() {
        datos.edit { it.remove(CLAVE) }
    }

    companion object {
        val CLAVE = stringPreferencesKey("smartbite.sesion")
    }
}
