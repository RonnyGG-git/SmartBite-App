package com.smartbite.movil.ui.componentes

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.smartbite.domain.modelo.Opcion

@Composable
fun CampoTexto(
    etiqueta: String,
    valor: String,
    error: String?,
    teclado: KeyboardType = KeyboardType.Text,
    oculto: Boolean = false,
    lineas: Int = 1,
    habilitado: Boolean = true,
    alCambiar: (String) -> Unit,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = alCambiar,
        label = { Text(etiqueta) },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        singleLine = lineas == 1,
        minLines = lineas,
        enabled = habilitado,
        visualTransformation = if (oculto) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = teclado),
        modifier = Modifier.fillMaxWidth(),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Selector(
    etiqueta: String,
    opciones: List<Opcion>,
    valor: String,
    error: String?,
    habilitado: Boolean = true,
    alElegir: (String) -> Unit,
) {
    var abierto by remember { mutableStateOf(false) }
    val visible = opciones.firstOrNull { it.valor == valor }?.etiqueta ?: if (valor.isBlank()) "" else "#$valor"
    ExposedDropdownMenuBox(expanded = abierto, onExpandedChange = { if (habilitado) abierto = it }) {
        OutlinedTextField(
            value = visible,
            onValueChange = {},
            readOnly = true,
            enabled = habilitado,
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            label = { Text(etiqueta) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = abierto) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, habilitado).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            if (opciones.isEmpty()) {
                DropdownMenuItem(text = { Text("No hay opciones todavía") }, onClick = { abierto = false })
            }
            opciones.forEach { opcion ->
                DropdownMenuItem(
                    text = { Text(opcion.etiqueta) },
                    onClick = {
                        alElegir(opcion.valor)
                        abierto = false
                    },
                )
            }
        }
    }
}

@Composable
fun Interruptor(etiqueta: String, valor: Boolean, habilitado: Boolean = true, alCambiar: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(etiqueta, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = valor, onCheckedChange = alCambiar, enabled = habilitado)
    }
}
