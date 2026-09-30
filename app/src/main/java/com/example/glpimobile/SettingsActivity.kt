package com.example.glpimobile

import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import android.widget.FrameLayout
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath

class SettingsActivity : AppCompatActivity() {

    private lateinit var switchDarkMode: SwitchCompat
    private lateinit var switchNotifGeral: SwitchCompat
    private lateinit var switchNotifRequerente: SwitchCompat
    private lateinit var switchNotifObservador: SwitchCompat
    private lateinit var switchNotifAtribuido: SwitchCompat
    private lateinit var switchNotifFinalizado: SwitchCompat
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
        switchNotifRequerente = findViewById(R.id.switch_notif_requerente)
        switchNotifObservador = findViewById(R.id.switch_notif_observador)
        switchNotifAtribuido = findViewById(R.id.switch_notif_atribuido)
        switchNotifFinalizado = findViewById(R.id.switch_notif_finalizado)
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
            val isSystemDark = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                               android.content.res.Configuration.UI_MODE_NIGHT_YES
            switchDarkMode.isChecked = isSystemDark
        } else {
            switchDarkMode.isChecked = (themeState == 1)
        }

        switchNotifGeral.isChecked = PreferenceManager.isNotifEnabled(this)
        switchNotifRequerente.isChecked = PreferenceManager.isNotifRequerenteEnabled(this)
        switchNotifObservador.isChecked = PreferenceManager.isNotifObservadorEnabled(this)
        switchNotifAtribuido.isChecked = PreferenceManager.isNotifAtribuidoEnabled(this)
        switchNotifFinalizado.isChecked = PreferenceManager.isNotifFinalizadoEnabled(this)

        // Desativar sub-notificações se a geral estiver desligada
        atualizarEstadoSubNotificacoes(switchNotifGeral.isChecked)
        switchNotifGeral.setOnCheckedChangeListener { _, isChecked ->
            atualizarEstadoSubNotificacoes(isChecked)
        }
    }

    private fun atualizarEstadoSubNotificacoes(enabled: Boolean) {
        switchNotifRequerente.isEnabled = enabled
        switchNotifObservador.isEnabled = enabled
        switchNotifAtribuido.isEnabled = enabled
        switchNotifFinalizado.isEnabled = enabled

        if (!enabled) {
            switchNotifRequerente.isChecked = false
            switchNotifObservador.isChecked = false
            switchNotifAtribuido.isChecked = false
            switchNotifFinalizado.isChecked = false
        }

        val alpha = if (enabled) 1.0f else 0.5f
        findViewById<View>(R.id.tv_notif_requerente).alpha = alpha
        findViewById<View>(R.id.tv_notif_observador).alpha = alpha
        findViewById<View>(R.id.tv_notif_atribuido).alpha = alpha
        findViewById<View>(R.id.tv_notif_finalizado).alpha = alpha
    }

    private fun guardarConfiguracoes() {
        val darkMode = switchDarkMode.isChecked
        val notifGeral = switchNotifGeral.isChecked
        val notifRequerente = switchNotifRequerente.isChecked
        val notifObservador = switchNotifObservador.isChecked
        val notifAtribuido = switchNotifAtribuido.isChecked
        val notifFinalizado = switchNotifFinalizado.isChecked

        // Guardar modo escuro
        PreferenceManager.setDarkModeState(this, if (darkMode) 1 else 0)

        // Aplicar Modo Escuro Imediatamente
        if (darkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        // Guardar preferências de notificação
        PreferenceManager.setNotifEnabled(this, notifGeral)
        PreferenceManager.setNotifRequerenteEnabled(this, notifRequerente)
        PreferenceManager.setNotifObservadorEnabled(this, notifObservador)
        PreferenceManager.setNotifAtribuidoEnabled(this, notifAtribuido)
        PreferenceManager.setNotifFinalizadoEnabled(this, notifFinalizado)

        AlertHelper.exibirAlertaPremium(this, "Configurações guardadas!", isError = false)
    }
}
