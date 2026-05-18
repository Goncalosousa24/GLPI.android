package com.example.glpimobile

import android.content.Intent
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath
import kotlinx.coroutines.*

class AssetHistoryActivity : AppCompatActivity() {

    private lateinit var rvHistory: RecyclerView
    private lateinit var searchBar: EditText
    private lateinit var progressLoading: LottieAnimationView
    private lateinit var nestedScrollView: NestedScrollView
    private lateinit var btnPrev: View
    private lateinit var btnNext: View

    private var currentRangeStart = 0
    private val PAGE_SIZE = 10
    private var searchJob: Job? = null
    private var fullList = mutableListOf<Map<String, Any>>()

    private val TOKEN_SESSAO = GlpiConfig.SESSION_TOKEN
    private val TOKEN_APP = GlpiConfig.APP_TOKEN

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_asset_history)

        initViews()
        setupListeners()
        carregarDados()
    }

    private fun initViews() {
        rvHistory = findViewById(R.id.rv_history_list)
        searchBar = findViewById(R.id.search_bar_history)
        progressLoading = findViewById(R.id.progress_history_list)
        nestedScrollView = findViewById(R.id.nested_scroll_history)
        btnPrev = findViewById(R.id.btn_prev_history)
        btnNext = findViewById(R.id.btn_next_history)

        rvHistory.layoutManager = LinearLayoutManager(this)
        
        val corAzul = ContextCompat.getColor(this, R.color.azul_glpi)
        val corBranca = ContextCompat.getColor(this, android.R.color.white)
        
        aplicarCorLottie(findViewById(R.id.lottie_seta_history), corBranca)
        aplicarCorLottie(findViewById(R.id.lottie_lupa_history), corAzul)
        aplicarCorLottie(findViewById(R.id.seta_prev_history), corAzul)
        aplicarCorLottie(findViewById(R.id.seta_next_history), corAzul)
        aplicarCorLottie(progressLoading, corAzul)
    }

    private fun setupListeners() {
        findViewById<FrameLayout>(R.id.btn_voltar_history).setOnClickListener { finish() }

        btnPrev.setOnClickListener {
            if (currentRangeStart >= PAGE_SIZE) {
                currentRangeStart -= PAGE_SIZE
                carregarDados()
            }
        }

        btnNext.setOnClickListener {
            currentRangeStart += PAGE_SIZE
            carregarDados()
        }

        searchBar.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                searchJob?.cancel()
                searchJob = CoroutineScope(Dispatchers.Main).launch {
                    delay(500)
                    currentRangeStart = 0
                    carregarDados(s.toString())
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private var totalCount = 0

    private fun carregarDados(query: String = "") {
        progressLoading.visibility = View.VISIBLE
        btnPrev.visibility = View.GONE
        btnNext.visibility = View.GONE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val range = "$currentRangeStart-${currentRangeStart + PAGE_SIZE - 1}"
                val tipos = listOf("Computer")
                val allResults = mutableListOf<Map<String, Any>>()
                var tempTotalCount = 0

                val jobs = tipos.map { tipo ->
                    async<List<Map<String, Any>>> {
                        try {
                            val response = if (query.isEmpty()) {
                                GlpiRetrofit.api.searchItemsWithTickets(tipo, TOKEN_SESSAO, TOKEN_APP, range)
                            } else {
                                GlpiRetrofit.api.searchByCriteriaWithTickets(tipo, TOKEN_SESSAO, TOKEN_APP, value = query)
                            }
                            
                            if (response.isSuccessful) {
                                val body = response.body()
                                tempTotalCount += body?.totalcount ?: 0
                                body?.data?.map { it.toMutableMap().apply { put("TipoReal", tipo) } } ?: emptyList()
                            } else emptyList()
                        } catch (e: Exception) { emptyList() }
                    }
                }

                allResults.addAll(jobs.awaitAll().flatten())

                withContext(Dispatchers.Main) {
                    totalCount = tempTotalCount
                    fullList = allResults.toMutableList()
                    rvHistory.adapter = HistoryListAdapter(fullList)
                    rvHistory.scheduleLayoutAnimation()
                    progressLoading.visibility = View.GONE
                    nestedScrollView.smoothScrollTo(0, 0)

                    btnPrev.visibility = if (currentRangeStart > 0) View.VISIBLE else View.GONE
                    
                    // 🔥 Só mostra Próximo se houver mais itens para além do range atual 🔥
                    btnNext.visibility = if (currentRangeStart + fullList.size < totalCount) View.VISIBLE else View.GONE
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { progressLoading.visibility = View.GONE }
            }
        }
    }

    private fun aplicarCorLottie(lottie: LottieAnimationView?, cor: Int) {
        lottie?.addValueCallback(KeyPath("**"), LottieProperty.COLOR_FILTER) {
            PorterDuffColorFilter(cor, PorterDuff.Mode.SRC_ATOP)
        }
    }

    // --- Adapter Interno ---
    inner class HistoryListAdapter(private val list: List<Map<String, Any>>) : 
        RecyclerView.Adapter<HistoryListAdapter.VH>() {

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvNome: TextView = v.findViewById(R.id.tv_equipamento_nome)
            val tvUser: TextView = v.findViewById(R.id.tv_utilizador)
            val tvSerial: TextView = v.findViewById(R.id.tv_detalhes_serial)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_inventory, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = list[position]
            val nome = item["1"]?.toString() ?: "Sem Nome"
            val user = item["7"]?.toString() ?: item["70"]?.toString() ?: "Sem Utilizador"
            val serial = item["5"]?.toString() ?: item["80"]?.toString() ?: "S/N: ---"
            val tipoReal = item["TipoReal"]?.toString() ?: "Computer"

            holder.tvNome.text = nome
            holder.tvUser.text = "Dono: $user"
            holder.tvSerial.text = "$tipoReal | $serial"

            holder.itemView.setOnClickListener {
                fun descobrirID(map: Map<String, Any>): Int {
                    val maxInt = 2147483647
                    val chavesPrioritarias = listOf("2", "id", "ID", "id_real", "0")
                    for (k in chavesPrioritarias) {
                        val value = map[k]
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


                val intent = Intent(this@AssetHistoryActivity, AssetDetailsActivity::class.java).apply {


                    putExtra("ASSET_ID", assetId)
                    putExtra("ASSET_NAME", nome)
                    putExtra("ASSET_TYPE", tipoReal)
                    putExtra("ASSET_SERIAL", serial)
                    putExtra("ASSET_LOCATION", "") // Localização não disponível neste ecrã
                }

                startActivity(intent)
            }

        }

        override fun getItemCount() = list.size
    }

    override fun dispatchTouchEvent(ev: android.view.MotionEvent?): Boolean {
        KeyboardHelper.handleTouchOutside(this, ev)
        return super.dispatchTouchEvent(ev)
    }
}
