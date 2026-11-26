package com.smartbite.adapter.http

import com.smartbite.adapter.dto.AgregarBody
import com.smartbite.adapter.dto.AsignarMesaBody
import com.smartbite.adapter.dto.CobrarBody
import com.smartbite.adapter.dto.CrearOrdenBody
import com.smartbite.adapter.dto.CuentaDto
import com.smartbite.adapter.dto.FacturaDto
import com.smartbite.adapter.dto.FacturaResumenDto
import com.smartbite.adapter.dto.IniciarBody
import com.smartbite.adapter.dto.LoginBody
import com.smartbite.adapter.dto.MesaDto
import com.smartbite.adapter.dto.MesaQrDto
import com.smartbite.adapter.dto.MetodoPagoDto
import com.smartbite.adapter.dto.OrdenDto
import com.smartbite.adapter.dto.Pagina
import com.smartbite.adapter.dto.PedidoCreadoDto
import com.smartbite.adapter.dto.PedidoMesaBody
import com.smartbite.adapter.dto.ProductoDto
import com.smartbite.adapter.dto.QrDto
import com.smartbite.adapter.dto.ReservaDto
import com.smartbite.adapter.dto.SesionDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface SmartBiteApi {

    @POST("api/v1/auth/login/")
    suspend fun login(@Body body: LoginBody): Response<SesionDto>

    @POST("api/v1/auth/logout/")
    suspend fun logout(@Header("Authorization") token: String): Response<Unit>

    @GET("api/v1/publico/mesas/{id}/pedido/")
    suspend fun mesaQr(@Path("id") mesaId: Int): Response<MesaQrDto>

    @GET("api/v1/publico/sucursales/{id}/menu/")
    suspend fun menu(@Path("id") sucursalId: Int): Response<List<ProductoDto>>

    @POST("api/v1/publico/mesas/{id}/pedido/")
    suspend fun pedirEnMesa(@Path("id") mesaId: Int, @Body body: PedidoMesaBody): Response<PedidoCreadoDto>

    @GET("api/v1/publico/pedidos/{codigo}/")
    suspend fun pedido(@Path("codigo") codigo: String): Response<OrdenDto>

    @GET("api/v1/mesas/")
    suspend fun mesas(@Header("Authorization") token: String): Response<List<MesaDto>>

    @GET("api/v1/productos/")
    suspend fun productos(@Header("Authorization") token: String, @Query("sucursal") sucursalId: Int): Response<List<ProductoDto>>

    @GET("api/v1/ordenes/{id}/")
    suspend fun orden(@Header("Authorization") token: String, @Path("id") ordenId: Int): Response<OrdenDto>

    @GET("api/v1/ordenes/?sin_mesa=1")
    suspend fun sinMesa(@Header("Authorization") token: String): Response<Pagina<OrdenDto>>

    @POST("api/v1/ordenes/")
    suspend fun abrirOrden(@Header("Authorization") token: String, @Body body: CrearOrdenBody): Response<OrdenDto>

    @POST("api/v1/ordenes/{id}/detalles/")
    suspend fun agregar(@Header("Authorization") token: String, @Path("id") ordenId: Int, @Body body: AgregarBody): Response<OrdenDto>

    @POST("api/v1/ordenes/{id}/asignar-mesa/")
    suspend fun asignarMesa(@Header("Authorization") token: String, @Path("id") ordenId: Int, @Body body: AsignarMesaBody): Response<OrdenDto>

    @POST("api/v1/ordenes/{id}/{accion}/")
    suspend fun accionOrden(
        @Header("Authorization") token: String,
        @Path("id") ordenId: Int,
        @Path("accion") accion: String,
    ): Response<OrdenDto>

    @GET("api/v1/reservas/")
    suspend fun reservas(@Header("Authorization") token: String): Response<List<ReservaDto>>

    @POST("api/v1/reservas/{id}/{accion}/")
    suspend fun accionReserva(
        @Header("Authorization") token: String,
        @Path("id") reservaId: Int,
        @Path("accion") accion: String,
    ): Response<ReservaDto>

    @GET("api/v1/cocina/ordenes/")
    suspend fun cocina(@Header("Authorization") token: String): Response<List<OrdenDto>>

    @POST("api/v1/cocina/ordenes/{id}/iniciar/")
    suspend fun iniciar(@Header("Authorization") token: String, @Path("id") ordenId: Int, @Body body: IniciarBody): Response<OrdenDto>

    @POST("api/v1/cocina/ordenes/{id}/listo/")
    suspend fun listo(@Header("Authorization") token: String, @Path("id") ordenId: Int): Response<OrdenDto>

    @GET("api/v1/caja/por-cobrar/")
    suspend fun porCobrar(@Header("Authorization") token: String): Response<List<OrdenDto>>

    @GET("api/v1/caja/para-llevar/")
    suspend fun paraLlevar(@Header("Authorization") token: String): Response<List<OrdenDto>>

    @GET("api/v1/caja/metodos/")
    suspend fun metodos(@Header("Authorization") token: String): Response<List<MetodoPagoDto>>

    @GET("api/v1/caja/ordenes/{id}/cuenta/")
    suspend fun cuenta(@Header("Authorization") token: String, @Path("id") ordenId: Int): Response<CuentaDto>

    @POST("api/v1/caja/ordenes/{id}/cobrar/")
    suspend fun cobrar(@Header("Authorization") token: String, @Path("id") ordenId: Int, @Body body: CobrarBody): Response<CuentaDto>

    @POST("api/v1/caja/ordenes/{id}/qr/")
    suspend fun qr(@Header("Authorization") token: String, @Path("id") ordenId: Int): Response<QrDto>

    @POST("api/v1/caja/pagos/{id}/anular/")
    suspend fun anularPago(@Header("Authorization") token: String, @Path("id") pagoId: Int): Response<CuentaDto>

    @POST("api/v1/caja/ordenes/{id}/{accion}/")
    suspend fun accionCaja(
        @Header("Authorization") token: String,
        @Path("id") ordenId: Int,
        @Path("accion") accion: String,
    ): Response<OrdenDto>

    @GET("api/v1/caja/ventas/")
    suspend fun ventas(@Header("Authorization") token: String): Response<Pagina<OrdenDto>>

    @GET("api/v1/caja/facturas/")
    suspend fun facturas(@Header("Authorization") token: String): Response<Pagina<FacturaResumenDto>>

    @GET("api/v1/caja/facturas/{id}/")
    suspend fun factura(@Header("Authorization") token: String, @Path("id") facturaId: Int): Response<FacturaDto>
}
