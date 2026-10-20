package com.smartfood.app.feature.recipes.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource

/**
 * Nombre: RecipePhoto.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Foto de una receta desde `drawable-nodpi` (spec 009, T007 y T008). Si el recurso no existe, deja un
 * fondo verde claro para que la tarjeta no se rompa. El tamaño y el recorte los pone quien la usa.
 *
 * @param photo Nombre del recurso (`receta_pollo_guisado`).
 * @param modifier Tamaño, forma y recorte.
 */
@Composable
fun RecipePhoto(
    photo: String?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val resourceId = remember(photo) {
        photo?.let { name -> context.resources.getIdentifier(name, "drawable", context.packageName) } ?: 0
    }
    if (resourceId != 0) {
        Image(
            painter = painterResource(id = resourceId),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        Box(modifier = modifier.background(MaterialTheme.colorScheme.primaryContainer))
    }
}
