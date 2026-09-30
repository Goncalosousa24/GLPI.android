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
        val userId = PreferenceManager.getUserId(context)

        try {
            // 1. Requerente (Requester)
            if (PreferenceManager.isNotifRequerenteEnabled(context)) {
                val res = GlpiRetrofit.api.getCountRequerenteAtivos(sessionToken, appToken, userId)
                val current = if (res.isSuccessful) res.body()?.totalcount ?: 0 else PreferenceManager.getLastRequerenteCount(context).coerceAtLeast(0)
                val last = PreferenceManager.getLastRequerenteCount(context)
                
                if (res.isSuccessful && last != -1 && current > last) {
                    val novosCount = current - last
                    val intent = android.content.Intent(context, OpenTicketsActivity::class.java).apply {
                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    val pendingIntent = android.app.PendingIntent.getActivity(
                        context, 10, intent, 
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                    )
                    NotificationHelper.showNotification(
                        context,
                        "Novo Ticket como Requerente",
                        if (novosCount == 1) "Tem um novo ticket ativo como requerente." 
                        else "Tem $novosCount novos tickets ativos como requerente.",
                        pendingIntent
                    )
                }
                PreferenceManager.setLastRequerenteCount(context, current)
            }

            // 2. Observador (Observer)
            if (PreferenceManager.isNotifObservadorEnabled(context)) {
                val res = GlpiRetrofit.api.getCountObservadorAtivos(sessionToken, appToken, userId)
                val current = if (res.isSuccessful) res.body()?.totalcount ?: 0 else PreferenceManager.getLastObservadorCount(context).coerceAtLeast(0)
                val last = PreferenceManager.getLastObservadorCount(context)
                
                if (res.isSuccessful && last != -1 && current > last) {
                    val novosCount = current - last
                    val intent = android.content.Intent(context, OpenTicketsActivity::class.java).apply {
                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    val pendingIntent = android.app.PendingIntent.getActivity(
                        context, 11, intent, 
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                    )
                    NotificationHelper.showNotification(
                        context,
                        "Novo Ticket como Observador",
                        if (novosCount == 1) "Tem um novo ticket ativo como observador." 
                        else "Tem $novosCount novos tickets ativos como observador.",
                        pendingIntent
                    )
                }
                PreferenceManager.setLastObservadorCount(context, current)
            }

            // 3. Atribuído (Assigned)
            if (PreferenceManager.isNotifAtribuidoEnabled(context)) {
                val res = GlpiRetrofit.api.getCountAtribuidoAtivos(sessionToken, appToken, userId)
                val current = if (res.isSuccessful) res.body()?.totalcount ?: 0 else PreferenceManager.getLastAtribuidoCount(context).coerceAtLeast(0)
                val last = PreferenceManager.getLastAtribuidoCount(context)
                
                if (res.isSuccessful && last != -1 && current > last) {
                    val novosCount = current - last
                    val intent = android.content.Intent(context, OpenTicketsActivity::class.java).apply {
                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    val pendingIntent = android.app.PendingIntent.getActivity(
                        context, 12, intent, 
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                    )
                    NotificationHelper.showNotification(
                        context,
                        "Novo Ticket Atribuído",
                        if (novosCount == 1) "Foi-lhe atribuído um novo ticket." 
                        else "Foram-lhe atribuídos $novosCount novos tickets.",
                        pendingIntent
                    )
                }
                PreferenceManager.setLastAtribuidoCount(context, current)
            }

            // 4. Finalizado (Finished/Closed)
            if (PreferenceManager.isNotifFinalizadoEnabled(context)) {
                val cal60 = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -60) }
                val dataLimite60 = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal60.time) + " 00:00:00"
                
                val res = GlpiRetrofit.api.getCountFinalizadosRecentes(
                    sessionToken, appToken, 
                    userId, userId, userId, userId,
                    dataLimite60
                )
                val current = if (res.isSuccessful) res.body()?.totalcount ?: 0 else PreferenceManager.getLastFinalizadoCount(context).coerceAtLeast(0)
                val last = PreferenceManager.getLastFinalizadoCount(context)

                if (res.isSuccessful && last != -1 && current > last) {
                    val novosCount = current - last
                    val intent = android.content.Intent(context, ResolvedTicketsActivity::class.java).apply {
                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    val pendingIntent = android.app.PendingIntent.getActivity(
                        context, 13, intent, 
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                    )
                    NotificationHelper.showNotification(
                        context,
                        "Ticket Finalizado",
                        if (novosCount == 1) "Um dos seus tickets foi finalizado." 
                        else "Foram finalizados $novosCount tickets recentemente.",
                        pendingIntent
                    )
                }
                PreferenceManager.setLastFinalizadoCount(context, current)
            }

            return Result.success()
        } catch (e: Exception) {
            Log.e("TicketCheckWorker", "Erro ao verificar tickets em segundo plano: ${e.message}")
            return Result.retry()
        }
    }
}
