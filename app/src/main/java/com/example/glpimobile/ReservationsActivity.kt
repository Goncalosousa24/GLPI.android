package com.example.glpimobile

import android.content.Intent
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.view.Gravity
import android.text.TextWatcher
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.AdapterView
import android.widget.Toast
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*
import android.os.Handler
import android.os.Looper

class ReservationsActivity : AppCompatActivity() {

    private lateinit var rvReservations: RecyclerView
    private lateinit var resAdapter: ReservationsAdapter
    private var searchBar: EditText? = null
    private lateinit var loadingView: LottieAnimationView
    private lateinit var btnAnterior: View
    private lateinit var btnProxima: View
    private lateinit var nestedScroll: androidx.core.widget.NestedScrollView
    
    private var paginaAtual = 0
    private val itensParaExibir = 10
    private val buscaRangeAPI = 100
    private var fetchJob: Job? = null

    private val TOKEN_SESSAO = GlpiConfig.SESSION_TOKEN
    private val TOKEN_APP = GlpiConfig.APP_TOKEN

    private var todosOsItems = mutableListOf<GlpiResDevice>()
    private var filtroEstadoAtual = "Todos"
    private var filtroTipoAtual = "Todos"

    override fun onCreate(savedInstanceState: Bundle?) {
        // Forçar Locale PT-PT para que os Pickers (Data/Hora) apareçam em Português
        val localePT = Locale("pt", "PT")
        Locale.setDefault(localePT)
        val config = resources.configuration
        config.setLocale(localePT)
        resources.updateConfiguration(config, resources.displayMetrics)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reservations)

        val corAzulGlpi = ContextCompat.getColor(this, R.color.azul_glpi)
        
        aplicarCorLottie(findViewById(R.id.lottie_seta_res), android.graphics.Color.WHITE)
        aplicarCorLottie(findViewById(R.id.lottie_lupa_res), corAzulGlpi)
        aplicarCorLottie(findViewById(R.id.lottie_ordem_res), corAzulGlpi)

        rvReservations = findViewById(R.id.rv_reservations)
        rvReservations.layoutManager = LinearLayoutManager(this)
        
        resAdapter = ReservationsAdapter(emptyList(), 
            onReserveClick = { item -> 
                if (PreferenceManager.isRestrictedProfile(this)) {
                    AlertHelper.exibirAlertaPremium(this, "Sem permissão para efetuar reservas.")
                } else {
                    efetuarReservaExplicita(item)
                }
            },
            onDetailsClick = { item -> mostrarDialogDetalhes(item) }
        )
        rvReservations.adapter = resAdapter

        searchBar = findViewById(R.id.search_bar_res)
        loadingView = findViewById(R.id.progress_res)
        btnAnterior = findViewById(R.id.btn_pagina_anterior_res)
        btnProxima = findViewById(R.id.btn_proxima_pagina_res)
        nestedScroll = findViewById(R.id.nested_scroll_res)
        
        aplicarCorLottie(loadingView, corAzulGlpi)
        aplicarCorLottie(findViewById(R.id.seta_voltar_res), corAzulGlpi)
        aplicarCorLottie(findViewById(R.id.seta_avancar_res), corAzulGlpi)
        
        btnAnterior.setOnClickListener {
            if (paginaAtual > 0) {
                paginaAtual--
                carregarReservas()
            }
        }
        
        btnProxima.setOnClickListener {
            paginaAtual++
            carregarReservas()
        }

        findViewById<FrameLayout>(R.id.btn_voltar_res).setOnClickListener {
            finish()
        }

        findViewById<FrameLayout>(R.id.btn_ordem_res).setOnClickListener {
            // playOrderAnimation() // Removido toque simples para responder apenas à troca
            mostrarDialogFiltroEstado()
        }

