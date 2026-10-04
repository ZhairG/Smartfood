package com.smartfood.app.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartfood.app.core.designsystem.theme.GreenDark
import com.smartfood.app.core.designsystem.theme.GreenDark2
import com.smartfood.app.core.designsystem.theme.GreenLight
import com.smartfood.app.core.designsystem.theme.GreenSymbol
import com.smartfood.app.core.designsystem.theme.OrangeMain
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.core.designsystem.theme.SurfaceDark

/**
 * Nombre: ActionSuccessScreen.kt
 * Autor: SmartFood Team
 * Fecha: Septiembre 2026
 * Descripción: Componente de pantalla atómico y reutilizable para acciones exitosas en Smartfood
 * ("Acción Exitosa" según los nodos 585:4564 y 223:1142 de Figma).
 *
 * Configura los esquemas de color exactos pedidos por el diseño:
 * - Modo Claro: Fondo GreenLight (#F0FDF4), Título GreenDark (#14532D), Cuerpo OrangeMain (#FB923C).
 * - Modo Oscuro: Fondo GreenDark2 (#10271C), Título #4ADE80, Cuerpo OrangeMain (#FB923C).
 * - Símbolo de Verificación: Círculos concéntricos con color #22C55E.
 *
 * @param title Título principal de la acción completada.
 * @param body Mensaje descriptivo o secundario.
 * @param buttonText Texto a mostrar dentro del botón de acción principal.
 * @param onButtonClick Callback ejecutado al presionar el botón.
 * @param modifier Modificador opcional para la apariencia exterior.
 */
@Composable
fun ActionSuccessScreen(
    title: String,
    body: String,
    buttonText: String,
    onButtonClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme() || MaterialTheme.colorScheme.surface == SurfaceDark

    val backgroundColor = if (isDark) GreenDark2 else GreenLight
    val titleColor = if (isDark) Color(0xFF4ADE80) else GreenDark
    val bodyColor = OrangeMain

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = backgroundColor,
        contentWindowInsets = WindowInsets(0.dp)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(backgroundColor)
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top))
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                .padding(horizontal = 34.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.widthIn(max = 380.dp)
            ) {
                ConcentricSuccessCheckIcon()

                Spacer(modifier = Modifier.height(34.dp))

                Text(
                    text = title,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = titleColor,
                    textAlign = TextAlign.Center,
                    lineHeight = 38.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = body,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = bodyColor,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            }

            SmartFoodPrimaryButton(
                text = buttonText,
                onClick = onButtonClick,
                modifier = Modifier
                    .widthIn(max = 380.dp)
                    .fillMaxWidth()
            )
        }
    }
}

/**
 * Símbolo de verificación verde concéntrico según especificación de Figma (#22C55E).
 */
@Composable
private fun ConcentricSuccessCheckIcon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(176.dp)
            .background(color = GreenSymbol.copy(alpha = 0.20f), shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(132.dp)
                .background(color = GreenSymbol.copy(alpha = 0.45f), shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .background(color = GreenSymbol, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Modo Claro")
@Composable
private fun ActionSuccessScreenLightPreview() {
    SmartfoodTheme(darkTheme = false) {
        ActionSuccessScreen(
            title = "¡Cuenta creada con éxito!",
            body = "Ya puedes acceder a todas nuestras funciones. Te enviamos un correo de confirmación.",
            buttonText = "Inicio de sesión",
            onButtonClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Modo Oscuro")
@Composable
private fun ActionSuccessScreenDarkPreview() {
    SmartfoodTheme(darkTheme = true) {
        ActionSuccessScreen(
            title = "¡Cuenta creada con éxito!",
            body = "Ya puedes acceder a todas nuestras funciones. Te enviamos un correo de confirmación.",
            buttonText = "Inicio de sesión",
            onButtonClick = {}
        )
    }
}
