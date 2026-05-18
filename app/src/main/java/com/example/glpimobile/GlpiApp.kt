package com.example.glpimobile

import android.app.Application

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
    }
}

