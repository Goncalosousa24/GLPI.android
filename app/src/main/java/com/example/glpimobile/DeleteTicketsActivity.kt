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

class DeleteTicketsActivity : AppCompatActivity() {

    private var rvTickets: RecyclerView? = null
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var btnAnterior: View
    private lateinit var btnProxima: View
    private lateinit var nestedScroll: androidx.core.widget.NestedScrollView
    private lateinit var tvListaVazia: TextView

    private var fullTicketsList: List<Map<String, Any>> = listOf()
    private var filteredTicketsList: List<Map<String, Any>> = listOf()
    private var expandedPosition: Int = -1
    private val pageSize = 10
    private var currentPage = 0
    private var currentOrder = "DESC"
    private var showDeletedOnly = false
    private var isRestrictedProfile = false

    private fun getSessionToken() = PreferenceManager.getSessionToken(this)
    private fun getAppToken() = PreferenceManager.getAppToken(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_delete_tickets)

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
        findViewById<View>(R.id.btn_ordem).setOnClickListener {
            currentOrder = if (currentOrder == "DESC") "ASC" else "DESC"
            playOrderAnimation()
            val rotation = if (currentOrder == "DESC") 0f else 180f
            lottieOrdem.rotation = rotation
            carregarTickets()
        }

        // Botão de Lixo Toggle
        val btnLixo = findViewById<View>(R.id.btn_lixo_toggle)
        val ivLixoIcon = findViewById<android.widget.ImageView>(R.id.iv_lixo_icon)
        val tvHeaderTitle = findViewById<android.widget.TextView>(R.id.tv_header_title)

        btnLixo.setOnClickListener {
            showDeletedOnly = !showDeletedOnly
            
            // Atualizar UI do botão
            if (showDeletedOnly) {
                btnLixo.setBackgroundResource(R.drawable.bg_trash_active)
                ivLixoIcon.setImageResource(R.drawable.trash)
            } else {
                btnLixo.setBackgroundResource(R.drawable.bg_cartao_brilhante)
                ivLixoIcon.setImageResource(R.drawable.lixo)
            }
            
            // Garantir que o ícone permanece azul_glpi
            ivLixoIcon.setColorFilter(corAzulGlpi, android.graphics.PorterDuff.Mode.SRC_IN)
            
            tvHeaderTitle.text = if (showDeletedOnly) "TICKETS ELIMINADOS" else "ELIMINAR TICKETS"
            
            // Recarregar tickets com o novo filtro
            playFilterAnimation()
            carregarTickets()
        }

        rvTickets = findViewById(R.id.rv_tickets_delete)
        rvTickets?.layoutManager = LinearLayoutManager(this)

        btnAnterior = findViewById(R.id.btn_pagina_anterior)
        btnProxima = findViewById(R.id.btn_proxima_pagina)
        nestedScroll = findViewById(R.id.nested_scroll_delete)
        tvListaVazia = findViewById(R.id.tv_lista_vazia)

        aplicarCorLottie(findViewById(R.id.lottie_seta_voltar), corAzulGlpi)
        aplicarCorLottie(findViewById(R.id.lottie_seta_avancar), corAzulGlpi)

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

        swipeRefresh = findViewById(R.id.swipeRefresh)
        swipeRefresh.setColorSchemeColors(corAzulGlpi)
        swipeRefresh.setOnRefreshListener { carregarTickets() }

        val searchBar = findViewById<android.widget.EditText>(R.id.search_bar_tickets)
        searchBar.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterSearch(s.toString())
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        // 🔥 Verificar se o perfil é apenas leitura ou observador
        val profileName = PreferenceManager.getUserProfile(this) ?: ""
        isRestrictedProfile = profileName.contains("leitura", ignoreCase = true) || 
                             profileName.contains("read-only", ignoreCase = true) ||
                             profileName.contains("observador", ignoreCase = true) ||
                             profileName.contains("observer", ignoreCase = true)
        
