package com.example.glpimobile

import android.graphics.Color
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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath
import kotlinx.coroutines.*

class ActionHistoryActivity : AppCompatActivity() {

    private lateinit var rvHistory: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var lottieLoading: LottieAnimationView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: ActionHistoryAdapter
    
    private lateinit var btnAnterior: View
    private lateinit var btnProxima: View
    private lateinit var nestedScroll: androidx.core.widget.NestedScrollView

    private var fullLogsList: List<Map<String, Any>> = listOf()
    private var currentPage = 0
    private val pageSize = 10

    private val USER_ID_ALVO get() = PreferenceManager.getUserId(this)
    private val TOKEN_SESSAO get() = GlpiConfig.SESSION_TOKEN
    private val TOKEN_APP get() = GlpiConfig.APP_TOKEN

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_action_history)

        inicializarUI()
        carregarHistorico()
    }

    private fun inicializarUI() {
        rvHistory = findViewById(R.id.rv_action_history)
        swipeRefresh = findViewById(R.id.swipeRefresh_history)
        lottieLoading = findViewById(R.id.lottie_loading_history)
        tvEmpty = findViewById(R.id.tv_empty_history)
        nestedScroll = findViewById(R.id.nested_scroll_history_scrollview)

        rvHistory.layoutManager = LinearLayoutManager(this)
        adapter = ActionHistoryAdapter(emptyList())
        rvHistory.adapter = adapter

        btnAnterior = findViewById(R.id.btn_pagina_anterior_history)
        btnProxima = findViewById(R.id.btn_proxima_pagina_history)

        findViewById<FrameLayout>(R.id.btn_voltar_history).setOnClickListener { 
            finish()
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }

        val corAzul = ContextCompat.getColor(this, R.color.azul_glpi)
        val lottieSeta = findViewById<LottieAnimationView>(R.id.lottie_seta_history)
        lottieSeta.addValueCallback(KeyPath("**"), LottieProperty.COLOR_FILTER) {
            PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP)
        }

        // Aplicar cor azul às setas de paginação
        aplicarCorLottie(findViewById(R.id.lottie_seta_voltar_history_pag), corAzul)
        aplicarCorLottie(findViewById(R.id.lottie_seta_avancar_history_pag), corAzul)

        btnAnterior.setOnClickListener {
            if (currentPage > 0) {
                currentPage--
                applyPagination()
            }
        }

        btnProxima.setOnClickListener {
            if ((currentPage + 1) * pageSize < fullLogsList.size) {
                currentPage++
                applyPagination()
            }
        }

        swipeRefresh.setOnRefreshListener { carregarHistorico() }
    }

    private fun applyPagination() {
        val start = currentPage * pageSize
        val end = minOf(start + pageSize, fullLogsList.size)
        val pageItems = if (fullLogsList.isEmpty()) listOf() else fullLogsList.subList(start, end)

        adapter.updateList(pageItems)
        rvHistory.scheduleLayoutAnimation()

        btnAnterior.visibility = if (currentPage > 0) View.VISIBLE else View.GONE
        btnProxima.visibility = if (end < fullLogsList.size) View.VISIBLE else View.GONE
        
        val showPagination = btnAnterior.visibility == View.VISIBLE || btnProxima.visibility == View.VISIBLE
        findViewById<View>(R.id.pagination_history).visibility = if (showPagination) View.VISIBLE else View.GONE

        // Procura o NestedScrollView dentro do SwipeRefresh
        nestedScroll.smoothScrollTo(0, 0)
    }

    private fun aplicarCorLottie(lottieView: LottieAnimationView?, cor: Int) {
        lottieView?.addValueCallback(KeyPath("**"), LottieProperty.COLOR_FILTER) {
            PorterDuffColorFilter(cor, PorterDuff.Mode.SRC_ATOP)
        }
    }

    private fun carregarHistorico() {
        if (!swipeRefresh.isRefreshing) {
            lottieLoading.visibility = View.VISIBLE
            rvHistory.visibility = View.GONE
            tvEmpty.visibility = View.GONE
            findViewById<View>(R.id.pagination_history).visibility = View.GONE
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = GlpiRetrofit.api.getFullSession(TOKEN_SESSAO, TOKEN_APP)
                val sessionInfo = response.body()
                val activeProfile = sessionInfo?.get("glpiactiveprofile") as? Map<String, Any>
                val activeUser = sessionInfo?.get("glpiactive_user") as? Map<String, Any>
                
                val tempId = activeUser?.get("id") ?: activeProfile?.get("users_id")
                val realUserId = tempId?.toString()?.toDoubleOrNull()?.toInt() ?: -1
                
                val realUserName = activeUser?.get("name")?.toString() ?: activeProfile?.get("name")?.toString() ?: "Utilizador Desconhecido"

                if (realUserId == -1) {
                    withContext(Dispatchers.Main) {
                        lottieLoading.visibility = View.GONE
                        swipeRefresh.isRefreshing = false
                        tvEmpty.text = "Erro: Sessão não identificada. Verifica o Token."
                        tvEmpty.visibility = View.VISIBLE
                    }
                    return@launch
                }

                val resposta = GlpiRetrofit.api.getLogsByUser(TOKEN_SESSAO, TOKEN_APP, userId = realUserId)
                withContext(Dispatchers.Main) {
                    fullLogsList = resposta
                    currentPage = 0
                    lottieLoading.visibility = View.GONE
                    swipeRefresh.isRefreshing = false
                    
                    findViewById<TextView>(R.id.tv_titulo_history).text = "HISTÓRICO: $realUserName"

                    if (fullLogsList.isEmpty()) {
                        tvEmpty.visibility = View.VISIBLE
                        rvHistory.visibility = View.GONE
                        findViewById<View>(R.id.pagination_history).visibility = View.GONE
                    } else {
                        tvEmpty.visibility = View.GONE
                        rvHistory.visibility = View.VISIBLE
                        findViewById<View>(R.id.pagination_history).visibility = View.VISIBLE
                        applyPagination()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    lottieLoading.visibility = View.GONE
                    swipeRefresh.isRefreshing = false
                    Toast.makeText(this@ActionHistoryActivity, "Erro: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
