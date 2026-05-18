package com.example.glpimobile

import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath

class SettingsActivity : AppCompatActivity() {

    private lateinit var switchDarkMode: SwitchCompat
    private lateinit var switchNotifGeral: SwitchCompat
    private lateinit var switchNotifAbertos: SwitchCompat
    private lateinit var switchNotifEncerrados: SwitchCompat
    private lateinit var btnGuardar: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Configurar Seta de Voltar
        val lottieSeta = findViewById<LottieAnimationView>(R.id.lottie_seta_settings)
        val corBranca = ContextCompat.getColor(this, android.R.color.white)
        lottieSeta?.addValueCallback(KeyPath("**"), LottieProperty.COLOR_FILTER) {
            PorterDuffColorFilter(corBranca, PorterDuff.Mode.SRC_ATOP)
        }

        findViewById<FrameLayout>(R.id.btn_voltar_settings)?.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }

        // Inicializar Views
        switchDarkMode = findViewById(R.id.switch_dark_mode)
        
        switchNotifGeral = findViewById(R.id.switch_notif_geral)
        switchNotifAbertos = findViewById(R.id.switch_notif_abertos)
        switchNotifEncerrados = findViewById(R.id.switch_notif_encerrados)
        btnGuardar = findViewById(R.id.btn_guardar_settings)

        // Carregar Valores Atuais
        carregarConfiguracoes()

        // Botão Guardar
        btnGuardar.setOnClickListener {
            guardarConfiguracoes()
        }
    }

    private fun carregarConfiguracoes() {
        val themeState = PreferenceManager.getDarkModeState(this)
        
        if (themeState == -1) {
            // Se nunca foi definido, mostramos o estado atual do sistema
            val isSystemDark = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == 
                               android.content.res.Configuration.UI_MODE_NIGHT_YES
            switchDarkMode.isChecked = isSystemDark
        } else {
            // Se já foi definido manualmente, seguimos o valor guardado
            switchDarkMode.isChecked = (themeState == 1)
        }
        
        switchNotifGeral.isChecked = PreferenceManager.isNotifEnabled(this)
        switchNotifAbertos.isChecked = PreferenceManager.isNotifOpenEnabled(this)
        switchNotifEncerrados.isChecked = PreferenceManager.isNotifClosedEnabled(this)
        // Notificações ativas ou inativas
        
        // Desativar sub-notificações se a geral estiver desligada
        atualizarEstadoSubNotificacoes(switchNotifGeral.isChecked)
        switchNotifGeral.setOnCheckedChangeListener { _, isChecked ->
            atualizarEstadoSubNotificacoes(isChecked)
        }
    }

    private fun atualizarEstadoSubNotificacoes(enabled: Boolean) {
        switchNotifAbertos.isEnabled = enabled
        switchNotifEncerrados.isEnabled = enabled
        
        // Sincronizar o estado de check
        switchNotifAbertos.isChecked = enabled
        switchNotifEncerrados.isChecked = enabled

        val alpha = if (enabled) 1.0f else 0.5f
        findViewById<View>(R.id.tv_notif_abertos).alpha = alpha
        findViewById<View>(R.id.tv_notif_encerrados).alpha = alpha
    }

    private fun guardarConfiguracoes() {
        val darkMode = switchDarkMode.isChecked
        
        val notifGeral = switchNotifGeral.isChecked
        val notifAbertos = switchNotifAbertos.isChecked
        val notifEncerrados = switchNotifEncerrados.isChecked
        // Guardar Alterações
 
        // Guardar Alterações (Transformamos em 1 para Dark ou 0 para Light, tornando-o independente)
        PreferenceManager.setDarkModeState(this, if (darkMode) 1 else 0)
        
        // Aplicar Modo Escuro Imediatamente
        if (darkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
        
        PreferenceManager.setNotifEnabled(this, notifGeral)
        PreferenceManager.setNotifOpenEnabled(this, notifAbertos)
        PreferenceManager.setNotifClosedEnabled(this, notifEncerrados)
        // Sincronização concluída
 
        AlertHelper.exibirAlertaPremium(this, "Configurações guardadas!", isError = false)
        
        // Pequeno atraso para o sistema processar a mudança de tema antes de voltar
        btnGuardar.postDelayed({
            finish()
        }, 500)
    }
}
