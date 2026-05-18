package com.example.glpimobile

import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath
import kotlinx.coroutines.*
import androidx.core.widget.NestedScrollView

class MyResolvedTicketsActivity : AppCompatActivity() {

    private var rvTickets: RecyclerView? = null
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var btnAnterior: View
    private lateinit var btnProxima: View
    private lateinit var nestedScroll: androidx.core.widget.NestedScrollView
    private lateinit var tvListaVazia: TextView

    private var fullTicketsList: List<Map<String, Any>> = listOf()
    private var currentPage = 0
    private val pageSize = 10
    private var currentOrder = "DESC"
    private val meuUserId = 7

    // 🔑 TOKENS SINCRONIZADOS COM A APP INTEIRA
    private val TOKEN_SESSAO = GlpiConfig.SESSION_TOKEN
    private val TOKEN_APP = GlpiConfig.APP_TOKEN

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_resolved_tickets)

        val corAzulGlpi = ContextCompat.getColor(this, R.color.azul_glpi)
        // 🔥 ADICIONEI A COR BRANCA AQUI 🔥
        val corBranca = ContextCompat.getColor(this, android.R.color.white)

        // 1. Botão Voltar (Atualizado para o novo fluxo)
        val btnVoltar = findViewById<FrameLayout>(R.id.btn_voltar)
        val lottieSeta = findViewById<LottieAnimationView>(R.id.lottie_seta)

        // 🔥 APLICAÇÃO DA COR BRANCA NA SETA 🔥
        aplicarCorLottie(lottieSeta, corBranca)

        btnVoltar.setOnClickListener { finish(); overridePendingTransition(R.anim.fade_in, R.anim.fade_out) }

        // 2. Lupa a Azul
        val lottieLupa = findViewById<LottieAnimationView>(R.id.lottie_lupa)
        aplicarCorLottie(lottieLupa, corAzulGlpi)

        // 3. Ordem a Azul
        val lottieOrdem = findViewById<LottieAnimationView>(R.id.lottie_ordem)
        aplicarCorLottie(lottieOrdem, corAzulGlpi)

        findViewById<FrameLayout>(R.id.btn_ordem).setOnClickListener {
            currentOrder = if (currentOrder == "DESC") "ASC" else "DESC"
            playOrderAnimation()
            lottieOrdem?.rotation = if (currentOrder == "DESC") 0f else 180f
            carregarMeusTicketsResolvidos()
        }

        // 4. RecyclerView e Swipe
        rvTickets = findViewById(R.id.rv_my_resolved_tickets)
        rvTickets?.layoutManager = LinearLayoutManager(this)

        btnAnterior = findViewById(R.id.btn_pagina_anterior_resolved)
        btnProxima = findViewById(R.id.btn_proxima_pagina_resolved)
        nestedScroll = findViewById(R.id.nested_scroll_resolved)
        tvListaVazia = findViewById(R.id.tv_lista_vazia)

        // Aplicar cor azul às setas de paginação
        aplicarCorLottie(findViewById(R.id.lottie_seta_voltar_resolved), corAzulGlpi)
        aplicarCorLottie(findViewById(R.id.lottie_seta_avancar_resolved), corAzulGlpi)

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

        swipeRefresh = findViewById(R.id.swipeRefresh)
        swipeRefresh.setColorSchemeColors(corAzulGlpi)
        swipeRefresh.setOnRefreshListener { carregarMeusTicketsResolvidos() }

        carregarMeusTicketsResolvidos()
        playFilterAnimation() // Play on open
    }

    private fun applyPagination() {
        val start = currentPage * pageSize
        val end = minOf(start + pageSize, fullTicketsList.size)
        val pageItems = if (fullTicketsList.isEmpty()) listOf() else fullTicketsList.subList(start, end)

        rvTickets?.adapter = TicketAdapter(pageItems)
        rvTickets?.scheduleLayoutAnimation()

        btnAnterior.visibility = if (currentPage > 0) View.VISIBLE else View.GONE
        btnProxima.visibility = if (end < fullTicketsList.size) View.VISIBLE else View.GONE

        nestedScroll.smoothScrollTo(0, 0)
    }

    private fun aplicarCorLottie(lottieView: LottieAnimationView?, cor: Int) {
        lottieView?.addValueCallback(KeyPath("**"), LottieProperty.COLOR_FILTER) {
            PorterDuffColorFilter(cor, PorterDuff.Mode.SRC_ATOP)
        }
    }

    private fun carregarMeusTicketsResolvidos() {
        swipeRefresh.isRefreshing = true

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = GlpiRetrofit.api.getTicketsResolvidosPorMim(
                    sessionToken = TOKEN_SESSAO,
                    appToken = TOKEN_APP,
                    order = currentOrder,
                    userId = meuUserId
                )

                withContext(Dispatchers.Main) {
                    fullTicketsList = response.data ?: emptyList()
                    currentPage = 0

                    if (fullTicketsList.isEmpty()) {
                        tvListaVazia.visibility = View.VISIBLE
                        rvTickets?.visibility = View.GONE
                        findViewById<View>(R.id.pagination_resolved).visibility = View.GONE
                    } else {
                        tvListaVazia.visibility = View.GONE
                        rvTickets?.visibility = View.VISIBLE
                        findViewById<View>(R.id.pagination_resolved).visibility = View.VISIBLE
                        applyPagination()
                    }
                    swipeRefresh.isRefreshing = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    swipeRefresh.isRefreshing = false
                    mostrarAlertaInfo("Erro", "Erro ao carregar os seus tickets resolvidos.")
                }
            }
        }
    }

    private fun mostrarAlertaInfo(titulo: String, mensagem: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_custom_info, null)
        val builder = AlertDialog.Builder(this, R.style.CustomAlertDialog)
        builder.setView(dialogView)
        builder.setCancelable(false)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialogView.findViewById<TextView>(R.id.tv_titulo_dialog).text = titulo
        dialogView.findViewById<TextView>(R.id.tv_mensagem_dialog).text = mensagem
        dialogView.findViewById<Button>(R.id.btn_dialog_ok).setOnClickListener { dialog.dismiss() }
        dialog.show()
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