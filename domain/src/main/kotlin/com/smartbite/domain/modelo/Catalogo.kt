package com.smartbite.domain.modelo

object CatalogoGestion {

    private val activo = Campo("activo", "Activo", TipoCampo.BOOLEANO)

    val sucursales = Recurso(
        clave = "sucursales", ruta = "gestion/sucursales/", titulo = "Sucursales", singular = "sucursal",
        campos = listOf(
            Campo("restaurante", "Restaurante", TipoCampo.OPCION, true, Fuente.DeRecurso("restaurantes")),
            Campo("nombre", "Nombre", TipoCampo.TEXTO, true),
            Campo("direccion", "Dirección", TipoCampo.TEXTO, true),
            Campo("telefono", "Teléfono", TipoCampo.TELEFONO, true),
            Campo("latitud", "Latitud", TipoCampo.DECIMAL),
            Campo("longitud", "Longitud", TipoCampo.DECIMAL),
            activo,
        ),
        lee = { it.administra }, gestiona = { it.administra },
        principal = { it.texto("nombre") },
        secundario = { "${it.texto("restaurante_nombre")} · ${it.texto("direccion")}" },
    )

    val restaurantes = Recurso(
        clave = "restaurantes", ruta = "gestion/restaurantes/", titulo = "Restaurantes", singular = "restaurante",
        campos = listOf(
            Campo("nombre", "Nombre", TipoCampo.TEXTO, true),
            Campo("nif", "NIT", TipoCampo.TEXTO, true),
            Campo("email", "Correo", TipoCampo.CORREO, true),
            Campo("direccion", "Dirección", TipoCampo.TEXTO, true),
            Campo("telefono", "Teléfono", TipoCampo.TELEFONO, true),
            Campo("telefono_secundario", "Teléfono secundario", TipoCampo.TELEFONO),
            Campo("logo_url", "Enlace del logo", TipoCampo.TEXTO),
            activo,
        ),
        lee = { it.administra }, gestiona = { it.administra },
        principal = { it.texto("nombre") },
        secundario = { "NIT ${it.texto("nif")} · Código para empleados: ${it.texto("codigo_acceso")}" },
        acciones = listOf(
            AccionRecurso("codigo", "Nuevo código de acceso", TipoAccion.ALTERNAR),
            AccionRecurso("elegir", "Trabajar con este restaurante", TipoAccion.ELEGIR, soloGestion = false),
        ),
    )

    val categorias = Recurso(
        clave = "categorias", ruta = "gestion/categorias/", titulo = "Categorías", singular = "categoría",
        campos = listOf(Campo("nombre", "Nombre", TipoCampo.TEXTO, true)),
        lee = { it.leeCatalogo }, gestiona = { it.gestionaCatalogo },
        principal = { it.texto("nombre") },
    )

    val productos = Recurso(
        clave = "productos", ruta = "gestion/productos/", titulo = "Platillos", singular = "platillo",
        campos = listOf(
            Campo("nombre", "Nombre", TipoCampo.TEXTO, true),
            Campo("descripcion", "Descripción", TipoCampo.TEXTO_LARGO),
            Campo("precio", "Precio", TipoCampo.DECIMAL, true),
            Campo("categoria", "Categoría", TipoCampo.OPCION, true, Fuente.DeRecurso("categorias")),
            Campo("sucursal", "Sucursal", TipoCampo.OPCION, true, Fuente.DeRecurso("sucursales")),
            Campo("url_imagen", "Enlace de imagen (si no subes foto)", TipoCampo.TEXTO),
            Campo("disponible", "Disponible", TipoCampo.BOOLEANO),
        ),
        lee = { it.leeCatalogo }, gestiona = { it.gestionaCatalogo },
        principal = { it.texto("nombre") },
        secundario = { "$${it.texto("precio")} · ${it.texto("categoria_nombre")} · ${if (it.si("disponible")) "Disponible" else "Agotado"}" },
        imagen = { it.valores["imagen"]?.takeIf { url -> url.isNotBlank() } },
        alerta = { !it.si("disponible") },
        acciones = listOf(
            AccionRecurso("disponibilidad", "Cambiar disponibilidad", TipoAccion.ALTERNAR),
            AccionRecurso("foto", "Cambiar foto", TipoAccion.FOTO),
        ),
    )

