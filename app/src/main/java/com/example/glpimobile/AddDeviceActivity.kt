package com.example.glpimobile
 
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
 
class AddDeviceActivity : AppCompatActivity() {
 
    private lateinit var autoTipo: AutoCompleteTextView
    private lateinit var autoLocalizacao: AutoCompleteTextView
    private lateinit var autoTecnico: AutoCompleteTextView
    private lateinit var autoEstado: AutoCompleteTextView
    private lateinit var etNome: EditText
    private lateinit var etSN: EditText
    private lateinit var tilTipo: TextInputLayout
    private lateinit var tilNome: TextInputLayout
    private lateinit var tilSN: TextInputLayout
    private lateinit var btnSalvar: Button
    private lateinit var nestedScrollView: NestedScrollView
    private lateinit var loadingOverlay: FrameLayout
 
    private var locationMap = mutableMapOf<String, Int>()
    private var technicianMap = mutableMapOf<String, Int>()
    private var stateMap = mutableMapOf<String, Int>()
 
 
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
        setContentView(R.layout.activity_add_device)
 
        // 1. Inicializar Views
        nestedScrollView = findViewById(R.id.nested_scroll_add)
        loadingOverlay = findViewById(R.id.loading_overlay_add)
        autoTipo = findViewById(R.id.auto_tipo_add)
        autoLocalizacao = findViewById(R.id.auto_localizacao_add)
        autoTecnico = findViewById(R.id.auto_tecnico_add)
        autoEstado = findViewById(R.id.auto_estado_add)
        etNome = findViewById(R.id.et_nome_add)
        etSN = findViewById(R.id.et_sn_add)
        tilTipo = findViewById(R.id.til_tipo_add)
        tilNome = findViewById(R.id.til_nome_add)
        tilSN = findViewById(R.id.til_sn_add)
        btnSalvar = findViewById(R.id.btn_save_device)
 
        // 2. Configurar Cores Lottie
        val lottieSeta = findViewById<LottieAnimationView>(R.id.lottie_seta_add)
        aplicarCorLottie(lottieSeta, ContextCompat.getColor(this, android.R.color.white))
 
        // 3. Configurar os Dropdowns
        configurarDropdowns()
        configurarTodosAutoScrolls()
 
        // 4. Botão Voltar
        findViewById<FrameLayout>(R.id.btn_voltar_add).setOnClickListener {
            finish()
        }
 
