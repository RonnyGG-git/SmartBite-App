from decimal import Decimal

from catalogo.models import Categoria, Producto
from cuentas.models import Rol, Usuario
from operativo.models import Mesa
from restaurantes.models import Restaurante, Sucursal

restaurante, _ = Restaurante.objects.update_or_create(
    nif="900000001", defaults={"nombre": "Demo Móvil", "email": "demo@movil.co", "direccion": "Calle 1"}
)
sucursal, _ = Sucursal.objects.get_or_create(restaurante=restaurante, nombre="Centro")
fuertes, _ = Categoria.objects.get_or_create(nombre="Platos fuertes", restaurante=restaurante)
bebidas, _ = Categoria.objects.get_or_create(nombre="Bebidas", restaurante=restaurante)
for nombre, precio, categoria in [
    ("Bandeja paisa", "32000", fuertes),
    ("Ajiaco santafereño", "28000", fuertes),
    ("Churrasco", "41000", fuertes),
    ("Limonada de coco", "9000", bebidas),
    ("Jugo de lulo", "7000", bebidas),
]:
    Producto.objects.get_or_create(
        nombre=nombre, sucursal=sucursal, defaults={"precio": Decimal(precio), "categoria": categoria, "disponible": True}
    )
for numero in (1, 2, 3):
    Mesa.objects.get_or_create(numero=numero, sucursal=sucursal)

for rol_nombre, correo in [
    ("ADMINISTRADOR", "admin@movil.co"),
    ("MESERO", "mesero@movil.co"),
    ("JEFE_COCINA", "cocina@movil.co"),
    ("CAJERO", "caja@movil.co"),
]:
    rol, _ = Rol.objects.get_or_create(nombre=rol_nombre)
    if not Usuario.objects.filter(email=correo).exists():
        Usuario.objects.create_user(
            email=correo, nombre=rol_nombre.title().replace("_", " "), password="Demo12345",
            rol=rol, restaurante=restaurante, email_verificado=True,
        )

restaurante.administrador = Usuario.objects.get(email="admin@movil.co")
restaurante.save(update_fields=["administrador"])

print("mesas", list(Mesa.objects.filter(sucursal=sucursal).values_list("id", "numero")))
print("sucursal", sucursal.pk)
