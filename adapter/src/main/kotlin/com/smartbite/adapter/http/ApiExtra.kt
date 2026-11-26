package com.smartbite.adapter.http

import com.google.gson.JsonElement
import com.smartbite.adapter.dto.CompraDto
import com.smartbite.adapter.dto.CorreoBody
import com.smartbite.adapter.dto.MensajeDto
import com.smartbite.adapter.dto.OpinionBody
import com.smartbite.adapter.dto.PagoClienteDto
import com.smartbite.adapter.dto.Pagina
import com.smartbite.adapter.dto.PedidoCreadoDto
import com.smartbite.adapter.dto.PedidoPublicoBody
import com.smartbite.adapter.dto.RegistroBody
import com.smartbite.adapter.dto.RegistroDto
import com.smartbite.adapter.dto.ReservaBody
import com.smartbite.adapter.dto.ReservaPublicaDto
import com.smartbite.adapter.dto.RestablecerBody
import com.smartbite.adapter.dto.SesionDto
import com.smartbite.adapter.dto.SucursalPublicaDto
import com.smartbite.adapter.dto.TableroDto
import com.smartbite.adapter.dto.VerificarBody
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

interface ApiExtra {

    @POST("api/v1/auth/registro/{tipo}/")
    suspend fun registrar(@Path("tipo") tipo: String, @Body body: RegistroBody): Response<RegistroDto>

    @POST("api/v1/auth/verificar/")
    suspend fun verificar(@Body body: VerificarBody): Response<SesionDto>

    @POST("api/v1/auth/reenviar/")
    suspend fun reenviar(@Body body: CorreoBody): Response<MensajeDto>

    @POST("api/v1/auth/recuperar/")
    suspend fun recuperar(@Body body: CorreoBody): Response<MensajeDto>

    @POST("api/v1/auth/recuperar/confirmar/")
    suspend fun restablecer(@Body body: RestablecerBody): Response<MensajeDto>

    @GET("api/v1/publico/sucursales/")
    suspend fun sucursales(@Query("q") busqueda: String?): Response<List<SucursalPublicaDto>>

    @POST("api/v1/publico/pedidos/")
    suspend fun pedirParaLlevar(@Body body: PedidoPublicoBody): Response<PedidoCreadoDto>

    @POST("api/v1/publico/sucursales/{id}/reservas/")
    suspend fun reservar(@Path("id") sucursalId: Int, @Body body: ReservaBody): Response<ReservaPublicaDto>

    @GET("api/v1/publico/reservas/{codigo}/")
    suspend fun reserva(@Path("codigo") codigo: String): Response<ReservaPublicaDto>

    @POST("api/v1/publico/reservas/{codigo}/")
    suspend fun cancelarReserva(@Path("codigo") codigo: String): Response<ReservaPublicaDto>

    @POST("api/v1/publico/opiniones/")
    suspend fun opinar(@Body body: OpinionBody): Response<JsonElement>

    @POST("api/v1/publico/pedidos/{codigo}/pago/")
    suspend fun pagarPedido(@Path("codigo") codigo: String): Response<PagoClienteDto>

    @GET("api/v1/gestion/reportes/pdf/")
    suspend fun reportePdf(
        @Header("Authorization") token: String,
        @Query("desde") desde: String,
        @Query("hasta") hasta: String,
    ): Response<ResponseBody>

    @GET
    suspend fun leer(@Header("Authorization") token: String, @Url ruta: String): Response<JsonElement>

    @POST
    suspend fun enviar(
        @Header("Authorization") token: String,
        @Url ruta: String,
        @Body cuerpo: Map<String, @JvmSuppressWildcards Any?>,
    ): Response<JsonElement>

    @PUT
    suspend fun reemplazar(
        @Header("Authorization") token: String,
        @Url ruta: String,
        @Body cuerpo: Map<String, @JvmSuppressWildcards Any?>,
    ): Response<JsonElement>

    @DELETE
    suspend fun borrar(@Header("Authorization") token: String, @Url ruta: String): Response<Unit>

    @Multipart
    @POST
    suspend fun subir(@Header("Authorization") token: String, @Url ruta: String, @Part archivo: MultipartBody.Part): Response<JsonElement>

    @GET("api/v1/gestion/dashboard/")
    suspend fun tablero(@Header("Authorization") token: String): Response<TableroDto>

    @GET("api/v1/gestion/compras/")
    suspend fun compras(@Header("Authorization") token: String): Response<Pagina<CompraDto>>

    @POST("api/v1/gestion/compras/")
    suspend fun crearCompra(@Header("Authorization") token: String, @Body cuerpo: Map<String, @JvmSuppressWildcards Any?>): Response<CompraDto>

    @POST("api/v1/gestion/compras/{id}/{accion}/")
    suspend fun accionCompra(@Header("Authorization") token: String, @Path("id") id: Int, @Path("accion") accion: String): Response<CompraDto>
}
