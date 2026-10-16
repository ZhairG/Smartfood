/**
 * Nombre: ExpiryDatePicker.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Calendario de la fecha de vencimiento (spec 006, CA-INV-02-04 y CA-INV-03-03). No deja elegir días
 * anteriores a hoy, salvo la fecha que el producto ya tenía guardada.
 *
 * @param selected Fecha vigente, o `null` si todavía no hay.
 * @param original Fecha guardada del producto que se edita; se puede conservar aunque ya pasó.
 * @param onConfirm Recibe la fecha elegida.
 * @param onDismiss Cierra el calendario sin cambios.
 */
package com.smartfood.app.feature.inventory.ui.components

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpiryDatePicker(
    selected: LocalDate?,
    original: LocalDate?,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    // "Hoy" es la fecha del teléfono (spec §9). El calendario trabaja en milisegundos UTC.
    val today = remember { LocalDate.now() }
    val selectableDates = remember(today, original) {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val date = utcTimeMillis.toLocalDate()
                return !date.isBefore(today) || date == original
            }

            override fun isSelectableYear(year: Int): Boolean =
                year >= today.year || year == original?.year
        }
    }
    val state = rememberDatePickerState(
        initialSelectedDateMillis = selected?.toUtcMillis(),
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

private fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
