package com.smartfood.app.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Botón primario utilizando la paleta de Material 3 y tipografía del tema (51 dp alto, esquinas 10 dp). */
@Composable
fun SmartFoodPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(10.dp) // Medida exacta de Figma

    Button(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondary, // Naranja
            contentColor = MaterialTheme.colorScheme.onSecondary, // Blanco
            disabledContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f),
            disabledContentColor = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.7f),
        ),
        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
        contentPadding = PaddingValues(0.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(51.dp) // Medida exacta de Figma
            .shadow(
                elevation = 4.dp, // Sombra 0/4/4 al 30% como en Figma
                shape = shape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.3f),
                spotColor = Color.Black.copy(alpha = 0.3f),
            ),
    ) {
        Text(text = text, style = MaterialTheme.typography.titleLarge) // Mapeado a Button (22.sp, SemiBold)
    }
}
