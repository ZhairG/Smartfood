package com.smartfood.app.feature.recipes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.smartfood.app.core.designsystem.theme.SmartFoodMotion

/**
 * Nombre: RecipePhoto.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Foto de una receta desde `drawable-nodpi` (spec 009, T007 y T008). Si el recurso no existe, deja un
 * fondo verde claro para que la tarjeta no se rompa. El tamaño y el recorte los pone quien la usa.
 *
 * La foto la carga Coil (spec 007, T021): la decodifica fuera del hilo principal, al tamaño de la tarjeta, y la guarda
 * en su caché. Con `painterResource`, cada foto de 800×600 tardaba unos 13 ms en el hilo principal del V2436 y la
 * pestaña Recetas saltaba cuadros. Mientras carga se ve el mismo fondo, y la foto aparece con una disolvencia de
 * 100 ms la primera vez (desviación aprobada, spec 007 §8).
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
    val placeholder = modifier.background(MaterialTheme.colorScheme.primaryContainer)
    if (resourceId != 0) {
        val request = remember(resourceId) {
            ImageRequest.Builder(context)
                .data(resourceId)
                .crossfade(SmartFoodMotion.SHORT)
                .build()
        }
        AsyncImage(
            model = request,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = placeholder
        )
    } else {
        Box(modifier = placeholder)
    }
}
