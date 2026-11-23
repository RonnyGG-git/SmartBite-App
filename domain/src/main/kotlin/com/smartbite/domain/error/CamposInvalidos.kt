package com.smartbite.domain.error

class CamposInvalidos(val errores: Map<String, String>) : RuntimeException(
    errores["general"] ?: "Revisa los campos marcados.",
)
