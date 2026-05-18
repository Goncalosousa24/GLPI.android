package com.example.glpimobile

import android.content.Intent
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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

class BorrowDeviceActivity : AppCompatActivity() {

// empty

    private lateinit var rvEquipamentos: RecyclerView
    private lateinit var borrowAdapter: BorrowDeviceAdapter
    private var searchBar: EditText? = null
    private lateinit var tvListaVazia: TextView
    private lateinit var nestedScrollView: NestedScrollView
    private var searchJob: Job? = null

    private var paginaAtual = 0
    private val itensParaExibir = 10
    private val buscaRangeAPI = 100

    private var itemTypeAtual = "All"

    private val TOKEN_SESSAO = GlpiConfig.SESSION_TOKEN
    private val TOKEN_APP = GlpiConfig.APP_TOKEN

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_borrow_device)

        val corAzulGlpi = ContextCompat.getColor(this, R.color.azul_glpi)
        val corBranca = ContextCompat.getColor(this, android.R.color.white)

        rvEquipamentos = findViewById(R.id.rv_borrow_devices)
        rvEquipamentos.layoutManager = LinearLayoutManager(this)
        borrowAdapter = BorrowDeviceAdapter(emptyList()) {
            carregarInventario()
        }
        rvEquipamentos.adapter = borrowAdapter

        searchBar = findViewById(R.id.search_bar_borrow)
        tvListaVazia = findViewById(R.id.tv_lista_vazia_borrow)
        nestedScrollView = findViewById(R.id.nested_scroll_main_borrow)

        configurarCoresLottie(corAzulGlpi, corBranca)

        findViewById<FrameLayout>(R.id.btn_voltar_borrow).setOnClickListener { voltarParaInventarioComSinal() }

        findViewById<View>(R.id.btn_pagina_anterior_borrow).setOnClickListener {
            if (paginaAtual > 0) { paginaAtual--; carregarInventario() }
        }

        findViewById<View>(R.id.btn_proxima_pagina_borrow).setOnClickListener {
            paginaAtual++; carregarInventario()
        }

        configurarPesquisa()

        findViewById<TextView>(R.id.btn_abrir_calendario).setOnClickListener {
            val intent = Intent(this, ReservationsActivity::class.java)
            startActivity(intent)
        }

        carregarInventario()
    }

    private fun configurarPesquisa() {
        searchBar?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                searchJob?.cancel()
                searchJob = CoroutineScope(Dispatchers.Main).launch {
                    delay(600)
                    carregarInventario()
                }
            }
            override fun beforeTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })
    }

    private fun voltarParaInventarioComSinal() {
        val intent = Intent(this, InventoryActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        intent.putExtra("ABRIR_MENU", true)
        startActivity(intent)
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        finish()
    }

    @Deprecated("Deprecated in Java")
    override fun dispatchTouchEvent(ev: android.view.MotionEvent?): Boolean {
        KeyboardHelper.handleTouchOutside(this, ev)
        return super.dispatchTouchEvent(ev)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        voltarParaInventarioComSinal()
    }

    private fun configurarCoresLottie(azul: Int, branco: Int) {
        aplicarCorLottie(findViewById(R.id.lottie_seta_borrow), branco)
        aplicarCorLottie(findViewById(R.id.lottie_lupa_borrow), azul)
        aplicarCorLottie(findViewById(R.id.seta_voltar_borrow), azul)
        aplicarCorLottie(findViewById(R.id.seta_avancar_borrow), azul)
    }

    // Funções de chips removidas


    private fun carregarInventario() {
        val termoAProcurar = searchBar?.text?.toString()?.trim() ?: ""
        val isSearching = termoAProcurar.isNotEmpty()
        
        // Se estamos a pesquisar, podemos só usar a API de pesquisa (searchByCriteria) que não usa paginação complexa (ou ajustamos o range)
        val range = if (isSearching) "0-100" else "${paginaAtual * itensParaExibir}-${(paginaAtual * itensParaExibir) + buscaRangeAPI}"

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val listaResultados = mutableListOf<GlpiEquipamento>()

                val forcedisplay = mapOf(
                    "forcedisplay[0]" to "1",
                    "forcedisplay[1]" to "2",
                    "forcedisplay[2]" to "5",
                    "forcedisplay[3]" to "70",
                    "forcedisplay[4]" to "8",
                    "forcedisplay[5]" to "24",
                    "forcedisplay[6]" to "15",
                    "forcedisplay[7]" to "31"
                )

                if (itemTypeAtual == "All") {
                    val tipos = listOf("Computer", "Monitor")
                    val jobs = tipos.map { tipo ->
                        async {
                            try {
                                val res = if (isSearching) {
                                    GlpiRetrofit.api.searchByCriteria(tipo, TOKEN_SESSAO, TOKEN_APP, 5, "contains", termoAProcurar)
                                } else {
                                    GlpiRetrofit.api.searchItems(tipo, TOKEN_SESSAO, TOKEN_APP, range, forcedisplay)
                                }
                                val resData = if (res.isSuccessful) res.body()?.data ?: emptyList() else emptyList()
                                processarResposta(resData, tipo)
                            } catch (e: Exception) { emptyList<GlpiEquipamento>() }
                        }
                    }
                    listaResultados.addAll(jobs.awaitAll().flatten())
                } else {
                    val res = if (isSearching) {
                        GlpiRetrofit.api.searchByCriteria(itemTypeAtual, TOKEN_SESSAO, TOKEN_APP, 5, "contains", termoAProcurar)
                    } else {
                        GlpiRetrofit.api.searchItems(itemTypeAtual, TOKEN_SESSAO, TOKEN_APP, range, forcedisplay)
                    }
                    val resData = if (res.isSuccessful) res.body()?.data ?: emptyList() else emptyList()
                    listaResultados.addAll(processarResposta(resData, itemTypeAtual))
                }


                withContext(Dispatchers.Main) {
                    val listaFinal = listaResultados.filter {
                        val matchBuscaLocal = !isSearching || (it.name?.contains(termoAProcurar, true) == true || it.serial?.contains(termoAProcurar, true) == true)
                        matchBuscaLocal
                    }.take(itensParaExibir)

                    borrowAdapter.updateData(listaFinal)
                    rvEquipamentos.scheduleLayoutAnimation()

                    nestedScrollView.smoothScrollTo(0, 0)

                    tvListaVazia.visibility = if (listaFinal.isEmpty()) View.VISIBLE else View.GONE
                    rvEquipamentos.visibility = if (listaFinal.isEmpty()) View.GONE else View.VISIBLE

                    findViewById<View>(R.id.btn_pagina_anterior_borrow).visibility = if (paginaAtual > 0 && !isSearching) View.VISIBLE else View.GONE
                    findViewById<View>(R.id.btn_proxima_pagina_borrow).visibility = if (listaFinal.size >= itensParaExibir && !isSearching) View.VISIBLE else View.GONE
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

            // 🔍 EXTRAÇÃO "REDE ALARGADA"
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

            val estadoVal = (itemMap["31"] ?: itemMap["10"])?.toString()?.trim() ?: "Não definido"
            val commentVal = itemMap["16"]?.toString() ?: ""

            // Campo 16 é o comment, mas aqui não é estritamente necessário na lista. O Adapter busca via API ao carregar.
            lista.add(GlpiEquipamento(
                id = idVal,
                name = nomeVal,
                serial = snVal,
                estadoStr = estadoVal,
                utilizador = finalUser,

                itemtype = tipo,
                comment = commentVal
            ))
        }
        return lista
    }

    private fun aplicarCorLottie(lottieView: LottieAnimationView?, cor: Int) {
        lottieView?.addValueCallback(KeyPath("**"), LottieProperty.COLOR_FILTER) {
            PorterDuffColorFilter(cor, PorterDuff.Mode.SRC_ATOP)
        }
    }
}
