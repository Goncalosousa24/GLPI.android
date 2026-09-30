package com.example.glpimobile

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.airbnb.lottie.LottieAnimationView
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*

class GeneralViewTicketsActivity : AppCompatActivity() {

    private lateinit var rvHistory: RecyclerView
    private lateinit var adapter: GeneralViewTicketsAdapter
    private lateinit var lottieLoading: LottieAnimationView
    private lateinit var tvEmpty: TextView
    private lateinit var swipeRefresh: SwipeRefreshLayout
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
    
    private val searchHandler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null

    private val TOKEN_SESSAO get() = GlpiConfig.SESSION_TOKEN
    private val TOKEN_APP get() = GlpiConfig.APP_TOKEN

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_general_view_tickets)

        rvHistory = findViewById(R.id.rv_general_view_tickets)
        lottieLoading = findViewById(R.id.lottie_loading_general_view)
        tvEmpty = findViewById(R.id.tv_empty_general_view)
        swipeRefresh = findViewById(R.id.swipeRefresh_general_view)
        etSearch = findViewById(R.id.et_search_general)
        btnFilter = findViewById(R.id.btn_filter_general)
        btnAnterior = findViewById(R.id.btn_pagina_anterior_general)
        btnProximo = findViewById(R.id.btn_proxima_pagina_general)
        nestedScroll = findViewById(R.id.nested_scroll_general_view)

        // Aplicar cor azul GLPI à animação de carregamento
        val corAzulGlpi = androidx.core.content.ContextCompat.getColor(this, R.color.azul_glpi)
        lottieLoading.addValueCallback(com.airbnb.lottie.model.KeyPath("**"), com.airbnb.lottie.LottieProperty.COLOR_FILTER) {
            android.graphics.PorterDuffColorFilter(corAzulGlpi, android.graphics.PorterDuff.Mode.SRC_ATOP)
        }

        // Configurar RecyclerView
        rvHistory.layoutManager = LinearLayoutManager(this)
        adapter = GeneralViewTicketsAdapter(mutableListOf())
        rvHistory.adapter = adapter

        // Botão voltar
        findViewById<FrameLayout>(R.id.btn_voltar_general_view).setOnClickListener {
            onBackPressed()
        }

        // Aplicar cor Branca à seta do Cabeçalho
        val lottieSetaHeader = findViewById<com.airbnb.lottie.LottieAnimationView>(R.id.lottie_seta_general_view)
        lottieSetaHeader?.addValueCallback(com.airbnb.lottie.model.KeyPath("**"), com.airbnb.lottie.LottieProperty.COLOR_FILTER) {
            android.graphics.PorterDuffColorFilter(android.graphics.Color.WHITE, android.graphics.PorterDuff.Mode.SRC_ATOP)
        }

        // Configurar pesquisa com debounce
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().lowercase().trim()
                currentQuery = query
                
                // 1. Filtragem Local Imediata
                applyFilters()
                
                // 2. Lógica Debounce para Pesquisa Global no Servidor
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

        // Botão de Filtro
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
            carregarTickets()
        }

        playFilterAnimation() // Play on open
        carregarTickets()
    }

    private fun showFilterMenu(view: View) {
        val items = arrayOf(
            "Todos",
            "Novo",
            "A processar (atribuído)",
            "A processar (planeado)",
            "Aguardando",
            "Resolvido",
            "Encerrado",
            "Não Resolvido (todos excepto encerrados e resolvidos)",
            "Não Encerrado (todos menos unicamente Encerrados)",
            "A processar (todos a processar)",
            "Resolvidos + Encerrados"
        )
        val statusIds = arrayOf(
            "TODOS",
            "1",
            "2",
            "3",
            "4",
            "5",
            "6",
            "1,2,3,4",
            "1,2,3,4,5",
            "2,3",
            "5,6"
        )
        
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
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                )
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding((24 * d).toInt(), (12 * d).toInt(), (24 * d).toInt(), (12 * d).toInt())
                
                val outValue = android.util.TypedValue()
                context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
                setBackgroundResource(outValue.resourceId)
                
                setOnClickListener {
                    currentStatusFilter = statusIds[index]
                    AlertHelper.exibirAlertaPremium(this@GeneralViewTicketsActivity, "Filtrado por: $title")
                    playFilterAnimation()
                    carregarTickets()
                    dialog.dismiss()
                }
            }

            val tv = TextView(this).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                text = title
                setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.texto_principal))
                textSize = 14f
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
                val rawEstado = extractId(ticket["12"])
                validStatuses.contains(rawEstado)
            }
        }

        // 2. Filtrar por Pesquisa Local (Título ou ID)
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
        
        // Active Filter UI state
        if (currentStatusFilter == "TODOS") {
            btnFilter.setBackgroundResource(R.drawable.bg_cartao_brilhante)
        } else {
            btnFilter.setBackgroundResource(R.drawable.bg_filtro_ativo)
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

    private fun executarPesquisaGlobal(query: String) {
        if (query.isEmpty()) return
        
        lifecycleScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) { 
                lottieLoading.visibility = View.VISIBLE
                tvEmpty.visibility = View.GONE
            }
            
            try {
                val isNumeric = query.all { it.isDigit() }
                
                // Realizar a pesquisa de tickets activos no GLPI (isDeleted = 0)
                val response = if (isNumeric) {
                    GlpiRetrofit.api.pesquisarTickets(TOKEN_SESSAO, TOKEN_APP, 2, "contains", query, range = "0-299", isDeleted = 0)
                } else {
                    GlpiRetrofit.api.pesquisarTickets(TOKEN_SESSAO, TOKEN_APP, 1, "contains", query, range = "0-299", isDeleted = 0)
                }
                
                withContext(Dispatchers.Main) {
                    lottieLoading.visibility = View.GONE
                    
                    if (response.isSuccessful) {
                        val searchData = response.body()?.data ?: emptyList()
                        if (searchData.isNotEmpty()) {
                            globalSearchData = searchData
                            applyFilters()
                        } else {
                            if (filteredTicketsList.isEmpty()) {
                                tvEmpty.visibility = View.VISIBLE
                                tvEmpty.text = "Nenhum ticket encontrado no GLPI."
                                adapter.updateList(emptyList())
                                btnAnterior.visibility = View.GONE
                                btnProximo.visibility = View.GONE
                            }
                        }
                    } else {
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

    private fun buildStatusCriteria(statusFilter: String): Map<String, String> {
        val criteriaMap = mutableMapOf<String, String>()
        if (statusFilter == "TODOS") return criteriaMap

        val statuses = statusFilter.split(",")
        if (statuses.size == 1) {
            criteriaMap["criteria[0][field]"] = "12"
            criteriaMap["criteria[0][searchtype]"] = "equals"
            criteriaMap["criteria[0][value]"] = statuses[0]
        } else {
            statuses.forEachIndexed { index, status ->
                if (index > 0) {
                    criteriaMap["criteria[$index][link]"] = "OR"
                }
                criteriaMap["criteria[$index][field]"] = "12"
                criteriaMap["criteria[$index][searchtype]"] = "equals"
                criteriaMap["criteria[$index][value]"] = status
            }
        }
        return criteriaMap
    }

    private fun carregarTickets() {
        lottieLoading.visibility = View.VISIBLE
        tvEmpty.visibility = View.GONE
        
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val filter = currentStatusFilter
                val resposta = if (filter == "TODOS") {
                    GlpiRetrofit.api.getTodosTicketsAtivos(
                        TOKEN_SESSAO, TOKEN_APP, 
                        range = "0-149",
                        isDeleted = 0
                    )
                } else {
                    val criteriaMap = buildStatusCriteria(filter)
                    GlpiRetrofit.api.pesquisarTicketsDinamico(
                        TOKEN_SESSAO, TOKEN_APP,
                        range = "0-149",
                        isDeleted = 0,
                        criteria = criteriaMap
                    )
                }

                withContext(Dispatchers.Main) {
                    lottieLoading.visibility = View.GONE
                    swipeRefresh.isRefreshing = false
                    
                    val bodyData = if (resposta.isSuccessful) resposta.body()?.data ?: emptyList() else emptyList()
                    if (bodyData.isEmpty()) {
                        fullTicketsList = listOf()
                        tvEmpty.visibility = View.VISIBLE
                        tvEmpty.text = "Nenhum ticket encontrado."
                    } else {
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

    private fun extractId(valor: Any?): String {
        if (valor == null) return ""
        if (valor is Map<*, *>) return valor["id"]?.toString()?.substringBefore(".") ?: ""
        if (valor is List<*>) {
            val primeiro = valor.firstOrNull()
            if (primeiro is Map<*, *>) return primeiro["id"]?.toString()?.substringBefore(".") ?: ""
            return primeiro?.toString()?.substringBefore(".") ?: ""
        }
        val s = valor.toString()
        return if (s.contains(".")) s.substringBefore(".") else s
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