        carregarTickets()
        playFilterAnimation() // Play on open
    }

    private fun applyPagination() {
        val listToPaginate = filteredTicketsList
        val start = currentPage * pageSize
        val end = minOf(start + pageSize, listToPaginate.size)
        val pageItems = if (listToPaginate.isEmpty()) listOf() else listToPaginate.subList(start, end)

        val adapter = TicketAdapter(
            tickets = pageItems,
            showDeleteButton = !showDeletedOnly,
            showTrashActions = showDeletedOnly,
            showResponderButton = false,
            onDeleteClick = { ticketId, ticket ->
                confirmarEliminacao(ticketId, ticket)
            },
            onRestoreClick = { ticketId, _ ->
                confirmarRestauro(ticketId)
            },
            onPurgeClick = { ticketId, _ ->
                confirmarPurga(ticketId)
            }
        )
        rvTickets?.adapter = adapter
        rvTickets?.scheduleLayoutAnimation()

        val hasPagination = listToPaginate.size > pageSize
        findViewById<View>(R.id.pagination_delete).visibility = if (hasPagination) View.VISIBLE else View.GONE
        btnAnterior.visibility = if (currentPage > 0) View.VISIBLE else View.GONE
        btnProxima.visibility = if (end < listToPaginate.size) View.VISIBLE else View.GONE

        nestedScroll.smoothScrollTo(0, 0)
    }

    private var searchJob: Job? = null

    private fun filterSearch(query: String) {
        searchJob?.cancel()
        if (query.isEmpty()) {
            filteredTicketsList = fullTicketsList
            currentPage = 0
            updateEmptyState()
            return
        }

        // Pesquisa no Servidor após 500ms de paragem na escrita
        searchJob = CoroutineScope(Dispatchers.Main).launch {
            delay(500)
            pesquisarTicketsNoServidor(query)
        }
    }

    private fun pesquisarTicketsNoServidor(query: String) {
        swipeRefresh.isRefreshing = true
        findViewById<View>(R.id.pagination_delete).visibility = View.GONE
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Determinar se pesquisa por ID (#123) ou Texto
                val textoLimpo = query.replace("#", "").trim()
                val campoPesquisa = if (textoLimpo.all { it.isDigit() }) 2 else 1
                
                // NOTA: pesquisarTickets já usa forcedisplay para trazer os dados formatados
                val response = GlpiRetrofit.api.pesquisarTickets(
                    getSessionToken(), 
                    getAppToken(), 
                    field = campoPesquisa, 
                    searchType = "contains",
                    value = textoLimpo,
                    range = "0-299",
                    isDeleted = if (showDeletedOnly) 1 else 0
                )
                
                withContext(Dispatchers.Main) {
                    filteredTicketsList = if (response.isSuccessful) response.body()?.data ?: emptyList() else emptyList()
                    currentPage = 0
                    updateEmptyState(query)
                    swipeRefresh.isRefreshing = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    swipeRefresh.isRefreshing = false
                }
            }
        }
    }

    private fun updateEmptyState(query: String = "") {
        if (filteredTicketsList.isEmpty() && query.isNotEmpty()) {
            tvListaVazia.text = "Nenhum ticket encontrado para '$query'"
            tvListaVazia.visibility = View.VISIBLE
            rvTickets?.visibility = View.GONE
            findViewById<View>(R.id.pagination_delete).visibility = View.GONE
        } else {
            tvListaVazia.visibility = if (filteredTicketsList.isEmpty()) View.VISIBLE else View.GONE
            rvTickets?.visibility = if (filteredTicketsList.isEmpty()) View.GONE else View.VISIBLE
            findViewById<View>(R.id.pagination_delete).visibility = if (filteredTicketsList.isEmpty()) View.GONE else View.VISIBLE
            applyPagination()
        }
    }

    private fun carregarTickets() {
        swipeRefresh.isRefreshing = true
        findViewById<View>(R.id.pagination_delete).visibility = View.GONE
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // OTIMIZAÇÃO: Carregar até 300 tickets para paginação local fluida
                val response = GlpiRetrofit.api.getTodosTicketsAtivos(
                    getSessionToken(), 
                    getAppToken(), 
                    order = currentOrder, 
                    range = "0-299",
                    isDeleted = if (showDeletedOnly) 1 else 0
                )
                withContext(Dispatchers.Main) {
                    fullTicketsList = if (response.isSuccessful) response.body()?.data ?: emptyList() else emptyList()
                    filteredTicketsList = fullTicketsList
                    currentPage = 0
                    updateEmptyState()
                    swipeRefresh.isRefreshing = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    swipeRefresh.isRefreshing = false
                    Toast.makeText(this@DeleteTicketsActivity, "Erro ao carregar tickets", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun confirmarEliminacao(ticketId: String, ticket: Map<String, Any>) {
        if (isRestrictedProfile) {
            AlertHelper.exibirAlertaPremium(this, "Sem permissão para eliminar tickets.")
            return
        }
        val titulo = ticket["1"]?.toString() ?: "este ticket"
        val dialogView = layoutInflater.inflate(R.layout.dialog_delete_confirmation, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<TextView>(R.id.tv_dialog_title).text = "MOVER PARA A RECICLAGEM"
        dialogView.findViewById<TextView>(R.id.tv_dialog_message).text = "Tem a certeza que deseja mover \"$titulo\" para a reciclagem?"
        
        dialogView.findViewById<View>(R.id.btn_cancelar_delete).setOnClickListener { dialog.dismiss() }
        
        dialogView.findViewById<View>(R.id.btn_confirmar_delete).setOnClickListener {
            dialog.dismiss()
            processarEliminacao(ticketId)
        }
        dialog.show()
    }

    private fun confirmarRestauro(ticketId: String) {
        if (isRestrictedProfile) {
            AlertHelper.exibirAlertaPremium(this, "Sem permissão para realizar esta ação.")
            return
        }
        val dialogView = layoutInflater.inflate(R.layout.dialog_delete_confirmation, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<TextView>(R.id.tv_dialog_title).text = "RESTAURAR TICKET"
        dialogView.findViewById<TextView>(R.id.tv_dialog_message).text = "Deseja restaurar este ticket e movê-lo para a lista de ativos?"
        
        dialogView.findViewById<View>(R.id.btn_cancelar_delete).setOnClickListener { dialog.dismiss() }
        val btnConfirmar = dialogView.findViewById<android.widget.Button>(R.id.btn_confirmar_delete)
        btnConfirmar.text = "RESTAURAR"
        btnConfirmar.setBackgroundResource(R.drawable.bg_botao_azul)
        
        btnConfirmar.setOnClickListener {
            dialog.dismiss()
            processarRestauro(ticketId)
        }
        dialog.show()
    }

    private fun confirmarPurga(ticketId: String) {
        if (isRestrictedProfile) {
            AlertHelper.exibirAlertaPremium(this, "Sem permissão para eliminar permanentemente.")
            return
        }
        val dialogView = layoutInflater.inflate(R.layout.dialog_delete_confirmation, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<TextView>(R.id.tv_dialog_title).text = "ELIMINAÇÃO DEFINITIVA"
        dialogView.findViewById<TextView>(R.id.tv_dialog_message).text = "ATENÇÃO: Esta ação é permanente e não poderá recuperar o ticket. Deseja continuar?"
        
        dialogView.findViewById<View>(R.id.btn_cancelar_delete).setOnClickListener { dialog.dismiss() }
        val btnConfirmar = dialogView.findViewById<android.widget.Button>(R.id.btn_confirmar_delete)
        btnConfirmar.text = "CONFIRMAR"
        btnConfirmar.setBackgroundResource(R.drawable.bg_botao_vermelho)
        
        btnConfirmar.setOnClickListener {
            dialog.dismiss()
            processarPurga(ticketId)
        }
        dialog.show()
    }

    private fun processarEliminacao(ticketId: String) {
        swipeRefresh.isRefreshing = true
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = GlpiRetrofit.api.deleteTicket(ticketId, getSessionToken(), getAppToken())
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        AlertHelper.exibirAlertaPremium(this@DeleteTicketsActivity, "Ticket #$ticketId movido para a reciclagem")
                        carregarTickets()
                    } else {
                        AlertHelper.exibirAlertaPremium(this@DeleteTicketsActivity, "Erro ao eliminar ticket: ${response.code()}", isError = true)
                        swipeRefresh.isRefreshing = false
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    swipeRefresh.isRefreshing = false
                    AlertHelper.exibirAlertaPremium(this@DeleteTicketsActivity, "Erro de rede ao eliminar", isError = true)
                }
            }
        }
    }

    private fun processarRestauro(ticketId: String) {
        swipeRefresh.isRefreshing = true
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Restaurar = PUT is_deleted: 0
                val input = mapOf("input" to mapOf("id" to ticketId, "is_deleted" to 0))
                val response = GlpiRetrofit.api.updateTicket(ticketId, getSessionToken(), getAppToken(), input)
                
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        AlertHelper.exibirAlertaPremium(this@DeleteTicketsActivity, "Ticket restaurado com sucesso!")
                        carregarTickets()
                    } else {
                        AlertHelper.exibirAlertaPremium(this@DeleteTicketsActivity, "Erro ao restaurar: ${response.code()}", isError = true)
                        swipeRefresh.isRefreshing = false
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    swipeRefresh.isRefreshing = false
                    AlertHelper.exibirAlertaPremium(this@DeleteTicketsActivity, "Erro: ${e.message}", isError = true)
                }
            }
        }
    }

    private fun processarPurga(ticketId: String) {
        swipeRefresh.isRefreshing = true
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Purga = DELETE force_purge: true
                val response = GlpiRetrofit.api.deleteTicket(ticketId, getSessionToken(), getAppToken(), forcePurge = true)
                
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        AlertHelper.exibirAlertaPremium(this@DeleteTicketsActivity, "Ticket eliminado permanentemente!")
                        carregarTickets()
                    } else {
                        AlertHelper.exibirAlertaPremium(this@DeleteTicketsActivity, "Erro ao purgar: ${response.code()}", isError = true)
                        swipeRefresh.isRefreshing = false
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    swipeRefresh.isRefreshing = false
                    AlertHelper.exibirAlertaPremium(this@DeleteTicketsActivity, "Erro: ${e.message}", isError = true)
                }
            }
        }
    }

    private fun aplicarCorLottie(lottieView: LottieAnimationView?, cor: Int) {
        lottieView?.addValueCallback(KeyPath("**"), LottieProperty.COLOR_FILTER) {
            PorterDuffColorFilter(cor, PorterDuff.Mode.SRC_ATOP)
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
