package com.example.glpimobile

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatDelegate

class GlpiApp : Application() {
    companion object {
        private var _instance: GlpiApp? = null
        val instance: GlpiApp
            get() = _instance ?: throw IllegalStateException("GlpiApp not initialized yet")
    }

    override fun onCreate() {
        super.onCreate()
        _instance = this
        
        // Aplicar Modo Escuro inteligente: -1 (Seguir Sistema), 0 (Forçar Claro), 1 (Forçar Escuro)
        when (PreferenceManager.getDarkModeState(this)) {
            1 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            0 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }

        // Registrar callbacks para aplicar a dispensa automática do teclado em todas as atividades
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {
                val root = activity.findViewById<View>(android.R.id.content)
                root?.post {
                    KeyboardHelper.setupDismissKeyboardOnScroll(root, activity)
                }
            }
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }
}

