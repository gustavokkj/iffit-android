package com.ifpr.androidapptemplate.ui.firebase

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.ifpr.androidapptemplate.MainActivity
import com.ifpr.androidapptemplate.R

class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.data["title"] ?: message.notification?.title ?: getString(R.string.app_name)
        val body = message.data["body"] ?: message.notification?.body ?: return
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channel = "iffit_atividades"
        if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(
            NotificationChannel(channel, getString(R.string.firebase_atividades_e_novidades), NotificationManager.IMPORTANCE_DEFAULT)
        )
        if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) return
        val intent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pending = PendingIntent.getActivity(this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(this, channel)
            .setSmallIcon(R.drawable.ic_notifications_black_24dp)
            .setContentTitle(title).setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true).setContentIntent(pending).build()
        try { manager.notify((message.messageId ?: body).hashCode(), notification) }
        catch (_: SecurityException) { /* Permission can be revoked between the check and notify. */ }
    }
}
