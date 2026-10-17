package com.smartfood.app.feature.profile

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartfood.app.R
import com.smartfood.app.core.designsystem.components.SmartFoodConfirmDialog
import com.smartfood.app.core.designsystem.theme.GreenCheck
import com.smartfood.app.core.designsystem.theme.SmartfoodTheme
import com.smartfood.app.core.designsystem.theme.spacing
import com.smartfood.app.core.designsystem.util.SystemBarEffect

/**
 * Nombre: ProfileScreen.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Pestaña Perfil (spec 001 HU-05; spec 006 CA-INV-09-03; Figma "Perfil" `306:369`). Encabezado verde
 * con el avatar de iniciales, el nombre y el correo; tres tarjetas de cifras; las opciones de la cuenta y "Cerrar
 * Sesión". Por ahora solo "Cerrar Sesión" funciona: las opciones y la cámara del avatar se ven deshabilitadas, y de
 * las cifras solo "Ingredientes en inventario" es real (spec 006 §8).
 *
 * Adaptable (spec 002): las medidas del Figma (412 dp de ancho) se escalan al ancho real, el encabezado se compacta
 * en pantallas bajas y "Cerrar Sesión" queda siempre visible abajo; las tarjetas y opciones se desplazan si no caben.
 *
 * @param state Estado de la pantalla.
 * @param onEvent Recibe lo que hace el usuario.
 * @param modifier Modificador opcional.
 */
@Composable
fun ProfileScreen(
    state: ProfileUiState,
    onEvent: (ProfileEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    // El encabezado es verde oscuro: la barra de estado lleva íconos claros.
    SystemBarEffect(isLightBackground = false)

    // Cambios sin subir al cerrar sesión (CA-INV-10-07, T051).
    if (state.dialog == ProfileDialog.ConfirmLogoutWithPending) {
        SmartFoodConfirmDialog(
            icon = {
                Icon(
                    imageVector = Icons.Outlined.CloudUpload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(MaterialTheme.spacing.iconDialogSize * DialogGlyphFraction)
                )
            },
            question = stringResource(id = R.string.profile_logout_pending_question),
            confirmText = stringResource(id = R.string.profile_logout_pending_confirm),
            dismissText = stringResource(id = R.string.profile_logout_pending_cancel),
            onConfirm = { onEvent(ProfileEvent.LogoutConfirmed) },
            onDismiss = { onEvent(ProfileEvent.DialogDismissed) }
        )
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        val dimens = profileDimens(maxWidth, maxHeight)

        Column(modifier = Modifier.fillMaxSize()) {
            ProfileHeader(state = state, dimens = dimens)

            Column(
                verticalArrangement = Arrangement.spacedBy(dimens.gap),
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = dimens.side, vertical = dimens.gap)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(dimens.statGap), modifier = Modifier.fillMaxWidth()) {
                    StatCard(
                        value = stringResource(id = R.string.profile_stat_recipes_value),
                        label = stringResource(id = R.string.profile_stat_recipes),
                        dimens = dimens,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        value = state.inventoryCount.toString(),
                        label = stringResource(id = R.string.profile_stat_ingredients),
                        dimens = dimens,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        value = stringResource(id = R.string.profile_stat_unavailable),
                        label = stringResource(id = R.string.profile_stat_savings),
                        dimens = dimens,
                        modifier = Modifier.weight(1f)
                    )
                }
                OptionsCard(dimens = dimens)
            }

            // "Cerrar Sesión" queda fijo abajo, siempre visible, como en el Figma (CA-05-03).
            OutlinedButton(
                onClick = { onEvent(ProfileEvent.LogoutClicked) },
                shape = RoundedCornerShape(MaterialTheme.spacing.buttonRadius),
                border = BorderStroke(LogoutBorder, MaterialTheme.colorScheme.secondary),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = dimens.side, end = dimens.side, bottom = dimens.gap)
                    .height(dimens.logoutHeight)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_figma_logout),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(dimens.logoutIcon)
                )
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.medium))
                Text(
                    text = stringResource(id = R.string.action_logout),
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = dimens.logoutText,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}

/** Encabezado verde con el avatar, el nombre y el correo. */
@Composable
private fun ProfileHeader(state: ProfileUiState, dimens: ProfileDimens) {
    val name = state.fullName ?: stringResource(id = R.string.profile_name_generic)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(top = dimens.headerPadding, bottom = dimens.headerPadding)
    ) {
        Box(modifier = Modifier.size(dimens.avatar)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(dimens.avatar)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                    .semantics { contentDescription = name }
            ) {
                Text(
                    text = state.initials.orEmpty(),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = dimens.initialsText,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            // Cambiar la foto todavía no existe: el botón se ve, pero está deshabilitado.
            val changePhoto = stringResource(id = R.string.cd_profile_change_photo)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(dimens.cameraBadge)
                    .background(MaterialTheme.colorScheme.secondary, CircleShape)
                    .border(CameraBadgeBorder, MaterialTheme.colorScheme.primaryContainer, CircleShape)
                    .clickable(enabled = false, role = Role.Button) {}
                    .semantics { contentDescription = changePhoto }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_figma_camera),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.size(dimens.cameraBadge * CameraIconFraction)
                )
            }
        }
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
        Text(
            text = name,
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = dimens.nameText, fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = dimens.side)
        )
        state.email?.let { email ->
            Text(
                text = email,
                color = MaterialTheme.colorScheme.primaryContainer,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = dimens.emailText,
                    fontWeight = FontWeight.Medium
                ),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = dimens.side)
            )
        }
    }
}

