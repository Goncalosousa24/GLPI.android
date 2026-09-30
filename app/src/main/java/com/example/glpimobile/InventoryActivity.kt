package com.example.glpimobile

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.*

class InventoryActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var lottieHamburger: LottieAnimationView
    private var isExpanded = false
    private val addedChips = mutableListOf<View>()
    private lateinit var rvInventario: RecyclerView
    private var fullInventoryList = mutableListOf<Map<String, Any>>()
    private lateinit var inventoryAdapter: InventoryAdapter
    private var paginaAtual = 0
    private val itensPorPagina = 10
    private var locationIdFiltro: String? = null
    private var locationNomeFiltro: String? = null

    private var itemTypeAtual = "All"
    private var manualTypeSelected = "All"

    // Flag para saber se o texto veio da câmara
    private var isScanResult = false
    private var isRestrictedProfile = false
    private var isReadOnlyProfile = false

    private lateinit var btnAnterior: View
    private lateinit var btnProxima: View
    private lateinit var paginationInventory: View
    private lateinit var nestedScroll: NestedScrollView
    private var searchJob: Job? = null

    private val SCAN_REQUEST_CODE = 1001
    private val TOKEN_SESSAO = GlpiConfig.SESSION_TOKEN
    private val TOKEN_APP = GlpiConfig.APP_TOKEN

    private val categoriasNomes = listOf("DPS", "DSI", "EXE", "Stock", "UIC")
    private val categoriasFilhos = mapOf(
        "DPS" to listOf("Ação Social", "Complexo de Lazer de Vila Verde", "CPCJ", "Loja Social", "Piscinas de Prado", "SQIP", "GIF"),
        "DSI" to listOf("Arquivo", "Sala Bastidores", "Sala de Arrumos Informática"),
        "EXE" to listOf("GRP"),
        "Stock" to listOf("Red Tagged"),
        "UIC" to listOf("Casa do Conhecimento")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inventory)

        drawerLayout = findViewById(R.id.drawer_layout_inventory)
        lottieHamburger = findViewById(R.id.lottie_hamburger)
        val btnMenu = findViewById<FrameLayout>(R.id.btn_menu_hamburger)
        val navViewDrawer = findViewById<NavigationView>(R.id.nav_view_drawer_inventory)

        val corBrancaPura = ContextCompat.getColor(this, android.R.color.white)
        lottieHamburger.addValueCallback(KeyPath("**"), LottieProperty.COLOR_FILTER) {
            PorterDuffColorFilter(corBrancaPura, PorterDuff.Mode.SRC_ATOP)
        }

        btnMenu.setOnClickListener {
            if (!drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.openDrawer(GravityCompat.START)
                lottieHamburger.playAnimation()
            }
        }

        navViewDrawer.setNavigationItemSelectedListener { item ->
            drawerLayout.closeDrawer(GravityCompat.START)
            val intent = when (item.itemId) {
                R.id.nav_add_device -> {
                    if (isRestrictedProfile) {
                        AlertHelper.exibirAlertaPremium(this, "Sem permissão para adicionar dispositivos.")
                        null
                    } else {
                        Intent(this, AddDeviceActivity::class.java)
                    }
                }
                R.id.nav_borrow_device -> Intent(this, ReservationsActivity::class.java)
                R.id.nav_activity -> Intent(this, LogsActivity::class.java)
                R.id.nav_asset_history -> Intent(this, AssetHistoryActivity::class.java)
                R.id.nav_report_problem -> {
                    if (isReadOnlyProfile) {
                        AlertHelper.exibirAlertaPremium(this, "Sem permissão para reportar problemas.")
                        null
                    } else {
                        Intent(this, ReportIssueActivity::class.java)
                    }
                }
                else -> null
            }

            intent?.let {
                startActivity(it)
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            }

            true
        }

        findViewById<FrameLayout>(R.id.btn_scan_search).setOnClickListener {
            val intent = Intent(this, ScannerActivity::class.java)
            startActivityForResult(intent, SCAN_REQUEST_CODE)
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }

        rvInventario = findViewById(R.id.rv_inventario)
        rvInventario.layoutManager = LinearLayoutManager(this)
        inventoryAdapter = InventoryAdapter(fullInventoryList)
        inventoryAdapter.onItemClick = { item ->
            val assetType = item["TipoReal"]?.toString() ?: ""
            val supportedTypes = listOf("Computer", "Monitor", "NetworkEquipment", "Printer", "Peripheral", "Phone")
            
            if (supportedTypes.contains(assetType)) {
                val intent = Intent(this, AssetDetailsActivity::class.java)
                           // Extração AGRESSIVA e REFINADA de ID
                fun descobrirID(map: Map<String, Any>): Int {
                    val maxInt = 2147483647
                    
                    // 1. Suspeitos do costume (campos diretos ou Mapas dropdown)
                    val chavesPrioritarias = listOf("2", "id", "ID", "id_real", "0")
                    for (k in chavesPrioritarias) {
                        val value = map[k]
                        // Se for um mapa (comum no GLPI 10 expand_dropdowns), procura o ID lá dentro
                        if (value is Map<*, *>) {
                            val subId = value["id"]?.toString()?.toDoubleOrNull()?.toInt() ?: 0
                            if (subId > 0 && subId != maxInt) return subId
                        }
                        val v = value?.toString() ?: ""
                        if (v.isNotEmpty() && v != "null") {
                            val id = v.toDoubleOrNull()?.toInt() ?: v.toIntOrNull() ?: 0
                            if (id > 0 && id != maxInt) return id
                        }
                    }
                    
                    // 2. Varredura exaustiva (usada como fallback)
                    for ((k, value) in map) {
                        if (k.contains("id", true) || k.all { it.isDigit() }) {
                            if (value is Map<*, *>) {
                                val subId = value["id"]?.toString()?.toDoubleOrNull()?.toInt() ?: 0
                                if (subId > 0 && subId != maxInt) return subId
                            }
                            val v = value?.toString() ?: ""
                            val id = v.toDoubleOrNull()?.toInt() ?: v.toIntOrNull() ?: 0
                            if (id > 0 && id != maxInt) return id
                        }
                    }
                    return 0
                }

                val assetId = descobrirID(item)



                
                val assetName = item["1"]?.toString() ?: "Equipamento"
                val serial = (item["5"]?.toString() ?: "")
                val location = (item["3"]?.toString() ?: "")

                intent.putExtra("ASSET_ID", assetId)
                intent.putExtra("ASSET_NAME", assetName)
                intent.putExtra("ASSET_TYPE", assetType)
                intent.putExtra("ASSET_SERIAL", serial)
                intent.putExtra("ASSET_LOCATION", location)
                
                startActivity(intent)
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            }
        }

        rvInventario.adapter = inventoryAdapter

        btnAnterior = findViewById(R.id.btn_pagina_anterior)
        btnProxima = findViewById(R.id.btn_proxima_pagina)
        paginationInventory = findViewById(R.id.pagination_inventory)
        nestedScroll = findViewById(R.id.nested_scroll_main)

        val lupaAnim = findViewById<LottieAnimationView>(R.id.lupa_animation)
        val azulGlpi = ContextCompat.getColor(this, R.color.azul_glpi)
        lupaAnim.addValueCallback(KeyPath("**"), LottieProperty.COLOR_FILTER) {
            PorterDuffColorFilter(azulGlpi, PorterDuff.Mode.SRC_ATOP)
        }

        val btnFiltro = findViewById<FrameLayout>(R.id.btn_filtro_inventory)
        val lottieFiltro = findViewById<LottieAnimationView>(R.id.lottie_filtro)
        lottieFiltro.addValueCallback(KeyPath("**"), LottieProperty.COLOR_FILTER) {
            PorterDuffColorFilter(azulGlpi, PorterDuff.Mode.SRC_ATOP)
        }
        btnFiltro.setOnClickListener { abrirDialogoFiltro() }

        configurarNavegacao()
        configurarChips()
        configurarBotaoMaisExpansivel()
        configurarPesquisa()
        carregarDadosInventario()

        btnProxima.setOnClickListener { paginaAtual++; carregarDadosInventario() }
        btnAnterior.setOnClickListener { if (paginaAtual > 0) { paginaAtual--; carregarDadosInventario() } }

        playFilterAnimation() // Play on open
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SCAN_REQUEST_CODE && resultCode == RESULT_OK) {
            val resultadoScan = data?.getStringExtra("SCAN_RESULT") ?: ""
            if (resultadoScan.isNotEmpty()) {

                isScanResult = true
                itemTypeAtual = "All"

                // Apaga os botões visualmente SÓ DURANTE o tempo em que o ecrã está a voltar do scanner
                val container = findViewById<LinearLayout>(R.id.container_chips)
                for (i in 0 until (container?.childCount ?: 0)) {
                    val child = container?.getChildAt(i)
                    if (child is TextView) child.isSelected = false
                }

                findViewById<EditText>(R.id.search_bar).setText(resultadoScan)
                searchJob?.cancel()
                pesquisarEquipamento(resultadoScan)

                // Exibir o belo alerta premium azul elétrico!
                AlertHelper.exibirAlertaPremium(this, "CÓDIGO LIDO: $resultadoScan", isError = false)

            } else {
                AlertHelper.exibirAlertaPremium(this, "NENHUM CÓDIGO LIDO", isError = true)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        verificarComandoMenu()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        verificarComandoMenu()
    }

    private fun verificarComandoMenu() {
        if (intent.getBooleanExtra("ABRIR_MENU", false)) {
            drawerLayout.openDrawer(GravityCompat.START)
            lottieHamburger.playAnimation()
            intent.removeExtra("ABRIR_MENU")
        }

        // 🔥 Atualizar estado de permissão baseado no perfil em cache ou sessão
        isRestrictedProfile = PreferenceManager.isRestrictedProfile(this)
        isReadOnlyProfile = PreferenceManager.isReadOnlyProfile(this)
        
        if (isRestrictedProfile) {
            Log.d("INVENTORY_PERM", "Modo restrito ativo")
        }
    }

    private fun carregarDadosInventario() {
        val range = "${paginaAtual * itensPorPagina}-${(paginaAtual * itensPorPagina) + itensPorPagina - 1}"
        val searchText = findViewById<EditText>(R.id.search_bar).text.toString().trim()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val listaResultados = mutableListOf<Map<String, Any>>()
                // LÓGICA SMART: Se estivermos a pesquisar e viermos do modo "TODOS", procuramos em tudo.
                // Se o utilizador escolheu MANUALMENTE um filtro, respeitamos esse filtro.
                val tipos = if (manualTypeSelected == "All") {
                    listOf("Computer", "Monitor", "NetworkEquipment", "Printer")
                } else {
                    listOf(manualTypeSelected)
                }

                val criteria = mutableMapOf<String, String>()
                var criteriaIndex = 0

                // Forçar a exibição dos campos necessários (Busca Alargada)
                criteria["forcedisplay[0]"] = "1" // Nome
                criteria["forcedisplay[1]"] = "2" // ID
                criteria["forcedisplay[2]"] = "3" // Localização
                criteria["forcedisplay[3]"] = "5" // Serial
                criteria["forcedisplay[4]"] = "70" // Utilizador (Principal)
                criteria["forcedisplay[5]"] = "8" // Técnico Responsável
                criteria["forcedisplay[6]"] = "24" // Utilizador (Alternativo/Legacy)
                criteria["forcedisplay[7]"] = "14" // Número Utilizador Alternativo
                criteria["forcedisplay[8]"] = "15" // Nome Utilizador Alternativo
                criteria["forcedisplay[9]"] = "31" // Estado

                // Filtro por Departamento (Field 3)
                locationIdFiltro?.let {
                    criteria["criteria[$criteriaIndex][field]"] = "3"
                    criteria["criteria[$criteriaIndex][searchtype]"] = "equals"
                    criteria["criteria[$criteriaIndex][value]"] = it
                    criteriaIndex++
                }

                // Pesquisa Inteligente: Procura no Nome (1) OU no Número de Série (5)
                if (searchText.isNotEmpty()) {
                    criteria["criteria[$criteriaIndex][link]"] = "AND"
                    criteria["criteria[$criteriaIndex][criteria][0][field]"] = "1"
                    criteria["criteria[$criteriaIndex][criteria][0][searchtype]"] = "contains"
                    criteria["criteria[$criteriaIndex][criteria][0][value]"] = searchText
                    criteria["criteria[$criteriaIndex][criteria][1][link]"] = "OR"
                    criteria["criteria[$criteriaIndex][criteria][1][field]"] = "5"
                    criteria["criteria[$criteriaIndex][criteria][1][searchtype]"] = "contains"
                    criteria["criteria[$criteriaIndex][criteria][1][value]"] = searchText
                }

                val jobs = tipos.map { tipo ->
                    async<Pair<List<Map<String, Any>>, Int>> {
                        try {
                            val res = GlpiRetrofit.api.searchInventory(
                                itemtype = tipo,
                                sessionToken = TOKEN_SESSAO,
                                appToken = TOKEN_APP,
                                range = range,
                                criteria = criteria
                            )
                            if (res.isSuccessful) {
                                val body = res.body()
                                val tc = body?.totalcount ?: 0
                                val list = body?.data?.map { item ->
                                    val mapaEditavel = item.toMutableMap()
                                    mapaEditavel["TipoReal"] = tipo
                                    mapaEditavel
                                } ?: emptyList()
                                Pair(list, tc)
                            } else Pair(emptyList(), 0)
                        } catch (e: Exception) { Pair(emptyList(), 0) }
                    }
                }

                val results = jobs.awaitAll()
                listaResultados.addAll(results.flatMap { it.first })
                val maxTotalCount = results.maxOfOrNull { it.second } ?: 0

                withContext(Dispatchers.Main) {
                    val btnFiltro = findViewById<FrameLayout>(R.id.btn_filtro_inventory)
                    if (locationIdFiltro != null) {
                        btnFiltro.setBackgroundResource(R.drawable.bg_filtro_ativo)
                    } else {
                        btnFiltro.setBackgroundResource(R.drawable.bg_cartao_brilhante)
                    }

                    // Auto-seleção visual do chip se houver resultados e for uma pesquisa
                    if (listaResultados.isNotEmpty() && searchText.isNotEmpty()) {
                        val primeiroTipo = listaResultados[0]["TipoReal"]?.toString() ?: "All"
                        atualizarChipVisualmente(primeiroTipo)
                    }

                    fullInventoryList = listaResultados.take(itensPorPagina).toMutableList()
                    inventoryAdapter.updateList(fullInventoryList)
                    rvInventario.scheduleLayoutAnimation()
                    
                    // 🔥 Passamos o total da API para a função de botões 🔥
                    atualizarBotoesPagina(fullInventoryList.size, maxTotalCount)
                    nestedScroll.smoothScrollTo(0, 0)
                }
            } catch (e: Exception) { }
        }
    }

    private fun pesquisarEquipamento(query: String) {
        // Agora o carregarDadosInventario já trata da pesquisa combinada
        carregarDadosInventario()
    }

    private fun atualizarChipVisualmente(tipoReal: String) {
        val container = findViewById<LinearLayout>(R.id.container_chips) ?: return

        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i)
            if (child is TextView) child.isSelected = false
        }

        if ((tipoReal == "Printer" || tipoReal == "NetworkEquipment") && !isExpanded) {
            findViewById<FrameLayout>(R.id.btn_expandir_categorias)?.performClick()
        }

        when (tipoReal) {
            "Computer" -> findViewById<TextView>(R.id.chip_computadores)?.isSelected = true
            "Monitor" -> findViewById<TextView>(R.id.chip_monitores)?.isSelected = true
            "All" -> findViewById<TextView>(R.id.chip_todos)?.isSelected = true
            "Printer" -> addedChips.find { (it as? TextView)?.text == "IMPRESSORAS" }?.isSelected = true
            "NetworkEquipment" -> addedChips.find { (it as? TextView)?.text == "REDE" }?.isSelected = true
        }
        itemTypeAtual = tipoReal
    }

    private fun atualizarBotoesPagina(tamanhoPaginaAtual: Int, maxTotalCount: Int) {
        val showAnterior = paginaAtual > 0
        btnAnterior.visibility = if (showAnterior) View.VISIBLE else View.GONE
        
        // 🔥 Lógica robusta: Próximo só se ainda houver itens para além do que já mostramos e a página estiver cheia 🔥
        val itensJaMostrados = paginaAtual * itensPorPagina + tamanhoPaginaAtual
        val showProxima = (itensJaMostrados < maxTotalCount) && (tamanhoPaginaAtual == itensPorPagina)
        btnProxima.visibility = if (showProxima) View.VISIBLE else View.GONE

        paginationInventory.visibility = if (showAnterior || showProxima) View.VISIBLE else View.GONE
    }

    private fun configurarChips() {
        configurarCliqueChip(findViewById(R.id.chip_todos), "All")
        configurarCliqueChip(findViewById(R.id.chip_computadores), "Computer")
        configurarCliqueChip(findViewById(R.id.chip_monitores), "Monitor")
        findViewById<View>(R.id.chip_todos)?.isSelected = true
    }

    // 🔥 A CORREÇÃO ESTÁ AQUI 🔥
    private fun configurarCliqueChip(view: View?, tipo: String) {
        view?.setOnClickListener { v ->
            val container = findViewById<LinearLayout>(R.id.container_chips)
            // 1. Desmarca todos os outros
            for (i in 0 until (container?.childCount ?: 0)) {
                val child = container?.getChildAt(i)
                if (child is TextView) child.isSelected = false
            }

            // 2. Marca o atual IMEDIATAMENTE e não espera pela API
            v.isSelected = true

            // 3. Atualiza o estado
            itemTypeAtual = tipo
            manualTypeSelected = tipo
            paginaAtual = 0
            
            // playFilterAnimation() // Removido por pedido do utilizador: os chips não devem ter animação

            // 4. Limpa a pesquisa e recarrega os dados
            val searchBar = findViewById<EditText>(R.id.search_bar)
            if (searchBar.text.isNotEmpty()) {
                searchBar.setText("") // Isto dispara o TextWatcher, que vai chamar carregarDadosInventario()
            } else {
                carregarDadosInventario()
            }
        }
    }

    private fun configurarPesquisa() {
        findViewById<EditText>(R.id.search_bar).addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val texto = s.toString().trim()
                paginaAtual = 0 // Reset pagination page when search changes
                if (texto.isEmpty()) {
                    // Se o utilizador estava em "TODOS" antes de pesquisar, volta ao "TODOS" visualmente
                    if (manualTypeSelected == "All") {
                        atualizarChipVisualmente("All")
                    }
                    carregarDadosInventario()
                } else {
                    if (isScanResult) {
                        isScanResult = false
                    } else {
                        searchJob?.cancel()
                        searchJob = CoroutineScope(Dispatchers.Main).launch {
                            delay(600)
                            pesquisarEquipamento(texto)
                        }
                    }
                }
            }
            override fun beforeTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })
    }

    private fun configurarBotaoMaisExpansivel() {
        val btnMais = findViewById<FrameLayout>(R.id.btn_expandir_categorias)
        val symbolSwitcher = findViewById<ViewSwitcher>(R.id.symbol_switcher)

        btnMais?.setOnClickListener {
            val containerChips = findViewById<LinearLayout>(R.id.container_chips)
            if (!isExpanded) {
                val extras = mapOf(
                    "IMPRESSORAS" to "Printer",
                    "REDE" to "NetworkEquipment"
                )

                extras.forEach { (nome, tipo) ->
                    val novoChip = criarChipObjeto(nome)
                    configurarCliqueChip(novoChip, tipo)
                    val index = containerChips.indexOfChild(btnMais)
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

    private fun criarChipObjeto(nome: String): TextView {
        return TextView(this).apply {
            text = nome; textSize = 10f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.selector_chip_filtros)
            setTextColor(ContextCompat.getColorStateList(context, R.color.selector_texto_chip))
            val d = resources.displayMetrics.density
            layoutParams = LinearLayout.LayoutParams((110 * d).toInt(), (32 * d).toInt()).apply {
                setMargins(0, 0, (8 * d).toInt(), 0)
            }
        }
    }

    private fun abrirDialogoFiltro() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val locations = GlpiRetrofit.api.getLocationsList(TOKEN_SESSAO, TOKEN_APP, "0-300")
                
                withContext(Dispatchers.Main) {
                    val dialogView = layoutInflater.inflate(R.layout.dialog_custom_filter_state, null)
                    val dialog = androidx.appcompat.app.AlertDialog.Builder(this@InventoryActivity)
                        .setView(dialogView)
                        .create()
                    dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
                    
                    dialogView.findViewById<TextView>(R.id.tv_filter_title).text = "SELECIONE O DEPARTAMENTO"
                    val container = dialogView.findViewById<LinearLayout>(R.id.container_filter_options)
                    val btnClose = dialogView.findViewById<View>(R.id.btn_close_filter)
                    btnClose.setOnClickListener { dialog.dismiss() }

                    val nomesFiltro = mutableListOf<Map<String, String>>()
                    locations.forEach { loc ->
                        val id = loc["id"]?.toString() ?: "0"
                        val name = (loc["name"]?.toString() ?: "").trim()
                        if (name.isNotEmpty()) {
                            nomesFiltro.add(mapOf("id" to id, "name" to name))
                        }
                    }

                    val nomesFiltroUnicos = nomesFiltro.distinctBy { it["name"]?.lowercase() }

                    // Adicionar "TODOS" no topo
                    adicionarItemFiltro(dialog, container, mapOf("id" to "null", "name" to "TODOS (REDEFINIR)"), false)

                    val itensNaoMapeados = nomesFiltroUnicos.toMutableList()
                    val estruturaFinal = mutableListOf<Pair<Map<String, String>, Boolean>>()

                    categoriasNomes.forEach { catNome ->
                        val pai = itensNaoMapeados.find { it["name"].equals(catNome, ignoreCase = true) }
                        if (pai != null) {
                            estruturaFinal.add(Pair(pai, false))
                            itensNaoMapeados.remove(pai)
                        }

                        val filhosNomes = categoriasFilhos[catNome] ?: emptyList()
                        val filhosEncontrados = itensNaoMapeados.filter { item ->
                            filhosNomes.any { it.equals(item["name"], ignoreCase = true) }
                        }
                        
                        filhosEncontrados.forEach { filho ->
                            estruturaFinal.add(Pair(filho, true))
                            itensNaoMapeados.remove(filho)
                        }
                    }
                    
                    // Adicionar o resto
                    itensNaoMapeados.forEach {
                        estruturaFinal.add(Pair(it, false))
                    }

                    estruturaFinal.forEach { (locData, isChild) ->
                        adicionarItemFiltro(dialog, container, locData, isChild)
                    }

                    dialog.show()
                    dialog.window?.setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@InventoryActivity, "Erro ao carregar locais", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun adicionarItemFiltro(dialog: androidx.appcompat.app.AlertDialog, container: LinearLayout, locData: Map<String, String>, isChild: Boolean) {
        val d = resources.displayMetrics.density
        val nome = locData["name"] ?: ""
        val isHeader = !isChild && this@InventoryActivity.categoriasNomes.any { it.equals(nome, ignoreCase = true) }
        val isSelected = locationIdFiltro == locData["id"] || (locationIdFiltro == null && locData["id"] == "null")

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                if (isHeader) LinearLayout.LayoutParams.WRAP_CONTENT else (48 * d).toInt()
            )
            gravity = android.view.Gravity.CENTER_VERTICAL
            
            // Indentação: 16dp para todos, +16dp extra para filhos (total 32dp conforme padrão de listas)
            val padLeft = if (isChild) (32 * d).toInt() else (16 * d).toInt()
            setPadding(padLeft, (8 * d).toInt(), (16 * d).toInt(), (8 * d).toInt())
            
            val outValue = android.util.TypedValue()
            context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
            setBackgroundResource(outValue.resourceId)
            
            setOnClickListener {
                val novoId = if (locData["id"] == "null") null else locData["id"]
                
                if (locationIdFiltro != novoId) {
                    locationIdFiltro = novoId
                    locationNomeFiltro = if (novoId == null) null else locData["name"]
                    playFilterAnimation() // Tocar apenas na mudança real
                }
                
                paginaAtual = 0
                carregarDadosInventario()
                dialog.dismiss()
            }
        }

        val tv = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            text = nome.uppercase()
            
            if (isHeader) {
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(ContextCompat.getColor(context, R.color.texto_principal))
                textSize = 14f
                isAllCaps = true
                typeface = androidx.core.content.res.ResourcesCompat.getFont(context, R.font.amiko_bold)
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            } else {
                setTextColor(ContextCompat.getColor(context, if (isChild) R.color.texto_secundario else R.color.texto_principal))
                textSize = 14f
                isAllCaps = true
                typeface = androidx.core.content.res.ResourcesCompat.getFont(context, R.font.amiko_bold)
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }
            gravity = android.view.Gravity.START or android.view.Gravity.CENTER_VERTICAL
        }

        row.addView(tv)

        if (!isHeader) {
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
            row.addView(rb)
        }

        container.addView(row)
    }

    private fun configurarNavegacao() {
        val navView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        navView.selectedItemId = R.id.nav_inventario
        navView.labelVisibilityMode = BottomNavigationView.LABEL_VISIBILITY_UNLABELED
        
        val corBrancaPura = ContextCompat.getColor(this, android.R.color.white)
        val corBrancaTransluscida = Color.parseColor("#99FFFFFF")
        val tintList = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf(-android.R.attr.state_checked)),
            intArrayOf(corBrancaPura, corBrancaTransluscida)
        )
        navView.itemIconTintList = tintList
        navView.itemTextColor = tintList
        ViewCompat.setOnApplyWindowInsetsListener(navView) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val layoutParams = view.layoutParams as ViewGroup.MarginLayoutParams
            layoutParams.bottomMargin = systemBars.bottom + 16
            view.layoutParams = layoutParams
            insets
        }
        navView.setOnItemSelectedListener { item ->
            if (item.itemId == R.id.nav_inventario) return@setOnItemSelectedListener true
            val intent = when (item.itemId) {
                R.id.nav_tickets -> Intent(this, DashboardActivity::class.java)
                R.id.nav_agenda -> Intent(this, AgendaActivity::class.java)
                R.id.nav_perfil -> Intent(this, ProfileDetailActivity::class.java)
                else -> null
            }
            intent?.let { startActivity(it); overridePendingTransition(R.anim.fade_in, R.anim.fade_out) }
            false
        }
    }

    override fun dispatchTouchEvent(ev: android.view.MotionEvent?): Boolean {
        KeyboardHelper.handleTouchOutside(this, ev)
        return super.dispatchTouchEvent(ev)
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) drawerLayout.closeDrawer(GravityCompat.START)
        else super.onBackPressed()
    }
}