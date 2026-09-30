package com.example.glpimobile

import android.content.Intent
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.view.View
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ResolvedTicketsActivity : AppCompatActivity() {

    private var fullTicketsList: List<Map<String, Any>> = listOf()
    private var filteredTicketsList: List<Map<String, Any>> = listOf()
    private lateinit var rvTickets: RecyclerView
    private lateinit var nestedScroll: androidx.core.widget.NestedScrollView
    private lateinit var swipeRefresh: androidx.swiperefreshlayout.widget.SwipeRefreshLayout
    private lateinit var btnAnterior: View
    private lateinit var btnProxima: View
    private lateinit var tvListaVazia: TextView

    private var currentPage = 0
    private val pageSize = 10
    private var currentOrder = "DESC"
    private var initialTicketId: String? = null
    private var isContextoPessoal = false

    // 0 = Todos, 5 = Resolvidos, 6 = Encerrados
    private var statusFiltro: Int = 0

    // 🔑 TOKENS SINCRONIZADOS
    private val TOKEN_SESSAO get() = GlpiConfig.SESSION_TOKEN
    private val TOKEN_APP get() = GlpiConfig.APP_TOKEN

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_resolved_tickets)

        val btnVoltar = findViewById<FrameLayout>(R.id.btn_voltar)
        initialTicketId = intent.getStringExtra("TICKET_ID")
        isContextoPessoal = intent.getBooleanExtra("FILTRO_MEUS", false)
        val lottieSeta = findViewById<LottieAnimationView>(R.id.lottie_seta)
        val lottieLupa = findViewById<LottieAnimationView>(R.id.lottie_lupa)

        val corAzulGlpi = ContextCompat.getColor(this, R.color.azul_glpi)
        val corBranca = ContextCompat.getColor(this, android.R.color.white)

        aplicarCorLottie(lottieSeta, corBranca)
        aplicarCorLottie(lottieLupa, corAzulGlpi)

        btnVoltar.setOnClickListener { voltarParaMenu() }

        // Botão de Ordem
        val lottieOrdem = findViewById<LottieAnimationView>(R.id.lottie_ordem)
        aplicarCorLottie(lottieOrdem, corAzulGlpi)
        findViewById<FrameLayout>(R.id.btn_ordem).setOnClickListener {
            currentOrder = if (currentOrder == "DESC") "ASC" else "DESC"
            playOrderAnimation()
            lottieOrdem?.rotation = if (currentOrder == "DESC") 0f else 180f
            carregarTicketsResolvidos()
        }

        // 🔵 Botão de Filtro
        val btnFiltro = findViewById<FrameLayout>(R.id.btn_filtro)
        val lottieFiltro = findViewById<LottieAnimationView>(R.id.lottie_filtro)
        aplicarCorLottie(lottieFiltro, corAzulGlpi)
        playFilterAnimation()

        btnFiltro.setOnClickListener {
            mostrarDialogFiltro()
        }

        rvTickets = findViewById(R.id.rv_tickets_resolvidos)
        rvTickets.layoutManager = LinearLayoutManager(this)

        nestedScroll = findViewById(R.id.nested_scroll_resolved)
        swipeRefresh = findViewById(R.id.swipe_refresh_resolved)
        btnAnterior = findViewById(R.id.btn_pagina_anterior_resolved)
        btnProxima = findViewById(R.id.btn_proxima_pagina_resolved)
        tvListaVazia = findViewById(R.id.tv_lista_vazia)

        swipeRefresh.setColorSchemeColors(corAzulGlpi)
        swipeRefresh.setOnRefreshListener {
            currentPage = 0
            carregarTicketsResolvidos()
        }

        btnAnterior.setOnClickListener {
            if (currentPage > 0) {
                currentPage--
                applyPagination()
            }
        }

        btnProxima.setOnClickListener {
            if ((currentPage + 1) * pageSize < filteredTicketsList.size) {
                currentPage++
                applyPagination()
            }
        }

        val searchBar = findViewById<EditText>(R.id.search_bar_tickets)
        searchBar.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyFilters(s.toString())
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        carregarTicketsResolvidos()
    }

    /** Extrai o ID numérico do campo de estado (campo "12") do ticket */
    private fun extrairStatus(ticket: Map<String, Any>): String {
        val raw = ticket["12"]
        return when {
            raw == null -> ""
            raw is Map<*, *> -> raw["id"]?.toString()?.substringBefore(".") ?: ""
            else -> raw.toString().substringBefore(".")
        }
    }

    /** Aplica filtro por estado e por texto simultaneamente */
    private fun applyFilters(query: String = "") {
        // 1. Filtrar por estado
        val afterStatusFilter = when (statusFiltro) {
            5 -> fullTicketsList.filter { extrairStatus(it) == "5" }
            6 -> fullTicketsList.filter { extrairStatus(it) == "6" }
            else -> fullTicketsList
        }

        // 2. Filtrar por texto
        filteredTicketsList = if (query.isEmpty()) {
            afterStatusFilter
        } else {
            afterStatusFilter.filter { ticket ->
                val subject = ticket["1"]?.toString() ?: ""
                val ticketId = ticket["2"]?.toString() ?: ""
                val content = ticket["21"]?.toString() ?: ""
                val reqId1 = ticket["22"]?.toString() ?: ""
                val reqId2 = ticket["4"]?.toString() ?: ""
                val reqName = TicketAdapter.UNAME_CACHE[reqId1] ?: TicketAdapter.UNAME_CACHE[reqId2] ?: ""

                subject.contains(query, ignoreCase = true) ||
                ticketId.contains(query, ignoreCase = true) ||
                content.contains(query, ignoreCase = true) ||
                reqName.contains(query, ignoreCase = true)
            }
        }
        currentPage = 0

        if (filteredTicketsList.isEmpty()) {
            val msg = when {
                query.isNotEmpty() && statusFiltro != 0 -> "Nenhum ticket encontrado para '$query'."
                query.isNotEmpty() -> "Nenhum ticket encontrado para '$query'."
                statusFiltro != 0 -> "Nenhum ticket para o filtro selecionado."
                else -> "Não existem tickets no histórico."
            }
            tvListaVazia?.text = msg
            tvListaVazia?.visibility = View.VISIBLE
            rvTickets.visibility = View.GONE
            findViewById<View>(R.id.pagination_resolved).visibility = View.GONE
        } else {
            tvListaVazia?.visibility = View.GONE
            rvTickets.visibility = View.VISIBLE
            applyPagination()
        }
    }

    private fun applyPagination() {
        val listToPaginate = filteredTicketsList
        val start = currentPage * pageSize
        val end = minOf(start + pageSize, listToPaginate.size)
        val pageItems = if (listToPaginate.isEmpty()) listOf() else listToPaginate.subList(start, end)

        rvTickets.adapter = TicketAdapter(pageItems, showResolutionDate = true, initialExpandedTicketId = initialTicketId)
        rvTickets.scheduleLayoutAnimation()

        val hasPagination = listToPaginate.size > pageSize
        findViewById<View>(R.id.pagination_resolved).visibility = if (hasPagination) View.VISIBLE else View.GONE
        btnAnterior.visibility = if (currentPage > 0) View.VISIBLE else View.GONE
        btnProxima.visibility = if (end < listToPaginate.size) View.VISIBLE else View.GONE

        nestedScroll.smoothScrollTo(0, 0)
    }

    private fun aplicarCorLottie(lottieView: LottieAnimationView?, cor: Int) {
        lottieView?.addValueCallback(KeyPath("**"), LottieProperty.COLOR_FILTER) {
            android.graphics.PorterDuffColorFilter(cor, android.graphics.PorterDuff.Mode.SRC_ATOP)
        }
    }

    private fun mostrarDialogFiltro() {
        val opcoes = arrayOf("TODOS", "RESOLVIDOS", "ENCERRADOS")
        val statusIds = arrayOf(0, 5, 6)

        val dialogView = layoutInflater.inflate(R.layout.dialog_custom_filter_state, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<TextView>(R.id.tv_filter_title).text = "FILTRAR POR ESTADO"
        val container = dialogView.findViewById<android.widget.LinearLayout>(R.id.container_filter_options)

        dialogView.findViewById<View>(R.id.btn_close_filter).setOnClickListener { dialog.dismiss() }

        val d = resources.displayMetrics.density
        val corAzul = ContextCompat.getColor(this, R.color.azul_glpi)

        opcoes.forEachIndexed { index, title ->
            val isSelected = statusFiltro == statusIds[index]

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
                    statusFiltro = statusIds[index]

                    // Atualizar visual do botão (ativo vs inativo)
                    val btnFiltroMain = this@ResolvedTicketsActivity.findViewById<FrameLayout>(R.id.btn_filtro)
                    if (statusFiltro != 0) {
                        btnFiltroMain?.setBackgroundResource(R.drawable.bg_filtro_ativo)
                    } else {
                        btnFiltroMain?.setBackgroundResource(R.drawable.bg_cartao_brilhante)
                    }

                    playFilterAnimation()

                    val currentQuery = this@ResolvedTicketsActivity
                        .findViewById<EditText>(R.id.search_bar_tickets).text.toString()
                    applyFilters(currentQuery)
                    dialog.dismiss()
                }
            }

            val tv = TextView(this).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                )
                text = title
                setTextColor(ContextCompat.getColor(context, R.color.texto_principal))
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
                buttonTintList = android.content.res.ColorStateList.valueOf(corAzul)
                isChecked = isSelected
                isClickable = false
                isFocusable = false
            }

            row.addView(tv)
            row.addView(rb)
            container.addView(row)
        }

        dialog.show()
        dialog.window?.setLayout(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun carregarTicketsResolvidos() {
        swipeRefresh.isRefreshing = true
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val cal = Calendar.getInstance()
                cal.set(Calendar.DAY_OF_MONTH, 1)
                val dataLimite = SimpleDateFormat("yyyy-MM-dd 00:00:00", Locale.getDefault()).format(cal.time)

                val userId = GlpiConfig.USER_ID

                val response = if (isContextoPessoal) {
                    val loginBusca = GlpiConfig.USER_NAME.split(".", " ").firstOrNull() ?: ""
                    val nomeCompleto = GlpiConfig.USER_FULL_NAME

                    GlpiRetrofit.api.getTicketsMeusFinalizadosHistorico(
                        TOKEN_SESSAO, TOKEN_APP,
                        userId, userId, userId, userId, userId, userId,
                        loginBusca, nomeCompleto.ifEmpty { loginBusca },
                        dataLimite = dataLimite, order = currentOrder, sort = 19, range = "0-299"
                    )
                } else {
                    GlpiRetrofit.api.getListaGeralFinalizadosHistorico(
                        TOKEN_SESSAO, TOKEN_APP,
                        dataLimite = dataLimite, order = currentOrder, sort = 19, range = "0-299"
                    )
                }

                withContext(Dispatchers.Main) {
                    fullTicketsList = if (response.isSuccessful) response.body()?.data ?: emptyList() else emptyList()

                    val currentQuery = findViewById<EditText>(R.id.search_bar_tickets).text.toString()
                    applyFilters(currentQuery)

                    currentPage = 0

                    if (initialTicketId != null) {
                        val index = filteredTicketsList.indexOfFirst {
                            val tid = it["2"]?.toString()?.replace(".0", "") ?: it["id"]?.toString()?.replace(".0", "")
                            tid == initialTicketId
                        }
                        if (index != -1) {
                            currentPage = index / pageSize
                        }
                    }

                    android.util.Log.d("API_RESOLVIDOS", "Tickets carregados: ${fullTicketsList.size}")

                    if (fullTicketsList.isEmpty()) {
                        tvListaVazia.text = "Não existem tickets no histórico."
                        tvListaVazia.visibility = View.VISIBLE
                        rvTickets.visibility = View.GONE
                        findViewById<View>(R.id.pagination_resolved).visibility = View.GONE
                    } else if (filteredTicketsList.isNotEmpty()) {
                        tvListaVazia.visibility = View.GONE
                        rvTickets.visibility = View.VISIBLE
                        applyPagination()
                    }
                    swipeRefresh.isRefreshing = false
                }
            } catch (e: Exception) {
                android.util.Log.e("API_RESOLVIDOS", "Erro ao carregar: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    swipeRefresh.isRefreshing = false
                    Toast.makeText(this@ResolvedTicketsActivity, "Erro: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun voltarParaMenu() {
        val intent = Intent(this, DashboardActivity::class.java)
        intent.putExtra("ABRIR_MENU", true)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        startActivity(intent)
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        finish()
    }

    // 🔥 SOBREPOSIÇÃO DO BOTÃO FÍSICO DO TELEMÓVEL 🔥
    override fun dispatchTouchEvent(ev: android.view.MotionEvent?): Boolean {
        KeyboardHelper.handleTouchOutside(this, ev)
        return super.dispatchTouchEvent(ev)
    }

    override fun onBackPressed() {
        voltarParaMenu()
    }
}