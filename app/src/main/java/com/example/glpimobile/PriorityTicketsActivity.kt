package com.example.glpimobile

import android.content.Intent
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class PriorityTicketsActivity : AppCompatActivity() {

    private var listaCompleta: List<Map<String, Any>> = emptyList()
    private var listaFiltrada: List<Map<String, Any>> = emptyList()
    private lateinit var rvTickets: RecyclerView
    private lateinit var btnAnterior: View
    private lateinit var btnProxima: View
    private lateinit var nestedScroll: androidx.core.widget.NestedScrollView
    private lateinit var tvListaVazia: TextView

    private var currentPage = 0
    private val pageSize = 10
    private var priorityFiltro: Int = 0 // 0 = Todos (4,5,6), 4 = Alta, 5 = Muito Alta, 6 = Principal
    private var currentOrder = "DESC"
    private var filtrarMeus = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_priority_tickets)

        val corAzul = ContextCompat.getColor(this, R.color.azul_glpi)
        val corBranca = ContextCompat.getColor(this, android.R.color.white)

        val lottieSeta = findViewById<LottieAnimationView>(R.id.lottie_seta)
        aplicarCorLottie(lottieSeta, corBranca)

        val lottieLupa = findViewById<LottieAnimationView>(R.id.lottie_lupa)
        aplicarCorLottie(lottieLupa, corAzul)

        val lottieOrdem = findViewById<LottieAnimationView>(R.id.lottie_ordem_priority)
        aplicarCorLottie(lottieOrdem, corAzul)

        findViewById<View>(R.id.btn_voltar).setOnClickListener { voltarParaMenu() }

        findViewById<FrameLayout>(R.id.btn_ordem_priority).setOnClickListener {
            currentOrder = if (currentOrder == "DESC") "ASC" else "DESC"
            playOrderAnimation()
            findViewById<LottieAnimationView>(R.id.lottie_ordem_priority)?.rotation = if (currentOrder == "DESC") 0f else 180f
            carregarTicketsPrioritarios()
        }

        rvTickets = findViewById<RecyclerView>(R.id.rv_tickets_priority)
        rvTickets.layoutManager = LinearLayoutManager(this)

        btnAnterior = findViewById<View>(R.id.btn_pagina_anterior_priority)
        btnProxima = findViewById<View>(R.id.btn_proxima_pagina_priority)
        nestedScroll = findViewById<androidx.core.widget.NestedScrollView>(R.id.nested_scroll_priority)
        tvListaVazia = findViewById<TextView>(R.id.tv_lista_vazia)

        lottieSeta.playAnimation()

        // Pintar as setas de paginação de azul
        aplicarCorLottie(findViewById(R.id.lottie_seta_voltar_priority), corAzul)
        aplicarCorLottie(findViewById(R.id.lottie_seta_avancar_priority), corAzul)

        // Botão de Filtro
        val btnFiltro = findViewById<View>(R.id.btn_filtro_priority)
        val lottieFiltro = findViewById<LottieAnimationView>(R.id.lottie_filtro)
        aplicarCorLottie(lottieFiltro, corAzul)
        
        btnFiltro.setOnClickListener {
            mostrarDialogFiltro()
        }

        btnAnterior.setOnClickListener {
            if (currentPage > 0) {
                currentPage--
                applyPagination()
            }
        }

        btnProxima.setOnClickListener {
            if ((currentPage + 1) * pageSize < listaFiltrada.size) {
                currentPage++
                applyPagination()
            }
        }

        val searchBar = findViewById<EditText>(R.id.search_bar_priority)
        searchBar.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                val text = s.toString().lowercase()
                listaFiltrada = if (text.isEmpty()) {
                    listaCompleta
                } else {
                    listaCompleta.filter { ticket ->
                        val assunto = ticket["1"]?.toString()?.lowercase() ?: ""
                        val id = ticket["2"]?.toString()?.lowercase() ?: ""
                        assunto.contains(text) || id.contains(text)
                    }
                }
                currentPage = 0
                applyPagination()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Se viermos do Dashboard em "Vista Pessoal"
        if (intent.getBooleanExtra("FILTRO_MEUS", false)) {
            filtrarMeus = true
            btnFiltro.setBackgroundResource(R.drawable.bg_filtro_ativo)
        }

        playFilterAnimation() // Play on open
        carregarTicketsPrioritarios()
    }

    private fun carregarTicketsPrioritarios() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userId = GlpiConfig.USER_ID
                val response = if (filtrarMeus) {
                    val loginBusca = GlpiConfig.USER_NAME.split(".", " ").firstOrNull() ?: ""
                    val nomeCompleto = GlpiConfig.USER_FULL_NAME
                    if (priorityFiltro == 0) {
                        GlpiRetrofit.api.getListaPrioritariosMeus(
                            sessionToken = GlpiConfig.SESSION_TOKEN,
                            appToken = GlpiConfig.APP_TOKEN,
                            userId1 = userId,
                            userId2 = userId,
                            userId3 = userId,
                            userId4 = userId,
                            userId5 = userId,
                            userId6 = userId,
                            login1 = loginBusca,
                            login2 = nomeCompleto.ifEmpty { loginBusca },
                            order = currentOrder,
                            range = "0-149",
                            sort = 19
                        )
                    } else {
                        val loginBuscaF = GlpiConfig.USER_NAME.split(".", " ").firstOrNull() ?: ""
                        val nomeCompletoF = GlpiConfig.USER_FULL_NAME
                        GlpiRetrofit.api.getListaPrioritariosPorPrioridadeMeus(
                            sessionToken = GlpiConfig.SESSION_TOKEN,
                            appToken = GlpiConfig.APP_TOKEN,
                            userId1 = userId,
                            userId2 = userId,
                            userId3 = userId,
                            userId4 = userId,
                            userId5 = userId,
                            userId6 = userId,
                            login1 = loginBuscaF,
                            login2 = nomeCompletoF.ifEmpty { loginBuscaF },
                            priorityValue1 = priorityFiltro,
                            order = currentOrder,
                            range = "0-149"
                        )
                    }
                } else {
                    // VISTA GERAL (Dashboard) -> Total da Equipa
                    if (priorityFiltro == 0) {
                        GlpiRetrofit.api.getListaPrioritariosTotal(
                            sessionToken = GlpiConfig.SESSION_TOKEN,
                            appToken = GlpiConfig.APP_TOKEN,
                            order = currentOrder,
                            range = "0-149"
                        )
                    } else {
                        GlpiRetrofit.api.getListaPrioritariosPorPrioridade(
                            sessionToken = GlpiConfig.SESSION_TOKEN,
                            appToken = GlpiConfig.APP_TOKEN,
                            priorityValue = priorityFiltro,
                            order = currentOrder,
                            range = "0-149"
                        )
                    }
                }


                withContext(Dispatchers.Main) {
                    val btnFiltro = findViewById<View>(R.id.btn_filtro_priority)
                    if (priorityFiltro != 0) {
                        btnFiltro.setBackgroundResource(R.drawable.bg_filtro_ativo)
                    } else {
                        btnFiltro.setBackgroundResource(R.drawable.bg_cartao_brilhante)
                    }

                    listaCompleta = response.data ?: emptyList()
                    listaFiltrada = listaCompleta
                    currentPage = 0
                    applyPagination()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@PriorityTicketsActivity, "Erro ao carregar lista: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun mostrarDialogFiltro() {
        val opcoes = arrayOf("TODOS", "ALTA", "MUITO ALTA", "PRINCIPAL")
        val statusIds = arrayOf(0, 4, 5, 6)

        val dialogView = layoutInflater.inflate(R.layout.dialog_custom_filter_state, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<TextView>(R.id.tv_filter_title).text = "FILTRAR POR PRIORIDADE"
        val container = dialogView.findViewById<android.widget.LinearLayout>(R.id.container_filter_options)
        
        dialogView.findViewById<View>(R.id.btn_close_filter).setOnClickListener { dialog.dismiss() }

        val d = resources.displayMetrics.density

        opcoes.forEachIndexed { index, title ->
            val isSelected = priorityFiltro == statusIds[index]
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
                    priorityFiltro = statusIds[index]
                    
                    val btnFiltroMain = this@PriorityTicketsActivity.findViewById<FrameLayout>(R.id.btn_filtro_priority)
                    if (priorityFiltro != 0) {
                        btnFiltroMain?.setBackgroundResource(R.drawable.bg_filtro_ativo)
                    } else {
                        btnFiltroMain?.setBackgroundResource(R.drawable.bg_cartao_brilhante)
                    }
                    
                    carregarTicketsPrioritarios()
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

    private fun applyPagination() {
        val start = currentPage * pageSize
        val end = minOf(start + pageSize, listaFiltrada.size)
        val pageItems = if (listaFiltrada.isEmpty()) listOf() else listaFiltrada.subList(start, end)

        rvTickets.adapter = TicketAdapter(pageItems, showCreationDate = true)
        rvTickets.scheduleLayoutAnimation()

        btnAnterior.visibility = if (currentPage > 0) View.VISIBLE else View.GONE
        btnProxima.visibility = if (end < listaFiltrada.size) View.VISIBLE else View.GONE

        nestedScroll.smoothScrollTo(0, 0)

        tvListaVazia.visibility = if (listaFiltrada.isEmpty()) View.VISIBLE else View.GONE
        findViewById<View>(R.id.pagination_priority).visibility = if (listaFiltrada.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun aplicarCorLottie(lottieView: LottieAnimationView?, cor: Int) {
        lottieView?.addValueCallback(KeyPath("**"), LottieProperty.COLOR_FILTER) {
            PorterDuffColorFilter(cor, PorterDuff.Mode.SRC_ATOP)
        }
    }

    @Suppress("DEPRECATION")
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

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        voltarParaMenu()
    }
}
