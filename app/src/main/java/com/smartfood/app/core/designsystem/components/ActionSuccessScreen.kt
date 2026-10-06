package com.smartfood.app.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.Dp
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
import com.smartfood.app.core.designsystem.theme.LocalFormMetrics
import com.smartfood.app.core.designsystem.theme.LocalIsDarkTheme
import com.smartfood.app.core.designsystem.theme.OrangeMain
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.core.designsystem.theme.ThemeMode
import com.smartfood.app.core.designsystem.util.SystemBarEffect
import com.smartfood.app.core.designsystem.theme.formMetricsForWindow

/**
 * <build>
 * <b>Nombre:</b> ActionSuccessScreen.kt<br>
 * <b>Autor:</b> SmartFood Team<br>
 * <b>Fecha:</b> 24/09/2026<br>
 * <b>Descripción:</b> Componente de pantalla atómico y reutilizable para acciones exitosas en Smartfood ("Acción<br>
 * Exitosa" según los nodos 585:4564 y 223:1142 de Figma).<br>
 * <br>
 * Configura los esquemas de color exactos pedidos por el diseño:<br>
 * - Modo Claro: Fondo GreenLight (#F0FDF4), Título GreenDark (#14532D), Cuerpo OrangeMain (#FB923C).<br>
 * - Modo Oscuro: Fondo GreenDark2 (#10271C), Título #4ADE80, Cuerpo OrangeMain (#FB923C).<br>
 * - Símbolo de Verificación: Círculos concéntricos con color #22C55E. El tamaño del símbolo y los márgenes se<br>
 *   adaptan al alto de la pantalla (CA-UI-01-02). El modo lo decide el tema de la app ([LocalIsDarkTheme], claro<br>
 *   por defecto), no el del sistema (CA-UI-05-01).<br>
 * </build>
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
    val isDark = LocalIsDarkTheme.current

    val backgroundColor = if (isDark) GreenDark2 else GreenLight
    val titleColor = if (isDark) Color(0xFF4ADE80) else GreenDark
    val bodyColor = OrangeMain

    SystemBarEffect(isLightBackground = !isDark)

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val metrics = formMetricsForWindow(maxHeight)
        CompositionLocalProvider(LocalFormMetrics provides metrics) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
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
                        .padding(horizontal = 34.dp, vertical = metrics.panelPaddingBottom + metrics.sectionSpacing),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Spacer(modifier = Modifier.height(metrics.sectionSpacing))

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.widthIn(max = 380.dp)
                    ) {
                        ConcentricSuccessCheckIcon(size = metrics.successIconSize)

                        Spacer(modifier = Modifier.height(metrics.sectionSpacing + 10.dp))

                        // Figma (nodo 585:4571): Bold 35 centrado; se reduce con el perfil de medidas.
                        Text(
                            text = title,
                            fontSize = metrics.titleFontSize,
                            fontWeight = FontWeight.Bold,
                            color = titleColor,
                            textAlign = TextAlign.Center,
                            lineHeight = metrics.titleLineHeight
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
    }
}

/**
 * Símbolo de verificación verde concéntrico según especificación de Figma (#22C55E).
 */
@Composable
private fun ConcentricSuccessCheckIcon(size: Dp, modifier: Modifier = Modifier) {
    // Proporciones de Figma: anillos de 176, 132 y 88 dp y check de 48 dp.
    Box(
        modifier = modifier
            .size(size)
            .background(color = GreenSymbol.copy(alpha = 0.20f), shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size * 0.75f)
                .background(color = GreenSymbol.copy(alpha = 0.45f), shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(size * 0.5f)
                    .background(color = GreenSymbol, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.27f)
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Modo Claro")
@Composable
private fun ActionSuccessScreenLightPreview() {
    SmartfoodTheme(themeMode = ThemeMode.LIGHT) {
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
    SmartfoodTheme(themeMode = ThemeMode.DARK) {
        ActionSuccessScreen(
            title = "¡Cuenta creada con éxito!",
            body = "Ya puedes acceder a todas nuestras funciones. Te enviamos un correo de confirmación.",
            buttonText = "Inicio de sesión",
            onButtonClick = {}
        )
    }
}
