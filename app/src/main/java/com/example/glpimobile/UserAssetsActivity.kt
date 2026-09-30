package com.example.glpimobile

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import androidx.constraintlayout.widget.ConstraintLayout
import kotlinx.coroutines.*

class UserAssetsActivity : AppCompatActivity() {

    enum class FilterType { ALL, WITH_ASSETS, WITHOUT_ASSETS }

    private lateinit var rvUserAssets: RecyclerView
    private lateinit var nestedScrollView: androidx.core.widget.NestedScrollView
    private lateinit var progressLoader: LottieAnimationView
    private lateinit var tvEmpty: TextView
    private lateinit var searchBar: EditText
    private lateinit var searchContainer: View
    private lateinit var scrollChips: View
    
    // Chips
    private lateinit var chipTodos: TextView
    private lateinit var chipComEquip: TextView
    private lateinit var chipSemEquip: TextView

    // Paginação
    private lateinit var layoutPaginacao: ConstraintLayout
    private lateinit var btnAnterior: ConstraintLayout
    private lateinit var btnProximo: ConstraintLayout
    private var paginaAtual = 0
    private val itensPorPagina = 10
    
    // Adaptadores e Listas
    private lateinit var inventoryAdapter: InventoryAdapter
    private lateinit var profileAdapter: ProfileAdapter
    private var fullUserList = mutableListOf<Map<String, Any>>()
    private var listaFiltradaTotal = mutableListOf<Map<String, Any>>()
    private var usersWithAssetsIds = mutableSetOf<String>()
    
    private var currentFilter = FilterType.ALL
    private var isShowingAssets = false
    private var isFromProfile = false
    private var selectedUserId: String? = null
    
