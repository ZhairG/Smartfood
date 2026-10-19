package com.smartfood.app.feature.profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import coil3.compose.AsyncImage
import com.smartfood.app.R
import com.smartfood.app.core.designsystem.theme.GreenSoft

/**
 * Nombre: ProfileAvatar.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Círculo del perfil con la foto o las iniciales y la insignia naranja de la cámara (spec 008, Figma
 * `306:369` y `361:649`). Lo usan el Perfil y "Editar información"; tocarlo abre la hoja "Agregar foto"
 * (CA-PER-02-08).
 *
 * @param photo Foto a mostrar (ruta del teléfono o URL), o `null` para las iniciales.
 * @param initials Iniciales que se ven sin foto.
 * @param size Diámetro del círculo.
 * @param initialsSize Tamaño de las iniciales.
 * @param badgeBorder Color del aro de la insignia: verde oscuro en "Editar información" y verde claro en el Perfil.
 * @param contentDescription Nombre que anuncia TalkBack.
 * @param onClick Acción al tocar, o `null` si no se puede tocar.
 * @param modifier Modificador opcional.
 */
@Composable
fun ProfileAvatar(
    photo: Any?,
    initials: String,
    size: Dp,
    initialsSize: TextUnit,
    badgeBorder: Color,
    contentDescription: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val badge = size * BadgeFraction
    Box(
        modifier = modifier
            .size(size)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .semantics { this.contentDescription = contentDescription }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                // Figma 362:727: #DCFCE7; primaryContainer es el mismo verde del fondo y el círculo no se vería.
                .background(GreenSoft)
        ) {
            if (photo != null) {
                AsyncImage(
                    model = photo,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(size)
                )
            } else {
                Text(
                    text = initials,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = initialsSize,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
        // Figma 362:729: aro de 30 px con el círculo naranja de 25 px y la cámara blanca de 13 px.
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                // Figma: la insignia empieza en x = 60 de 100, así que queda 10 px antes del borde derecho.
                .offset(x = -size * BadgeInsetFraction)
                .size(badge)
                .background(MaterialTheme.colorScheme.secondary, CircleShape)
                .border(badge * BadgeRingFraction, badgeBorder, CircleShape)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_figma_camera),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondary,
                modifier = Modifier.size(badge * CameraIconFraction)
            )
        }
    }
}

// Proporciones del Figma (362:726): insignia de 30 px en un círculo de 100, aro de 2,5 px y cámara de 13 px.
private const val BadgeFraction = 0.3f
private const val BadgeInsetFraction = 0.1f
private const val BadgeRingFraction = 2.5f / 30f
private const val CameraIconFraction = 13f / 30f