    val mesas = Recurso(
        clave = "mesas", ruta = "gestion/mesas/", titulo = "Mesas", singular = "mesa",
        campos = listOf(
            Campo("numero", "Número", TipoCampo.ENTERO, true),
            Campo("capacidad", "Capacidad", TipoCampo.ENTERO, true),
            Campo("sucursal", "Sucursal", TipoCampo.OPCION, true, Fuente.DeRecurso("sucursales")),
            Campo("activa", "Activa", TipoCampo.BOOLEANO),
        ),
        lee = { it.atiendeMesas }, gestiona = { it.administra },
        principal = { "Mesa ${it.texto("numero")}" },
        secundario = { "${it.texto("capacidad")} personas · ${it.texto("sucursal_nombre")} · ${it.texto("estado_nombre")}" },
        acciones = listOf(AccionRecurso("qr", "Ver QR de la mesa", TipoAccion.QR, soloGestion = false)),
    )

    val items = Recurso(
        clave = "items", ruta = "gestion/items/", titulo = "Inventario", singular = "ítem",
        campos = listOf(
            Campo("nombre", "Nombre", TipoCampo.TEXTO, true),
            Campo("descripcion", "Descripción", TipoCampo.TEXTO),
            Campo("unidad_medida", "Unidad", TipoCampo.OPCION, true, Fuente.Unidades),
            Campo("stock_actual", "Stock actual", TipoCampo.ENTERO, true),
            Campo("stock_minimo", "Stock mínimo", TipoCampo.ENTERO, true),
            Campo("costo_unitario", "Costo unitario", TipoCampo.DECIMAL, true),
            Campo("ubicacion", "Ubicación", TipoCampo.TEXTO),
            Campo("sucursal", "Sucursal", TipoCampo.OPCION, true, Fuente.DeRecurso("sucursales")),
        ),
        lee = { it.leeInventario }, gestiona = { it.gestionaInventario },
        principal = { it.texto("nombre") },
        secundario = { "Stock ${it.texto("stock_actual")} ${it.texto("unidad_medida")} · mínimo ${it.texto("stock_minimo")} · ${it.texto("sucursal_nombre")}" },
        alerta = { it.si("stock_bajo") },
        acciones = listOf(
            AccionRecurso(
                "ajustar", "Ajustar stock", TipoAccion.FORMULARIO,
                campos = listOf(
                    Campo("tipo", "Movimiento", TipoCampo.OPCION, true, Fuente.Fija(listOf(Opcion("ENTRADA", "Entrada"), Opcion("SALIDA", "Salida")))),
                    Campo("cantidad", "Cantidad", TipoCampo.ENTERO, true),
                    Campo("motivo", "Motivo", TipoCampo.TEXTO, true),
                ),
            ),
        ),
    )

    val movimientos = Recurso(
        clave = "movimientos", ruta = "gestion/movimientos/", titulo = "Movimientos", singular = "movimiento",
        campos = emptyList(),
        lee = { it.gestionaInventario }, gestiona = { false },
        principal = { "${it.texto("tipo_nombre")} · ${it.texto("cantidad")} × ${it.texto("item_nombre")}" },
        secundario = { "${it.texto("motivo")} · ${it.texto("creado_en").take(16).replace("T", " ")}" },
        alerta = { it.texto("tipo") != "ENTRADA" },
        creable = false, editable = false, borrable = false,
    )

