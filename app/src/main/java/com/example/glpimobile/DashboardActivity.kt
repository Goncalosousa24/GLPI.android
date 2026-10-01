package com.example.glpimobile

import android.animation.ValueAnimator
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import android.view.animation.AccelerateDecelerateInterpolator
import com.google.firebase.messaging.FirebaseMessaging
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class DashboardActivity : AppCompatActivity() {

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var rvResultadosPesquisa: RecyclerView
    private lateinit var rvAtividades: RecyclerView
    private lateinit var drawerLayout: DrawerLayout
    private var searchJob: Job? = null

    private var isPersonalView = false
    private var hasInitialRefreshDone = false
    private var ivVistaPessoal: ImageView? = null
    private var isReadOnlyProfile = false

    private var lastAbertos = -1
    private var lastProgresso = -1
    private var lastResolvidos = -1
    private var lastPrioritarios = -1

    companion object {
        // Flag estática para saber se já fizemos a primeira carga da sessão
        private var hasInitialRefreshDoneGlobal = false

        fun resetRefreshFlag() {
            hasInitialRefreshDoneGlobal = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        drawerLayout = findViewById(R.id.drawer_layout)
        swipeRefresh = findViewById(R.id.swipeRefresh)

        rvAtividades = findViewById(R.id.rv_ultimas_atividades)
        rvAtividades.isNestedScrollingEnabled = false
        rvAtividades.layoutManager = LinearLayoutManager(this)

        rvResultadosPesquisa = findViewById(R.id.rv_resultados_pesquisa)
        rvResultadosPesquisa.layoutManager = LinearLayoutManager(this)

        val corBranca = ContextCompat.getColor(this, android.R.color.white)
        val corAzulGlpi = ContextCompat.getColor(this, R.color.azul_glpi)

        configurarLotties(corBranca, corAzulGlpi)
        configurarMenuLateral()
        configurarPesquisa()
        configurarCliquesBotoes()
        configurarMenuInferior()
        configurarBotaoVistaPessoal()
        
        // Restaurar estado da vista pessoal
        isPersonalView = PreferenceManager.isPersonalViewEnabled(this)
        
        // Carregar dados da cache para evitar mostrar "0"
        carregarDadosCacheados()
        
        // Só atualiza automaticamente se for a primeira vez que entramos na app nesta sessão
        if (!hasInitialRefreshDoneGlobal) {
            atualizarDadosDashboard()
            hasInitialRefreshDoneGlobal = true
        }
        
        swipeRefresh.setOnRefreshListener { 
            verificarSincroniaPerfil()
            atualizarDadosDashboard() 
        }

        // Registo automático de notificações (FCM) no arranque
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                FcmHelper.enviarTokenParaServidor(this, token)
            }
        }

        // --- Agendamento da Monitorização Local (15 em 15 min) ---
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<TicketCheckWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "TicketMonitor",
            ExistingPeriodicWorkPolicy.KEEP, // Mantém o existente para não reiniciar o ciclo de 15 min
            workRequest
        )

        // Exibir alerta de bem-vindo se vier do login
        if (intent.getBooleanExtra("SHOW_WELCOME", false)) {
            AlertHelper.exibirAlertaPremium(this, "Bem-vindo!", isError = false)
            intent.removeExtra("SHOW_WELCOME")
        }

        requestNotificationPermission()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 🔥 Sincronizar estado da vista pessoal ao voltar
        val savedPersonalView = PreferenceManager.isPersonalViewEnabled(this)
        if (savedPersonalView != isPersonalView) {
            isPersonalView = savedPersonalView
            atualizarDadosDashboard()
        } else {
            // Mesmo que o boolean seja igual, garantimos que a UI visual do botão está correta
            val btnPersonal = findViewById<FrameLayout>(R.id.btn_vista_pessoal)
            btnPersonal?.let {
                it.isSelected = isPersonalView
                val density = resources.displayMetrics.density
                val strokeWidth = if (isPersonalView) (2.5f * density).toInt() else (1 * density).toInt()
                (it.background as? android.graphics.drawable.GradientDrawable)?.let { bg ->
                    val color = if (isPersonalView) ContextCompat.getColor(this, R.color.azul_glpi) 
                                else ContextCompat.getColor(this, R.color.borda_cartao)
                    bg.setStroke(strokeWidth, color)
                }
            }
        }
        
        // 🔥 Verificar se o perfil mudou na Web sempre que voltamos à Dashboard
        verificarSincroniaPerfil()

        if (intent.getBooleanExtra("ABRIR_MENU", false)) {
            drawerLayout.openDrawer(GravityCompat.START)
            intent.removeExtra("ABRIR_MENU")
        }

        // Se o flag global foi resetado externamente (ex: troca de perfil), atualizar agora
        if (!hasInitialRefreshDoneGlobal) {
            atualizarDadosDashboard()
            hasInitialRefreshDoneGlobal = true
        }
    }

    private fun verificarSincroniaPerfil() {
        val tokenSessao = GlpiConfig.SESSION_TOKEN
        val tokenApp = GlpiConfig.APP_TOKEN
        
        if (tokenSessao.isEmpty()) return

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Obter Sessão Atual com cache buster
                val res = GlpiRetrofit.api.getFullSession(tokenSessao, tokenApp, System.currentTimeMillis())
                if (res.isSuccessful) {
                    val body = res.body()
                    val sessionMap = body?.get("session") as? Map<*, *>
                    val activeProfile = sessionMap?.get("glpiactiveprofile") as? Map<*, *>
                    
                    val serverProfileName = activeProfile?.get("name")?.toString() ?: ""
                    val cachedProfileName = PreferenceManager.getUserProfile(this@DashboardActivity) ?: ""

                    // Se a API reportar um perfil diferente do que a App acha que tem, ajustamos a Cache
                    // Isto certifica que respeita sempre as mudanças do Botão Azul e NUNCA tenta reverter
                    if (serverProfileName.isNotEmpty() && serverProfileName != cachedProfileName) {
                        Log.d("DASHBOARD_SYNC", "Sincronização necessária! Server: $serverProfileName | Cache App: $cachedProfileName")
                        
                        withContext(Dispatchers.Main) {
                            PreferenceManager.setUserProfile(this@DashboardActivity, serverProfileName)
                            isReadOnlyProfile = PreferenceManager.isReadOnlyProfile(this@DashboardActivity)
                            PreferenceManager.clearAllDataCache(this@DashboardActivity)
                            recarregarDadosAoMudarPerfil()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            isReadOnlyProfile = PreferenceManager.isReadOnlyProfile(this@DashboardActivity)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("DASHBOARD_SYNC", "Erro na sincronização: ${e.message}")
            }
        }
    }

    private fun recarregarDadosAoMudarPerfil() {
        // Reset de flags e limpeza de UI antes de novo fetch
        hasInitialRefreshDone = false
        // help clean the hasInitialRefreshDoneGlobal from companion
        resetRefreshFlag()
        lastAbertos = -1
        lastProgresso = -1
        lastResolvidos = -1
        lastPrioritarios = -1
        
        // Disparar o refresh que já existe na Dashboard
        atualizarDadosDashboard()
    }

    private fun configurarBotaoVistaPessoal() {
        val btnPersonal = findViewById<FrameLayout>(R.id.btn_vista_pessoal)
        ivVistaPessoal = findViewById<ImageView>(R.id.iv_vista_pessoal)
        
        btnPersonal.setOnClickListener {
            isPersonalView = !isPersonalView
            PreferenceManager.setPersonalViewEnabled(this, isPersonalView)
            it.isSelected = isPersonalView
            
            if (isPersonalView) {
                AlertHelper.exibirAlertaPremium(this, "Modo: Vista Pessoal")
            } else {
                AlertHelper.exibirAlertaPremium(this, "Modo: Vista Geral")
            }
            
            // Reset dos contadores para forçar animação
            lastAbertos = -1
            lastProgresso = -1
            lastResolvidos = -1
            lastPrioritarios = -1
            
            atualizarDadosDashboard()
        }
    }

    @Suppress("DEPRECATION")
    private fun configurarMenuLateral() {
        val navViewDrawer = findViewById<NavigationView>(R.id.nav_view_drawer)
        navViewDrawer.setNavigationItemSelectedListener { menuItem ->
            val intent = when (menuItem.itemId) {
                R.id.nav_criar_ticket -> {
                    if (isReadOnlyProfile) {
                        AlertHelper.exibirAlertaPremium(this, "Sem permissão para criar tickets.")
                        null
                    } else {
                        Intent(this, CreateTicketActivity::class.java)
                    }
                }
                R.id.nav_vista_geral -> Intent(this, GeneralViewTicketsActivity::class.java)
                R.id.nav_delete_tickets -> Intent(this, DeleteTicketsActivity::class.java)
                R.id.nav_estatisticas -> Intent(this, StatisticsActivity::class.java)
                R.id.nav_editar_tickets -> Intent(this, EditTicketsActivity::class.java)
                else -> null
            }

            if (intent != null) {
                startActivity(intent)
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            } else {
                Log.w("MENU_DASHBOARD", "ID não encontrado no Kotlin: ${menuItem.itemId}")
            }

            true
        }
    }

    @Suppress("DEPRECATION")
    private fun configurarMenuInferior() {
        val navView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        navView.selectedItemId = R.id.nav_tickets

        navView.labelVisibilityMode = BottomNavigationView.LABEL_VISIBILITY_UNLABELED
 
        val corBrancaPura = ContextCompat.getColor(this, android.R.color.white)
        val corBrancaTransluscida = Color.parseColor("#99FFFFFF")
        val tintList = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf(-android.R.attr.state_checked)),
            intArrayOf(corBrancaPura, corBrancaTransluscida)
        )
        navView.itemIconTintList = tintList
        navView.itemTextColor = tintList

        ViewCompat.setOnApplyWindowInsetsListener(navView) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val layoutParams = view.layoutParams as ViewGroup.MarginLayoutParams
            layoutParams.bottomMargin = systemBars.bottom + 16
            view.layoutParams = layoutParams
            insets
        }

        navView.setOnItemSelectedListener { item ->
            if (item.itemId == R.id.nav_tickets) return@setOnItemSelectedListener true
            val intent = when (item.itemId) {
                R.id.nav_agenda -> Intent(this, AgendaActivity::class.java)
                R.id.nav_inventario -> Intent(this, InventoryActivity::class.java)
                R.id.nav_perfil -> Intent(this, ProfileDetailActivity::class.java)
                else -> null
            }
            if (intent != null) {
                startActivity(intent)
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            }
            false
        }
    }

    private fun configurarLotties(corBranca: Int, corAzul: Int) {
        findViewById<View>(R.id.lottie_hamburger)?.let { aplicarCorLottie(it, corBranca) }
        findViewById<View>(R.id.lupa_anim_dashboard)?.let { aplicarCorLottie(it, corAzul) }
        findViewById<View>(R.id.btn_abertos)?.let { aplicarCorLottieNoContainer(it, corAzul) }
        findViewById<View>(R.id.btn_progresso)?.let { aplicarCorLottieNoContainer(it, corAzul) }
        findViewById<View>(R.id.btn_resolvido)?.let { aplicarCorLottieNoContainer(it, corAzul) }
        findViewById<View>(R.id.btn_prioritarios)?.let { aplicarCorLottieNoContainer(it, corAzul) }
    }

    private fun atualizarDadosDashboard() {
        swipeRefresh.isRefreshing = true
        
        // Etiquetas fixas conforme solicitado
        findViewById<TextView>(R.id.tv_label_abertos)?.text = "NOVOS"
        findViewById<TextView>(R.id.tv_label_progresso)?.text = "EM PROGRESSO"
        findViewById<TextView>(R.id.tv_label_resolvido)?.text = "FINALIZADOS"
        findViewById<TextView>(R.id.tv_label_prioritarios)?.text = "PRIORITÁRIOS"
        
        // Atualizar visual do botão através do estado de seleção (Selectors nativos)
        val btnPersonal = findViewById<FrameLayout>(R.id.btn_vista_pessoal)
        btnPersonal.isSelected = isPersonalView
        
        // Ajustar espessura da borda dinamicamente através do background drawable
        val density = resources.displayMetrics.density
        val strokeWidth = if (isPersonalView) (2.5f * density).toInt() else (1 * density).toInt()
        
        (btnPersonal.background as? android.graphics.drawable.GradientDrawable)?.let {
             val currentStrokeColor = if (isPersonalView) ContextCompat.getColor(this, R.color.azul_glpi) 
                                     else ContextCompat.getColor(this, R.color.borda_cartao)
             it.setStroke(strokeWidth, currentStrokeColor)
        }

        // Deixar o Dashboard atualizar mesmo em modo offline para aproveitar o interceptor de mock

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                var sessionRealId = 0
                // 1. Verificar/Atualizar a sessão com cache buster para garantir sincronia com a Web
                val resSession = GlpiRetrofit.api.getFullSession(GlpiConfig.SESSION_TOKEN, GlpiConfig.APP_TOKEN, System.currentTimeMillis())
                if (resSession.isSuccessful) {
                    val sessionBody = resSession.body()
                    val sessionMap = sessionBody?.get("session") as? Map<*, *>
                    val possibleId = sessionMap?.get("glpiID") 
                                 ?: sessionMap?.get("glpi_id")
                                 ?: sessionMap?.get("id")
                                 ?: sessionMap?.get("users_id")
                                 ?: (sessionMap?.get("glpiactiveprofile") as? Map<*, *>)?.get("users_id")
                    
                    sessionRealId = when (possibleId) {
                        is Double -> possibleId.toInt()
                        is Int -> possibleId
                        is String -> possibleId.toIntOrNull() ?: 0
                        else -> 0
                    }
                    
                    if (sessionRealId > 0) {
                        PreferenceManager.setUserId(this@DashboardActivity, sessionRealId)
                        Log.d("DASHBOARD_AUTH", "ID do utilizador atualizado: $sessionRealId")
                    }
                    
                    val glpiName = sessionMap?.get("glpiname")?.toString()
                    if (glpiName != null) {
                        PreferenceManager.setUserName(this@DashboardActivity, glpiName)
                    }

                    // 🔥 Extrair nome de exibição para buscas mais precisas (ex: Gonçalo Pinto)
                    val realName = sessionMap?.get("glpirealname")?.toString() ?: ""
                    val firstName = sessionMap?.get("glpifirstname")?.toString() ?: ""
                    val fullName = "$firstName $realName".trim()
                    if (fullName.isNotEmpty()) {
                        PreferenceManager.setUserFullName(this@DashboardActivity, fullName)
                        Log.d("DASHBOARD_AUTH", "Nome completo extraído: $fullName")
                    }

                    // 🔥 Extrair nome do perfil para verificar permissões de leitura (Read-Only)
                    val activeProfile = sessionMap?.get("glpiactiveprofile") as? Map<*, *>
                    val profileName = activeProfile?.get("name")?.toString() ?: ""

                    // 🔥 PERSISTIR O PERFIL E VERIFICAR PERMISSÕES 🔥
                    PreferenceManager.setUserProfile(this@DashboardActivity, profileName)
                    isReadOnlyProfile = PreferenceManager.isReadOnlyProfile(this@DashboardActivity)
                    
                    if (isReadOnlyProfile) {
                        Log.d("DASHBOARD_PERM", "Perfil detetado como Leitura (Read-Only): $profileName. Funcionalidades de escrita bloqueadas.")
                    }
                } else {
                    Log.e("DASHBOARD_AUTH", "Falha ao obter sessão: ${resSession.code()}")
                }

                // 🔥 USAR O ID REAL OBTIDO DA SESSÃO (ou o guardado como fallback)
                val finalUserId = if (sessionRealId > 0) sessionRealId else GlpiConfig.USER_ID
                val tokenSessao = GlpiConfig.SESSION_TOKEN
                val tokenApp = GlpiConfig.APP_TOKEN
                
                // Calcular primeiro dia do mês atual para os Resolvidos (Geral)
                val calMes = Calendar.getInstance()
                calMes.set(Calendar.DAY_OF_MONTH, 1)
                val dataLimiteMes = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calMes.time) + " 00:00:00"

                // Calcular últimos 60 dias para os Resolvidos (Pessoal)
                val calRecent = Calendar.getInstance()
                calRecent.add(Calendar.DAY_OF_YEAR, -60)
                val dataLimiteRecent = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calRecent.time) + " 00:00:00"
                
                Log.d("DASHBOARD_API", "Buscando dados | Geral: $dataLimiteMes | Pessoal: $dataLimiteRecent")
                
                // 🔥 PREPARAR NOMES DE BUSCA PARA FALLBACK 🔥
                val loginBusca: String = GlpiConfig.USER_NAME.split(".", " ").firstOrNull() ?: ""
                val nomeCompleto: String = GlpiConfig.USER_FULL_NAME

                // Usamos runCatching para garantir que falhas individuais não matem a Dashboard
                val defAbertos = async { 
                    runCatching {
                        if (isPersonalView) GlpiRetrofit.api.getCountAbertosMeus(
                            tokenSessao, tokenApp, 
                            finalUserId, finalUserId, finalUserId, finalUserId, finalUserId, finalUserId,
                            loginBusca, nomeCompleto.ifEmpty { loginBusca }
                        )
                        else GlpiRetrofit.api.getCountAbertosTotal(tokenSessao, tokenApp)
                    }.getOrNull()
                }
                val defProgresso = async { 
                    runCatching {
                        if (isPersonalView) GlpiRetrofit.api.getCountProgressoMeus(
                            tokenSessao, tokenApp, 
                            finalUserId, finalUserId, finalUserId, finalUserId, finalUserId, finalUserId,
                            loginBusca, nomeCompleto.ifEmpty { loginBusca }
                        )
                        else GlpiRetrofit.api.getCountProgressoTotal(tokenSessao, tokenApp)
                    }.getOrNull()
                }
                val defResolvidos = async { 
                    runCatching {
                        if (isPersonalView) {
                            // VISTA PESSOAL: Mês Atual
                            GlpiRetrofit.api.getCountResolvidosRecentes(
                                tokenSessao, tokenApp, 
                                finalUserId, finalUserId, finalUserId, finalUserId, finalUserId, finalUserId,
                                loginBusca, nomeCompleto.ifEmpty { loginBusca },
                                dataLimiteMes
                            )
                        } else {
                            // VISTA GERAL: Toda a Equipa - Mês Atual
                            GlpiRetrofit.api.getCountResolvidosGlobal(tokenSessao, tokenApp, dataLimiteMes)
                        }
                    }.getOrNull()
                }
                val defPrioritarios = async { 
                    runCatching {
                        if (isPersonalView) GlpiRetrofit.api.getCountPrioritariosMeus(
                            tokenSessao, tokenApp, 
                            finalUserId, finalUserId, finalUserId, finalUserId, finalUserId, finalUserId,
                            loginBusca, nomeCompleto.ifEmpty { loginBusca }
                        )
                        else GlpiRetrofit.api.getCountPrioritariosDashboard(tokenSessao, tokenApp)
                    }.getOrNull()
                }
                val defAtividades = async { 
                    runCatching {
                        if (isPersonalView) GlpiRetrofit.api.getUltimasAtividades(
                            GlpiConfig.SESSION_TOKEN, GlpiConfig.APP_TOKEN, 
                            finalUserId, finalUserId, finalUserId, finalUserId, finalUserId, finalUserId,
                            loginBusca, nomeCompleto.ifEmpty { loginBusca }
                        )
                        else GlpiRetrofit.api.getUltimasAtividadesGeral(GlpiConfig.SESSION_TOKEN, GlpiConfig.APP_TOKEN)
                    }.getOrNull()
                }

                val resAbertos = defAbertos.await()
                val resProgresso = defProgresso.await()
                val resResolvidos = defResolvidos.await()
                val resPrioritarios = defPrioritarios.await()
                val resAtividades = defAtividades.await()

                withContext(Dispatchers.Main) {
                    // Extrair totais de forma segura
                    val nAbertos = if (resAbertos?.isSuccessful == true) resAbertos.body()?.totalcount ?: 0 else lastAbertos.coerceAtLeast(0)
                    val nProgresso = if (resProgresso?.isSuccessful == true) resProgresso.body()?.totalcount ?: 0 else lastProgresso.coerceAtLeast(0)
                    val nResolvidos = if (resResolvidos?.isSuccessful == true) resResolvidos.body()?.totalcount ?: 0 else lastResolvidos.coerceAtLeast(0)
                    val nPrioritarios = if (resPrioritarios?.isSuccessful == true) resPrioritarios.body()?.totalcount ?: 0 else lastPrioritarios.coerceAtLeast(0)

                    // Logs de debug para ajudar a identificar critérios incorretos
                    if (resAbertos?.isSuccessful == false) Log.e("DASHBOARD_API", "Erro Abertos: ${resAbertos.errorBody()?.string()}")
                    if (resProgresso?.isSuccessful == false) Log.e("DASHBOARD_API", "Erro Progresso: ${resProgresso.errorBody()?.string()}")
                    if (resResolvidos?.isSuccessful == false) Log.e("DASHBOARD_API", "Erro Resolvidos: ${resResolvidos.errorBody()?.string()}")
                    if (resPrioritarios?.isSuccessful == false) Log.e("DASHBOARD_API", "Erro Prioritários: ${resPrioritarios.errorBody()?.string()}")

                    // Feedback premium: Se o valor aumentou, mostra o badge e pulsa o card
                    if (lastAbertos != -1 && nAbertos > lastAbertos) {
                        findViewById<TextView>(R.id.tv_badge_abertos)?.let { exibirBadgeIncremento(it, nAbertos - lastAbertos) }
                    } else if (lastAbertos != -1 && nAbertos < lastAbertos) {
                        findViewById<TextView>(R.id.tv_badge_reducao_abertos)?.let { exibirBadgeDecremento(it, lastAbertos - nAbertos) }
                    }

                    if (lastProgresso != -1 && nProgresso > lastProgresso) {
                        findViewById<TextView>(R.id.tv_badge_progresso)?.let { exibirBadgeIncremento(it, nProgresso - lastProgresso) }
                    } else if (lastProgresso != -1 && nProgresso < lastProgresso) {
                        findViewById<TextView>(R.id.tv_badge_reducao_progresso)?.let { exibirBadgeDecremento(it, lastProgresso - nProgresso) }
                    }

                    if (lastResolvidos != -1 && nResolvidos > lastResolvidos) {
                        findViewById<TextView>(R.id.tv_badge_resolvido)?.let { exibirBadgeIncremento(it, nResolvidos - lastResolvidos) }
                    } else if (lastResolvidos != -1 && nResolvidos < lastResolvidos) {
                        findViewById<TextView>(R.id.tv_badge_reducao_resolvido)?.let { exibirBadgeDecremento(it, lastResolvidos - nResolvidos) }
                    }

                    if (lastPrioritarios != -1 && nPrioritarios > lastPrioritarios) {
                        findViewById<TextView>(R.id.tv_badge_prioritarios)?.let { exibirBadgeIncremento(it, nPrioritarios - lastPrioritarios) }
                    } else if (lastPrioritarios != -1 && nPrioritarios < lastPrioritarios) {
                        findViewById<TextView>(R.id.tv_badge_reducao_prioritarios)?.let { exibirBadgeDecremento(it, lastPrioritarios - nPrioritarios) }
                    }

                    // Animar o número rolando do valor anterior para o novo
                    findViewById<TextView>(R.id.tv_numero_abertos)?.let { animarContador(it, if (lastAbertos == -1) 0 else lastAbertos, nAbertos) }
                    findViewById<TextView>(R.id.tv_numero_progresso)?.let { animarContador(it, if (lastProgresso == -1) 0 else lastProgresso, nProgresso) }
                    findViewById<TextView>(R.id.tv_numero_resolvido)?.let { animarContador(it, if (lastResolvidos == -1) 0 else lastResolvidos, nResolvidos) }
                    findViewById<TextView>(R.id.tv_numero_prioritarios)?.let { animarContador(it, if (lastPrioritarios == -1) 0 else lastPrioritarios, nPrioritarios) }

                    lastAbertos = nAbertos
                    lastProgresso = nProgresso
                    lastResolvidos = nResolvidos
                    lastPrioritarios = nPrioritarios

                    // Guardar na Cache para persistência
                    PreferenceManager.setDashAbertos(this@DashboardActivity, nAbertos)
                    PreferenceManager.setDashProgresso(this@DashboardActivity, nProgresso)
                    PreferenceManager.setDashResolvidos(this@DashboardActivity, nResolvidos)
                    PreferenceManager.setDashPrioritarios(this@DashboardActivity, nPrioritarios)

                    // Cor do número muda em modo pessoal
                    val corNumero = ContextCompat.getColor(this@DashboardActivity, R.color.azul_glpi)
                    findViewById<TextView>(R.id.tv_numero_abertos)?.setTextColor(corNumero)
                    findViewById<TextView>(R.id.tv_numero_progresso)?.setTextColor(corNumero)
                    findViewById<TextView>(R.id.tv_numero_resolvido)?.setTextColor(corNumero)
                    findViewById<TextView>(R.id.tv_numero_prioritarios)?.setTextColor(corNumero)

                    val dadosAtividades: List<Map<String, Any>> = if (resAtividades?.isSuccessful == true) resAtividades.body()?.data ?: emptyList() else emptyList()
                    val limit = if (isPersonalView) 15 else 5
                    val listToShow = dadosAtividades.take(limit)
                    rvAtividades.adapter = TicketAdapter(listToShow, mostrarMotivo = true)
                    
                    if (listToShow.isNotEmpty()) {
                        // Guardar atividades na cache
                        try {
                            val json = Gson().toJson(listToShow)
                            PreferenceManager.setDashAtividades(this@DashboardActivity, json)
                        } catch (e: Exception) {
                            Log.e("DASHB_CACHE", "Erro ao guardar atividades: ${e.message}")
                        }
                    } else {
                        PreferenceManager.setDashAtividades(this@DashboardActivity, "[]")
                    }
                    rvAtividades.scheduleLayoutAnimation()
                    swipeRefresh.isRefreshing = false
                }
            } catch (e: Exception) {
                Log.e("DASHBOARD_FATAL", "Erro crítico ao atualizar dashboard: ${e.message}", e)
                withContext(Dispatchers.Main) { 
                    swipeRefresh.isRefreshing = false
                    Toast.makeText(this@DashboardActivity, "Erro ao carregar dados: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } finally {
                withContext(Dispatchers.Main) { swipeRefresh.isRefreshing = false }
            }
        }
    }

    private fun aplicarCorLottie(view: View?, cor: Int) {
        if (view is LottieAnimationView) {
            view.addValueCallback(KeyPath("**"), LottieProperty.COLOR_FILTER) {
                PorterDuffColorFilter(cor, PorterDuff.Mode.SRC_ATOP)
            }
        }
    }

    private fun aplicarCorLottieNoContainer(containerView: View?, cor: Int) {
        if (containerView !is ViewGroup) return
        val container = containerView as ViewGroup
        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i)
            if (child is LottieAnimationView) aplicarCorLottie(child, cor)
        }
    }

    private fun animarContador(textView: TextView, valorInicial: Int, valorFinal: Int) {
        if (valorInicial == valorFinal) {
            textView.text = valorFinal.toString()
            return
        }
        val animator = ValueAnimator.ofInt(valorInicial, valorFinal)
        animator.duration = 1000 // Aumento ligeiro para ser mais elegante
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.addUpdateListener { textView.text = it.animatedValue.toString() }
        animator.start()
    }

    private fun exibirBadgeIncremento(badge: TextView, valor: Int) {
        badge.text = "+$valor"
        badge.visibility = View.VISIBLE
        badge.alpha = 0f
        badge.scaleX = 0f
        badge.scaleY = 0f
        badge.translationY = 0f

        // Animação Premium: Pop-in elástico seguido de subida suave
        badge.animate()
            .alpha(1f)
            .scaleX(1.1f)
            .scaleY(1.1f)
            .setDuration(300)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                badge.animate()
                    .scaleX(0.8f)
                    .scaleY(0.8f)
                    .translationY(50f) // Agora desce (efeito de queda) para indicar redução
                    .alpha(0f)
                    .setDuration(1500)
                    .setStartDelay(300)
                    .setListener(object : android.animation.AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: android.animation.Animator) {
                            badge.visibility = View.GONE
                        }
                    })
            }
        
        pulsarCard(badge.parent as? View)
    }

    private fun exibirBadgeDecremento(badge: TextView, valor: Int) {
        badge.text = "-$valor"
        badge.visibility = View.VISIBLE
        badge.alpha = 0f
        badge.scaleX = 0f
        badge.scaleY = 0f
        badge.translationY = 0f

        // Animação Premium: Pop-in elástico seguido de SUBIDA suave (mesma direção do incremento)
        badge.animate()
            .alpha(1f)
            .scaleX(1.1f)
            .scaleY(1.1f)
            .setDuration(300)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                badge.animate()
                    .scaleX(0.8f)
                    .scaleY(0.8f)
                    .translationY(-50f) // Agora sobe para não sair do cartão no canto inferior
                    .alpha(0f)
                    .setDuration(2000)
                    .setStartDelay(500)
                    .setListener(object : android.animation.AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: android.animation.Animator) {
                            badge.visibility = View.GONE
                        }
                    })
            }
        
        pulsarCard(badge.parent as? View)
    }

    private fun pulsarCard(card: View?) {
        card?.animate()?.scaleX(1.05f)?.scaleY(1.05f)?.setDuration(200)?.withEndAction {
            card.animate().scaleX(1f).scaleY(1f).setDuration(200).start()
        }?.start()
    }


    private fun configurarPesquisa() {
        val searchBar = findViewById<EditText>(R.id.search_bar_dashboard)
        searchBar.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                if (query.isEmpty()) {
                    swipeRefresh.visibility = View.VISIBLE
                    rvResultadosPesquisa.visibility = View.GONE
                } else {
                    swipeRefresh.visibility = View.GONE
                    rvResultadosPesquisa.visibility = View.VISIBLE
                    pesquisarTicketsNaApi(query)
                }
            }
            override fun beforeTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })
    }

    private fun pesquisarTicketsNaApi(query: String) {
        searchJob?.cancel()
        searchJob = CoroutineScope(Dispatchers.Main).launch {
            delay(500)
            val textoLimpo = query.replace("#", "").trim()
            val campoPesquisa = if (textoLimpo.all { it.isDigit() }) 2 else 1
            withContext(Dispatchers.IO) {
                try {
                    val resposta = GlpiRetrofit.api.pesquisarTickets(GlpiConfig.SESSION_TOKEN, GlpiConfig.APP_TOKEN, campoPesquisa, "contains", textoLimpo)
                    withContext(Dispatchers.Main) { 
                        val data = if (resposta.isSuccessful) resposta.body()?.data ?: emptyList() else emptyList()
                        rvResultadosPesquisa.adapter = TicketAdapter(data) 
                        rvResultadosPesquisa.scheduleLayoutAnimation()
                    }
                } catch (e: Exception) {}
            }
        }
    }

    private fun configurarCliquesBotoes() {
        val clickListener = View.OnClickListener { view ->
            val intent = when (view.id) {
                R.id.btn_abertos -> Intent(this, OpenTicketsActivity::class.java).apply {
                    if (isPersonalView) putExtra("FILTRO_MEUS", true)
                }
                R.id.btn_progresso -> Intent(this, ProgressTicketsActivity::class.java).apply {
                    if (isPersonalView) putExtra("FILTRO_MEUS", true)
                }
                R.id.btn_resolvido -> Intent(this, ResolvedTicketsActivity::class.java).apply {
                    if (isPersonalView) putExtra("FILTRO_MEUS", true)
                }
                R.id.btn_prioritarios -> Intent(this, PriorityTicketsActivity::class.java).apply {
                    if (isPersonalView) putExtra("FILTRO_MEUS", true)
                }
                else -> null
            }
            intent?.let { startActivity(it) }
        }
        findViewById<ConstraintLayout>(R.id.btn_abertos).setOnClickListener(clickListener)
        findViewById<ConstraintLayout>(R.id.btn_progresso).setOnClickListener(clickListener)
        findViewById<ConstraintLayout>(R.id.btn_resolvido).setOnClickListener(clickListener)
        findViewById<ConstraintLayout>(R.id.btn_prioritarios).setOnClickListener(clickListener)
        findViewById<FrameLayout>(R.id.btn_menu_hamburger).setOnClickListener { drawerLayout.openDrawer(GravityCompat.START) }
    }


    override fun dispatchTouchEvent(ev: android.view.MotionEvent?): Boolean {
        KeyboardHelper.handleTouchOutside(this, ev)
        return super.dispatchTouchEvent(ev)
    }

    // Silencing deprecated warning while maintaining compatibility
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    private fun carregarDadosCacheados() {
        val context = this
        val abertos = PreferenceManager.getDashAbertos(context)
        val progresso = PreferenceManager.getDashProgresso(context)
        val resolvidos = PreferenceManager.getDashResolvidos(context)
        val prioritarios = PreferenceManager.getDashPrioritarios(context)

        findViewById<TextView>(R.id.tv_numero_abertos)?.text = abertos.toString()
        findViewById<TextView>(R.id.tv_numero_progresso)?.text = progresso.toString()
        findViewById<TextView>(R.id.tv_numero_resolvido)?.text = resolvidos.toString()
        findViewById<TextView>(R.id.tv_numero_prioritarios)?.text = prioritarios.toString()

        // Sincronizar visual do botão de vista pessoal na carga da cache
        val btnPersonal = findViewById<FrameLayout>(R.id.btn_vista_pessoal)
        btnPersonal?.let {
            it.isSelected = isPersonalView
            val density = resources.displayMetrics.density
            val strokeWidth = if (isPersonalView) (2.5f * density).toInt() else (1 * density).toInt()
            (it.background as? android.graphics.drawable.GradientDrawable)?.let { bg ->
                val color = if (isPersonalView) ContextCompat.getColor(this, R.color.azul_glpi) 
                            else ContextCompat.getColor(this, R.color.borda_cartao)
                bg.setStroke(strokeWidth, color)
            }
        }
        
        // Se estiver em vista pessoal, garantir a cor azul nos números mesmo na cache
        val corNumero = ContextCompat.getColor(this, R.color.azul_glpi)
        findViewById<TextView>(R.id.tv_numero_abertos)?.setTextColor(corNumero)
        findViewById<TextView>(R.id.tv_numero_progresso)?.setTextColor(corNumero)
        findViewById<TextView>(R.id.tv_numero_resolvido)?.setTextColor(corNumero)
        findViewById<TextView>(R.id.tv_numero_prioritarios)?.setTextColor(corNumero)

        // Inicializar os trackers de "último valor" para evitar badges falsos na primeira carga
        lastAbertos = abertos
        lastProgresso = progresso
        lastResolvidos = resolvidos
        lastPrioritarios = prioritarios

        val jsonAtividades = PreferenceManager.getDashAtividades(context)
        if (!jsonAtividades.isNullOrEmpty()) {
            try {
                val type = object : com.google.gson.reflect.TypeToken<List<Map<String, Any>>>() {}.type
                val listaAtividades: List<Map<String, Any>> = com.google.gson.Gson().fromJson(jsonAtividades, type)
                rvAtividades.adapter = TicketAdapter(listaAtividades, mostrarMotivo = true)
            } catch (e: Exception) {
                android.util.Log.e("DASHB_CACHE", "Erro ao carregar atividades cache: ${e.message}")
            }
        }
    }
}
