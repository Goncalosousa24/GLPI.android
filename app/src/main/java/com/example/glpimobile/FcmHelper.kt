package com.example.glpimobile

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object FcmHelper {

    fun enviarTokenParaServidor(context: Context, token: String) {
        val userId = GlpiConfig.USER_ID.toString()
        val sessionToken = GlpiConfig.SESSION_TOKEN
        val appToken = GlpiConfig.APP_TOKEN

        if (sessionToken.isEmpty()) {
            Log.e("FCM", "Não é possível registar o token: Session-Token vazio.")
            return
        }

        val tokenData = mapOf(
            "users_id" to userId,
            "fcm_token" to token,
            "platform" to "android"
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = GlpiRetrofit.api.registerFcmToken(sessionToken, appToken, tokenData)
                if (response.isSuccessful) {
                    Log.d("FCM", "Token registado no servidor com sucesso!")
                } else {
                    Log.e("FCM", "Erro ao registar token: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("FCM", "Erro de rede ao registar token: ${e.message}")
            }
        }
    }
}