    private var searchJob: Job? = null
    private val TOKEN_SESSAO = GlpiConfig.SESSION_TOKEN
    private val TOKEN_APP = GlpiConfig.APP_TOKEN

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_assets)

        // Inicialização UI
        rvUserAssets = findViewById(R.id.rv_user_assets)
        nestedScrollView = findViewById(R.id.nested_scroll_user_assets)
        progressLoader = findViewById(R.id.progress_user_assets)
        tvEmpty = findViewById(R.id.tv_empty_user_assets)
        searchBar = findViewById(R.id.search_bar_user_assets)
        searchContainer = findViewById(R.id.search_container_user_assets)
        scrollChips = findViewById(R.id.scroll_chips_user_assets)
        
        chipTodos = findViewById<TextView>(R.id.chip_todos_user_assets)
        chipComEquip = findViewById<TextView>(R.id.chip_com_equip_user_assets)
        chipSemEquip = findViewById<TextView>(R.id.chip_sem_equip_user_assets)

        layoutPaginacao = findViewById(R.id.pagination_user_assets)
        btnAnterior = findViewById(R.id.btn_pagina_anterior_user)
        btnProximo = findViewById(R.id.btn_proxima_pagina_user)

        rvUserAssets.layoutManager = LinearLayoutManager(this)
        
        // Inicializar adaptadores
        inventoryAdapter = InventoryAdapter(emptyList())
        profileAdapter = ProfileAdapter(emptyList(), showEmail = true) { user ->
            val userId = user["2"]?.toString() ?: ""
            val userName = user["1"]?.toString() ?: "Utilizador"
            selecionarUtilizador(userId, userName)
        }
        
        rvUserAssets.adapter = profileAdapter

        // Configurar Lotties
        val corBranca = ContextCompat.getColor(this, android.R.color.white)
        val corAzulGlpi = ContextCompat.getColor(this, R.color.azul_glpi)

        aplicarCorLottie(findViewById(R.id.lottie_voltar_user_assets), corBranca)
        aplicarCorLottie(findViewById(R.id.lupa_anim_user_assets), corAzulGlpi)
        aplicarCorLottie(progressLoader, corAzulGlpi)
        aplicarCorLottie(findViewById(R.id.seta_voltar_user), corAzulGlpi)
        aplicarCorLottie(findViewById(R.id.seta_avancar_user), corAzulGlpi)

        findViewById<FrameLayout>(R.id.btn_voltar_user_assets).setOnClickListener {
            onBackPressed()
        }

        // Listeners dos Chips
        chipTodos.setOnClickListener { atualizarFiltro(FilterType.ALL) }
        chipComEquip.setOnClickListener { atualizarFiltro(FilterType.WITH_ASSETS) }
        chipSemEquip.setOnClickListener { atualizarFiltro(FilterType.WITHOUT_ASSETS) }
        
        // Estado inicial do chip
        chipTodos.isSelected = true

        // Listeners de Paginação
        btnAnterior.setOnClickListener {
            if (paginaAtual > 0) {
                paginaAtual--
                atualizarExibicaoPaginada()
            }
        }
        btnProximo.setOnClickListener {
            if ((paginaAtual + 1) * itensPorPagina < listaFiltradaTotal.size) {
                paginaAtual++
                atualizarExibicaoPaginada()
            }
        }

        // Listener de Pesquisa
        searchBar.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (!isShowingAssets) {
                    paginaAtual = 0
                    aplicarTodosFiltros()
                }
            }
            override fun beforeTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })

        // Limpar foco e fechar teclado ao tocar fora
        findViewById<View>(R.id.user_assets_layout_main)?.setOnTouchListener { _, _ ->
            if (searchBar.hasFocus()) {
                searchBar.clearFocus()
                val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.hideSoftInputFromWindow(searchBar.windowToken, 0)
            }
            false
        }
        
        // Carregar dados ou filtrar por utilizador vindo do Perfil
        val extraId = intent.getStringExtra("EXTRA_USER_ID")
        val extraName = intent.getStringExtra("EXTRA_USER_NAME")

        if (extraId != null && extraName != null) {
            isFromProfile = true
            selecionarUtilizador(extraId, extraName)
        } else {
            carregarDadosIniciais()
        }
    }

    private fun aplicarCorLottie(lottieView: LottieAnimationView?, cor: Int) {
        lottieView?.addValueCallback(
            KeyPath("**"),
            LottieProperty.COLOR_FILTER
        ) { PorterDuffColorFilter(cor, PorterDuff.Mode.SRC_ATOP) }
    }

    private fun carregarDadosIniciais() {
        progressLoader.visibility = View.VISIBLE
        tvEmpty.visibility = View.GONE
        layoutPaginacao.visibility = View.GONE
        searchContainer.visibility = View.VISIBLE
        scrollChips.visibility = View.VISIBLE
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Carregar utilizadores em chunks
                val forcedisplayUser = mapOf(
                    "forcedisplay[0]" to "1",
                    "forcedisplay[1]" to "2"
                )
                
                val combinedUsers = mutableListOf<Map<String, Any>>()
                var offsetUser = 0
                val limit = 500
                var hasMoreUser = true
                
                while (hasMoreUser) {
                    val range = "$offsetUser-${offsetUser + limit - 1}"
                    val responseUsers = GlpiRetrofit.api.searchItems(
                        itemtype = "User",
                        sessionToken = TOKEN_SESSAO,
                        appToken = TOKEN_APP,
                        range = range,
                        criteria = forcedisplayUser
                    )
                    
                    if (responseUsers.isSuccessful) {
                        val data = responseUsers.body()?.data
                        if (!data.isNullOrEmpty()) {
                            combinedUsers.addAll(data)
                            if (data.size < limit) {
                                hasMoreUser = false
                            } else {
                                offsetUser += limit
                            }
                        } else {
                            hasMoreUser = false
                        }
                    } else {
                        hasMoreUser = false
                    }
                }

                // 2. Carregar IDs/Logins de utilizadores com equipamentos em chunks
                val tipos = listOf("Computer", "Monitor", "NetworkEquipment", "Printer")
                val assetUserIdentifiers = mutableSetOf<String>()
                
                tipos.forEach { tipo ->
                    try {
                        val forcedisplayAsset = mapOf(
                            "forcedisplay[0]" to "70" // Campo do utilizador
                        )
                        
                        var offsetAsset = 0
                        var hasMoreAsset = true
                        
                        while (hasMoreAsset) {
                            val range = "$offsetAsset-${offsetAsset + limit - 1}"
                            val res = GlpiRetrofit.api.searchItems(
                                itemtype = tipo,
                                sessionToken = TOKEN_SESSAO,
                                appToken = TOKEN_APP,
                                range = range,
                                criteria = forcedisplayAsset
                            )

                            if (res.isSuccessful) {
                                val resData = res.body()?.data
                                if (!resData.isNullOrEmpty()) {
                                    resData.forEach { item ->
                                        val identifier = item["70"]?.toString() ?: ""
                                        if (identifier.isNotEmpty() && identifier != "0" && identifier != "null") {
                                            assetUserIdentifiers.add(identifier.lowercase())
                                        }
                                    }
                                    if (resData.size < limit) {
                                        hasMoreAsset = false
                                    } else {
                                        offsetAsset += limit
                                    }
                                } else {
                                    hasMoreAsset = false
                                }
                            } else {
                                hasMoreAsset = false
                            }
                        }
                    } catch (e: Exception) {}
                }
                
                withContext(Dispatchers.Main) {
                    fullUserList = combinedUsers.filter {
                        val name = it["1"]?.toString()?.lowercase() ?: ""
                        name != "glpi" && name != "tech" && name != "normal" && name != "post-only"
                    }.distinctBy { it["2"]?.toString() }.toMutableList()
                    
                    usersWithAssetsIds = assetUserIdentifiers
                    
                    aplicarTodosFiltros()
                    progressLoader.visibility = View.GONE
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressLoader.visibility = View.GONE
                    tvEmpty.text = "Erro ao carregar dados."
                    tvEmpty.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun atualizarFiltro(filtro: FilterType) {
        if (isShowingAssets) return
        
        currentFilter = filtro
        paginaAtual = 0
        chipTodos.isSelected = (filtro == FilterType.ALL)
        chipComEquip.isSelected = (filtro == FilterType.WITH_ASSETS)
        chipSemEquip.isSelected = (filtro == FilterType.WITHOUT_ASSETS)
        
        aplicarTodosFiltros()
    }

    private fun aplicarTodosFiltros() {
        val query = searchBar.text.toString().trim()
        
        var filtrados = fullUserList.filter {
            val name = it["1"]?.toString() ?: ""
            KeyboardHelper.removeAccents(name).contains(KeyboardHelper.removeAccents(query), ignoreCase = true)
        }
        
        filtrados = when (currentFilter) {
            FilterType.ALL -> filtrados
            FilterType.WITH_ASSETS -> filtrados.filter { usersWithAssetsIds.contains(it["1"]?.toString()?.lowercase()) }
            FilterType.WITHOUT_ASSETS -> filtrados.filter { !usersWithAssetsIds.contains(it["1"]?.toString()?.lowercase()) }
        }
        
        listaFiltradaTotal = filtrados.toMutableList()
        atualizarExibicaoPaginada()
    }

    private fun atualizarExibicaoPaginada() {
        if (isShowingAssets) {
            layoutPaginacao.visibility = View.GONE
            return
        }

        val total = listaFiltradaTotal.size
        val inicio = paginaAtual * itensPorPagina
        val fim = minOf(inicio + itensPorPagina, total)
        
        if (total == 0) {
            profileAdapter.updateList(emptyList())
            tvEmpty.visibility = View.VISIBLE
            tvEmpty.text = "Nenhum utilizador encontrado."
            layoutPaginacao.visibility = View.GONE
            return
        }

        val subLista = listaFiltradaTotal.subList(inicio, fim)
        profileAdapter.updateList(subLista)
        rvUserAssets.scheduleLayoutAnimation()
        tvEmpty.visibility = View.GONE
        
        // Controles de Visibilidade
        layoutPaginacao.visibility = if (total > itensPorPagina) View.VISIBLE else View.GONE
        btnAnterior.visibility = if (paginaAtual > 0) View.VISIBLE else View.GONE
        btnProximo.visibility = if (fim < total) View.VISIBLE else View.GONE
        
        nestedScrollView.smoothScrollTo(0, 0)
    }

    private fun selecionarUtilizador(userId: String, userName: String) {
        selectedUserId = userId
        isShowingAssets = true
        searchContainer.visibility = View.GONE
        scrollChips.visibility = View.GONE
        layoutPaginacao.visibility = View.GONE
        pesquisarEquipamentosPorUtilizador(userId)
    }

    private fun pesquisarEquipamentosPorUtilizador(userId: String) {
        searchJob?.cancel()
        searchJob = CoroutineScope(Dispatchers.Main).launch {
            progressLoader.visibility = View.VISIBLE
            tvEmpty.visibility = View.GONE
            rvUserAssets.adapter = inventoryAdapter
            
            try {
                val allAssets = mutableListOf<Map<String, Any>>()
                val tipos = listOf("Computer", "Monitor", "NetworkEquipment", "Printer")

                withContext(Dispatchers.IO) {
                    val deferreds = mutableListOf<Deferred<List<Map<String, Any>>>>()
                    tipos.forEach { tipo ->
                        val deferred = async {
                            try {
                                // Pesquisa por utilizador (campo 70) OU técnico responsável (campo 24)
                                val res = GlpiRetrofit.api.searchByUserOrTech(
                                    itemtype = tipo,
                                    sessionToken = TOKEN_SESSAO,
                                    appToken = TOKEN_APP,
                                    value0 = userId,
                                    value1 = userId,
                                    range = "0-999"
                                )
                                val resData = if (res.isSuccessful) res.body()?.data ?: emptyList() else emptyList()
                                resData.map { item ->
                                    val map = item.toMutableMap()
                                    map["TipoReal"] = tipo
                                    map as Map<String, Any>
                                }
                            } catch (e: Exception) { emptyList<Map<String, Any>>() }
                        }
                        deferreds.add(deferred)
                    }
                    allAssets.addAll(deferreds.awaitAll().flatten())
                }

                withContext(Dispatchers.Main) {
                    inventoryAdapter.updateList(allAssets)
                    if (allAssets.isEmpty()) {
                        tvEmpty.text = "Este utilizador não tem equipamentos associados."
                        tvEmpty.visibility = View.VISIBLE
                    }
                    progressLoader.visibility = View.GONE
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    tvEmpty.text = "Erro ao pesquisar equipamentos."
                    tvEmpty.visibility = View.VISIBLE
                    progressLoader.visibility = View.GONE
                }
            }
        }
    }

    override fun dispatchTouchEvent(ev: android.view.MotionEvent?): Boolean {
        KeyboardHelper.handleTouchOutside(this, ev)
        return super.dispatchTouchEvent(ev)
    }

    override fun onBackPressed() {
        if (isFromProfile) {
            finish()
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            return
        }
        
        if (isShowingAssets) {
            isShowingAssets = false
            selectedUserId = null
            searchBar.setText("")
            searchBar.isEnabled = true
            searchContainer.visibility = View.VISIBLE
            scrollChips.visibility = View.VISIBLE
            rvUserAssets.adapter = profileAdapter
            paginaAtual = 0
            aplicarTodosFiltros()
            return
        }
        super.onBackPressed()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }
}
