package com.smartbite.adapter.almacen

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.gson.Gson
import com.smartbite.domain.modelo.RestauranteRef
import com.smartbite.domain.modelo.Rol
import com.smartbite.domain.modelo.Sesion
import com.smartbite.domain.puerto.salida.SesionStore
import kotlinx.coroutines.flow.first

class SesionDataStore(
    private val datos: DataStore<Preferences>,
    private val gson: Gson = Gson(),
) : SesionStore {

    private data class Ref(val id: Int?, val nombre: String?)

    private data class Guardada(
        val token: String?,
        val nombre: String?,
        val rol: String?,
        val restaurante: String?,
        val restaurantes: List<Ref>?,
        val restauranteId: Int?,
    )

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
        return Sesion(
            token = guardada.token,
            nombre = guardada.nombre.orEmpty(),
            rol = rol,
            restaurante = guardada.restaurante,
            restaurantes = guardada.restaurantes.orEmpty().mapNotNull { r -> r.id?.let { RestauranteRef(it, r.nombre.orEmpty()) } },
            restauranteId = guardada.restauranteId,
        )
    }

    override suspend fun guardar(sesion: Sesion) {
        val texto = gson.toJson(
            Guardada(
                sesion.token, sesion.nombre, sesion.rol.name, sesion.restaurante,
                sesion.restaurantes.map { Ref(it.id, it.nombre) }, sesion.restauranteId,
            ),
        )
        datos.edit { it[CLAVE] = texto }
    }

    override suspend fun borrar() {
        datos.edit { it.remove(CLAVE) }
    }

    companion object {
        val CLAVE = stringPreferencesKey("smartbite.sesion")
    }
}
