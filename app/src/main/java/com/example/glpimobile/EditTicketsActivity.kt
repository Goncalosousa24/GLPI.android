package com.example.glpimobile

import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.constraintlayout.widget.ConstraintLayout
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath
import kotlinx.coroutines.*
import java.util.Calendar
import java.util.Locale

class EditTicketsActivity : AppCompatActivity() {

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var rvEditTickets: RecyclerView
    private lateinit var searchBar: EditText
    private var searchJob: Job? = null

    private var currentFilter: Int = 0
    private var fullTicketsList: List<Map<String, Any>> = listOf()
    private var currentPage: Int = 0
    private val pageSize: Int = 10

    private lateinit var btnAnterior: View
    private lateinit var btnProxima: View
    private lateinit var nestedScroll: androidx.core.widget.NestedScrollView
    private lateinit var paginationLayout: View

    private val TOKEN_SESSAO = GlpiConfig.SESSION_TOKEN
    private val TOKEN_APP = GlpiConfig.APP_TOKEN

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_tickets)

        swipeRefresh = findViewById(R.id.swipeRefresh)
        rvEditTickets = findViewById(R.id.rv_edit_tickets)
        searchBar = findViewById(R.id.search_bar_edit)
        btnAnterior = findViewById(R.id.btn_pagina_anterior_edit)
        btnProxima = findViewById(R.id.btn_proxima_pagina_edit)
        nestedScroll = findViewById(R.id.nested_scroll_edit)
        paginationLayout = findViewById(R.id.pagination_edit)

        rvEditTickets.layoutManager = LinearLayoutManager(this)

        configurarVisual()
        configurarCliques()
        configurarPesquisa()
        configurarFiltros()

        carregarTickets()
        playFilterAnimation() // Play on open
        swipeRefresh.setOnRefreshListener { carregarTickets() }
    }

    private fun configurarVisual() {
        val corBranca = ContextCompat.getColor(this, android.R.color.white)
        val corAzulGlpi = ContextCompat.getColor(this, R.color.azul_glpi)
        
        aplicarCorLottie(findViewById(R.id.lottie_seta), corBranca)
        aplicarCorLottie(findViewById(R.id.lottie_lupa), corAzulGlpi)
        aplicarCorLottie(findViewById(R.id.lottie_seta_voltar_edit), corAzulGlpi)
        aplicarCorLottie(findViewById(R.id.lottie_seta_avancar_edit), corAzulGlpi)
    }

    private fun configurarFiltros() {
        val btnFiltro = findViewById<View>(R.id.btn_filtro_opcoes)
        val lottieFiltro = findViewById<LottieAnimationView>(R.id.lottie_filtro)
        val azulGlpi = ContextCompat.getColor(this, R.color.azul_glpi)
        
        aplicarCorLottie(lottieFiltro, azulGlpi)

        btnFiltro.setOnClickListener {
            showFilterDialog()
        }
    }

    private fun showFilterDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_custom_filter_state, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<TextView>(R.id.tv_filter_title).text = "FILTRAR ESTADO"
        val container = dialogView.findViewById<android.widget.LinearLayout>(R.id.container_filter_options)
        val btnClose = dialogView.findViewById<android.view.View>(R.id.btn_close_filter)
        btnClose.setOnClickListener { dialog.dismiss() }

        val labels = arrayOf("Todos", "Novos / Abertos", "Em Progresso")
        
        val d = resources.displayMetrics.density

        labels.forEachIndexed { index, title ->
            val isSelected = currentFilter == index
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
                    currentFilter = index
                    searchBar.text.clear()
                    atualizarVisualFiltro()
                    carregarTickets()
                    playFilterAnimation()
                    dialog.dismiss()
                }
            }

            val tv = TextView(this).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
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

    private fun atualizarVisualFiltro() {
        val btnFiltro = findViewById<View>(R.id.btn_filtro_opcoes)
        if (currentFilter == 0) {
            btnFiltro.setBackgroundResource(R.drawable.bg_cartao_brilhante)
        } else {
            btnFiltro.setBackgroundResource(R.drawable.bg_filtro_ativo)
        }
    }

    private fun aplicarCorLottie(view: LottieAnimationView?, cor: Int) {
        view?.addValueCallback(KeyPath("**"), LottieProperty.COLOR_FILTER) {
            PorterDuffColorFilter(cor, PorterDuff.Mode.SRC_ATOP)
        }
    }

    private fun configurarCliques() {
        findViewById<View>(R.id.btn_voltar).setOnClickListener {
            finish()
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }

        btnAnterior.setOnClickListener {
            if (currentPage > 0) {
                currentPage--
                applyPagination()
            }
        }

        btnProxima.setOnClickListener {
            if ((currentPage + 1) * pageSize < fullTicketsList.size) {
                currentPage++
                applyPagination()
            }
        }
    }

    private fun applyPagination() {
        val start = currentPage * pageSize
        val end = minOf(start + pageSize, fullTicketsList.size)
        val paginatedList = if (fullTicketsList.isEmpty()) listOf() else fullTicketsList.subList(start, end)

        rvEditTickets.adapter = TicketAdapter(paginatedList, showEditButton = true)
        rvEditTickets.scheduleLayoutAnimation()

        val showAnterior = currentPage > 0
        val showProxima = end < fullTicketsList.size

        btnAnterior.visibility = if (showAnterior) View.VISIBLE else View.GONE
        btnProxima.visibility = if (showProxima) View.VISIBLE else View.GONE
        paginationLayout.visibility = if (showAnterior || showProxima) View.VISIBLE else View.GONE

        nestedScroll.smoothScrollTo(0, 0)
    }

    private fun carregarTickets() {
        swipeRefresh.isRefreshing = true
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val resposta = when (currentFilter) {
                    1 -> GlpiRetrofit.api.getTicketsNovosEdit(TOKEN_SESSAO, TOKEN_APP, range = "0-300", order = "DESC")
                    2 -> GlpiRetrofit.api.getTicketsEmProgressoEdit(TOKEN_SESSAO, TOKEN_APP, range = "0-300", order = "DESC")
                    else -> GlpiRetrofit.api.getUltimasAtividadesGeral(TOKEN_SESSAO, TOKEN_APP, "0-100")
                }

                withContext(Dispatchers.Main) {
                    fullTicketsList = if (resposta.isSuccessful) resposta.body()?.data ?: emptyList() else emptyList()
                    currentPage = 0
                    applyPagination()
                    swipeRefresh.isRefreshing = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    swipeRefresh.isRefreshing = false
                    Toast.makeText(this@EditTicketsActivity, "Erro ao carregar tickets", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun configurarPesquisa() {
        searchBar.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                if (query.isEmpty()) {
                    carregarTickets()
                } else {
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
                    val resposta = GlpiRetrofit.api.pesquisarTickets(TOKEN_SESSAO, TOKEN_APP, campoPesquisa, "contains", textoLimpo)
                    withContext(Dispatchers.Main) {
                        fullTicketsList = if (resposta.isSuccessful) resposta.body()?.data ?: emptyList() else emptyList()
                        currentPage = 0
                        applyPagination()
                    }
                } catch (e: Exception) {}
            }
        }
    }

    override fun dispatchTouchEvent(ev: android.view.MotionEvent?): Boolean {
        KeyboardHelper.handleTouchOutside(this, ev)
        return super.dispatchTouchEvent(ev)
    }
}