        configurarChipsTipo()
        configurarPesquisa()
        playFilterAnimation() // Play on open
        carregarReservas()
    }

    private fun configurarCliqueChip(view: View?, tipo: String) {
        view?.setOnClickListener { v ->
            val container = findViewById<LinearLayout>(R.id.container_chips_res)
            for (i in 0 until (container?.childCount ?: 0)) {
                val child = container?.getChildAt(i)
                if (child is TextView) child.isSelected = false
            }
            v.isSelected = true
            if (filtroTipoAtual != tipo) {
                filtroTipoAtual = tipo
                paginaAtual = 0
                carregarReservas()
            }
        }
    }

    private fun configurarChipsTipo() {
        val chipTodos = findViewById<TextView>(R.id.chip_res_todos)
        val chipComputadores = findViewById<TextView>(R.id.chip_res_computadores)
        val chipPerifericos = findViewById<TextView>(R.id.chip_res_perifericos)

        configurarCliqueChip(chipTodos, "Todos")
        configurarCliqueChip(chipComputadores, "Computer")
        configurarCliqueChip(chipPerifericos, "Peripheral")

        chipTodos?.isSelected = true
    }

    private fun mostrarDialogFiltroEstado() {
        val estados = arrayOf("Todos", "Livres", "Reservados")
        
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
            val isSelected = filtroEstadoAtual == title
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
                    // Fechar imediatamente para dar feedback visual de fluidez
                    dialog.dismiss()
                    
                    val changed = filtroEstadoAtual != title
                    if (changed) {
                        filtroEstadoAtual = title
                        playOrderAnimation() // Tocar animação apenas na troca real
                        paginaAtual = 0
                    }
                    
                    // 🔥 CORREÇÃO DE OVALIDADE: Usar o fundo universal de 16dp 🔥
                    val btnOrdem = this@ReservationsActivity.findViewById<FrameLayout>(R.id.btn_ordem_res)
                    if (filtroEstadoAtual != "Todos") {
                        btnOrdem?.setBackgroundResource(R.drawable.bg_filtro_ativo)
                    } else {
                        btnOrdem?.setBackgroundResource(R.drawable.bg_cartao_brilhante)
                    }
                    
                    // Usar Handler para o delay - Alternativa compatível que evita erros de lifecycleScope
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        try {
                            if (changed) {
                                carregarReservas()
                            } else {
                                aplicarFiltrosCombinados()
                            }
                        } catch (e: Exception) {
                            Log.e("FILTER_ERROR", "Erro ao aplicar filtro de estado", e)
                        }
                    }, 100)
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

    private fun aplicarFiltrosCombinados() {
        try {
            val busca = searchBar?.text?.toString()?.trim() ?: ""
            // Criar uma cópia defensiva para evitar ConcurrentModificationException
            val snapshot = ArrayList(todosOsItems)
            
            val filtrados = snapshot.filter { item ->
                val matchesBusca = busca.isEmpty() || item.name.contains(busca, ignoreCase = true)
                val matchesEstado = when (filtroEstadoAtual) {
                    "Livres" -> !item.isReservedNow
                    "Reservados" -> item.isReservedNow
                    else -> true
                }
                val matchesTipo = when (filtroTipoAtual) {
                    "Todos" -> true
                    else -> item.itemType.equals(filtroTipoAtual, ignoreCase = true)
                }
                matchesBusca && matchesEstado && matchesTipo
            }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
            .take(itensParaExibir)
            
            resAdapter.updateData(filtrados)

            // 🔥 Visibilidade inteligente da seta proxima 🔥
            btnProxima.visibility = if (filtrados.size >= itensParaExibir) View.VISIBLE else View.GONE
        } catch (e: Exception) {
            Log.e("FILTER_ERROR", "Falha ao filtrar dados: ${e.message}", e)
        }
    }

    private fun configurarPesquisa() {
        searchBar?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                aplicarFiltrosCombinados()
            }
            override fun beforeTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })
    }

    private fun carregarReservas() {
        // Cancelar carregamento anterior se ainda estiver a correr
        fetchJob?.cancel()
        
        loadingView.visibility = View.VISIBLE
        btnAnterior.visibility = View.GONE
        btnProxima.visibility = View.GONE
        todosOsItems.clear()
        resAdapter.updateData(emptyList())

        fetchJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                val rangeStr = "${paginaAtual * itensParaExibir}-${(paginaAtual * itensParaExibir) + buscaRangeAPI - 1}"
                val resListResponse = GlpiRetrofit.api.getAllReservationItems(TOKEN_SESSAO, TOKEN_APP, rangeStr)
                
                if (resListResponse.isSuccessful) {
                    val rawItems = resListResponse.body() ?: emptyList()
                    val totalItems = mutableListOf<GlpiResDevice>()
                    val chunks = rawItems.chunked(10)

                    for (chunk in chunks) {
                        if (!isActive) break // Se o Job foi cancelado, parar imediatamente
                        
                        val chunkJobs = chunk.map { data ->
                            async {
                                try {
                                    val resId = data["id"].toString().toDoubleOrNull()?.toInt() ?: 0
                                    val type = data["itemtype"]?.toString() ?: "Unknown"
                                    val itemsId = data["items_id"].toString().toDoubleOrNull()?.toInt() ?: 0

                                    // 1. Verificar se o ReservationItem em si está ativo no GLPI
                                    val isActiveVal = data["is_active"]
                                    val isActiveRes = if (isActiveVal == null) {
                                        true // Assume ativo por omissão se o campo não vier
                                    } else {
                                        when (isActiveVal) {
                                            is Boolean -> isActiveVal
                                            is Number -> isActiveVal.toInt() != 0
                                            else -> isActiveVal.toString().toDoubleOrNull()?.toInt() != 0
                                        }
                                    }
                                    if (!isActiveRes) {
                                        Log.d("GLPI_RES", "Ignorado: Item de reserva inativo no GLPI (is_active = 0): resId=$resId")
                                        return@async null
                                    }

                                    // 2. Fetch de detalhes do equipamento genérico. Se falhar, é órfão (purged/eliminado)
                                    val detailResponse = GlpiRetrofit.api.getGenericItem(type, itemsId, TOKEN_SESSAO, TOKEN_APP)
                                    if (!detailResponse.isSuccessful) {
                                        Log.d("GLPI_RES", "Ignorado: Item órfão/inexistente no GLPI: type=$type, id=$itemsId")
                                        return@async null
                                    }

                                    val detailBody = detailResponse.body() ?: return@async null

                                    // 3. Verificar se o equipamento está na lixeira (is_deleted = 1)
                                    val isDeletedVal = detailBody["is_deleted"]
                                    val isDeleted = when (isDeletedVal) {
                                        is Boolean -> isDeletedVal
                                        is Number -> isDeletedVal.toInt() != 0
                                        else -> detailBody["is_deleted"]?.toString()?.toDoubleOrNull()?.toInt() != 0
                                    }
                                    if (isDeleted) {
                                        Log.d("GLPI_RES", "Ignorado: Equipamento está na lixeira (is_deleted = 1): name=${detailBody["name"]}")
                                        return@async null
                                    }

                                    val name = detailBody["name"]?.toString() ?: "Dispositivo $itemsId"
                                    val serial = detailBody["serial"]?.toString() ?: ""

                                    // Fetch reservas
                                    val reservasResponse = GlpiRetrofit.api.getReservationsForItem(resId, TOKEN_SESSAO, TOKEN_APP)
                                    
                                    var isReservedNow = false
                                    var nextDate: String? = null
                                    var currentUser: String? = null
                                    var activeComment: String? = null
                                    var activeEndDate: String? = null

                                    if (reservasResponse.isSuccessful) {
                                        val rList = reservasResponse.body() ?: emptyList()
                                        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                        val displayFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                                        val now = Date()
                                        val futuras = mutableListOf<Date>()

                                        Log.d("GLPI_RES", "Item: $name, Reservas encontradas: ${rList.size}")

                                        for (r in rList) {
                                            val bStr = r["begin"]?.toString()
                                            val eStr = r["end"]?.toString()
                                            
                                            if (bStr != null && eStr != null) {
                                                try {
                                                    val bDate = format.parse(bStr)
                                                    val eDate = format.parse(eStr)
                                                    
                                                    if (bDate != null && eDate != null) {
                                                        // Verificação com margem de 1 minuto para evitar bugs de segundos
                                                        val isNow = now.after(bDate) && now.before(eDate)
                                                        
                                                        if (isNow) {
                                                            isReservedNow = true
                                                            // Extração inteligente do nome do utilizador (pode ser Map, List ou String)
                                                            val rawUser = r["users_id"]
                                                            val uVal = when (rawUser) {
                                                                is List<*> -> {
                                                                    if (rawUser.size > 1) {
                                                                        rawUser[1]?.toString() ?: "Utilizador"
                                                                    } else {
                                                                        rawUser.firstOrNull()?.toString() ?: "Utilizador"
                                                                    }
                                                                }
                                                                is Map<*, *> -> {
                                                                    rawUser["name"]?.toString() ?: rawUser["completename"]?.toString() ?: "Utilizador"
                                                                }
                                                                else -> {
                                                                    rawUser?.toString() ?: "Utilizador"
                                                                }
                                                            }
                                                            currentUser = formatarStringNome(uVal) ?: uVal
                                                            activeComment = r["comment"]?.toString() ?: ""
                                                            activeEndDate = displayFormat.format(eDate)
                                                            
                                                            Log.d("GLPI_RES", "Ativa! De $bStr até $eStr")
                                                        }
                                                        
                                                        if (bDate.after(now)) futuras.add(bDate)
                                                    }
                                                } catch (e: Exception) {
                                                    Log.e("GLPI_RES", "Erro parse data: $bStr / $eStr")
                                                }
                                            }
                                        }
                                        if (futuras.isNotEmpty()) {
                                            futuras.sort()
                                            nextDate = displayFormat.format(futuras.first())
                                        }
                                    }


                                    GlpiResDevice(resId, type, itemsId, name, isReservedNow, nextDate, currentUser, activeComment, activeEndDate, serial)
                                } catch (e: Exception) { null }
                            }
                        }
                        
                        val results = chunkJobs.awaitAll().filterNotNull()
                        totalItems.addAll(results)
                    }
                    
                    withContext(Dispatchers.Main) {
                        todosOsItems = ArrayList(totalItems)
                        aplicarFiltrosCombinados()
                        
                        loadingView.visibility = View.GONE
                        rvReservations.scheduleLayoutAnimation()
                        nestedScroll.smoothScrollTo(0, 0)
                        btnAnterior.visibility = if (paginaAtual > 0) View.VISIBLE else View.GONE
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        AlertHelper.exibirAlertaPremium(this@ReservationsActivity, "Erro ao listar itens", true)
                        loadingView.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (e !is CancellationException) {
                        AlertHelper.exibirAlertaPremium(this@ReservationsActivity, "Falha de rede: ${e.message}", true)
                    }
                    loadingView.visibility = View.GONE
                }
            }
        }
    }

    private fun efetuarReservaExplicita(item: GlpiResDevice) {
        val intent = Intent(this, AssetReservationActivity::class.java).apply {
            putExtra("EXTRA_NAME", item.name)
            putExtra("EXTRA_SERIAL", item.serial)
            putExtra("EXTRA_TYPE", item.itemType)
            putExtra("EXTRA_ITEMS_ID", item.itemsId)
            putExtra("EXTRA_RESERVATION_ITEMS_ID", item.reservationItemsId)
        }
        startActivityForResult(intent, 100)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == RESULT_OK) {
            carregarReservas()
            AlertHelper.exibirAlertaPremium(this, "Operação efetuada com sucesso!", false)
        }
    }

    private fun mostrarDialogBuscaUsuario(onSelected: (String, Int) -> Unit) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_user_search, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val etSearch = dialogView.findViewById<EditText>(R.id.et_search_user)
        val rvUsers = dialogView.findViewById<RecyclerView>(R.id.rv_user_results)
        val pbSearch = dialogView.findViewById<View>(R.id.pb_search_user)
        val tvNoUsers = dialogView.findViewById<View>(R.id.tv_no_users)
        val btnClose = dialogView.findViewById<View>(R.id.btn_close_user_search)

        rvUsers.layoutManager = LinearLayoutManager(this)
        val searchAdapter = UserSearchAdapter { user ->
            val id = (user["2"] as? Double)?.toInt() ?: (user["2"]?.toString()?.toIntOrNull() ?: 0)
            val realname = user["9"]?.toString() ?: ""
            val firstname = user["34"]?.toString() ?: ""
            val fullName = "$firstname $realname".trim().ifEmpty { user["1"]?.toString() ?: "Utilizador" }
            onSelected(fullName, id)
            dialog.dismiss()
        }
        rvUsers.adapter = searchAdapter

        btnClose.setOnClickListener { dialog.dismiss() }

        // Carregar lista inicial imediatamente
        executarBuscaUsuario("", pbSearch, searchAdapter, tvNoUsers)

        var searchHandler: Handler? = null
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                searchHandler?.removeCallbacksAndMessages(null)
                searchHandler = Handler(Looper.getMainLooper())
                searchHandler?.postDelayed({
                    val query = s?.toString() ?: ""
                    executarBuscaUsuario(query, pbSearch, searchAdapter, tvNoUsers)
                }, 500)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        dialog.show()
    }

    private fun executarBuscaUsuario(query: String, pb: View, adapter: UserSearchAdapter, emptyView: View) {
        pb.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val criteria = mutableMapOf(
                    "forcedisplay[0]" to "1",
                    "forcedisplay[1]" to "2",
                    "forcedisplay[2]" to "34",
                    "forcedisplay[3]" to "9"
                )

                if (query.isNotEmpty()) {
                    criteria["criteria[0][field]"] = "1"
                    criteria["criteria[0][searchtype]"] = "contains"
                    criteria["criteria[0][value]"] = query
                    
                    criteria["criteria[1][link]"] = "OR"
                    criteria["criteria[1][field]"] = "9"
                    criteria["criteria[1][searchtype]"] = "contains"
                    criteria["criteria[1][value]"] = query
                    
                    criteria["criteria[2][link]"] = "OR"
                    criteria["criteria[2][field]"] = "34"
                    criteria["criteria[2][searchtype]"] = "contains"
                    criteria["criteria[2][value]"] = query
                }

                val response = GlpiRetrofit.api.pesquisarUsuarios(TOKEN_SESSAO, TOKEN_APP, criteria)
                withContext(Dispatchers.Main) {
                    pb.visibility = View.GONE
                    if (response.isSuccessful) {
                        val usersList = response.body()?.data ?: emptyList()
                        adapter.updateList(usersList)
                        emptyView.visibility = if (usersList.isEmpty()) View.VISIBLE else View.GONE
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    pb.visibility = View.GONE
                }
            }
        }
    }

    private fun enviarReservaParaApi(item: GlpiResDevice, begin: String, end: String, comments: String, userId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main) {
                loadingView.visibility = View.VISIBLE
            }
            try {
                val reqPayload = mutableMapOf<String, Any>(
                    "input" to mutableMapOf(
                        "reservationitems_id" to item.reservationItemsId,
                        "begin" to begin,
                        "end" to end,
                        "comment" to if (comments.isBlank()) "Reserva efetuada via App GLPIMobile" else comments,
                        "users_id" to userId
                    )
                )

                val finalCreate = GlpiRetrofit.api.createReservation(TOKEN_SESSAO, TOKEN_APP, reqPayload)


                withContext(Dispatchers.Main) {
                    loadingView.visibility = View.GONE
                    if (finalCreate.isSuccessful) {
                        AlertHelper.exibirAlertaPremium(this@ReservationsActivity, "Reserva efetivada no GLPI!", false)
                        carregarReservas()
                    } else {
                        AlertHelper.exibirAlertaPremium(this@ReservationsActivity, "Erro: Servidor rejeitou a reserva", true)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loadingView.visibility = View.GONE
                    AlertHelper.exibirAlertaPremium(this@ReservationsActivity, "Sem ligação à API", true)
                }
            }
        }
    }

    private fun mostrarDialogDetalhes(item: GlpiResDevice) {
        val builder = AlertDialog.Builder(this, R.style.CustomAlertDialog)
        val dialogView = layoutInflater.inflate(R.layout.dialog_info_reserva, null)
        builder.setView(dialogView)

        val tvHeader = dialogView.findViewById<TextView>(R.id.tv_dialog_header)
        val tvContent = dialogView.findViewById<TextView>(R.id.tv_dialog_content)
        val btnFechar = dialogView.findViewById<Button>(R.id.btn_dialog_fechar)

        tvHeader.text = item.name
        
        val sb = StringBuilder()
        sb.append("TIPO: ${item.itemType}\n")
        sb.append("ESTADO: ${if (item.isReservedNow) "RESERVADO" else "LIVRE"}\n\n")

        if (item.isReservedNow) {
            sb.append("RESERVADO POR:\n${item.currentUser ?: "Não disponível"}\n\n")
            sb.append("ATÉ A DATA:\n${item.activeEndDate ?: "Não disponível"}\n\n")
            if (!item.activeComment.isNullOrBlank()) {
                sb.append("OBSERVAÇÕES:\n${item.activeComment}")
            }
        } else {
            sb.append("Este dispositivo encontra-se atualmente disponível para reserva.\n\n")
            if (item.nextReservationDate != null) {
                sb.append("PRÓXIMA RESERVA AGENDADA:\n${item.nextReservationDate}")
            } else {
                sb.append("Sem reservas futuras agendadas.")
            }
        }

        tvContent.text = sb.toString()

        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        btnFechar.setOnClickListener { dialog.dismiss() }
        
        dialog.show()
    }

    private fun formatarStringNome(raw: String?): String? {
        if (raw == null) return null
        var s = raw.trim()
        if (s.isEmpty() || s == "null") return null
        if (s.startsWith("Utilizador #", ignoreCase = true)) {
            val potential = s.substring("Utilizador #".length).trim()
            if (potential.isNotEmpty() && !potential.all { it.isDigit() }) {
                s = potential
            }
        }
        s = s.replace(".", " ")
        return s.split(" ")
            .filter { it.isNotEmpty() }
            .joinToString(" ") { word ->
                word.lowercase().replaceFirstChar { 
                    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() 
                }
            }
    }

    private fun aplicarCorLottie(lottieView: LottieAnimationView?, cor: Int) {
        lottieView?.addValueCallback(KeyPath("**"), LottieProperty.COLOR_FILTER) {
            PorterDuffColorFilter(cor, PorterDuff.Mode.SRC_ATOP)
        }
    }

    override fun dispatchTouchEvent(ev: android.view.MotionEvent?): Boolean {
        KeyboardHelper.handleTouchOutside(this, ev)
        return super.dispatchTouchEvent(ev)
    }
}
