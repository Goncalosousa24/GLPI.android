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
import java.text.SimpleDateFormat
import java.util.*

class ExpiredTicketsActivity : AppCompatActivity() {

    private var rvTickets: RecyclerView? = null
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var btnAnterior: View
    private lateinit var btnProxima: View
    private lateinit var nestedScroll: androidx.core.widget.NestedScrollView
    private lateinit var tvListaVazia: TextView

    private var fullTicketsList: List<Map<String, Any>> = listOf()
    private var currentPage = 0
    private val pageSize = 10
    private var currentOrder = "ASC"
    private val meuUserId = GlpiConfig.USER_ID
    private var filterType = "INDIVIDUAL" // "INDIVIDUAL" or "EQUIPA"
    private var selectedUserId: Int? = GlpiConfig.USER_ID

    private val TOKEN_SESSAO = GlpiConfig.SESSION_TOKEN
    private val TOKEN_APP = GlpiConfig.APP_TOKEN

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expired_tickets)

        val corAzulGlpi = ContextCompat.getColor(this, R.color.azul_glpi)
        val corBranca = ContextCompat.getColor(this, android.R.color.white)

        val btnVoltar = findViewById<FrameLayout>(R.id.btn_voltar)
        val lottieSeta = findViewById<LottieAnimationView>(R.id.lottie_seta)
        aplicarCorLottie(lottieSeta, corBranca)

        btnVoltar.setOnClickListener { voltarParaMenu() }

        val lottieLupa = findViewById<LottieAnimationView>(R.id.lottie_lupa)
        aplicarCorLottie(lottieLupa, corAzulGlpi)

        val lottieOrdem = findViewById<LottieAnimationView>(R.id.lottie_ordem)
        aplicarCorLottie(lottieOrdem, corAzulGlpi)

        findViewById<FrameLayout>(R.id.btn_ordem).setOnClickListener {
            currentOrder = if (currentOrder == "DESC") "ASC" else "DESC"
            playOrderAnimation()
            lottieOrdem?.rotation = if (currentOrder == "DESC") 0f else 180f
            carregarTicketsExpirados()
        }

        rvTickets = findViewById(R.id.rv_tickets_expirados)
        rvTickets?.layoutManager = LinearLayoutManager(this)

        btnAnterior = findViewById(R.id.btn_pagina_anterior_expired)
        btnProxima = findViewById(R.id.btn_proxima_pagina_expired)
        nestedScroll = findViewById(R.id.nested_scroll_expired)
        tvListaVazia = findViewById(R.id.tv_lista_vazia)

        // Aplicar cor azul às setas de paginação
        aplicarCorLottie(findViewById(R.id.lottie_seta_voltar_expired), corAzulGlpi)
        aplicarCorLottie(findViewById(R.id.lottie_seta_avancar_expired), corAzulGlpi)

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
        swipeRefresh.setOnRefreshListener { carregarTicketsExpirados() }

        configurarFiltros()
        playFilterAnimation() // Play on open
        carregarTicketsExpirados()
    }

    private fun configurarFiltros() {
        val chipEquipa = findViewById<TextView>(R.id.chip_equipa)
        val chipIndividual = findViewById<TextView>(R.id.chip_individual)

        chipEquipa.setOnClickListener {
            chipEquipa.isSelected = true
            chipIndividual.isSelected = false
            filterType = "EQUIPA"
            selectedUserId = null
            playFilterAnimation()
            carregarTicketsExpirados()
        }

        chipIndividual.setOnClickListener {
            chipIndividual.isSelected = true
            chipEquipa.isSelected = false
            filterType = "INDIVIDUAL"
            selectedUserId = GlpiConfig.USER_ID
            playFilterAnimation()
            carregarTicketsExpirados()
        }

        // Estado inicial
        chipIndividual.isSelected = true
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

    private fun carregarTicketsExpirados() {
        swipeRefresh.isRefreshing = true

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = if (selectedUserId == null) {
                    GlpiRetrofit.api.getTodosTicketsExpirados(
                        sessionToken = TOKEN_SESSAO,
                        appToken = TOKEN_APP
                    )
                } else {
                    GlpiRetrofit.api.getTicketsExpirados(
                        sessionToken = TOKEN_SESSAO,
                        appToken = TOKEN_APP,
                        userId = selectedUserId!!
                    )
                }

                withContext(Dispatchers.Main) {
                    val allFetched = response
                    
                    // Filtrar em Kotlin para garantir precisão na data e status
                    fullTicketsList = allFetched.data?.filter { ticket ->
                        val status = extractId(ticket["12"])
                        val dueDateStr = ticket["18"]?.toString()
                        
                        // Um ticket é considerado "fora de prazo" se tiver uma data limite
                        // e essa data for anterior à data/hora atual.
                        isExpired(dueDateStr, status)
                    } ?: emptyList()
                    fullTicketsList = fullTicketsList.take(50)
                    
                    currentPage = 0
                    
                    if (fullTicketsList.isEmpty()) {
                        tvListaVazia.visibility = View.VISIBLE
                        rvTickets?.visibility = View.GONE
                        findViewById<View>(R.id.pagination_expired).visibility = View.GONE
                    } else {
                        tvListaVazia.visibility = View.GONE
                        rvTickets?.visibility = View.VISIBLE
                        findViewById<View>(R.id.pagination_expired).visibility = View.VISIBLE
                        applyPagination()
                    }
                    swipeRefresh.isRefreshing = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    swipeRefresh.isRefreshing = false
                    mostrarAlertaInfo("Aviso", "Não foram encontrados tickets expirados.")
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

    private fun isExpired(dueDateStr: String?, status: String?): Boolean {
        if (dueDateStr.isNullOrEmpty() || status == "5" || status == "6") return false
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val dueDate = sdf.parse(dueDateStr)
            dueDate?.before(Date()) ?: false
        } catch (e: Exception) {
            false
        }
    }

    private fun extractId(v: Any?): String {
        if (v == null) return ""
        if (v is Map<*, *>) return v["id"]?.toString()?.substringBefore(".") ?: ""
        if (v is List<*>) return (v.firstOrNull() as? Map<*, *>)?.get("id")?.toString()?.substringBefore(".") ?: ""
        val s = v.toString()
        return if (s.contains(".")) s.substringBefore(".") else s
    }

    private fun voltarParaMenu() {
        val intent = Intent(this, AgendaActivity::class.java)
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
