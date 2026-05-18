package com.example.glpimobile

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.airbnb.lottie.LottieAnimationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*

class MyTicketsHistoryActivity : AppCompatActivity() {

    private lateinit var rvHistory: RecyclerView
    private lateinit var adapter: TicketHistoryAdapter
    private lateinit var lottieLoading: LottieAnimationView
    private lateinit var tvEmpty: TextView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var tvTitulo: TextView
    private lateinit var etSearch: EditText
    private lateinit var btnFilter: FrameLayout
    private lateinit var btnAnterior: androidx.constraintlayout.widget.ConstraintLayout
    private lateinit var btnProximo: androidx.constraintlayout.widget.ConstraintLayout
    private lateinit var nestedScroll: androidx.core.widget.NestedScrollView

    private var fullTicketsList: List<Map<String, Any>> = listOf()
    private var filteredTicketsList: List<Map<String, Any>> = listOf()
    private var currentQuery: String = ""
    private var currentStatusFilter: String = "TODOS"
    private var globalSearchData: List<Map<String, Any>>? = null
    
    private var currentPage: Int = 0
    private val pageSize: Int = 10
    private var filterMonthOnly: Boolean = false
    
    private val searchHandler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null

    private val TOKEN_SESSAO get() = GlpiConfig.SESSION_TOKEN
    private val TOKEN_APP get() = GlpiConfig.APP_TOKEN

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_tickets_history)

        filterMonthOnly = intent.getBooleanExtra("EXTRA_FILTER_MONTH", false)

        tvTitulo = findViewById(R.id.tv_titulo_my_tickets)
        rvHistory = findViewById(R.id.rv_my_tickets_history)
        lottieLoading = findViewById(R.id.lottie_loading_my_tickets)
        tvEmpty = findViewById(R.id.tv_empty_my_tickets)
        swipeRefresh = findViewById(R.id.swipeRefresh_my_tickets)
        etSearch = findViewById(R.id.et_search_history)
        btnFilter = findViewById(R.id.btn_filter_history)
        btnAnterior = findViewById(R.id.btn_pagina_anterior_history)
        btnProximo = findViewById(R.id.btn_proxima_pagina_history)
        nestedScroll = findViewById(R.id.nested_scroll_my_tickets)

        // Aplicar cor azul GLPI à animação de carregamento
        val corAzulGlpi = androidx.core.content.ContextCompat.getColor(this, R.color.azul_glpi)
        lottieLoading.addValueCallback(com.airbnb.lottie.model.KeyPath("**"), com.airbnb.lottie.LottieProperty.COLOR_FILTER) {
            android.graphics.PorterDuffColorFilter(corAzulGlpi, android.graphics.PorterDuff.Mode.SRC_ATOP)
        }

        rvHistory.layoutManager = LinearLayoutManager(this)
        adapter = TicketHistoryAdapter(mutableListOf())
        rvHistory.adapter = adapter

        findViewById<FrameLayout>(R.id.btn_voltar_my_tickets).setOnClickListener {
            onBackPressed()
        }

        // Aplicar cor Branca à seta do Cabeçalho
        val lottieSetaHeader = findViewById<com.airbnb.lottie.LottieAnimationView>(R.id.lottie_seta_my_tickets)
        lottieSetaHeader?.addValueCallback(com.airbnb.lottie.model.KeyPath("**"), com.airbnb.lottie.LottieProperty.COLOR_FILTER) {
            android.graphics.PorterDuffColorFilter(android.graphics.Color.WHITE, android.graphics.PorterDuff.Mode.SRC_ATOP)
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().lowercase().trim()
                currentQuery = query
                
                // 1. Filtragem Local Imediata
                applyFilters()
                
                // 2. Lógica Debounce para Pesquisa Global (no servidor)
                searchRunnable?.let { searchHandler.removeCallbacks(it) }
                
                if (query.isNotEmpty()) {
                    searchRunnable = Runnable {
                        executarPesquisaGlobal(query)
                    }
                    searchHandler.postDelayed(searchRunnable!!, 600)
                } else {
                    globalSearchData = null
                }
            }
        })

        // Botão de Filtro (Abre Menu)
        btnFilter.setOnClickListener {
            showFilterMenu(it)
        }

        // Navegação de Paginação
        btnAnterior.setOnClickListener {
            if (currentPage > 0) {
                currentPage--
                applyPagination()
            }
        }

        btnProximo.setOnClickListener {
            if ((currentPage + 1) * pageSize < filteredTicketsList.size) {
                currentPage++
                applyPagination()
            }
        }

        swipeRefresh.setOnRefreshListener {
            carregarHistorico()
        }

        playFilterAnimation() // Play on open
        carregarHistorico()
    }

    private fun showFilterMenu(view: View) {
        val items = arrayOf("TODOS", "NOVO", "PROCESSAMENTO", "RESOLVIDOS")
        val statusIds = arrayOf("TODOS", "1", "2,3,4", "5,6")
        
        val dialogView = layoutInflater.inflate(R.layout.dialog_custom_filter_state, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val container = dialogView.findViewById<android.widget.LinearLayout>(R.id.container_filter_options)
        val btnClose = dialogView.findViewById<android.view.View>(R.id.btn_close_filter)

        btnClose.setOnClickListener { dialog.dismiss() }

        val d = resources.displayMetrics.density

        items.forEachIndexed { index, title ->
            val isSelected = currentStatusFilter == statusIds[index]
            val row = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    (48 * d).toInt()
                )
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding((24 * d).toInt(), 0, (24 * d).toInt(), 0)
                
                val outValue = android.util.TypedValue()
                context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
                setBackgroundResource(outValue.resourceId)
                
                setOnClickListener {
                    currentStatusFilter = statusIds[index]
                    AlertHelper.exibirAlertaPremium(this@MyTicketsHistoryActivity, "Filtrado por: $title")
                    playFilterAnimation()
                    applyFilters()
                    dialog.dismiss()
                }
            }

            val tv = TextView(this).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                text = title
                setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.texto_principal))
                textSize = 14f
                isAllCaps = true
                typeface = androidx.core.content.res.ResourcesCompat.getFont(context, R.font.amiko_bold)
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                gravity = android.view.Gravity.START or android.view.Gravity.CENTER_VERTICAL
            }

            val rb = android.widget.RadioButton(this).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                )
                buttonTintList = android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(context, R.color.azul_glpi))
                isChecked = isSelected
                isClickable = false
                isFocusable = false
            }

            row.addView(tv)
            row.addView(rb)
            container.addView(row)
        }

        dialog.show()
        dialog.window?.setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun applyFilters() {
        // Se temos resultados de uma pesquisa global e ainda há texto na barra, usamos esses
        val baseList = if (currentQuery.isNotEmpty() && globalSearchData != null) {
            globalSearchData!!
        } else {
            fullTicketsList
        }

        var filtered = baseList

        // 1. Filtrar por Estado
        if (currentStatusFilter != "TODOS") {
            val validStatuses = currentStatusFilter.split(",")
            filtered = filtered.filter { ticket ->
                val rawEstado = ticket["12"]?.toString() ?: ""
                val cleanEstado = if (rawEstado.contains(".")) rawEstado.substringBefore(".") else rawEstado
                validStatuses.contains(cleanEstado)
            }
        }

        // 2. Filtrar por Pesquisa (Título ou ID)
        if (currentQuery.isNotEmpty()) {
            filtered = filtered.filter { ticket ->
                val titulo = ticket["1"]?.toString()?.lowercase() ?: ""
                val rawId = ticket["2"]?.toString() ?: ticket["id"]?.toString() ?: ""
                val cleanId = if (rawId.contains(".")) rawId.substringBefore(".") else rawId
                
                titulo.contains(currentQuery) || cleanId.contains(currentQuery)
            }
        }

        filteredTicketsList = filtered
        currentPage = 0 // Sempre resetar para a primeira página ao filtrar
        applyPagination()
        
        if (currentStatusFilter == "TODOS") {
            btnFilter.setBackgroundResource(R.drawable.bg_cartao_brilhante)
        } else {
            btnFilter.setBackgroundResource(R.drawable.bg_filtro_borda_azul_transparente)
        }

        if (filtered.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            tvEmpty.text = "Nenhum ticket corresponde aos filtros."
        } else {
            tvEmpty.visibility = View.GONE
        }
    }

    private fun applyPagination() {
        val start = currentPage * pageSize
        val end = minOf(start + pageSize, filteredTicketsList.size)
        
        val pageItems = if (filteredTicketsList.isEmpty()) {
            listOf()
        } else {
            filteredTicketsList.subList(start, end)
        }
        
        adapter.updateList(pageItems)
        rvHistory.scheduleLayoutAnimation()

        btnAnterior.visibility = if (currentPage > 0) View.VISIBLE else View.GONE
        btnProximo.visibility = if (end < filteredTicketsList.size) View.VISIBLE else View.GONE
        
        // Garantir que a lista surge sempre do topo
        nestedScroll.smoothScrollTo(0, 0)
    }

    private fun formatarNome(nome: String): String {
        if (nome.isEmpty()) return nome
        return nome.split(".")
            .joinToString(" ") { word ->
                word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
    }

    private fun executarPesquisaGlobal(query: String) {
        if (query.isEmpty()) return
        
        lifecycleScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) { 
                lottieLoading.visibility = View.VISIBLE
                tvEmpty.visibility = View.GONE
            }
            
            try {
                // Identificar se é ID (apenas números) ou texto
                val isNumeric = query.all { it.isDigit() }
                
                val response = if (isNumeric) {
                    // Pesquisar por ID (Campo 2)
                    GlpiRetrofit.api.pesquisarTickets(TOKEN_SESSAO, TOKEN_APP, 2, "contains", query)
                } else {
                    // Pesquisar por Título/Assunto (Campo 1)
                    GlpiRetrofit.api.pesquisarTickets(TOKEN_SESSAO, TOKEN_APP, 1, "contains", query)
                }
                
                withContext(Dispatchers.Main) {
                    lottieLoading.visibility = View.GONE
                    
                    if (response.isSuccessful) {
                        val searchData = response.body()?.data ?: emptyList()
                        if (searchData.isNotEmpty()) {
                            // Guardar resultados e reaplicar filtros (para respeitar o estado selecionado)
                            globalSearchData = searchData
                            applyFilters()
                        } else {
                            // Se não encontrou nada globalmente e a lista local já estava vazia
                            if (filteredTicketsList.isEmpty()) {
                                tvEmpty.visibility = View.VISIBLE
                                tvEmpty.text = "Nenhum ticket encontrado no GLPI."
                                adapter.updateList(emptyList())
                                btnAnterior.visibility = View.GONE
                                btnProximo.visibility = View.GONE
                            }
                        }
                    } else {
                        // Erro silencioso ou retry local
                        if (filteredTicketsList.isEmpty()) {
                            tvEmpty.visibility = View.VISIBLE
                            tvEmpty.text = "Erro ao pesquisar no servidor."
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    lottieLoading.visibility = View.GONE
                    if (filteredTicketsList.isEmpty()) {
                        tvEmpty.visibility = View.VISIBLE
                        tvEmpty.text = "Sem ligação ao servidor."
                    }
                }
            }
        }
    }

    private fun carregarHistorico() {
        lottieLoading.visibility = View.VISIBLE
        tvEmpty.visibility = View.GONE
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Obter o ID do utilizador real da sessão
                val response = GlpiRetrofit.api.getFullSession(TOKEN_SESSAO, TOKEN_APP)
                val sessionInfo = response.body()
                val activeProfile = sessionInfo?.get("glpiactiveprofile") as? Map<String, Any>
                val activeUser = sessionInfo?.get("glpiactive_user") as? Map<String, Any>
                
                val tempId = activeUser?.get("id") ?: activeProfile?.get("users_id")
                
                // Prioridade absoluta ao ID definido no PreferenceManager (aquele que o utilizador escolheu)
                val realUserId = PreferenceManager.getUserId(this@MyTicketsHistoryActivity)
                
                val realUserName = activeUser?.get("name")?.toString() ?: activeProfile?.get("name")?.toString() ?: "Utilizador #$realUserId"

                // Chamar API para obter os 150 tickets
                val resposta = GlpiRetrofit.api.getHistoricoTicketsPaginado(
                    TOKEN_SESSAO, TOKEN_APP, 
                    range = "0-150", 
                    userId1 = realUserId,
                    userId2 = realUserId,
                    userId3 = realUserId,
                    cacheBuster = System.currentTimeMillis()
                )

                withContext(Dispatchers.Main) {
                    lottieLoading.visibility = View.GONE
                    swipeRefresh.isRefreshing = false
                    
                    // Atualizar título para HISTÓRICO
                    tvTitulo.text = "HISTÓRICO"

                    val bodyData = if (resposta.isSuccessful) resposta.body()?.data ?: emptyList() else emptyList()
                    if (bodyData.isEmpty()) {
                        fullTicketsList = listOf()
                        tvEmpty.visibility = View.VISIBLE
                        tvEmpty.text = "Nenhum ticket encontrado no histórico."
                    } else {
                        // Não removemos tickets, mostramos tudo mas aplicamos o contexto visual no applyFilters -> applyPagination
                        fullTicketsList = bodyData
                        applyFilters()
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    lottieLoading.visibility = View.GONE
                    swipeRefresh.isRefreshing = false
                    tvEmpty.visibility = View.VISIBLE
                    tvEmpty.text = "Erro ao carregar tickets: ${e.message}"
                }
            }
        }
    }

    override fun dispatchTouchEvent(ev: android.view.MotionEvent?): Boolean {
        KeyboardHelper.handleTouchOutside(this, ev)
        return super.dispatchTouchEvent(ev)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }
}
