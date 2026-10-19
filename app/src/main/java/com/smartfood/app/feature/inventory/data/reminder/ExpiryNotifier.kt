package com.smartfood.app.feature.inventory.data.reminder

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.smartfood.app.MainActivity
import com.smartfood.app.R
import com.smartfood.app.feature.inventory.domain.model.InventoryItem
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Nombre: ExpiryNotifier.kt
 * Autor: SmartFood Team
 * Fecha: Octubre 2026
 * Descripción: Publica y descarta la notificación del sistema con los productos por vencer (HU-INV-08, plan §6).
 * Crea el canal "Vencimientos" (API 26+) y arma el texto con plurales, nombrando el producto más próximo
 * (CA-INV-08-02). Si el permiso `POST_NOTIFICATIONS` no está concedido no publica nada y la app sigue funcionando
 * (RNF-INV-07). No es una pantalla de la app: vive en la bandeja del sistema.
 *
 * @param context Contexto de la aplicación, para recursos, el canal y el servicio de notificaciones.
 */
@Singleton
class ExpiryNotifier @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Crea el canal y publica el aviso de los [items] por vencer (CA-INV-08-01, 08-02). Si la lista está vacía,
     * descarta cualquier aviso anterior en lugar de publicar (CA-INV-08-03).
     */
    fun notifyExpiring(items: List<InventoryItem>) {
        if (items.isEmpty()) {
            cancel()
            return
        }
        if (!canPostNotifications()) return
        createChannel()
        val body = context.resources.getQuantityString(
            R.plurals.notification_expiry_body,
            items.size,
            items.first().name,
            items.size - 1
        )
        publish(
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_expiry)
                .setContentTitle(context.getString(R.string.notification_expiry_title))
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(openApp())
                .setAutoCancel(true)
                .build()
        )
    }

    /**
     * Crea el canal y publica el aviso de inventario vacío (spec 008, CA-PER-04-04). Usa el mismo canal, icono y
     * `PendingIntent` que el aviso de vencimientos: cuando el inventario está vacío no puede haber productos por
     * vencer, así que ambos avisos nunca se muestran a la vez.
     */
    fun notifyEmptyInventory() {
        if (!canPostNotifications()) return
        createChannel()
        val body = context.getString(R.string.notification_empty_inventory_body)
        publish(
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_expiry)
                .setContentTitle(context.getString(R.string.notification_empty_inventory_title))
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(openApp())
                .setAutoCancel(true)
                .build()
        )
    }

    /** Descarta la notificación visible, por ejemplo al cerrar sesión (CA-INV-08-06). */
    fun cancel() {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    /** Sin permiso en API 33+ no se publica nada, pero el inventario funciona igual (RNF-INV-07). */
    private fun canPostNotifications(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    /** Solo se llega aquí con el permiso concedido o en API < 33, por eso se silencia el aviso de lint. */
    @SuppressLint("MissingPermission", "NotificationPermission")
    private fun publish(notification: Notification) {
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_expiry_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_expiry_description)
        }
        NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }

    /**
     * Abre [MainActivity] con la pila limpia: la Splash decide si va a Mi Inventario o al inicio de sesión, según
     * siga habiendo sesión (CA-INV-08-04).
     */
    private fun openApp(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        return PendingIntent.getActivity(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private companion object {
        /** Canal de los avisos de vencimiento (plan §6). */
        const val CHANNEL_ID = "expiry_reminders"

        /** Identificador fijo: el aviso se reemplaza cada día y `cancel()` descarta ese mismo (CA-INV-08-06). */
        const val NOTIFICATION_ID = 1001

        const val REQUEST_CODE = 0
    }
}
