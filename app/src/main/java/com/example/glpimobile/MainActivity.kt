package com.example.glpimobile

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import android.widget.ImageView
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // 🔥 Forçar o tema da APP antes de carregar o Splash do sistema
        when (PreferenceManager.getDarkModeState(this)) {
            1 -> androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES)
            0 -> androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO)
            else -> androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }

        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // --- AUTOLOGIN / CACHED SESSION CHECK (Temporariamente desativado para testar o ecrã de login) ---
        /*
        val sessionToken = PreferenceManager.getSessionToken(this)
        if (sessionToken.isNotEmpty()) {
            irParaDashboard()
            return
        }
        */

        // --- VERIFICAÇÃO BIOMÉTRICA ---
        if (PreferenceManager.isBiometricEnabled(this)) {
            BiometricHelper.showBiometricPrompt(
                activity = this,
                onSuccess = { irParaDashboard() },
                onError = { /* Continua no login manual */ }
            )
        }

        val etUsername = findViewById<EditText>(R.id.et_username)
        val etPassword = findViewById<EditText>(R.id.et_password)
        val tvLogo = findViewById<android.widget.TextView>(R.id.tv_glpi_logo)
        val btnEntrar = findViewById<Button>(R.id.btn_entrar)
        val lottieCarregar = findViewById<com.airbnb.lottie.LottieAnimationView>(R.id.lottie_carregar)
        val ivToggle = findViewById<ImageView>(R.id.iv_password_toggle)
        
        var isPasswordVisible = false
        ivToggle?.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                etPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
                ivToggle.setImageResource(R.drawable.ic_visibility_on)
            } else {
                etPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                ivToggle.setImageResource(R.drawable.ic_visibility_off)
            }
            etPassword.setSelection(etPassword.text.length)
        }
        
        // Aplicar cor azul à animação de carregamento
        lottieCarregar?.setAnimation(R.raw.loading)
        lottieCarregar?.addValueCallback(
            com.airbnb.lottie.model.KeyPath("**"),
            com.airbnb.lottie.LottieProperty.COLOR_FILTER
        ) { com.airbnb.lottie.SimpleColorFilter(androidx.core.content.ContextCompat.getColor(this, R.color.azul_glpi)) }
        
        tvLogo?.let { aplicarGradienteLogo(it) }

        etUsername.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                etUsername.setBackgroundResource(R.drawable.bg_caixa_texto_branca)
                btnEntrar?.isEnabled = true
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        etPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                etPassword.setBackgroundResource(R.drawable.bg_caixa_texto_branca)
                btnEntrar?.isEnabled = true
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnEntrar?.setOnClickListener {
            val user = etUsername.text.toString().trim()
            val pass = etPassword.text.toString().trim()

            if (user.isEmpty() || pass.isEmpty()) {
                AlertHelper.exibirAlertaPremium(this, "Por favor, preencha as credenciais.", isError = true)
                return@setOnClickListener
            }

            executarLogin(user, pass, etUsername, etPassword, btnEntrar, lottieCarregar)
        }


    }



    private fun aplicarGradienteLogo(textView: android.widget.TextView) {
        val paint = textView.paint
        val width = paint.measureText(textView.text.toString())
        
        val textShader: android.graphics.Shader = android.graphics.LinearGradient(
            0f, 0f, width, 0f,
            intArrayOf(
                androidx.core.content.ContextCompat.getColor(this, R.color.azul_glpi_light),
                androidx.core.content.ContextCompat.getColor(this, R.color.azul_glpi),
                androidx.core.content.ContextCompat.getColor(this, R.color.azul_glpi_dark)
            ), null, android.graphics.Shader.TileMode.CLAMP
        )
        textView.paint.shader = textShader
        textView.invalidate()
    }

    private fun executarLogin(user: String, pass: String, etUsername: EditText, etPassword: EditText, btnEntrar: Button?, lottieCarregar: com.airbnb.lottie.LottieAnimationView?) {
        val authHeader = "Basic " + Base64.encodeToString("$user:$pass".toByteArray(), Base64.NO_WRAP)
        val appToken = GlpiConfig.APP_TOKEN

        CoroutineScope(Dispatchers.Main).launch {
            btnEntrar?.isEnabled = false
            lottieCarregar?.visibility = android.view.View.VISIBLE

            // 🔥 SE MODO OFFLINE ATIVO, SALTAMOS A API 🔥
            if (GlpiConfig.OFFLINE_MODE) {
                delay(800) // Simular um pequeno delay de carregamento
                PreferenceManager.setSessionToken(this@MainActivity, "mock_token")
                PreferenceManager.setUserId(this@MainActivity, 1)
                PreferenceManager.setUserProfile(this@MainActivity, "Admin (Offline)")
                irParaDashboard(showWelcome = true)
                return@launch
            }

            try {
                val response = withContext(Dispatchers.IO) {
                    GlpiRetrofit.api.initSession(authHeader, appToken)
                }

                if (response.isSuccessful && response.body() != null) {
                    val sessionToken = response.body()!!.session_token

                    val sessionDetails = withContext(Dispatchers.IO) {
                        GlpiRetrofit.api.getFullSession(sessionToken, appToken)
                    }

                    if (sessionDetails.isSuccessful && sessionDetails.body() != null) {
                        val body = sessionDetails.body()!!
                        val sessionMap = body["session"] as? Map<*, *>
                        val glpiId = (sessionMap?.get("glpiID") as? Double)?.toInt()
                            ?: (sessionMap?.get("glpiID") as? Int)
                            ?: (sessionMap?.get("glpi_id") as? Double)?.toInt()
                            ?: (sessionMap?.get("glpi_id") as? Int) ?: 0

                        val glpiName = sessionMap?.get("glpiname")?.toString() ?: user

                        PreferenceManager.setSessionToken(this@MainActivity, sessionToken)
                        if (glpiId > 0) {
                            PreferenceManager.setUserId(this@MainActivity, glpiId)
                        }
                        PreferenceManager.setUserName(this@MainActivity, glpiName)

                        val activeProfile = sessionMap?.get("glpiactiveprofile") as? Map<*, *>
                        val profileName = activeProfile?.get("name")?.toString() ?: ""
                        PreferenceManager.setUserProfile(this@MainActivity, profileName)

                        irParaDashboard(showWelcome = true)
                    } else {
                        throw Exception("Erro ao obter detalhes da sessão")
                    }
                } else {
                    btnEntrar?.isEnabled = false
                    lottieCarregar?.visibility = android.view.View.GONE
                    etUsername.setBackgroundResource(R.drawable.bg_caixa_texto_erro)
                    etPassword.setBackgroundResource(R.drawable.bg_caixa_texto_erro)
                    AlertHelper.exibirAlertaPremium(this@MainActivity, "Credenciais inválidas.", isError = true)
                }
            } catch (e: Exception) {
                btnEntrar?.isEnabled = false
                lottieCarregar?.visibility = android.view.View.GONE
                AlertHelper.exibirAlertaPremium(this@MainActivity, "Erro de ligação: ${e.message}", isError = true)
            }
        }
    }

    private fun irParaDashboard(showWelcome: Boolean = false) {
        val intent = Intent(this, DashboardActivity::class.java)
        if (showWelcome) intent.putExtra("SHOW_WELCOME", true)
        startActivity(intent)
        finish()
    }

    override fun dispatchTouchEvent(ev: android.view.MotionEvent?): Boolean {
        KeyboardHelper.handleTouchOutside(this, ev)
        return super.dispatchTouchEvent(ev)
    }
}