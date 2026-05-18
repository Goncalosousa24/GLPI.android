package com.example.glpimobile

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
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
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.util.Log
import kotlinx.coroutines.*

import java.util.concurrent.ConcurrentHashMap

class AssetDetailsActivity : AppCompatActivity() {

    private val deviceCache = ConcurrentHashMap<String, String>()

    private lateinit var tvNameHeader: TextView
    private lateinit var tvTypeDetail: TextView
    private lateinit var tvIdDetail: TextView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var progressLoading: LottieAnimationView
    private lateinit var rvPorts: RecyclerView
    private lateinit var llVazio: LinearLayout
    private lateinit var tvVazio: TextView
    private lateinit var lottieVazio: LottieAnimationView

    private var assetId: Int = 0
    private var assetName: String = ""
    private var assetType: String = ""
    private var assetSerial: String = ""

    private val TOKEN_SESSAO = GlpiConfig.SESSION_TOKEN
    private val TOKEN_APP = GlpiConfig.APP_TOKEN

    private var paginaAtual: Int = 0
    private val tamanhoPagina: Int = 10
    private lateinit var layoutPaginacao: View
    private lateinit var btnPrev: View
    private lateinit var btnNext: View

    private var currentViewMode = "TICKETS" // "PORTS" ou "TICKETS"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_asset_details)

        // Recuperar dados do Intent
        assetId = intent.getIntExtra("ASSET_ID", 0)
        assetName = intent.getStringExtra("ASSET_NAME") ?: "Equipamento"
        assetType = intent.getStringExtra("ASSET_TYPE") ?: "Computer"
        assetSerial = intent.getStringExtra("ASSET_SERIAL") ?: ""

        initViews()
        setupListeners()
        
