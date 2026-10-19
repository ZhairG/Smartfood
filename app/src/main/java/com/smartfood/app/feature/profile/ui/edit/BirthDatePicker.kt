package com.smartfood.app.feature.profile.ui.edit

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.smartfood.app.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Nombre: BirthDatePicker.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Calendario de la fecha de nacimiento (spec 008, CA-PER-02-07). Es el del inventario
 * (`ExpiryDatePicker`) al revés: solo deja elegir fechas hasta hoy.
 *
 * @param selected Fecha elegida, o `null`.
 * @param onConfirm Recibe la fecha al tocar "Aceptar".
 * @param onDismiss Cierra el calendario sin cambiar nada.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BirthDatePicker(
    selected: LocalDate?,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    // "Hoy" es la fecha del teléfono. El calendario trabaja en milisegundos UTC.
    val today = remember { LocalDate.now() }
    val selectableDates = remember(today) {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean = !utcTimeMillis.toLocalDate().isAfter(today)

            override fun isSelectableYear(year: Int): Boolean = year <= today.year
        }
    }
    val state = rememberDatePickerState(
        initialSelectedDateMillis = selected?.toUtcMillis(),
        yearRange = MIN_YEAR..today.year,
        selectableDates = selectableDates
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { state.selectedDateMillis?.let { onConfirm(it.toLocalDate()) } },
                enabled = state.selectedDateMillis != null
            ) {
                Text(text = stringResource(id = R.string.editor_date_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.editor_date_cancel))
            }
        }
    ) {
        DatePicker(state = state)
    }
}

private const val MIN_YEAR = 1900

private fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
