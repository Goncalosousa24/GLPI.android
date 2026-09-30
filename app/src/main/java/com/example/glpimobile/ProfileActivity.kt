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

class ProfileActivity : AppCompatActivity() {

    private var isExpanded = false
    private val addedChips = mutableListOf<View>()
    private var fullUserList = mutableListOf<Map<String, Any>>()
    private var listaFiltrada = mutableListOf<Map<String, Any>>()
    private lateinit var profileAdapter: ProfileAdapter
    private var perfilAtual = "TODOS"
    private var paginaAtual = 0
    private val itensPorPagina = 10
    private var searchJob: kotlinx.coroutines.Job? = null

    private lateinit var btnAnterior: View
    private lateinit var btnProximo: View
    private lateinit var spacePerfil: View
    private lateinit var nestedScroll: NestedScrollView

    private val TOKEN_SESSAO = GlpiConfig.SESSION_TOKEN
    private val TOKEN_APP = GlpiConfig.APP_TOKEN

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val btnVoltar = findViewById<FrameLayout>(R.id.btn_voltar_profile)
        val lottieSeta = findViewById<LottieAnimationView>(R.id.lottie_seta_profile)

        // Seta a Branco para contrastar com o gradiente Azul
        val corBrancaPura = ContextCompat.getColor(this, android.R.color.white)
        lottieSeta?.addValueCallback(KeyPath("**"), LottieProperty.COLOR_FILTER) {
            PorterDuffColorFilter(corBrancaPura, PorterDuff.Mode.SRC_ATOP)
        }

