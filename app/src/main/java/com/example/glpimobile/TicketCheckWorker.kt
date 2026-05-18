package com.example.glpimobile

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.text.SimpleDateFormat
import java.util.*

class TicketCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext

        if (!PreferenceManager.isNotifEnabled(context)) {
            return Result.success()
        }

        val sessionToken = GlpiConfig.SESSION_TOKEN
        val appToken = GlpiConfig.APP_TOKEN

        try {
            // 1. Verificar TICKETS ABERTOS
            if (PreferenceManager.isNotifOpenEnabled(context)) {
                val resAbertos = GlpiRetrofit.api.getCountAbertosTotal(sessionToken, appToken)
                val currentAbertos = if (resAbertos.isSuccessful) resAbertos.body()?.totalcount ?: 0 else PreferenceManager.getLastOpenCount(context).coerceAtLeast(0)
                val lastAbertos = PreferenceManager.getLastOpenCount(context)
                
                if (resAbertos.isSuccessful && lastAbertos != -1 && currentAbertos > lastAbertos) {
                    val novosCount = currentAbertos - lastAbertos
                    
                    // Logic for fetching latest ticket ID removed for performance as it's no longer used for auto-expansion.

                    val intent = android.content.Intent(context, OpenTicketsActivity::class.java).apply {
                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    val pendingIntent = android.app.PendingIntent.getActivity(
                        context, 0, intent, 
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                    )

                    NotificationHelper.showNotification(
                        context,
                        "Novo Ticket Aberto",
                        if (novosCount == 1) "Um novo ticket foi aberto no seu GLPI." 
                        else "Existem $novosCount novos tickets abertos no seu GLPI.",
                        pendingIntent
                    )
                }
                PreferenceManager.setLastOpenCount(context, currentAbertos)
            }

            // 2. Verificar TICKETS RESOLVIDOS (últimos 60 dias)
            if (PreferenceManager.isNotifClosedEnabled(context)) {
                val cal60 = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -60) }
                val dataLimite60 = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal60.time) + " 00:00:00"
                
                val userId = PreferenceManager.getUserId(context)
                val loginBusca = GlpiConfig.USER_NAME.split(".", " ").firstOrNull() ?: ""
                val nomeCompleto = GlpiConfig.USER_FULL_NAME
                
                val resResolvidos = GlpiRetrofit.api.getCountResolvidosRecentes(
                    sessionToken, appToken, 
                    userId, userId, userId, userId, userId, userId,
                    loginBusca, nomeCompleto.ifEmpty { loginBusca },
                    dataLimite60
                )
                
                val currentResolvidos = if (resResolvidos.isSuccessful) resResolvidos.body()?.totalcount ?: 0 else PreferenceManager.getLastClosedCount(context).coerceAtLeast(0)
                val lastResolvidos = PreferenceManager.getLastClosedCount(context)

                if (resResolvidos.isSuccessful && lastResolvidos != -1 && currentResolvidos > lastResolvidos) {
                    val novosCount = currentResolvidos - lastResolvidos

                    // Logic for fetching latest ticket ID removed for performance.

                    val intent = android.content.Intent(context, ResolvedTicketsActivity::class.java).apply {
                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    val pendingIntent = android.app.PendingIntent.getActivity(
                        context, 1, intent, 
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                    )

                    NotificationHelper.showNotification(
                        context,
                        "Ticket Resolvido",
                        if (novosCount == 1) "Um dos seus tickets foi resolvido." 
                        else "Foram resolvidos $novosCount tickets recentemente.",
                        pendingIntent
                    )
                }
                PreferenceManager.setLastClosedCount(context, currentResolvidos)
            }

            return Result.success()
        } catch (e: Exception) {
            Log.e("TicketCheckWorker", "Erro ao verificar tickets em segundo plano: ${e.message}")
            return Result.retry()
        }
    }
}