    val proveedores = Recurso(
        clave = "proveedores", ruta = "gestion/proveedores/", titulo = "Proveedores", singular = "proveedor",
        campos = listOf(
            Campo("nombre", "Nombre", TipoCampo.TEXTO, true),
            Campo("contacto", "Contacto", TipoCampo.TEXTO),
            Campo("telefono", "Teléfono", TipoCampo.TELEFONO),
            Campo("email", "Correo", TipoCampo.CORREO),
            Campo("direccion", "Dirección", TipoCampo.TEXTO),
            activo,
        ),
        lee = { it.gestionaInventario }, gestiona = { it.gestionaInventario },
        principal = { it.texto("nombre") },
        secundario = { listOf(it.texto("contacto"), it.texto("telefono")).filter(String::isNotBlank).joinToString(" · ") },
        alerta = { !it.si("activo") },
    )

    val recetas = Recurso(
        clave = "recetas", ruta = "gestion/recetas/", titulo = "Recetas", singular = "ingrediente",
        campos = listOf(
            Campo("producto", "Platillo", TipoCampo.OPCION, true, Fuente.DeRecurso("productos")),
            Campo("item_inventario", "Ingrediente del inventario", TipoCampo.OPCION, true, Fuente.DeRecurso("items")),
            Campo("cantidad_requerida", "Cantidad por platillo", TipoCampo.DECIMAL, true),
            Campo("unidad_medida", "Unidad", TipoCampo.OPCION, true, Fuente.Unidades),
            Campo("activo", "Activo", TipoCampo.BOOLEANO),
        ),
        lee = { it.leeInventario }, gestiona = { it.gestionaCatalogo },
        principal = { "${it.texto("producto_nombre")} ← ${it.texto("item_nombre")}" },
        secundario = { "${it.texto("cantidad_requerida")} ${it.texto("unidad_medida")}" },
    )

    val usuarios = Recurso(
        clave = "usuarios", ruta = "gestion/usuarios/", titulo = "Usuarios", singular = "usuario",
        campos = listOf(
            Campo("nombre", "Nombre", TipoCampo.TEXTO, true),
            Campo("email", "Correo", TipoCampo.CORREO, true),
            Campo("rol", "Rol", TipoCampo.OPCION, true, Fuente.RolesEmpleado),
            Campo("sucursal", "Sucursal", TipoCampo.OPCION, true, Fuente.DeRecurso("sucursales")),
            Campo("password1", "Contraseña", TipoCampo.CLAVE, true, soloAlCrear = true),
            Campo("password2", "Repite la contraseña", TipoCampo.CLAVE, true, soloAlCrear = true),
            Campo("is_active", "Cuenta activa", TipoCampo.BOOLEANO, soloAlEditar = true),
        ),
        lee = { it.administra }, gestiona = { it.administra },
        principal = { it.texto("nombre") },
        secundario = { "${it.texto("email")} · ${it.texto("rol_nombre")}" },
        alerta = { !it.si("is_active") },
        acciones = listOf(AccionRecurso("activo", "Activar o desactivar", TipoAccion.ALTERNAR)),
    )

    val opiniones = Recurso(
        clave = "opiniones", ruta = "gestion/opiniones/", titulo = "Opiniones", singular = "opinión",
        campos = emptyList(),
        lee = { it.administra }, gestiona = { it.administra },
        principal = { "${it.texto("tipo_nombre")}: ${it.texto("mensaje")}" },
        secundario = {
            listOf(
                it.texto("nombre").ifBlank { "Anónimo" },
                it.texto("contacto"),
                it.valores["mesa_numero"]?.let { m -> "Mesa $m" }.orEmpty(),
                if (it.si("revisada")) "Revisada" else "Sin revisar",
            ).filter(String::isNotBlank).joinToString(" · ")
        },
        alerta = { !it.si("revisada") },
        acciones = listOf(AccionRecurso("revisar", "Marcar revisada o pendiente", TipoAccion.ALTERNAR)),
        creable = false, editable = false, borrable = false,
    )

    val todos: List<Recurso> = listOf(
        productos, categorias, recetas, items, movimientos, proveedores, mesas, usuarios, sucursales, restaurantes, opiniones,
    )

    fun porClave(clave: String): Recurso = todos.first { it.clave == clave }
}