        // 5. Botão Salvar
        btnSalvar.setOnClickListener {
            validarESubmeter()
        }
    }
 
    // Limpa o foco e esconde o teclado ao clicar fora de um campo de texto
    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev?.action == MotionEvent.ACTION_DOWN) {
            val v = currentFocus
            if (v is EditText || v is AutoCompleteTextView) {
                val outRect = Rect()
                v.getGlobalVisibleRect(outRect)
                if (!outRect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                    v.clearFocus()
                    val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.windowToken, 0)
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }
 
    private fun configurarTodosAutoScrolls() {
        // Garantir que ao clicar em qualquer dropdown, o scroll sobe para dar espaço em baixo
        configurarAutoScroll(autoTipo)
        configurarAutoScroll(autoLocalizacao)
        configurarAutoScroll(autoTecnico)
        configurarAutoScroll(autoEstado)
    }
 
    private fun configurarAutoScroll(view: AutoCompleteTextView) {
        // Quando ganha foco (pelo teclado ou clique)
        view.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                focarEScrollar(view)
            }
        }
        // Quando é clicado diretamente
        view.setOnClickListener {
            focarEScrollar(view)
            // Delay for showing dropdown AFTER the scroll starts
            Handler(Looper.getMainLooper()).postDelayed({
                view.showDropDown()
            }, 500)
        }
    }
 
    private fun focarEScrollar(view: View) {
        // Encontrar a TextInputLayout (pai da AutoCompleteTextView)
        var layoutPai = view.parent
        while (layoutPai != null && layoutPai !is com.google.android.material.textfield.TextInputLayout) {
            layoutPai = layoutPai.parent
        }
        
        val inputLayout = layoutPai as? View ?: view
        val parentLayout = inputLayout.parent as? ViewGroup
        
        // No nosso layout, o título é o TextView imediatamente anterior ao TextInputLayout no LinearLayout
        var scrollTargetY = inputLayout.top
        if (parentLayout != null) {
            val index = parentLayout.indexOfChild(inputLayout)
            if (index > 0) {
                val viewAnterior = parentLayout.getChildAt(index - 1)
                if (viewAnterior is TextView) {
                    scrollTargetY = viewAnterior.top
                }
            }
        }
 
        // Forçar o scroll com um pequeno delay para garantir que o layout está pronto
        nestedScrollView.post {
            nestedScrollView.smoothScrollTo(0, scrollTargetY - 20)
        }
    }
 
    private fun configurarDropdowns() {
        // Dropdown de Tipo de Dispositivo (Inteligente)
        val tipos = arrayOf("Computador", "Monitor", "Dispositivo de Rede", "Impressora")
        val adapterTipo = ArrayAdapter(this, android.R.layout.simple_list_item_1, tipos)
        autoTipo.setAdapter(adapterTipo)
 
        // Carregar localizações dinamicamente da API com hierarquia
        carregarLocalizacoes()
 
        // Carregar técnicos e perfis especiais
        carregarTecnicos()
 
        // Carregar estados da API
        carregarEstados()

        // Limpar espaços de hierarquia ao selecionar localização
        autoLocalizacao.setOnItemClickListener { parent, _, position, _ ->
            val selecionado = parent.getItemAtPosition(position).toString()
            autoLocalizacao.setText(selecionado.trim(), false)
        }
    }
 
    private fun carregarLocalizacoes() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val locations = GlpiRetrofit.api.getLocationsList(GlpiConfig.SESSION_TOKEN, GlpiConfig.APP_TOKEN, "0-300")
                
                val nomesFiltro = mutableListOf<Map<String, String>>()
                locationMap.clear()

                locations.forEach { loc ->
                    val name = (loc["name"]?.toString() ?: "").trim()
                    val idRaw = loc["id"]?.toString() ?: ""
                    val id = idRaw.toIntOrNull() ?: idRaw.toDoubleOrNull()?.toInt() ?: 0
                    
                    if (name.isNotEmpty()) {
                        nomesFiltro.add(mapOf("name" to name))
                        locationMap[name] = id
                    }
                }
 
                val nomesFiltroUnicos = nomesFiltro.distinctBy { it["name"]?.lowercase() }
                val itensNaoMapeados = nomesFiltroUnicos.toMutableList()
                val nomesFinal = mutableListOf<String>()
 
                // Organizar Hierarquia (Lógica idêntica ao InventoryActivity)
                categoriasNomes.forEach { catNome ->
                    val pai = itensNaoMapeados.find { it["name"].equals(catNome, ignoreCase = true) }
                    if (pai != null) {
                        nomesFinal.add(pai["name"] ?: "")
                        itensNaoMapeados.remove(pai)
                    }
 
                    val filhosNomes = categoriasFilhos[catNome] ?: emptyList()
                    val filhosEncontrados = itensNaoMapeados.filter { item ->
                        filhosNomes.any { it.equals(item["name"], ignoreCase = true) }
                    }
                    
                    filhosEncontrados.sortedBy { it["name"] }.forEach { filho ->
                        nomesFinal.add("      ${filho["name"]}")
                        itensNaoMapeados.remove(filho)
                    }
                }
                
                // Adicionar o resto por ordem alfabética
                nomesFinal.addAll(itensNaoMapeados.mapNotNull { it["name"] }.sorted())
 
                withContext(Dispatchers.Main) {
                    val adapterLocalizacao = ArrayAdapter(this@AddDeviceActivity, android.R.layout.simple_list_item_1, nomesFinal)
                    autoLocalizacao.setAdapter(adapterLocalizacao)
                }
            } catch (e: Exception) {
                Log.e("GLPI_ADD", "Erro ao carregar localizações: ${e.message}")
            }
        }
    }
 
    private fun carregarTecnicos() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Opções especiais exigidas pelo utilizador no topo
                val tecnicosFinal = mutableListOf("glpi", "normal", "post-only", "tech")
                
                val criteria = mapOf(
                    "forcedisplay[0]" to "1",
                    "forcedisplay[1]" to "2"
                )
                
                val combinedUsers = mutableListOf<Map<String, Any>>()
                var offset = 0
                val limit = 500
                var hasMore = true
                
                while (hasMore) {
                    val range = "$offset-${offset + limit - 1}"
                    val response = GlpiRetrofit.api.pesquisarUsuarios(GlpiConfig.SESSION_TOKEN, GlpiConfig.APP_TOKEN, criteria, range = range)
                    
                    if (response.isSuccessful) {
                        val data = response.body()?.data
                        if (!data.isNullOrEmpty()) {
                            combinedUsers.addAll(data)
                            if (data.size < limit) {
                                hasMore = false
                            } else {
                                offset += limit
                            }
                        } else {
                            hasMore = false
                        }
                    } else {
                        hasMore = false
                    }
                }

                technicianMap.clear()

                combinedUsers.distinctBy { it["2"]?.toString() }.forEach { item ->
                    val name = item["1"]?.toString() ?: ""
                    val id = item["2"]?.toString()?.toDoubleOrNull()?.toInt() ?: 0
                    if (name.isNotEmpty()) {
                        technicianMap[name] = id
                    }
                }

                val nomesUsuarios = technicianMap.keys.sortedBy { it.lowercase() }
                tecnicosFinal.addAll(nomesUsuarios)
 
                withContext(Dispatchers.Main) {
                    val adapterTecnico = ArrayAdapter(this@AddDeviceActivity, android.R.layout.simple_list_item_1, tecnicosFinal)
                    autoTecnico.setAdapter(adapterTecnico)
                }
            } catch (e: Exception) {
                Log.e("GLPI_ADD", "Erro ao carregar técnicos: ${e.message}")
            }
        }
    }

    private fun carregarEstados() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val states = GlpiRetrofit.api.getStatesList(GlpiConfig.SESSION_TOKEN, GlpiConfig.APP_TOKEN)
                stateMap.clear()

                states.forEach { state ->
                    val name = (state["name"]?.toString() ?: "").trim()
                    val idRaw = state["id"]?.toString() ?: ""
                    val id = idRaw.toIntOrNull() ?: idRaw.toDoubleOrNull()?.toInt() ?: 0
                    
                    if (name.isNotEmpty()) {
                        stateMap[name] = id
                    }
                }

                withContext(Dispatchers.Main) {
                    val nomesEstados = stateMap.keys.sortedBy { it.lowercase() }
                    val adapterEstado = ArrayAdapter(this@AddDeviceActivity, android.R.layout.simple_list_item_1, nomesEstados)
                    autoEstado.setAdapter(adapterEstado)
                }
            } catch (e: Exception) {
                Log.e("GLPI_ADD", "Erro ao carregar estados: ${e.message}")
            }
        }
    }
 
    private fun validarESubmeter() {
        // Limpar erros prévios
        tilTipo.error = null
        tilNome.error = null
        tilSN.error = null
 
        val tipoSelecionado = autoTipo.text.toString()
        val nome = etNome.text.toString().trim()
        val localizacaoRaw = autoLocalizacao.text.toString()
        val tecnico = autoTecnico.text.toString()
        val sn = etSN.text.toString().trim()
        val estado = autoEstado.text.toString()
 
        // Limpar os espaços de hierarquia (ex: "      CPCJ" -> "CPCJ")
        val localizacao = localizacaoRaw.trim()
 
        // Apenas Tipo e Nome são obrigatórios agora
        var obrigatorioEmFalta = false
        if (tipoSelecionado.isEmpty()) {
            tilTipo.error = "CAMPO OBRIGATÓRIO (SELECIONE O TIPO)"
            obrigatorioEmFalta = true
        }
        if (nome.isEmpty()) {
            tilNome.error = "CAMPO OBRIGATÓRIO (DIGITE O NOME)"
            obrigatorioEmFalta = true
        }
 
        if (obrigatorioEmFalta) return
 
        // Mostrar loading
        loadingOverlay.visibility = View.VISIBLE
 
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Verificar Duplicados em segundo plano
                val existeNome = verificarDuplicadoGlobal(nome, 1) // 1 = Nome
                
                // Só verificar SN se o utilizador tiver preenchido
                val existeSN = if (sn.isNotEmpty()) verificarDuplicadoGlobal(sn, 5) else false
 
                withContext(Dispatchers.Main) {
                    loadingOverlay.visibility = View.GONE
                    
                    var erroEncontrado = false
                    if (existeNome) {
                        tilNome.error = "ESTE NOME JÁ EXISTE NO INVENTÁRIO (CHECK GLPI)"
                        erroEncontrado = true
                    }
                    if (existeSN) {
                        tilSN.error = "ESTE NÚMERO DE SÉRIE JÁ EXISTE NO INVENTÁRIO"
                        erroEncontrado = true
                    }
 
                    if (!erroEncontrado) {
                        // Determinar o ItemType do GLPI (Inteligência do sistema)
                        val itemTypeGlpi = when (tipoSelecionado) {
                            "Computador" -> "Computer"
                            "Monitor" -> "Monitor"
                            "Dispositivo de Rede" -> "NetworkEquipment"
                            "Impressora" -> "Printer"
                            else -> "Peripheral"
                        }

                        // Criar o objeto de entrada com os IDs reais (Busca Robusta)
                        val technicianId = technicianMap.entries.find { it.key.trim().equals(tecnico.trim(), ignoreCase = true) }?.value
                        val locId = locationMap.entries.find { it.key.trim().equals(localizacao.trim(), ignoreCase = true) }?.value
                        val stateId = stateMap.entries.find { it.key.trim().equals(estado.trim(), ignoreCase = true) }?.value

                        Log.d("GLPI_ADD_DEBUG", "FIM: Tecnico='$tecnico' ID=$technicianId | Localizacao='$localizacao' ID=$locId | Estado='$estado' ID=$stateId")

                        // Construir o input dinamicamente para evitar campos nulos
                        val input = mutableMapOf<String, Any>()
                        input["name"] = nome
                        input["comment"] = "Adicionado via GLPI Mobile App"
                        input["entities_id"] = "0" // Forçar Entidade Raiz para teste
                        
                        // Enviar as datas corretas usando o relógio local do Android
                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                        val currentDateTime = sdf.format(java.util.Date())
                        input["date_creation"] = currentDateTime
                        input["date_mod"] = currentDateTime

                        if (sn.isNotEmpty()) input["serial"] = sn
                        if (locId != null && locId > 0) input["locations_id"] = locId.toString()
                        if (stateId != null && stateId > 0) input["states_id"] = stateId.toString()
                        if (technicianId != null && technicianId > 0) {
                            input["users_id_tech"] = technicianId.toString()
                            input["users_id"] = technicianId.toString()
                        }

                        val requestBody = mapOf("input" to input)
                        Log.d("GLPI_ADD_DEBUG", "JSON a enviar: $requestBody")
                        
                        loadingOverlay.visibility = View.VISIBLE
                        
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val res = GlpiRetrofit.api.addDevice(
                                    itemtype = itemTypeGlpi,
                                    sessionToken = GlpiConfig.SESSION_TOKEN,
                                    appToken = GlpiConfig.APP_TOKEN,
                                    request = requestBody
                                )

                                withContext(Dispatchers.Main) {
                                    if (res.isSuccessful) {
                                        val newIdRaw = res.body()?.id
                                        val newId = newIdRaw?.toString()?.toDoubleOrNull()?.toInt() ?: 0
                                        
                                        if (newId > 0) {
                                            Log.d("GLPI_ADD", "Criado com ID $newId. A forçar sincronização de campos...")
                                            
                                            // Realizar um UPDATE (PUT) para garantir que os campos são gravados
                                            CoroutineScope(Dispatchers.IO).launch {
                                                try {
                                                    // TESTE: Adicionar também o utilizador logado como reserva se o técnico falhar?
                                                    // Por agora, vamos manter o técnico mas adicionar LOGS do erro.
                                                    val updateRes = GlpiRetrofit.api.updateDevice(
                                                        itemtype = itemTypeGlpi,
                                                        id = newId,
                                                        sessionToken = GlpiConfig.SESSION_TOKEN,
                                                        appToken = GlpiConfig.APP_TOKEN,
                                                        request = requestBody
                                                    )
                                                    
                                                    withContext(Dispatchers.Main) {
                                                        loadingOverlay.visibility = View.GONE
                                                        if (updateRes.isSuccessful) {
                                                            Log.d("GLPI_ADD", "Sincronização OK para ID $newId")
                                                        } else {
                                                            val err = updateRes.errorBody()?.string() ?: "Erro"
                                                            Log.e("GLPI_ADD", "Erro na sincronização: $err")
                                                        }
                                                        
                                                        AlertHelper.exibirAlertaPremium(this@AddDeviceActivity, "Dispositivo adicionado com sucesso!", false)
                                                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ finish() }, 3000)
                                                    }
                                                } catch (e: Exception) {
                                                    withContext(Dispatchers.Main) {
                                                        loadingOverlay.visibility = View.GONE
                                                        Log.e("GLPI_ADD", "Falha crítica no update: ${e.message}")
                                                        AlertHelper.exibirAlertaPremium(this@AddDeviceActivity, "Dispositivo adicionado!", false)
                                                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ finish() }, 3000)
                                                    }
                                                }
                                            }
                                        } else {
                                            loadingOverlay.visibility = View.GONE
                                            AlertHelper.exibirAlertaPremium(this@AddDeviceActivity, "Dispositivo adicionado!", false)
                                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ finish() }, 3000)
                                        }
                                    } else {
                                        loadingOverlay.visibility = View.GONE
                                        val erroBody = res.errorBody()?.string() ?: "Erro desconhecido"
                                        Log.e("GLPI_ADD", "Erro API: $erroBody")
                                        AlertHelper.exibirAlertaPremium(this@AddDeviceActivity, "Erro GLPI: $erroBody", true)
                                    }
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    loadingOverlay.visibility = View.GONE
                                    AlertHelper.exibirAlertaPremium(this@AddDeviceActivity, "Falha: ${e.message}", true)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loadingOverlay.visibility = View.GONE
                    AlertHelper.exibirAlertaPremium(this@AddDeviceActivity, "Erro ao validar dados: ${e.message}", true)
                }
            }
        }
    }
 
    /**
     * Verifica se um valor (nome ou SN) já existe em qualquer categoria de inventário
     */
    private suspend fun verificarDuplicadoGlobal(valor: String, campoId: Int): Boolean {
        val tiposParaVerificar = listOf("Computer", "Monitor", "NetworkEquipment", "Printer", "Peripheral")
        val searchType = "equals" // Usar "equals" para evitar falsos positivos com substrings comuns (ex: "re")
        
        for (tipo in tiposParaVerificar) {
            try {
                val response = GlpiRetrofit.api.searchByCriteria(
                    itemtype = tipo,
                    sessionToken = GlpiConfig.SESSION_TOKEN,
                    appToken = GlpiConfig.APP_TOKEN,
                    field = campoId,
                    searchType = searchType,
                    value = valor
                )
                
                if (response.isSuccessful) {
                    val total = response.body()?.totalcount ?: 0
                    Log.d("GLPI_CHECK", "Busca em $tipo ($valor): Encontrados $total")
                    if (total > 0) return true
                } else {
                    Log.e("GLPI_CHECK", "Erro API em $tipo: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("GLPI_CHECK", "Erro ao verificar $tipo: ${e.message}")
            }
        }
        return false
    }
 
    private fun aplicarCorLottie(lottieView: LottieAnimationView?, cor: Int) {
        lottieView?.addValueCallback(KeyPath("**"), LottieProperty.COLOR_FILTER) {
            PorterDuffColorFilter(cor, PorterDuff.Mode.SRC_ATOP)
        }
    }
}