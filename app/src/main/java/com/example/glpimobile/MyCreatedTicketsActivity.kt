package com.example.glpimobile

import android.content.Intent
import android.util.Log
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
import android.widget.Toast
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

class MyCreatedTicketsActivity : AppCompatActivity() {

    private var rvTickets: RecyclerView? = null
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var nestedScroll: androidx.core.widget.NestedScrollView
    private lateinit var btnAnterior: View
    private lateinit var btnProximo: View
    
    private var fullTicketsList: List<Map<String, Any>> = listOf()
    private var filteredTicketsList: List<Map<String, Any>> = listOf()
    private var currentPage: Int = 0
    private val pageSize: Int = 10
    
    private var currentOrder = "DESC"
    private var meuUserId = 0

    private val TOKEN_SESSAO = GlpiConfig.SESSION_TOKEN
    private val TOKEN_APP = GlpiConfig.APP_TOKEN

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_created_tickets)

        meuUserId = PreferenceManager.getUserId(this)
        Log.d("MY_TICKETS", "Utilizador ID: $meuUserId")

        val corAzulGlpi = ContextCompat.getColor(this, R.color.azul_glpi)
        val corBranca = ContextCompat.getColor(this, android.R.color.white)

        val btnVoltar = findViewById<FrameLayout>(R.id.btn_voltar)
        val lottieSeta = findViewById<LottieAnimationView>(R.id.lottie_seta)

        // Seta pintada a branco
        lottieSeta.repeatCount = 0
        lottieSeta.playAnimation()
        aplicarCorLottie(lottieSeta, corBranca)

        btnVoltar.setOnClickListener {
            voltarParaMenu()
        }

        val lottieLupa = findViewById<LottieAnimationView>(R.id.lottie_lupa)
        lottieLupa.repeatCount = 0
        lottieLupa.playAnimation()
        aplicarCorLottie(lottieLupa, corAzulGlpi)

        val lottieOrdem = findViewById<LottieAnimationView>(R.id.lottie_ordem)
        lottieOrdem.repeatCount = 0
        aplicarCorLottie(lottieOrdem, corAzulGlpi)

        findViewById<FrameLayout>(R.id.btn_ordem).setOnClickListener {
            currentOrder = if (currentOrder == "DESC") "ASC" else "DESC"
            playOrderAnimation()
            lottieOrdem?.rotation = if (currentOrder == "DESC") 0f else 180f
            carregarMeusTickets()
        }

        rvTickets = findViewById(R.id.rv_my_created_tickets)
        rvTickets?.layoutManager = LinearLayoutManager(this)

        swipeRefresh = findViewById(R.id.swipeRefresh)
        swipeRefresh.setColorSchemeColors(corAzulGlpi)
        swipeRefresh.setOnRefreshListener { carregarMeusTickets() }

        nestedScroll = findViewById(R.id.nested_scroll_my_created_tickets)
        btnAnterior = findViewById(R.id.btn_pagina_anterior)
        btnProximo = findViewById(R.id.btn_proxima_pagina)

        val lottieVoltar = findViewById<LottieAnimationView>(R.id.lottie_seta_voltar)
        val lottieAvancar = findViewById<LottieAnimationView>(R.id.lottie_seta_avancar)
        aplicarCorLottie(lottieVoltar, corAzulGlpi)
        aplicarCorLottie(lottieAvancar, corAzulGlpi)

        btnAnterior = findViewById(R.id.btn_pagina_anterior)

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

        val searchBar = findViewById<android.widget.EditText>(R.id.search_bar_tickets)
        searchBar.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterSearch(s.toString())
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        carregarMeusTickets()
    }

    private fun aplicarCorLottie(lottieView: LottieAnimationView?, cor: Int) {
        lottieView?.addValueCallback(KeyPath("**"), LottieProperty.COLOR_FILTER) {
            PorterDuffColorFilter(cor, PorterDuff.Mode.SRC_ATOP)
        }
    }

    private fun carregarMeusTickets() {
        swipeRefresh.isRefreshing = true

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = GlpiRetrofit.api.getTicketsCriadosPorMimTudo(
                        sessionToken = TOKEN_SESSAO,
                        appToken = TOKEN_APP,
                        userId1 = meuUserId,
                        userId2 = meuUserId,
                        userId3 = meuUserId,
                        userId4 = meuUserId,
                        userId5 = meuUserId,
                        userId6 = meuUserId,
                        login1 = "",
                        login2 = "",
                        order = currentOrder,
                        range = "0-149"
                    )

                withContext(Dispatchers.Main) {
                    fullTicketsList = response.data ?: emptyList()
                    filteredTicketsList = fullTicketsList
                    currentPage = 0
                    applyPagination()
                    swipeRefresh.isRefreshing = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    swipeRefresh.isRefreshing = false
                    Toast.makeText(this@MyCreatedTicketsActivity, "Erro ao carregar", Toast.LENGTH_SHORT).show()
                }
            }
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
        
        if (pageItems.isEmpty()) {
            val mensagem = "Não existem tickets no sistema para exibição."
            mostrarAlertaInfo("Meus Pedidos", mensagem)
            rvTickets?.visibility = View.GONE
        } else {
            rvTickets?.visibility = View.VISIBLE
            rvTickets?.adapter = TicketAdapter(pageItems, showCreationDate = true)
            rvTickets?.scheduleLayoutAnimation()
        }

        btnAnterior.visibility = if (currentPage > 0) View.VISIBLE else View.GONE
        btnProximo.visibility = if (end < filteredTicketsList.size) View.VISIBLE else View.GONE
        
        // Efeito suave: Scroll para o topo ao mudar de página
        nestedScroll.smoothScrollTo(0, 0)
    }

    private fun filterSearch(query: String) {
        filteredTicketsList = if (query.isEmpty()) {
            fullTicketsList
        } else {
            fullTicketsList.filter { ticket ->
                val subject = ticket["2"]?.toString() ?: ""
                val content = ticket["21"]?.toString() ?: ""
                val id = ticket["1"]?.toString() ?: ""
                
                subject.contains(query, ignoreCase = true) || 
                content.contains(query, ignoreCase = true) ||
                id.contains(query, ignoreCase = true)
            }
        }
        currentPage = 0
        applyPagination()
    }

    private fun mostrarAlertaInfo(titulo: String, mensagem: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_custom_info, null)
        val builder = AlertDialog.Builder(this, R.style.CustomAlertDialog)
        builder.setView(dialogView)
        builder.setCancelable(false)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val lottieNotif = dialogView.findViewById<LottieAnimationView>(R.id.lottie_notificacao)
        aplicarCorLottie(lottieNotif, ContextCompat.getColor(this, R.color.azul_glpi))
        lottieNotif.repeatCount = 0
        lottieNotif.playAnimation()

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
