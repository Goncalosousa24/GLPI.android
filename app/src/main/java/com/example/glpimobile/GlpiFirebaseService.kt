package com.example.glpimobile

import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class GlpiFirebaseService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d("FCM", "Mensagem recebida de: ${remoteMessage.from}")

        // 1. Verificar se as notificações globais estão ativas
        if (!PreferenceManager.isNotifEnabled(this)) return

        // 2. Extrair dados da mensagem (Data payload ou Notification payload)
        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "GLPI Mobile"
        val body = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: ""
        val type = remoteMessage.data["ticket_type"] // "open" ou "closed"

        // 3. Filtrar pelas preferências do utilizador
        if (type == "open" && !PreferenceManager.isNotifOpenEnabled(this)) return
        if (type == "closed" && !PreferenceManager.isNotifClosedEnabled(this)) return

        // 4. Mostrar a notificação
        val intent = Intent(this, DashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        NotificationHelper.showNotification(this, title, body, pendingIntent)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "Novo Token de Dispositivo: $token")
        
        // Enviar o novo token para o servidor GLPI automaticamente
        FcmHelper.enviarTokenParaServidor(this, token)
    }
}
