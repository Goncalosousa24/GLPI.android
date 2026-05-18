package com.example.glpimobile

import android.content.Intent
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.view.View
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

class OpenTicketsActivity : AppCompatActivity() {

    private lateinit var rvTickets: RecyclerView
    private lateinit var btnAnterior: View
    private lateinit var btnProxima: View
    private lateinit var nestedScroll: androidx.core.widget.NestedScrollView
    private lateinit var tvListaVazia: TextView

    private var fullTicketsList: List<Map<String, Any>> = listOf()
    private var currentPage = 0
    private var pageSize = 10
    private var initialTicketId: String? = null
    private var currentOrder = "DESC"
    private var statusFiltro = 0 // 0 = Gerais, 1 = Criados por mim

    private val TOKEN_SESSAO get() = GlpiConfig.SESSION_TOKEN
    private val TOKEN_APP get() = GlpiConfig.APP_TOKEN

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_open_tickets)

        initialTicketId = intent.getStringExtra("TICKET_ID")

        val corAzul = ContextCompat.getColor(this, R.color.azul_glpi)
        val corBranca = ContextCompat.getColor(this, android.R.color.white)

        val lottieSeta = findViewById<LottieAnimationView>(R.id.lottie_seta)
        aplicarCorLottie(lottieSeta, corBranca)

        val lottieLupa = findViewById<LottieAnimationView>(R.id.lottie_lupa)
        aplicarCorLottie(lottieLupa, corAzul)

        findViewById<FrameLayout>(R.id.btn_voltar).setOnClickListener { voltarParaMenu() }

        rvTickets = findViewById<RecyclerView>(R.id.rv_tickets_abertos)
        rvTickets.layoutManager = LinearLayoutManager(this)

        btnAnterior = findViewById<View>(R.id.btn_pagina_anterior_open)
        btnProxima = findViewById<View>(R.id.btn_proxima_pagina_open)
        nestedScroll = findViewById<androidx.core.widget.NestedScrollView>(R.id.nested_scroll_open)
        tvListaVazia = findViewById<TextView>(R.id.tv_lista_vazia)

        // Aplicar cor azul às setas de paginação
        aplicarCorLottie(findViewById(R.id.lottie_seta_voltar_open), corAzul)
        aplicarCorLottie(findViewById(R.id.lottie_seta_avancar_open), corAzul)

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

        val lottieOrdemOpen = findViewById<LottieAnimationView>(R.id.lottie_ordem_open)
        aplicarCorLottie(lottieOrdemOpen, corAzul)
        findViewById<FrameLayout>(R.id.btn_ordem_open).setOnClickListener {
            currentOrder = if (currentOrder == "DESC") "ASC" else "DESC"
            playOrderAnimation()
            lottieOrdemOpen.rotation = if (currentOrder == "DESC") 0f else 180f
            carregarTickets()
        }

        val btnFiltroOpen = findViewById<FrameLayout>(R.id.btn_filtro_open)
        val lottieFiltroOpen = findViewById<LottieAnimationView>(R.id.lottie_filtro)
        aplicarCorLottie(lottieFiltroOpen, corAzul)
        btnFiltroOpen.setOnClickListener {
            playFilterAnimation()
            showFilterDialog()
        }

        // Se viermos do Dashboard em "Vista Pessoal"
        if (intent.getBooleanExtra("FILTRO_MEUS", false)) {
            statusFiltro = 1 // Criados por mim
            // USER pediu: Não contar como filtro ativo visualmente ao entrar pela vista pessoal
            btnFiltroOpen.setBackgroundResource(R.drawable.bg_cartao_brilhante)
        }

        playFilterAnimation() // Play on open
        carregarTickets()
    }

    private fun applyPagination() {
        val start = currentPage * pageSize
        val end = minOf(start + pageSize, fullTicketsList.size)
        val pageItems = if (fullTicketsList.isEmpty()) listOf() else fullTicketsList.subList(start, end)

        rvTickets.adapter = TicketAdapter(pageItems, showCreationDate = true, initialExpandedTicketId = initialTicketId)
        rvTickets.scheduleLayoutAnimation()

        btnAnterior.visibility = if (currentPage > 0) View.VISIBLE else View.GONE
        btnProxima.visibility = if (end < fullTicketsList.size) View.VISIBLE else View.GONE

        nestedScroll.smoothScrollTo(0, 0)
    }

    private fun aplicarCorLottie(lottieView: LottieAnimationView?, cor: Int) {
        lottieView?.addValueCallback(KeyPath("**"), LottieProperty.COLOR_FILTER) {
            PorterDuffColorFilter(cor, PorterDuff.Mode.SRC_ATOP)
        }
    }

    private fun carregarTickets() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userId = GlpiConfig.USER_ID
                val response = if (statusFiltro == 1) {
                        val loginBusca = GlpiConfig.USER_NAME.split(".", " ").firstOrNull() ?: ""
                        val nomeCompleto = GlpiConfig.USER_FULL_NAME
                        GlpiRetrofit.api.getTicketsCriadosPorMim(
                            sessionToken = TOKEN_SESSAO,
                            appToken = TOKEN_APP,
                            userId1 = userId,
                            userId2 = userId,
                            userId3 = userId,
                            userId4 = userId,
                            userId5 = userId,
                            userId6 = userId,
                            login1 = loginBusca,
                            login2 = nomeCompleto.ifEmpty { loginBusca },
                            order = currentOrder,
                            range = "0-149"
                        )
                } else {
                    GlpiRetrofit.api.getTicketsCriadosGerais(
                        sessionToken = TOKEN_SESSAO,
                        appToken = TOKEN_APP,
                        order = currentOrder,
                        range = "0-149"
                    )
                }

                withContext(Dispatchers.Main) {
                    fullTicketsList = response.data ?: emptyList()
                    currentPage = 0

                    if (initialTicketId != null) {
                        val index = fullTicketsList.indexOfFirst {
                            val tid = it["2"]?.toString()?.replace(".0", "") ?: it["id"]?.toString()?.replace(".0", "")
                            tid == initialTicketId
                        }
                        if (index != -1) {
                            currentPage = index / pageSize
                        }
                    }

                    if (fullTicketsList.isEmpty()) {
                        tvListaVazia.visibility = View.VISIBLE
                        rvTickets.visibility = View.GONE
                        findViewById<View>(R.id.pagination_open).visibility = View.GONE
                    } else {
                        tvListaVazia.visibility = View.GONE
                        rvTickets.visibility = View.VISIBLE
                        findViewById<View>(R.id.pagination_open).visibility = View.VISIBLE
                        applyPagination()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@OpenTicketsActivity, "Erro: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showFilterDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_custom_filter_state, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<TextView>(R.id.tv_filter_title).text = "FILTRAR TICKETS"
        val container = dialogView.findViewById<android.widget.LinearLayout>(R.id.container_filter_options)
        
        val veioDaPessoal = intent.getBooleanExtra("FILTRO_MEUS", false)
        
        // USER pediu: Ordem dinâmica. Na pessoal "Criados" em cima, na Geral "Gerais" em cima.
        val options = if (veioDaPessoal) {
            listOf("CRIADOS POR MIM" to 1, "GERAIS (TODOS)" to 0)
        } else {
            listOf("GERAIS (TODOS)" to 0, "CRIADOS POR MIM" to 1)
        }
        
        val d = resources.displayMetrics.density

        options.forEach { (title, itemValue) ->
            val isSelected = statusFiltro == itemValue
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
                    statusFiltro = itemValue
                    
                    val btnFiltroMain = this@OpenTicketsActivity.findViewById<FrameLayout>(R.id.btn_filtro_open)
                    
                    // Lógica para o destaque do botão:
                    // Se viermos da vista pessoal (intent extra true), o destaque só aparece se mudarmos para "Gerais"
                    // Se viermos da vista geral, o destaque só aparece se mudarmos para "Criados por mim"
                    val modoPadrao = if (veioDaPessoal) 1 else 0
                    
                    if (statusFiltro != modoPadrao) {
                        btnFiltroMain?.setBackgroundResource(R.drawable.bg_filtro_ativo)
                    } else {
                        btnFiltroMain?.setBackgroundResource(R.drawable.bg_cartao_brilhante)
                    }
                    
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

        dialogView.findViewById<View>(R.id.btn_close_filter).setOnClickListener { dialog.dismiss() }
        
        dialog.show()
        dialog.window?.setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        
        val btnFiltro = findViewById<FrameLayout>(R.id.btn_filtro_open)
        val modoPadrao = if (veioDaPessoal) 1 else 0
        
        if (statusFiltro != modoPadrao) {
            btnFiltro.setBackgroundResource(R.drawable.bg_filtro_ativo)
        } else {
            btnFiltro.setBackgroundResource(R.drawable.bg_cartao_brilhante)
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

    override fun dispatchTouchEvent(ev: android.view.MotionEvent?): Boolean {
        KeyboardHelper.handleTouchOutside(this, ev)
        return super.dispatchTouchEvent(ev)
    }

    override fun onBackPressed() {
        voltarParaMenu()
    }
}