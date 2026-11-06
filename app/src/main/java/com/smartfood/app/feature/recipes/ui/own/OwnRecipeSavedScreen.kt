package com.smartfood.app.feature.recipes.ui.own

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Pantalla de confirmación al crear o editar una receta propia (spec 010, T014). */
@Composable
fun OwnRecipeSavedScreen(name: String, edited: Boolean, onView: () -> Unit, onCreateAnother: () -> Unit, onBack: () -> Unit) {
    Column(Modifier.padding(24.dp)) {
        Text(if (edited) "¡Receta actualizada!" else "¡Receta guardada!")
        Text("\"$name\" ${if (edited) "ha sido actualizada." else "ya está en Creadas por mí."}")
        Button(onClick = onView) { Text("Ver receta") }
        Button(onClick = onCreateAnother) { Text("Crear otra") }
        Button(onClick = onBack) { Text("Volver") }
    }
}
