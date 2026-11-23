package com.smartbite.application.servicio

import com.smartbite.application.puerto.entrada.CasosDeUso
import com.smartbite.application.puerto.entrada.CuentasCasos
import com.smartbite.application.puerto.entrada.GestionCasos
import com.smartbite.application.puerto.entrada.OperacionCasos
import com.smartbite.application.puerto.entrada.PublicoCasos

class Fachada(
    operacion: OperacionCasos,
    cuentas: CuentasCasos,
    publico: PublicoCasos,
    gestion: GestionCasos,
) : CasosDeUso,
    OperacionCasos by operacion,
    CuentasCasos by cuentas,
    PublicoCasos by publico,
    GestionCasos by gestion
