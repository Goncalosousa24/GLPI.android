package com.example.glpimobile

import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath
import kotlinx.coroutines.*

class LogsActivity : AppCompatActivity() {

    private var isExpanded = false
    private val addedChips = mutableListOf<View>()

    private lateinit var rvEquipamentos: RecyclerView
    private lateinit var logAdapter: ActivityLogAdapter
    private var searchBar: EditText? = null
    private lateinit var tvListaVazia: TextView
    private lateinit var nestedScrollView: NestedScrollView

    private var paginaAtual = 0
    private val itensParaExibir = 10
    private val buscaRangeAPI = 100

    private var itemTypeAtual = "All"
    private var estadoFiltroAtual: String = "Todos"

    private val TOKEN_SESSAO = GlpiConfig.SESSION_TOKEN
    private val TOKEN_APP = GlpiConfig.APP_TOKEN

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logs)

        val corAzulGlpi = ContextCompat.getColor(this, R.color.azul_glpi)
        val corBranca = ContextCompat.getColor(this, android.R.color.white)

        rvEquipamentos = findViewById(R.id.rv_activity_logs)
        rvEquipamentos.layoutManager = LinearLayoutManager(this)
        logAdapter = ActivityLogAdapter(emptyList(), onAlterarClick = { item ->
            if (PreferenceManager.isRestrictedProfile(this)) {
                AlertHelper.exibirAlertaPremium(this@LogsActivity, "Sem permissão para alterar estados.")
            } else {
                mostrarDialogAlterarEstado(item)
            }
        })
        rvEquipamentos.adapter = logAdapter

        searchBar = findViewById(R.id.search_bar_logs)
        tvListaVazia = findViewById(R.id.tv_lista_vazia)
        nestedScrollView = findViewById(R.id.nested_scroll_main)

        configurarCoresLottie(corAzulGlpi, corBranca)

        findViewById<FrameLayout>(R.id.btn_voltar).setOnClickListener { finish() }
        findViewById<FrameLayout>(R.id.btn_ordem).setOnClickListener {
            playOrderAnimation()
            mostrarDialogFiltroEstado()
        }

        findViewById<View>(R.id.btn_pagina_anterior).setOnClickListener {
            if (paginaAtual > 0) { paginaAtual--; carregarInventario() }
        }

        findViewById<View>(R.id.btn_proxima_pagina).setOnClickListener {
            paginaAtual++; carregarInventario()
        }

        configurarChipsIniciais()
        configurarBotaoMaisExpansivel()

        playFilterAnimation() // Trigger animation on open
        carregarInventario()
    }

    private fun configurarCoresLottie(azul: Int, branco: Int) {
        aplicarCorLottie(findViewById(R.id.lottie_seta), branco)
        aplicarCorLottie(findViewById(R.id.plus_animation), branco)
        aplicarCorLottie(findViewById(R.id.minus_animation), branco)
        aplicarCorLottie(findViewById(R.id.lottie_lupa), azul)
        aplicarCorLottie(findViewById(R.id.lottie_ordem), azul)
        aplicarCorLottie(findViewById(R.id.seta_voltar), branco)
        aplicarCorLottie(findViewById(R.id.seta_avancar), azul)
    }

    private fun configurarBotaoMaisExpansivel() {
        val btnMais = findViewById<FrameLayout>(R.id.btn_expandir_categorias)
        val containerChips = findViewById<LinearLayout>(R.id.container_chips)
        val symbolSwitcher = findViewById<ViewSwitcher>(R.id.symbol_switcher)
        val espacoFlexivel = findViewById<Space>(R.id.espaco_flexivel)

        btnMais?.setOnClickListener {
            if (!isExpanded) {
                // Adicionamos Impressoras e Redes dinamicamente
                val extras = mapOf(
                    "IMPRESSORAS" to "Printer",
                    "REDES" to "NetworkEquipment"
                )

                extras.forEach { (nome, tipo) ->
                    val novoChip = criarChipObjeto(nome)
                    configurarCliqueChip(novoChip, tipo)

                    val index = containerChips.indexOfChild(espacoFlexivel)
                    containerChips.addView(novoChip, index)
                    addedChips.add(novoChip)
                }

                symbolSwitcher.displayedChild = 1
                isExpanded = true
            } else {
                addedChips.forEach { containerChips.removeView(it) }
                addedChips.clear()
                symbolSwitcher.displayedChild = 0
                isExpanded = false
            }
        }
    }

    private fun configurarChipsIniciais() {
        val chipTodos = findViewById<TextView>(R.id.chip_todos)
        val chipComp = findViewById<TextView>(R.id.chip_computadores)
        val chipMon = findViewById<TextView>(R.id.chip_monitores)

        configurarCliqueChip(chipTodos, "All")
        configurarCliqueChip(chipComp, "Computer")
        configurarCliqueChip(chipMon, "Monitor")

        chipTodos?.isSelected = true
    }

    private fun configurarCliqueChip(view: View?, tipo: String) {
        view?.setOnClickListener { v ->
            val container = findViewById<LinearLayout>(R.id.container_chips)
            for (i in 0 until (container?.childCount ?: 0)) {
                val child = container?.getChildAt(i)
                if (child is TextView) child.isSelected = false
            }
            v.isSelected = true
            itemTypeAtual = tipo
            paginaAtual = 0
            
            playFilterAnimation()
            
            carregarInventario()
        }
    }

    private fun criarChipObjeto(nome: String): TextView {
        return TextView(this).apply {
            text = nome
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            includeFontPadding = false
            setBackgroundResource(R.drawable.selector_chip_filtros)
            setTextColor(ContextCompat.getColorStateList(context, R.color.selector_texto_chip))
            val d = resources.displayMetrics.density
            layoutParams = LinearLayout.LayoutParams((110 * d).toInt(), (32 * d).toInt()).apply {
                setMargins(0, 0, (8 * d).toInt(), 0)
            }
        }
    }

    private fun carregarInventario() {
        val range = "${paginaAtual * itensParaExibir}-${(paginaAtual * itensParaExibir) + buscaRangeAPI}"

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val listaResultados = mutableListOf<GlpiEquipamento>()

                val forcedisplay = mapOf(
                    "forcedisplay[0]" to "1",  // Nome
                    "forcedisplay[1]" to "2",  // ID
                    "forcedisplay[2]" to "5",  // Serial
                    "forcedisplay[3]" to "70", // Utilizador Principal
                    "forcedisplay[4]" to "8",  // Técnico Responsável
                    "forcedisplay[5]" to "24", // Utilizador Alternativo
                    "forcedisplay[6]" to "15", // Nome Alternativo
                    "forcedisplay[7]" to "31"  // Estado
                )

                var totalCountAPI = 0
                if (itemTypeAtual == "All") {
                    val tipos = listOf("Computer", "Monitor", "NetworkEquipment", "Printer")
                    val jobs = tipos.map { tipo ->
                        async<List<GlpiEquipamento>> {
                            try {
                                val res = GlpiRetrofit.api.searchItems(tipo, TOKEN_SESSAO, TOKEN_APP, range, forcedisplay)
                                if (res.isSuccessful) {
                                    val body = res.body()
                                    totalCountAPI += body?.totalcount ?: 0
                                    processarResposta(body?.data ?: emptyList(), tipo)
                                } else emptyList()
                            } catch (e: Exception) { emptyList() }
                        }
                    }
                    listaResultados.addAll(jobs.awaitAll().flatten())
                } else {
                    val res = GlpiRetrofit.api.searchItems(itemTypeAtual, TOKEN_SESSAO, TOKEN_APP, range, forcedisplay)
                    if (res.isSuccessful) {
                        val body = res.body()
                        totalCountAPI = body?.totalcount ?: 0
                        listaResultados.addAll(processarResposta(body?.data ?: emptyList(), itemTypeAtual))
                    }
                }

                withContext(Dispatchers.Main) {
                    val listaFinal = listaResultados.filter {
                        val statusLimpo = it.estadoStr ?: "Não definido"
                        estadoFiltroAtual == "Todos" || statusLimpo.contains(estadoFiltroAtual, ignoreCase = true)
                    }.take(itensParaExibir)

                    logAdapter.updateData(listaFinal)
                    rvEquipamentos.scheduleLayoutAnimation()

                    nestedScrollView.smoothScrollTo(0, 0)

                    tvListaVazia.visibility = if (listaFinal.isEmpty()) View.VISIBLE else View.GONE
                    rvEquipamentos.visibility = if (listaFinal.isEmpty()) View.GONE else View.VISIBLE

                    findViewById<View>(R.id.btn_pagina_anterior).visibility = if (paginaAtual > 0) View.VISIBLE else View.GONE
                    
                    // 🔥 Próximo só se houver mais na API para além do range atual 🔥
                    val itensJaCarregados = paginaAtual * itensParaExibir + listaResultados.size
                    findViewById<View>(R.id.btn_proxima_pagina).visibility = if (itensJaCarregados < totalCountAPI) View.VISIBLE else View.GONE

                    val btnOrdem = findViewById<FrameLayout>(R.id.btn_ordem)
                    if (estadoFiltroAtual != "Todos") {
                        btnOrdem.setBackgroundResource(R.drawable.bg_filtro_borda_azul_transparente)
                    } else {
                        btnOrdem.setBackgroundResource(R.drawable.bg_cartao_brilhante)
                    }
                }
            } catch (e: Exception) {
                Log.e("GLPI_ERROR", "Erro: ${e.message}")
            }
        }
    }

    private fun processarResposta(data: List<Map<String, Any>>?, tipo: String): List<GlpiEquipamento> {
        val lista = mutableListOf<GlpiEquipamento>()
        data?.forEach { itemMap ->
            val idVal = (itemMap["2"] as? Double)?.toInt() ?: 
                        (itemMap["id"] as? Double)?.toInt() ?: 
                        itemMap["2"]?.toString()?.toDoubleOrNull()?.toInt() ?: 0
            val nomeVal = itemMap["1"]?.toString() ?: "Desconhecido"
            val snVal = itemMap["5"]?.toString() ?: itemMap["80"]?.toString() ?: "---"

            // 🔍 EXTRAÇÃO "REDE ALARGADA" (Igual ao Inventário)
            fun extractName(raw: Any?): String {
                return when (raw) {
                    is Map<*, *> -> raw["name"]?.toString() ?: raw["login"]?.toString() ?: ""
                    else -> raw?.toString() ?: ""
                }.let { if (it == "null" || it == "[]") "" else it }.trim()
            }

            val n70 = extractName(itemMap["70"]) 
            val n8 = extractName(itemMap["8"])   
            val n24 = extractName(itemMap["24"]) 
            val n15 = extractName(itemMap["15"]) 

            val finalUser = when {
                n70.isNotEmpty() -> n70
                n8.isNotEmpty() -> n8
                n24.isNotEmpty() -> n24
                n15.isNotEmpty() -> n15
                else -> "Sem utilizador"
            }


            // 🔥 Busca inteligente: tenta campo 31 ou campo 10 (comum em impressoras)
            val estadoVal = (itemMap["31"] ?: itemMap["10"])?.toString()?.trim() ?: "Não definido"

            lista.add(GlpiEquipamento(
                id = idVal,
                name = nomeVal,
                serial = snVal,
                estadoStr = estadoVal,
                utilizador = finalUser,

                itemtype = tipo
            ))
        }
        return lista
    }

    private fun mostrarDialogFiltroEstado() {
        val estados = arrayOf("Todos", "Novo", "Avariado", "Vigor", "Usado", "Informado")
        
        val dialogView = layoutInflater.inflate(R.layout.dialog_custom_filter_state, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<TextView>(R.id.tv_filter_title).text = "FILTRAR ESTADO"
        val container = dialogView.findViewById<android.widget.LinearLayout>(R.id.container_filter_options)
        val btnClose = dialogView.findViewById<android.view.View>(R.id.btn_close_filter)
        btnClose.setOnClickListener { dialog.dismiss() }

        val d = resources.displayMetrics.density

        estados.forEachIndexed { index, title ->
            val isSelected = estadoFiltroAtual == title
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
                    estadoFiltroAtual = title
                    paginaAtual = 0
                    playOrderAnimation()
                    carregarInventario()
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

    private fun aplicarCorLottie(lottieView: LottieAnimationView?, cor: Int) {
        lottieView?.addValueCallback(KeyPath("**"), LottieProperty.COLOR_FILTER) {
            PorterDuffColorFilter(cor, PorterDuff.Mode.SRC_ATOP)
        }
    }

    private fun verificarPerfilRestrito(): Boolean {
        val profileName = PreferenceManager.getUserProfile(this) ?: ""
        return profileName.contains("leitura", ignoreCase = true) || 
               profileName.contains("read-only", ignoreCase = true) ||
               profileName.contains("observador", ignoreCase = true) ||
               profileName.contains("observer", ignoreCase = true)
    }

    private fun mostrarDialogAlterarEstado(item: GlpiEquipamento) {
        val progressDialog = AlertDialog.Builder(this)
            .setMessage("A carregar estados...")
            .setCancelable(false)
            .show()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val res = GlpiRetrofit.api.getStatesList(TOKEN_SESSAO, TOKEN_APP)
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    exibirDialogComEstados(item, res)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    AlertHelper.exibirAlertaPremium(this@LogsActivity, "Erro ao carregar estados.")
                }
            }
        }
    }

    private fun exibirDialogComEstados(item: GlpiEquipamento, listaEstados: List<Map<String, Any>>) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_custom_filter_state, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<TextView>(R.id.tv_filter_title).text = "ALTERAR ESTADO"
        val container = dialogView.findViewById<LinearLayout>(R.id.container_filter_options)
        val btnClose = dialogView.findViewById<View>(R.id.btn_close_filter)
        btnClose.setOnClickListener { dialog.dismiss() }

        val d = resources.displayMetrics.density
        val estadoAtualNome = item.estadoStr?.trim() ?: ""
        
        var idSelecionado = -1
        var nomeSelecionado = ""
        val listaRadioButtons = mutableListOf<RadioButton>()

        // 1. Criar lista mutável para incluir a opção manual "Nenhum"
        val listaCompleta = listaEstados.toMutableList()
        
        // Adicionamos "Nenhum" (ID 0) no início da lista se não existir
        if (!listaCompleta.any { it["id"]?.toString() == "0" }) {
            listaCompleta.add(0, mapOf("id" to 0, "name" to "NENHUM"))
        }

        listaCompleta.forEach { estadoMap ->
            val idEstado = (estadoMap["id"] as? Double)?.toInt() ?: 
                          estadoMap["id"]?.toString()?.toIntOrNull() ?: 0
            val nomeEstado = estadoMap["name"]?.toString() ?: "Desconhecido"
            
            val isSelected = if (idEstado == 0) {
                estadoAtualNome.equals("Não definido", ignoreCase = true) || estadoAtualNome.isEmpty()
            } else {
                estadoAtualNome.equals(nomeEstado, ignoreCase = true)
            }
            
            if (isSelected) {
                idSelecionado = idEstado
                nomeSelecionado = nomeEstado
            }
            
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    (48 * d).toInt()
                )
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding((16 * d).toInt(), 0, (16 * d).toInt(), 0)
                
                val outValue = android.util.TypedValue()
                context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
                setBackgroundResource(outValue.resourceId)
            }

            val tv = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                text = nomeEstado.uppercase()
                setTextColor(ContextCompat.getColor(context, R.color.texto_principal))
                textSize = 14f
                typeface = androidx.core.content.res.ResourcesCompat.getFont(context, R.font.amiko_bold)
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                gravity = android.view.Gravity.START or android.view.Gravity.CENTER_VERTICAL
            }

            val rb = RadioButton(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                buttonTintList = android.content.res.ColorStateList.valueOf(ContextCompat.getColor(context, R.color.azul_glpi))
                isChecked = isSelected
                isClickable = false
                isFocusable = false
            }
            
            listaRadioButtons.add(rb)
            
            row.setOnClickListener {
                idSelecionado = idEstado
                nomeSelecionado = nomeEstado
                listaRadioButtons.forEach { it.isChecked = false }
                rb.isChecked = true
            }

            row.addView(tv)
            row.addView(rb)
            container.addView(row)
        }

        val btnConfirmar = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (50 * d).toInt()
            ).apply {
                setMargins((16 * d).toInt(), (24 * d).toInt(), (16 * d).toInt(), (8 * d).toInt())
            }
            text = "CONFIRMAR"
            setTextColor(Color.WHITE)
            textSize = 14f
            gravity = Gravity.CENTER
            typeface = androidx.core.content.res.ResourcesCompat.getFont(context, R.font.amiko_bold)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setBackgroundResource(R.drawable.bg_botao_azul)
            
            setOnClickListener {
                if (idSelecionado != -1) {
                    atualizarEstadoNoGlpi(item, idSelecionado, nomeSelecionado)
                    dialog.dismiss()
                } else {
                    Toast.makeText(context, "Selecione um estado", Toast.LENGTH_SHORT).show()
                }
            }
        }
        container.addView(btnConfirmar)

        dialog.show()
        dialog.window?.setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun atualizarEstadoNoGlpi(item: GlpiEquipamento, novoId: Int, nomeEstado: String) {
        val inputData = mapOf("input" to mapOf("states_id" to novoId))
        Log.d("GLPI_STATE_DEBUG", "A atualizar ${item.itemtype} ID ${item.id} para Estado ID $novoId")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = GlpiRetrofit.api.updateItem(
                    itemtype = item.itemtype,
                    id = item.id,
                    sessionToken = TOKEN_SESSAO,
                    appToken = TOKEN_APP,
                    input = inputData
                )
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        AlertHelper.exibirAlertaPremium(this@LogsActivity, "Sucesso: $nomeEstado", false)
                        carregarInventario() // Refresh para ver o novo estado
                    } else {
                        AlertHelper.exibirAlertaPremium(this@LogsActivity, "Erro ao atualizar no servidor")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    AlertHelper.exibirAlertaPremium(this@LogsActivity, "Erro: ${e.message}")
                }
            }
        }
    }

    override fun dispatchTouchEvent(ev: android.view.MotionEvent?): Boolean {
        KeyboardHelper.handleTouchOutside(this, ev)
        return super.dispatchTouchEvent(ev)
    }
}