        // Verificação Obrigatória por Serial:
        // O ID recebido da pesquisa pode ser errado (ex: o número do campo em vez do valor).
        // Se tivermos o Serial, usamo-lo SEMPRE para confirmar/corrigir o ID real no servidor.
        if (assetSerial.isNotEmpty() && assetSerial != "---") {
            recuperarIdRemotamente()
        } else if (assetId == 0 && assetName.isNotEmpty()) {
            recuperarIdRemotamente()
        } else {
            carregarDados()
        }
    }


    private fun recuperarIdRemotamente() {
        progressLoading.visibility = View.VISIBLE
        progressLoading.playAnimation()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // === TENTATIVA 1: Busca direta pelo Serial (a mais fiável) ===
                if (assetSerial.isNotEmpty() && assetSerial != "---") {
                    val criteria = mapOf(
                        "criteria[0][field]" to "5",
                        "criteria[0][searchtype]" to "equals",
                        "criteria[0][value]" to assetSerial
                    )
                    val res = GlpiRetrofit.api.searchInventory(
                        itemtype = assetType,
                        sessionToken = TOKEN_SESSAO,
                        appToken = TOKEN_APP,
                        range = "0-1",
                        criteria = criteria
                    )
                    if (res.isSuccessful) {
                        val data = res.body()?.data ?: emptyList()
                        if (data.isNotEmpty()) {
                            val item = data[0]
                            // Verificar campo "2" diretamente (ID na pesquisa sem critérios aninhados)
                            val rawId = item["2"]?.toString()?.toDoubleOrNull()?.toInt() ?: 0
                            if (rawId > 0 && rawId != Int.MAX_VALUE) {
                                assetId = rawId
                            }
                        }
                    }
                }

                // === TENTATIVA 2: Fallback - Busca pelo Nome (se Serial falhou) ===
                if (assetId <= 0 || assetId == Int.MAX_VALUE) {
                    val criteria = mapOf(
                        "criteria[0][field]" to "1",
                        "criteria[0][searchtype]" to "equals",
                        "criteria[0][value]" to assetName
                    )
                    val res = GlpiRetrofit.api.searchInventory(
                        itemtype = assetType,
                        sessionToken = TOKEN_SESSAO,
                        appToken = TOKEN_APP,
                        range = "0-1",
                        criteria = criteria
                    )
                    if (res.isSuccessful) {
                        val data = res.body()?.data ?: emptyList()
                        if (data.isNotEmpty()) {
                            val item = data[0]
                            val rawId = item["2"]?.toString()?.toDoubleOrNull()?.toInt() ?: 0
                            if (rawId > 0 && rawId != Int.MAX_VALUE) {
                                assetId = rawId
                            }
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    progressLoading.visibility = View.GONE
                    tvIdDetail.text = "ID GLPI: #$assetId"
                    carregarDados()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressLoading.visibility = View.GONE
                    carregarDados()
                }
            }
        }
    }


    private fun initViews() {
        tvNameHeader = findViewById(R.id.tv_asset_name_header)
        tvTypeDetail = findViewById(R.id.tv_asset_type_detail)
        tvIdDetail = findViewById(R.id.tv_asset_id_detail)
        

        rvPorts = findViewById(R.id.rv_asset_ports)
        rvPorts.layoutManager = LinearLayoutManager(this)

        val rvHistory = findViewById<RecyclerView>(R.id.rv_asset_history)
        rvHistory?.layoutManager = LinearLayoutManager(this)

        swipeRefresh = findViewById(R.id.swipe_details)
        progressLoading = findViewById(R.id.progress_details)
        llVazio = findViewById(R.id.ll_vazio_details)
        tvVazio = findViewById(R.id.tv_vazio_details)
        lottieVazio = findViewById(R.id.lottie_vazio_details)

        // Paginação
        layoutPaginacao = findViewById(R.id.layout_paginacao_detalhes)
        btnPrev = findViewById(R.id.btn_prev_detalhes)
        btnNext = findViewById(R.id.btn_next_detalhes)

        tvNameHeader.text = assetName.uppercase()
        tvTypeDetail.text = "TIPO: ${assetPtType().uppercase()}"
        tvIdDetail.text = "ID GLPI: #$assetId"

        if (assetType == "NetworkEquipment") {
            currentViewMode = "PORTS"
            rvPorts.visibility = View.VISIBLE
            rvHistory?.visibility = View.GONE
        } else {
            currentViewMode = "TICKETS"
            rvPorts.visibility = View.GONE
            rvHistory?.visibility = View.VISIBLE
        }
        
        val colorAzul = ContextCompat.getColor(this, R.color.azul_glpi)
        val colorBranco = ContextCompat.getColor(this, android.R.color.white)
        
        aplicarCorLottie(findViewById(R.id.lottie_seta_details), colorBranco)
        aplicarCorLottie(progressLoading, colorAzul)
        aplicarCorLottie(findViewById(R.id.seta_prev_detalhes), colorAzul)
        aplicarCorLottie(findViewById(R.id.seta_next_detalhes), colorAzul)
        swipeRefresh.setColorSchemeColors(colorAzul)
    }

    private fun aplicarCorLottie(lottie: LottieAnimationView?, cor: Int) {
        lottie?.addValueCallback(KeyPath("**"), LottieProperty.COLOR_FILTER) {
            PorterDuffColorFilter(cor, PorterDuff.Mode.SRC_ATOP)
        }
    }

    private fun setupListeners() {
        findViewById<View>(R.id.btn_voltar_details).setOnClickListener { finish() }
        swipeRefresh.setOnRefreshListener { 
            paginaAtual = 0
            carregarDados() 
        }

        btnPrev.setOnClickListener {
            if (paginaAtual > 0) {
                paginaAtual--
                carregarDados()
                findViewById<androidx.core.widget.NestedScrollView>(R.id.nested_scroll_details)?.scrollTo(0, 0)
            }
        }

        btnNext.setOnClickListener {
            paginaAtual++
            carregarDados()
            findViewById<androidx.core.widget.NestedScrollView>(R.id.nested_scroll_details)?.scrollTo(0, 0)
        }
    }

    private fun assetPtType(): String {
        return when (assetType) {
            "NetworkEquipment" -> "Equipamento de Rede"
            "Computer" -> "Computador"
            "Monitor" -> "Monitor"
            "Printer" -> "Impressora"
            "Peripheral" -> "Periférico"
            else -> assetType
        }
    }

    private fun carregarDados() {
        if (currentViewMode == "PORTS") {
            carregarPortasRede()
        } else {
            carregarTicketsAssociados()
        }
    }

    private fun carregarTicketsAssociados() {
        rvPorts.visibility = View.GONE
        val rvHistory = findViewById<RecyclerView>(R.id.rv_asset_history)
        rvHistory?.visibility = View.VISIBLE

        progressLoading.visibility = View.VISIBLE
        llVazio.visibility = View.GONE
        layoutPaginacao.visibility = View.GONE
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Dual Fetch: Relações oficiais E Pesquisa Direta por campos items_id/itemtype
                // Isto garante que apanhamos tickets ligados como "Elemento Associado" (Item_Ticket) 
                // E tickets onde o equipamento é o item principal (armazenado diretamente no Ticket)
                
                val taskRelations = async {
                    try {
                        val relRes = GlpiRetrofit.api.getItemTicketsRelation(assetType, assetId, TOKEN_SESSAO, TOKEN_APP)
                        val relStr = relRes.body()?.string() ?: ""
                        val relations = org.json.JSONArray(if (relStr.startsWith("[")) relStr else "[]")
                        val ids = mutableListOf<String>()
                        for (i in 0 until relations.length()) {
                            val obj = relations.optJSONObject(i)
                            if (obj != null && obj.has("tickets_id")) {
                                ids.add(obj.optString("tickets_id"))
                            }
                        }
                        ids
                    } catch (e: Exception) { emptyList<String>() }
                }

                val taskDirectSearch = async {
                    try {
                        val searchRes = GlpiRetrofit.api.getTicketsByItemSearch(TOKEN_SESSAO, TOKEN_APP, valueId = assetId, valueType = assetType)
                        if (searchRes.isSuccessful) searchRes.body()?.data ?: emptyList()
                        else emptyList()
                    } catch (e: Exception) { emptyList<Map<String, Any>>() }
                }

                val ticketIdsFromRel = taskRelations.await()
                val ticketsFromSearch = taskDirectSearch.await()

                val finalTicketsList = mutableListOf<Map<String, Any>>()
                val processedIds = mutableSetOf<String>()

                // Adicionar os resultados da pesquisa direta primeiro (costumam ser mais completos)
                ticketsFromSearch.forEach { ticket ->
                    val id = ticket["2"]?.toString() ?: ticket["id"]?.toString() ?: ""
                    val idLimpo = if (id.contains(".")) id.substringBefore(".") else id
                    if (idLimpo.isNotEmpty() && !processedIds.contains(idLimpo)) {
                        finalTicketsList.add(ticket)
                        processedIds.add(idLimpo)
                    }
                }

                // Verificar se há IDs obtidos via relação que ainda não foram carregados
                val remainingIds = ticketIdsFromRel.filter { !processedIds.contains(it) }
                if (remainingIds.isNotEmpty()) {
                    val criteria = mutableMapOf<String, String>()
                    remainingIds.forEachIndexed { index, tId ->
                        if (index == 0) {
                            criteria["criteria[0][field]"] = "2"
                            criteria["criteria[0][searchtype]"] = "equals"
                            criteria["criteria[0][value]"] = tId
                        } else {
                            criteria["criteria[$index][link]"] = "OR"
                            criteria["criteria[$index][field]"] = "2"
                            criteria["criteria[$index][searchtype]"] = "equals"
                            criteria["criteria[$index][value]"] = tId
                        }
                    }
                    criteria["range"] = "0-100" 
                    
                    val ticRes = GlpiRetrofit.api.getTicketsByIds(TOKEN_SESSAO, TOKEN_APP, criteria)
                    val ticStr = ticRes.body()?.string() ?: ""
                    val ticObj = org.json.JSONObject(if (ticStr.startsWith("{")) ticStr else "{}")
                    val dataArr = ticObj.optJSONArray("data") ?: org.json.JSONArray()
                    
                    val gson = com.google.gson.Gson()
                    for (i in 0 until dataArr.length()) {
                        try {
                            val ticketMap = gson.fromJson(dataArr.getJSONObject(i).toString(), Map::class.java) as Map<String, Any>
                            val id = ticketMap["2"]?.toString() ?: ticketMap["id"]?.toString() ?: ""
                            val idLimpo = if (id.contains(".")) id.substringBefore(".") else id
                            if (idLimpo.isNotEmpty() && !processedIds.contains(idLimpo)) {
                                finalTicketsList.add(ticketMap)
                                processedIds.add(idLimpo)
                            }
                        } catch(e: Exception){}
                    }
                }
                
                withContext(Dispatchers.Main) {
                    progressLoading.visibility = View.GONE
                    swipeRefresh.isRefreshing = false
                    
                    if (finalTicketsList.isEmpty()) {
                        llVazio.visibility = View.VISIBLE
                        findViewById<TextView>(R.id.tv_vazio_details)?.text = "Nenhum ticket associado."
                        rvHistory?.adapter = null
                    } else {
                        llVazio.visibility = View.GONE
                        // Ordenar por ID descendente (mais recentes primeiro)
                        val sortedList = finalTicketsList.sortedByDescending { 
                            val id = it["2"]?.toString() ?: it["id"]?.toString() ?: "0"
                            id.toDoubleOrNull()?.toInt() ?: 0
                        }
                        rvHistory?.adapter = TicketHistoryAdapter(sortedList, hideStatusTag = true, disableExpansion = true)
                        rvHistory?.scheduleLayoutAnimation()
                    }
                }
            } catch(e: Exception) {
                withContext(Dispatchers.Main) {
                    progressLoading.visibility = View.GONE
                    llVazio.visibility = View.VISIBLE
                    findViewById<TextView>(R.id.tv_vazio_details)?.text = "Erro ao carregar histórico."
                    swipeRefresh.isRefreshing = false
                }
            }
        }
    }


    private fun carregarPortasRede() {
        progressLoading.visibility = View.VISIBLE
        llVazio.visibility = View.GONE
        layoutPaginacao.visibility = View.GONE
        
        val inicio = paginaAtual * tamanhoPagina
        val fim = inicio + tamanhoPagina - 1
        val rangeStr = "$inicio-$fim"

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Usar o range dinâmico
                val searchResponse = GlpiRetrofit.api.getNetworkPortsSearch(
                    sessionToken = TOKEN_SESSAO,
                    appToken = TOKEN_APP,
                    range = rangeStr,
                    value20 = assetType,
                    value21 = assetId.toString()
                )
                val ports = if (searchResponse.isSuccessful) searchResponse.body()?.data ?: emptyList() else emptyList()

                // Mini-scrapper Assíncrono para reconstruir a coluna "Ligado A" e buscar o MAC verdadeiro
                val enrichedPorts = ports.map { port ->
                    async<Map<String, Any>> {
                        val mutablePort = port.toMutableMap()
                        try {
                            val baseId = port["2"]?.toString()?.replace(".0", "")
                            if (!baseId.isNullOrEmpty() && baseId != "null") {
                                // 1. Buscar a connection na Pivot
                                var connResponse = GlpiRetrofit.api.getPortConnection(
                                    sessionToken = TOKEN_SESSAO,
                                    appToken = TOKEN_APP,
                                    searchField = 3,
                                    portId = baseId
                                )
                                
                                if (!connResponse.isSuccessful || connResponse.body()?.data.isNullOrEmpty()) {
                                    connResponse = GlpiRetrofit.api.getPortConnection(
                                        sessionToken = TOKEN_SESSAO,
                                        appToken = TOKEN_APP,
                                        searchField = 4,
                                        portId = baseId
                                    )
                                }
                                
                                // O connResponse tem dados da pivot. Descobrir qual o id que não é o nosso baseId
                                var remoteId: String? = null
                                connResponse.body()?.data?.firstOrNull()?.let { pivotData ->
                                    val id1 = pivotData["3"]?.toString()?.replace(".0", "")
                                    val id2 = pivotData["4"]?.toString()?.replace(".0", "")
                                    if (id1 == baseId && id2 != null) remoteId = id2
                                    else if (id2 == baseId && id1 != null) remoteId = id1
                                }
                                
                                if (!remoteId.isNullOrEmpty() && remoteId != "null") {
                                    // 2. Buscar dados da máquina remota
                                    val remoteResponse = GlpiRetrofit.api.getRemotePortDetails(
                                        sessionToken = TOKEN_SESSAO,
                                        appToken = TOKEN_APP,
                                        remotePortId = remoteId!!
                                    )
                                    val remoteData = if (remoteResponse.isSuccessful) remoteResponse.body()?.data?.firstOrNull() else null
                                    if (remoteData != null) {
                                         val rType = remoteData["20"]?.toString()?.replace("[]", "")?.replace("null", "") ?: ""
                                         
                                         if (rType.isNotEmpty()) {
                                             mutablePort["itemtype_injected"] = rType
                                             
                                             // 3. Buscar o Nome Amigável (Ex: AT01)
                                             val itemIdRaw = remoteData["21"]?.toString()?.replace(".0", "") ?: ""
                                             if (itemIdRaw.isNotEmpty() && itemIdRaw != "null") {
                                                 val cacheKey = "$rType:$itemIdRaw"
                                                 var friendlyName = deviceCache[cacheKey]
                                                 
                                                 if (friendlyName == null) {
                                                     try {
                                                         android.util.Log.d("GLPI_DEBUG", "Buscando $rType ID $itemIdRaw de forma direta")
                                                         val itemResponse = GlpiRetrofit.api.getDeviceById(
                                                             itemtype = rType,
                                                             id = itemIdRaw,
                                                             sessionToken = TOKEN_SESSAO,
                                                             appToken = TOKEN_APP
                                                         )
                                                         friendlyName = itemResponse["name"]?.toString()
                                                         android.util.Log.d("GLPI_DEBUG", "Encontrado nome: $friendlyName")
                                                         
                                                         if (friendlyName.isNullOrEmpty() || friendlyName == "null") {
                                                             friendlyName = itemIdRaw
                                                         }
                                                         deviceCache[cacheKey] = friendlyName
                                                     } catch (e: Exception) {
                                                         android.util.Log.e("GLPI_DEBUG", "Erro ao recuperar $rType $itemIdRaw", e)
                                                         friendlyName = itemIdRaw
                                                     }
                                                 }
                                                 
                                                 if (!friendlyName.isNullOrEmpty()) {
                                                     mutablePort["39_injected"] = friendlyName
                                                 }
                                             }
                                         }

                                         fun extractValue(obj: Any?): String {
                                             if (obj == null) return ""
                                             if (obj is Map<*, *>) return obj["name"]?.toString() ?: obj["1"]?.toString() ?: obj.toString()
                                             if (obj is List<*>) {
                                                 if (obj.isNotEmpty()) {
                                                     val first = obj[0]
                                                     if (first is Map<*, *>) return first["name"]?.toString() ?: first.toString()
                                                     return first.toString()
                                                 }
                                                 return ""
                                             }
                                             return obj.toString()
                                         }

                                         // O MAC do dispositivo remoto está no campo 4 da NetworkPort remota em GLPI 10+
                                         android.util.Log.d("GLPI_DEBUG", "Scraper remoto OK. Port $baseId -> RemoteID: $remoteId. remoteData.keys: ${remoteData.keys}, remoteData: $remoteData")
                                         
                                         val rMac = extractValue(remoteData["4"]).replace("[]", "").replace("null", "").trim()
                                         val finalMac = if (rMac.length > 5 && rMac.contains(":")) rMac else ""
                                         
                                         if (finalMac.isNotEmpty() && finalMac != "0" && finalMac != "0.0") {
                                             mutablePort["6_injected"] = finalMac
                                         }
                                         
                                         val rIp = extractValue(remoteData["126"]).replace("[]", "").replace("null", "").trim()
                                         if (rIp.isNotEmpty() && rIp != "0" && rIp != "0.0") {
                                             mutablePort["126_injected"] = rIp
                                         }
                                     }
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("GLPI_DEBUG", "Erro ao resolver ligação da porta", e)
                        }
                        mutablePort
                    }
                }.awaitAll()
                withContext(Dispatchers.Main) {
                    swipeRefresh.isRefreshing = false
                    progressLoading.visibility = View.GONE
                    
                    android.util.Log.d("GLPI_DEBUG", "Portas recebidas para ID $assetId: ${ports.size}")
                    
                    if (ports.isEmpty()) {
                        llVazio.visibility = View.VISIBLE
                        tvVazio.text = if (paginaAtual == 0) "Nenhuma porta encontrada" else "Fim da lista"
                        rvPorts.adapter = null
                        layoutPaginacao.visibility = if (paginaAtual > 0) View.VISIBLE else View.GONE
                    } else {
                        llVazio.visibility = View.GONE
                        val sortedPorts = enrichedPorts.sortedBy { port ->
                            val numStr = port["3"]?.toString()?.replace(".0", "") 
                                ?: port["logical_number"]?.toString()?.replace(".0", "") 
                                ?: ""
                            numStr.toIntOrNull() ?: 999
                        }
                        rvPorts.adapter = NetworkPortAdapter(sortedPorts, assetName, assetSerial)
                        rvPorts.scheduleLayoutAnimation()
                        
                        // Atualizar controles de paginação
                        layoutPaginacao.visibility = View.VISIBLE
                        btnPrev.visibility = if (paginaAtual == 0) View.GONE else View.VISIBLE
                        btnNext.visibility = if (ports.size < tamanhoPagina) View.GONE else View.VISIBLE
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    swipeRefresh.isRefreshing = false
                    progressLoading.visibility = View.GONE
                    android.util.Log.e("GLPI_DEBUG", "Erro ao carregar portas", e)
                    val errorMsg = e.message ?: "Erro desconhecido"
                    Toast.makeText(this@AssetDetailsActivity, "Erro: $errorMsg", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
