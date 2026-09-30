package com.example.glpimobile

import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.airbnb.lottie.LottieAnimationView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.google.android.material.button.MaterialButton
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*

class ProfileDetailActivity : AppCompatActivity() {
    companion object {
        private var hasInitialRefreshDoneGlobal = false
    }
    private var realUserId: Int = GlpiConfig.USER_ID.takeIf { it > 0 } ?: 0
    private var availableProfiles: List<Pair<Int, String>> = emptyList()
    private var activeProfileId: Int = 0
    private lateinit var loadingOverlay: View
    private val TOKEN_SESSAO get() = GlpiConfig.SESSION_TOKEN
    private val TOKEN_APP get() = GlpiConfig.APP_TOKEN

    private var drawerLayout: DrawerLayout? = null
    private var lottieHamburger: LottieAnimationView? = null
    private var swipeRefresh: androidx.swiperefreshlayout.widget.SwipeRefreshLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_profile_detail)

            loadingOverlay = findViewById<View>(R.id.loading_overlay_profile) ?: View(this)

            // --- 0. PRE-CARREGAMENTO IMEDIATO VIA CACHE ---
            preCarregarCacheLocal()

            // 1. Configurar Drawer e Menu Hamburguer
            drawerLayout = findViewById(R.id.drawer_layout_profile_detail)
            lottieHamburger = findViewById(R.id.lottie_hamburger_profile_detail)
            val btnMenu = findViewById<FrameLayout>(R.id.btn_menu_hamburger_profile_detail)
            val navViewDrawer = findViewById<NavigationView>(R.id.nav_view_drawer_profile_detail)

            btnMenu?.setOnClickListener {
                val drawer = drawerLayout
                if (drawer != null && !drawer.isDrawerOpen(androidx.core.view.GravityCompat.START)) {
                    drawer.openDrawer(androidx.core.view.GravityCompat.START)
                    lottieHamburger?.playAnimation()
                }
            }

            // Tornar o ícone do menu hambúrguer branco
            val corBranca = ContextCompat.getColor(this, android.R.color.white)
            lottieHamburger?.addValueCallback(
                com.airbnb.lottie.model.KeyPath("**"),
                com.airbnb.lottie.LottieProperty.COLOR_FILTER
            ) { android.graphics.PorterDuffColorFilter(corBranca, android.graphics.PorterDuff.Mode.SRC_ATOP) }

            navViewDrawer?.setNavigationItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_utilizadores -> {
                        startActivity(Intent(this, ProfileActivity::class.java))
                    }
                    R.id.nav_user_assets -> {
                        startActivity(Intent(this, UserAssetsActivity::class.java))
                    }
                    R.id.nav_statistics -> {
                        startActivity(Intent(this, MyStatisticsActivity::class.java))
                    }
                    R.id.nav_settings -> {
                        startActivity(Intent(this, SettingsActivity::class.java))
                    }
                    R.id.nav_my_tickets_history -> {
                        startActivity(Intent(this, MyTicketsHistoryActivity::class.java))
                    }
                }
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                true
            }

            // 2. Configurar Bottom Navigation
            configurarNavegacaoInferior()

            // 3. Lógica das Animações do Avatar (Simplificada com ImageView no XML se necessário)
            
            findViewById<ConstraintLayout>(R.id.card_equipamentos_profile)?.setOnClickListener {
                val intent = Intent(this, UserAssetsActivity::class.java)
                intent.putExtra("EXTRA_USER_ID", realUserId.toString())
                val nomeAtual = findViewById<TextView>(R.id.tv_nome_perfil_detail)?.text?.toString() ?: "Utilizador"
                intent.putExtra("EXTRA_USER_NAME", nomeAtual)
                startActivity(intent)
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            }

            findViewById<ConstraintLayout>(R.id.card_tickets_mes_profile)?.setOnClickListener {
                val intent = Intent(this, MyTicketsHistoryActivity::class.java)
                intent.putExtra("EXTRA_FILTER_MONTH", true)
                startActivity(intent)
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            }

            findViewById<View>(R.id.btn_logout_profile)?.setOnClickListener {
                PreferenceManager.setSessionToken(this, "")
                PreferenceManager.clearAllDataCache(this)
                val intent = Intent(this, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(intent)
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            }

            // 4. Tintar as animações de carregamento de AZUL
            val corAzulGlpi = ContextCompat.getColor(this, R.color.azul_glpi)
            val lottieEquip = findViewById<LottieAnimationView>(R.id.lottie_loading_equipamentos)
            val lottieTickets = findViewById<LottieAnimationView>(R.id.lottie_loading_tickets_mes)
            
            listOf(lottieEquip, lottieTickets).forEach { lottie ->
                lottie?.addValueCallback(
                    com.airbnb.lottie.model.KeyPath("**"),
                    com.airbnb.lottie.LottieProperty.COLOR_FILTER
                ) { android.graphics.PorterDuffColorFilter(corAzulGlpi, android.graphics.PorterDuff.Mode.SRC_ATOP) }
            }

            // Tintar o Lottie do Overlay de carregamento (Switch Profile) de AZUL
            val loadingOverlayView = findViewById<View>(R.id.loading_overlay_profile)
            if (loadingOverlayView is ViewGroup) {
                for (i in 0 until (loadingOverlayView as ViewGroup).childCount) {
                    val child = (loadingOverlayView as ViewGroup).getChildAt(i)
                    if (child is LottieAnimationView) {
                        child.addValueCallback(
                            com.airbnb.lottie.model.KeyPath("**"),
                            com.airbnb.lottie.LottieProperty.COLOR_FILTER
                        ) { android.graphics.PorterDuffColorFilter(corAzulGlpi, android.graphics.PorterDuff.Mode.SRC_ATOP) }
                    }
                }
            }

            // 5. Animação de Entrada do Avatar ("Surgir")
            val ivAvatar = findViewById<LottieAnimationView>(R.id.iv_avatar_perfil)
            ivAvatar?.let { lottie ->
                lottie.alpha = 0f
                lottie.scaleX = 0.5f
                lottie.scaleY = 0.5f
                lottie.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(1000)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .start()
            }

            // 6. Swipe Refresh
            swipeRefresh = findViewById(R.id.swipe_refresh_profile)
            swipeRefresh?.setColorSchemeColors(corAzulGlpi)
            swipeRefresh?.setOnRefreshListener {
                carregarDadosIniciais()
            }

            // 7. Atualização de fundo constante para manter dados vitais da sessão e opções de Perfil
            carregarDadosIniciais()
        } catch (e: Exception) {
            AlertHelper.exibirAlertaPremium(this, "Erro ao iniciar Perfil: ${e.message}", isError = true)
            finish()
        }
    }

    private fun configurarNavegacaoInferior() {
        val navView = findViewById<BottomNavigationView>(R.id.bottom_navigation_profile_detail) ?: return
        navView.selectedItemId = R.id.nav_perfil
        navView.labelVisibilityMode = BottomNavigationView.LABEL_VISIBILITY_UNLABELED
        
        // Ajustar margens dinamicamente para respeitar as barras do sistema (Insects)
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(navView) { view: View, insets: androidx.core.view.WindowInsetsCompat ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            val layoutParams = view.layoutParams as android.view.ViewGroup.MarginLayoutParams
            layoutParams.bottomMargin = systemBars.bottom + 16
            view.layoutParams = layoutParams
            insets
        }

        navView.setOnItemSelectedListener { item ->
            if (item.itemId == R.id.nav_perfil) return@setOnItemSelectedListener true
            
            val intent = when (item.itemId) {
                R.id.nav_tickets -> Intent(this, DashboardActivity::class.java)
                R.id.nav_agenda -> Intent(this, AgendaActivity::class.java)
                R.id.nav_inventario -> Intent(this, InventoryActivity::class.java)
                else -> null
            }
            
            intent?.let {
                startActivity(it)
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            }
            false
        }
    }

    private fun carregarDadosIniciais(forcedProfileName: String? = null) {
        swipeRefresh?.post { swipeRefresh?.isRefreshing = true }
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 🔥 Otimização Supra-Soma: Disparar TUDO em paralelo desde o primeiro milisegundo
                // Usamos o USER_ID do Config como "palpite" para começar logo as contagens
                val palpiteUserId = GlpiConfig.USER_ID
                
                val defSession = async { runCatching<retrofit2.Response<Map<String, Any>>> { GlpiRetrofit.api.getFullSession(TOKEN_SESSAO, TOKEN_APP) }.getOrNull() }
                val defEquipamentos = async { buscarTotalEquipamentos(palpiteUserId) }
                val defTickets = async { buscarTotalTicketsMes(palpiteUserId) }
                
                // Esperar os resultados
                val sessionResponse = defSession.await()
                val sessionInfo = sessionResponse?.body()
                
                // 2. Obter o ID real da sessão
                val sessionMap = sessionInfo?.get("session") as? Map<*, *>
                realUserId = (sessionMap?.get("glpiID") as? Double)?.toInt() 
                             ?: (sessionMap?.get("glpiID") as? Int)
                             ?: (sessionMap?.get("glpi_id") as? Double)?.toInt() 
                             ?: (sessionMap?.get("glpi_id") as? Int) ?: GlpiConfig.USER_ID
                
                val (finalEquip, finalTickets) = if (realUserId != palpiteUserId) {
                    // Recalcular apenas se o ID for diferente
                    val dE = async { buscarTotalEquipamentos(realUserId) }
                    val dT = async { buscarTotalTicketsMes(realUserId) }
                    Pair(dE.await(), dT.await())
                } else {
                    Pair(defEquipamentos.await(), defTickets.await())
                }
                
                withContext(Dispatchers.Main) {
                    val tvEquip = findViewById<TextView>(R.id.tv_count_equipamentos)
                    val tvTickets = findViewById<TextView>(R.id.tv_count_tickets_mes)
                    val lottieEquip = findViewById<com.airbnb.lottie.LottieAnimationView>(R.id.lottie_loading_equipamentos)
                    val lottieTickets = findViewById<com.airbnb.lottie.LottieAnimationView>(R.id.lottie_loading_tickets_mes)
                    
                    if (tvEquip != null) {
                        tvEquip.text = finalEquip.toString()
                        tvEquip.visibility = android.view.View.VISIBLE
                        lottieEquip?.visibility = android.view.View.GONE
                        // 🔥 Guardar Cache
                        PreferenceManager.setAssetsCount(this@ProfileDetailActivity, finalEquip)
                        // 🔥 Guardar o ID REAL do utilizador autenticado
                        PreferenceManager.setUserId(this@ProfileDetailActivity, realUserId)
                    }
                    if (tvTickets != null) {
                        tvTickets.text = finalTickets.toString()
                        tvTickets.visibility = android.view.View.VISIBLE
                        lottieTickets?.visibility = android.view.View.GONE
                        // 🔥 Guardar Cache
                        PreferenceManager.setTicketsCount(this@ProfileDetailActivity, finalTickets)
                    }
                    
                    carregarDadosPerfil(realUserId, sessionResponse, forcedProfileName)
                }
            } catch (e: Exception) {
                Log.e("PROFILE_FATAL", "Erro crítico ao carregar perfil: ${e.message}", e)
                withContext(Dispatchers.Main) { 
                    carregarDadosPerfil(realUserId, null) 
                }
            } finally {
                withContext(Dispatchers.Main) { swipeRefresh?.isRefreshing = false }
            }
        }
    }

    private suspend fun buscarTotalTicketsMes(userId: Int): Int {
        val targetId = userId
        
        return try {
            val resposta = GlpiRetrofit.api.getHistoricoTicketsPaginado(
                TOKEN_SESSAO, TOKEN_APP, 
                range = "0-300", 
                userId1 = targetId,
                userId2 = targetId,
                userId3 = targetId,
                userId4 = targetId,
                cacheBuster = System.currentTimeMillis()
            )
            
            val sdf = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault())
            val currentMonth = sdf.format(java.util.Date())
            
            val bodyData = if (resposta.isSuccessful) resposta.body()?.data ?: emptyList() else emptyList()
            val ticketsMes = bodyData.filter { ticket ->
                val dateCriacao = ticket["15"]?.toString() ?: ""
                val dateModificacaco = ticket["19"]?.toString() ?: ""
                dateCriacao.contains(currentMonth) || dateModificacaco.contains(currentMonth)
            }
            ticketsMes.size
        } catch (e: Exception) {
            0
        }
    }

    private suspend fun buscarTotalEquipamentos(userId: Int): Int {
        return try {
            val tipos = listOf("Computer", "Monitor", "NetworkEquipment", "Printer")
            // 🔥 Pesquisa por utilizador (campo 70) OU técnico responsável (campo 24)
            val deferreds = tipos.map { tipo ->
                CoroutineScope(Dispatchers.IO).async {
                    try {
                        val response = GlpiRetrofit.api.searchByUserOrTech(
                            itemtype = tipo,
                            sessionToken = TOKEN_SESSAO,
                            appToken = TOKEN_APP,
                            value0 = userId.toString(),
                            value1 = userId.toString(),
                            range = "0-999"
                        )
                        if (response.isSuccessful) response.body()?.totalcount ?: 0 else 0
                    } catch (e: Exception) { 0 }
                }
            }
            deferreds.awaitAll().sum()
        } catch (e: Exception) {
            0
        }
    }


    private suspend fun carregarDadosPerfil(userId: Int, sessionResponse: retrofit2.Response<Map<String, Any>>?, forcedProfileName: String? = null) {
        val tvNome = findViewById<TextView>(R.id.tv_nome_perfil_detail) ?: return
        val tvEmail = findViewById<TextView>(R.id.tv_email_perfil_detail) ?: return
        val tvPerfil = findViewById<TextView>(R.id.tv_entidade_perfil_detail) ?: return
        val tvSubTitulo = findViewById<TextView>(R.id.tv_tipo_perfil_detail) ?: return

        try {
            val responseSession = sessionResponse ?: runCatching<retrofit2.Response<Map<String, Any>>> { GlpiRetrofit.api.getFullSession(TOKEN_SESSAO, TOKEN_APP) }.getOrNull()
            val info = responseSession?.body()
            if (info == null) {
                 withContext(Dispatchers.Main) { 
                     tvPerfil.text = "Sessão expirada" 
                     Log.e("PROFILE_ERROR", "Sessão nula ou erro: ${responseSession?.code()}")
                 }
                 return
            }
            val sessionData = info["session"] as? Map<String, Any> ?: info
            
            val activeProfile = sessionData["glpiactiveprofile"] as? Map<String, Any>
            activeProfileId = (activeProfile?.get("id") as? Double)?.toInt() ?: (activeProfile?.get("id") as? Int) ?: 0
            
            // 🔥 Tentar várias chaves para o nome do perfil ativo
            var profileName = forcedProfileName 
                             ?: activeProfile?.get("name")?.toString() 
                             ?: sessionData["glpiactivename"]?.toString() 
                             ?: ""

            // Extrair lista de perfis disponíveis
            val profilesMap = sessionData["glpiprofiles"] as? Map<String, Any>
            availableProfiles = profilesMap?.mapNotNull { (key, value) ->
                val id = key.toIntOrNull() ?: return@mapNotNull null
                val name = (value as? Map<String, Any>)?.get("name")?.toString() ?: "Perfil $id"
                id to name
            } ?: emptyList()

            // 🔥 Fallback: Se não conseguimos extrair o nome do perfil ativo da sessão, 
            // mas temos o ID e a lista de perfis, procuramos na lista.
            if (profileName.isEmpty() && activeProfileId > 0) {
                profileName = availableProfiles.find { it.first == activeProfileId }?.second ?: ""
            }

            // Primeira atualização visual se tivermos pronto (Entity name)
            withContext(Dispatchers.Main) {
                if (profileName.isNotEmpty()) {
                    tvPerfil.text = profileName
                }
            }

            val response = try {
                GlpiRetrofit.api.searchByCriteria(
                    itemtype = "User",
                    sessionToken = TOKEN_SESSAO,
                    appToken = TOKEN_APP,
                    field = 2,
                    value = userId.toString(),
                    f0 = 1, f1 = 2, f2 = 5, f3 = 20, f4 = 80
                )
            } catch (e: Exception) { null }

            val userData = if (response != null && response.isSuccessful) response.body()?.data?.firstOrNull() else null
            
            if (userData != null) {
                val nameRaw = userData["1"]?.toString() ?: "Utilizador"
                val name = formatarNome(nameRaw)
                
                // 📧 Tratar Email
                val emailRaw = userData["5"]?.toString() ?: ""
                val email = if (emailRaw.isEmpty() || emailRaw == "sem@email.pt") "Não possui e-mail associado" else emailRaw
                
                // 🏢 Tratar Perfil e Entidade (Limpar formatos de lista [A, B, C])
                val rawProfile = userData["20"]?.toString() ?: profileName
                val cleanProfile = formatarListaUnica(rawProfile)
                
                val rawEntity = userData["80"]?.toString() ?: ""
                val cleanEntity = formatarListaUnica(rawEntity)

                withContext(Dispatchers.Main) {
                    tvNome.text = name
                    tvEmail.text = email
                    
                    // 🔥 Lógica de Prioridade: Mostrar o Perfil ATIVO (profileName) 
                    // Se não tivermos o ativo, usamos o primeiro da lista de perfis do utilizador
                    val textoPerfil = if (profileName.isNotEmpty()) profileName else cleanProfile
                    tvPerfil.text = textoPerfil
                    
                    tvSubTitulo.text = cleanEntity

                    // 🔥 Guardar em cache para a próxima vez ser instantâneo
                    // Guardamos o perfil ATIVO (profileName) como prioritário na cache
                    val perfilParaCache = if (profileName.isNotEmpty()) profileName else cleanProfile
                    
                    PreferenceManager.setUserName(this@ProfileDetailActivity, name)
                    PreferenceManager.setUserEmail(this@ProfileDetailActivity, email)
                    PreferenceManager.setUserProfile(this@ProfileDetailActivity, perfilParaCache)
                    PreferenceManager.setUserEntity(this@ProfileDetailActivity, cleanEntity)
                }
            } else {
                withContext(Dispatchers.Main) {
                    if (tvNome.text == "A carregar...") tvNome.text = "Não disponível"
                    if (tvEmail.text == "A carregar...") tvEmail.text = "Não disponível"
                    if (tvPerfil.text == "A carregar...") tvPerfil.text = "Não disponível"
                    if (tvSubTitulo.text == "A carregar...") tvSubTitulo.text = "Não disponível"
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                tvPerfil.text = "Erro ao carregar"
                if (tvNome.text == "A carregar...") tvNome.text = "Erro"
                if (tvEmail.text == "A carregar...") tvEmail.text = "Erro"
                if (tvSubTitulo.text == "A carregar...") tvSubTitulo.text = "Erro"
            }
        }
    }


    private fun preCarregarCacheLocal() {
        val tvNome = findViewById<TextView>(R.id.tv_nome_perfil_detail)
        val tvEmail = findViewById<TextView>(R.id.tv_email_perfil_detail)
        val tvPerfil = findViewById<TextView>(R.id.tv_entidade_perfil_detail)
        val tvSubTitulo = findViewById<TextView>(R.id.tv_tipo_perfil_detail)

        val tvEquip = findViewById<TextView>(R.id.tv_count_equipamentos)
        val tvTickets = findViewById<TextView>(R.id.tv_count_tickets_mes)
        val lottieEquip = findViewById<com.airbnb.lottie.LottieAnimationView>(R.id.lottie_loading_equipamentos)
        val lottieTickets = findViewById<com.airbnb.lottie.LottieAnimationView>(R.id.lottie_loading_tickets_mes)

        val cachedName = PreferenceManager.getUserName(this)
        val cachedEmail = PreferenceManager.getUserEmail(this)
        val cachedProfile = PreferenceManager.getUserProfile(this)
        val cachedEntity = PreferenceManager.getUserEntity(this)

        if (!cachedName.isNullOrEmpty() && cachedName != "Utilizador") {
            tvNome?.text = cachedName
        } else {
            tvNome?.text = "A carregar..."
        }

        if (!cachedEmail.isNullOrEmpty()) {
            tvEmail?.text = cachedEmail
        } else {
            tvEmail?.text = "A carregar..."
        }

        if (!cachedProfile.isNullOrEmpty()) {
            tvPerfil?.text = cachedProfile
        } else {
            tvPerfil?.text = "A carregar..."
        }

        if (!cachedEntity.isNullOrEmpty()) {
            tvSubTitulo?.text = cachedEntity
        } else {
            tvSubTitulo?.text = "A carregar..."
        }

        // 🔥 Garantir que o realUserId é lido da cache imediatamente (antes da chamada de rede)
        val cachedUserId = PreferenceManager.getUserId(this)
        if (cachedUserId > 0) {
            realUserId = cachedUserId
        }

        // Carregar contagens do cache (-1 significa sem cache)
        val cAssets = PreferenceManager.getAssetsCount(this)
        val cTickets = PreferenceManager.getTicketsCount(this)
        
        if (cAssets >= 0) {
            tvEquip?.text = cAssets.toString()
            tvEquip?.visibility = android.view.View.VISIBLE
            lottieEquip?.visibility = android.view.View.GONE
        }
        
        if (cTickets >= 0) {
            tvTickets?.text = cTickets.toString()
            tvTickets?.visibility = android.view.View.VISIBLE
            lottieTickets?.visibility = android.view.View.GONE
        }
    }


    override fun onBackPressed() {
        val drawer = drawerLayout
        if (drawer != null && drawer.isDrawerOpen(androidx.core.view.GravityCompat.START)) {
            drawer.closeDrawer(androidx.core.view.GravityCompat.START)
        }
    }

    private fun formatarListaUnica(raw: String): String {
        if (raw.isEmpty()) return ""
        // Se o formato fôr [A, B, C], removemos os parênteses
        val limpo = raw.trim().removePrefix("[").removeSuffix("]")
        
        // Dividir por vírgula e tirar espaços
        val itens = limpo.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        
        // Se estiver vazio, retorna a original limpa
        if (itens.isEmpty()) return limpo
        
        // Retornar apenas valores ÚNICOS e juntar com vírgula
        return itens.distinct().joinToString(", ")
    }

    private fun formatarNome(nome: String): String {
        if (nome.isEmpty()) return nome
        return nome.split(".")
            .joinToString(" ") { word ->
                word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
    }

    override fun dispatchTouchEvent(ev: android.view.MotionEvent?): Boolean {
        KeyboardHelper.handleTouchOutside(this, ev)
        return super.dispatchTouchEvent(ev)
    }
}