/** Tarjeta de una cifra (Figma: 105 × 93, cifra en verde y texto gris). */
@Composable
private fun StatCard(value: String, label: String, dimens: ProfileDimens, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .heightIn(min = dimens.statHeight)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(dimens.cardRadius))
            .border(CardBorder, MaterialTheme.colorScheme.outline, RoundedCornerShape(dimens.cardRadius))
            .padding(horizontal = MaterialTheme.spacing.extraSmall, vertical = MaterialTheme.spacing.small)
    ) {
        Text(
            text = value,
            color = GreenCheck,
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = dimens.statValueText, fontWeight = FontWeight.Bold)
        )
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = dimens.statLabelText, fontWeight = FontWeight.Medium),
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

/** Opciones de la cuenta, separadas por líneas. Todavía no tienen pantalla: se ven deshabilitadas. */
@Composable
private fun OptionsCard(dimens: ProfileDimens) {
    val options = listOf(
        R.drawable.ic_figma_profile_edit to R.string.profile_option_edit,
        R.drawable.ic_figma_profile_preferences to R.string.profile_option_preferences,
        R.drawable.ic_figma_profile_notifications to R.string.profile_option_notifications,
        R.drawable.ic_figma_profile_security to R.string.profile_option_password,
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(dimens.cardRadius))
            .border(CardBorder, MaterialTheme.colorScheme.outline, RoundedCornerShape(dimens.cardRadius))
    ) {
        options.forEachIndexed { index, (icon, textRes) ->
            OptionRow(icon = icon, text = stringResource(id = textRes), dimens = dimens)
            if (index < options.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun OptionRow(@DrawableRes icon: Int, text: String, dimens: ProfileDimens) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = dimens.optionHeight)
            .clickable(enabled = false, role = Role.Button) {}
            .padding(horizontal = dimens.optionSide)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(dimens.optionIconBox)
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(OptionIconRadius))
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                tint = GreenCheck,
                modifier = Modifier.size(dimens.optionIconBox * OptionIconFraction)
            )
        }
        Spacer(modifier = Modifier.width(dimens.optionSide))
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = dimens.optionText, fontWeight = FontWeight.Medium),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Icon(
            painter = painterResource(id = R.drawable.ic_figma_chevron_right),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(dimens.chevron)
        )
    }
}

/**
 * Medidas del Perfil escaladas al tamaño real. El Figma mide 412 × 917 dp; en pantallas más angostas todo se reduce
 * en la misma proporción (sin bajar del 80 %) y, si el alto es menor que el del Figma, el encabezado se compacta.
 */
@Immutable
private data class ProfileDimens(
    val side: Dp,
    val gap: Dp,
    val headerPadding: Dp,
    val avatar: Dp,
    val cameraBadge: Dp,
    val initialsText: TextUnit,
    val nameText: TextUnit,
    val emailText: TextUnit,
    val statGap: Dp,
    val statHeight: Dp,
    val statValueText: TextUnit,
    val statLabelText: TextUnit,
    val cardRadius: Dp,
    val optionHeight: Dp,
    val optionSide: Dp,
    val optionIconBox: Dp,
    val optionText: TextUnit,
    val logoutHeight: Dp,
    val logoutText: TextUnit,
    val logoutIcon: Dp,
    val chevron: Dp,
)

private fun profileDimens(width: Dp, height: Dp): ProfileDimens {
    val scale = (width / FigmaWidth).coerceIn(MinScale, 1f)
    // En pantallas más bajas que el Figma, el encabezado y los espacios verticales se reducen.
    val compact = (height / FigmaHeight).coerceIn(MinScale, 1f)
    return ProfileDimens(
        side = 28.dp * scale,
        gap = 20.dp * scale * compact,
        headerPadding = 24.dp * compact,
        avatar = 100.dp * scale * compact,
        cameraBadge = 30.dp * scale,
        initialsText = 35.sp * scale * compact,
        nameText = 25.sp * scale,
        emailText = 18.sp * scale,
        statGap = 21.dp * scale,
        statHeight = 93.dp * scale * compact,
        statValueText = 25.sp * scale,
        statLabelText = 12.sp * scale,
        cardRadius = 25.dp * scale,
        optionHeight = 61.dp * scale * compact,
        optionSide = 21.dp * scale,
        optionIconBox = 35.dp * scale,
        optionText = 15.sp * scale,
        logoutHeight = 48.dp * scale,
        logoutText = 18.sp * scale,
        logoutIcon = 22.dp * scale,
        chevron = 15.dp * scale,
    )
}

// Medidas del Figma "Perfil" (306:369). No son de formulario, así que no van en FormMetrics.
private val FigmaWidth = 412.dp
private val FigmaHeight = 917.dp
private const val MinScale = 0.8f
private val CameraBadgeBorder = 2.dp
private const val CameraIconFraction = 0.55f
private val CardBorder = 1.dp
private val OptionIconRadius = 7.dp
private const val OptionIconFraction = 0.65f
private val LogoutBorder = 1.dp

@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun ProfileScreenPreview() {
    SmartfoodTheme {
        ProfileScreen(
            state = ProfileUiState(
                isLoading = false,
                firstName = "Pepito",
                fullName = "Pepito Perez",
                email = "pepitoperez@gmail.com",
                initials = "PP",
                inventoryCount = 34
            ),
            onEvent = {}
        )
    }
}

/** El glifo del aviso ocupa el 40 % del círculo, como en los demás diálogos (T025). */
private const val DialogGlyphFraction = 0.4f