        btnVoltar?.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }

        configurarNavegacaoInferior()

        val rvPerfis = findViewById<RecyclerView>(R.id.rv_perfis)
        rvPerfis.layoutManager = LinearLayoutManager(this)
        profileAdapter = ProfileAdapter(emptyList())
        rvPerfis.adapter = profileAdapter

        btnAnterior = findViewById(R.id.btn_perfil_anterior)
        btnProximo = findViewById(R.id.btn_perfil_proximo)
        spacePerfil = findViewById(R.id.space_perfil)
        nestedScroll = findViewById(R.id.nested_scroll_profile)

        // Estilização Lottie Lupa e Setas de Paginação
        val lupaAnim = findViewById<LottieAnimationView>(R.id.lupa_anim_profile)
        val corAzulGlpi = ContextCompat.getColor(this, R.color.azul_glpi)
        
        aplicarCorLottie(lupaAnim, corAzulGlpi)
        aplicarCorLottie(findViewById(R.id.lottie_filtro_profile), corAzulGlpi)
        aplicarCorLottie(findViewById(R.id.lottie_seta_voltar_profile_pag), corAzulGlpi)
        aplicarCorLottie(findViewById(R.id.lottie_seta_avancar_profile_pag), corAzulGlpi)

        btnAnterior.setOnClickListener {
            if (paginaAtual > 0) {
                paginaAtual--
                atualizarEcraPaginacao()
                nestedScroll.smoothScrollTo(0, 0)
            }
        }
        btnProximo.setOnClickListener {
            val maxPaginas = if (listaFiltrada.isEmpty()) 0 else (listaFiltrada.size - 1) / itensPorPagina
            if (paginaAtual < maxPaginas) {
                paginaAtual++
                atualizarEcraPaginacao()
                nestedScroll.smoothScrollTo(0, 0)
            }
        }

        findViewById<FrameLayout>(R.id.btn_filtro_profile)?.setOnClickListener {
            showFilterDialog()
        }

        findViewById<EditText>(R.id.search_bar_profile)?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { 
                searchJob?.cancel()
                searchJob = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                    kotlinx.coroutines.delay(500)
                    val apiJob = carregarUtilizadores(s?.toString() ?: "")
                    searchJob = apiJob
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Ao clicar num espaço vazio, tirar o foco da barra de pesquisa e esconder o teclado
        val searchBar = findViewById<EditText>(R.id.search_bar_profile)
        findViewById<View>(R.id.profile_layout_main)?.setOnTouchListener { _, _ ->
            if (searchBar?.hasFocus() == true) {
                searchBar.clearFocus()
                val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.hideSoftInputFromWindow(searchBar.windowToken, 0)
            }
            false
        }

        // Limpar cache antiga e sempre carregar do servidor (garante dados sem duplicados)
        PreferenceManager.setUserListCache(this, null)
        carregarUtilizadores()

        playFilterAnimation() // Play on open
    }

    private fun aplicarCorLottie(lottieView: LottieAnimationView?, cor: Int) {
        lottieView?.addValueCallback(KeyPath("**"), LottieProperty.COLOR_FILTER) {
            PorterDuffColorFilter(cor, PorterDuff.Mode.SRC_ATOP)
        }
    }

    private fun configurarNavegacaoInferior() {
        val navView = findViewById<BottomNavigationView>(R.id.bottom_navigation_profile)
        navView?.let { nav ->
            nav.selectedItemId = R.id.nav_perfil

            // Tinta Branca aplicada aos ícones
            nav.labelVisibilityMode = BottomNavigationView.LABEL_VISIBILITY_UNLABELED
            
            val corBrancaPura = ContextCompat.getColor(this, android.R.color.white)
            val corBrancaTransluscida = Color.parseColor("#99FFFFFF")
            val tintList = ColorStateList(
                arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf(-android.R.attr.state_checked)),
                intArrayOf(corBrancaPura, corBrancaTransluscida)
            )
            nav.itemIconTintList = tintList
            nav.itemTextColor = tintList

            try { nav.itemActiveIndicatorColor = ColorStateList.valueOf(Color.TRANSPARENT) } catch (e: Exception) {}

            // Margem inferior garantida para o menu flutuar sem cortar
            ViewCompat.setOnApplyWindowInsetsListener(nav) { view, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                val layoutParams = view.layoutParams as ViewGroup.MarginLayoutParams
                layoutParams.bottomMargin = systemBars.bottom + 16
                view.layoutParams = layoutParams
                insets
            }

            nav.setOnItemSelectedListener { item ->
                if (item.itemId == R.id.nav_perfil) {
                    abrirMeuPerfil()
                    return@setOnItemSelectedListener true
                }

                val intent = when (item.itemId) {
                    R.id.nav_tickets -> Intent(this, DashboardActivity::class.java)
                    R.id.nav_agenda -> Intent(this, AgendaActivity::class.java)
                    R.id.nav_inventario -> Intent(this, InventoryActivity::class.java)
                    else -> null
                }

                intent?.let {
                    startActivity(it)
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                }
                false
            }
        }
    }

    private fun abrirMeuPerfil() {
        val intent = Intent(this, ProfileDetailActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }

    private fun carregarUtilizadores(query: String = ""): kotlinx.coroutines.Job {
        return CoroutineScope(Dispatchers.IO).launch {
            try {
                val forcedisplay = mutableMapOf(
                    "forcedisplay[0]" to "1", // Nome
                    "forcedisplay[1]" to "2", // ID
                    "forcedisplay[2]" to "20" // Perfil
                )
                
                if (query.isNotEmpty()) {
                    forcedisplay["criteria[0][field]"] = "1"
                    forcedisplay["criteria[0][searchtype]"] = "contains"
                    forcedisplay["criteria[0][value]"] = query
                }
                
                val combinedUsers = mutableListOf<Map<String, Any>>()
                var offset = 0
                val limit = 500
                var hasMore = true
                
                while (hasMore) {
                    val range = "$offset-${offset + limit - 1}"
                    val resposta = GlpiRetrofit.api.searchItems(
                        itemtype = "User",
                        sessionToken = TOKEN_SESSAO,
                        appToken = TOKEN_APP,
                        range = range,
                        criteria = forcedisplay
                    )
                    
                    if (resposta.isSuccessful) {
                        val data = resposta.body()?.data
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

                withContext(Dispatchers.Main) {
                    // Guardar TODAS as linhas (um utilizador pode ter múltiplos perfis)
                    // A deduplicação por nome é feita após aplicar o filtro de perfil
                    fullUserList = combinedUsers.filter {
                        val nome = (it["1"]?.toString() ?: "").lowercase()
                        nome != "glpi" && nome != "tech" && nome != "normal" && nome != "post-only" && nome.isNotEmpty()
                    }.toMutableList()
                    
                    // Guardar em cache apenas se não houver pesquisa
                    if (query.isEmpty()) {
                        try {
                            val json = com.google.gson.Gson().toJson(fullUserList)
                            PreferenceManager.setUserListCache(this@ProfileActivity, json)
                        } catch (e: Exception) {}
                    }

                    aplicarFiltroEAtualizar()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ProfileActivity, "Erro ao ligar ao servidor", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun aplicarFiltroEAtualizar() {
        listaFiltrada = if (perfilAtual == "TODOS") {
            fullUserList.toMutableList()
        } else {
            fullUserList.filter { 
                val role = it["20"]?.toString() ?: ""
                if (perfilAtual == "Admin") {
                    // Se o filtro for Admin, garantimos que não apanha "Super-Admin"
                    role.contains("Admin", ignoreCase = true) && !role.contains("Super-", ignoreCase = true)
                } else {
                    role.contains(perfilAtual, ignoreCase = true)
                }
            }.toMutableList()
        }

        val texto = findViewById<EditText>(R.id.search_bar_profile)?.text?.toString() ?: ""
        if (texto.isNotEmpty()) {
            val queryNormalized = KeyboardHelper.removeAccents(texto)
            listaFiltrada = listaFiltrada.filter {
                val nameNormalized = KeyboardHelper.removeAccents(it["1"]?.toString() ?: "")
                val profileNormalized = KeyboardHelper.removeAccents(it["20"]?.toString() ?: "")
                nameNormalized.contains(queryNormalized, ignoreCase = true) ||
                        profileNormalized.contains(queryNormalized, ignoreCase = true)
            }.toMutableList()
        }
        
        // Remover duplicados pelo nome do utilizador (campo "1")
        listaFiltrada = listaFiltrada.distinctBy { it["1"]?.toString()?.lowercase() }.toMutableList()
        paginaAtual = 0
        atualizarEcraPaginacao()
        
        // Atualizar cor do botão de filtro
        val btnFiltro = findViewById<FrameLayout>(R.id.btn_filtro_profile)
        if (perfilAtual == "TODOS") {
            btnFiltro?.setBackgroundResource(R.drawable.bg_cartao_brilhante)
        } else {
            btnFiltro?.setBackgroundResource(R.drawable.bg_filtro_ativo)
        }
    }

    private fun showFilterDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_custom_filter_state, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<TextView>(R.id.tv_filter_title).text = "FILTRAR POR PERFIL"
        val container = dialogView.findViewById<android.widget.LinearLayout>(R.id.container_filter_options)
        val btnClose = dialogView.findViewById<android.view.View>(R.id.btn_close_filter)
        btnClose.setOnClickListener { dialog.dismiss() }

        val profiles = arrayOf("TODOS", "Super-Admin", "Observer", "Admin", "Read-Only", "Supervisor", "Hotliner", "Technician")
        val displayNames = arrayOf("Todos", "Super-Admin", "Observador", "Admin", "Leitura (Read-Only)", "Supervisor", "Hotliner", "Técnico")

        val d = resources.displayMetrics.density

        profiles.forEachIndexed { index, profileKey ->
            val title = displayNames[index]
            val isSelected = perfilAtual == profileKey
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
                    if (perfilAtual != profileKey) {
                        perfilAtual = profileKey
                        playFilterAnimation() // Play filter animation on change
                        aplicarFiltroEAtualizar()
                    }
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

    private fun atualizarEcraPaginacao() {
        val inicio = paginaAtual * itensPorPagina
        val fim = (inicio + itensPorPagina).coerceAtMost(listaFiltrada.size)
        val subLista = if (inicio < listaFiltrada.size) listaFiltrada.subList(inicio, fim) else emptyList()
        val rvPerfis = findViewById<RecyclerView>(R.id.rv_perfis)
        val llEmpty = findViewById<View>(R.id.ll_empty_state_profile)
        
        profileAdapter.updateList(subLista)
        
        if (subLista.isEmpty() && listaFiltrada.isEmpty()) {
            rvPerfis.visibility = View.GONE
            llEmpty.visibility = View.VISIBLE
        } else {
            rvPerfis.visibility = View.VISIBLE
            llEmpty.visibility = View.GONE
            rvPerfis.scheduleLayoutAnimation()
        }

        btnAnterior.visibility = if (paginaAtual > 0) View.VISIBLE else View.GONE
        btnProximo.visibility = if (fim < listaFiltrada.size) View.VISIBLE else View.GONE
        spacePerfil.visibility = if (btnAnterior.visibility == View.VISIBLE && btnProximo.visibility == View.VISIBLE) View.VISIBLE else View.GONE
    }

}