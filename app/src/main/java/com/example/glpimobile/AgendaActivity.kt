package com.example.glpimobile

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*
import android.view.animation.AccelerateDecelerateInterpolator

class AgendaActivity : AppCompatActivity() {

    private lateinit var rvTickets: RecyclerView
    private lateinit var rvCalendar: RecyclerView
    private lateinit var calendarAdapter: CalendarAdapter
    private lateinit var tvMonthYear: TextView
    private lateinit var tvDataTitulo: TextView
    private lateinit var tvSemTarefas: TextView
    private lateinit var pbCalendarFullLoading: ProgressBar
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private var currentCalendar = Calendar.getInstance()
    private var selectedDayCalendar = Calendar.getInstance()

    private var filterType = "INDIVIDUAL" // EQUIPA, INDIVIDUAL
    private var selectedUserId: Int? = null // ID do utilizador autenticado
    private var selectedDate: String = ""
    private var ticketsDoMesCache: List<Map<String, Any>>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Idioma PT
        val locale = Locale("pt", "PT")
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)

        setContentView(R.layout.activity_agenda)

        rvTickets = findViewById(R.id.rv_tickets_agenda)
        pbCalendarFullLoading = findViewById(R.id.pb_calendar_full_loading)
        rvCalendar = findViewById(R.id.rv_calendar_grid)
        tvMonthYear = findViewById(R.id.tv_month_year)
        tvDataTitulo = findViewById(R.id.tv_data_titulo)
        tvSemTarefas = findViewById(R.id.tv_sem_tarefas_agenda)
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_agenda)

        swipeRefreshLayout.setColorSchemeColors(ContextCompat.getColor(this, R.color.azul_glpi))
        swipeRefreshLayout.setOnRefreshListener {
            carregarMensalidade()
        }

        rvTickets.layoutManager = LinearLayoutManager(this)

        // 🔥 CARREGAR FILTRO DA SESSÃO ATUAL (GlpiConfig) 🔥
        filterType = GlpiConfig.currentAgendaFilter
        selectedUserId = if (filterType == "EQUIPA") null else PreferenceManager.getUserId(this)

        // Setup Bottom Nav
        configurarNavegacao()

        // Setup Filters (Agora já usará o filterType correto para o UI)
        configurarFiltros()

        // Setup Calendar
        setupCalendar()
        

        // Initial Date
        selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        tvDataTitulo.text = "Tickets de ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())}:"

        // Carregar cache local primeiro para exibição instantânea (Cache-then-Network)
        loadAgendaFromCache()
        renderCalendar()
        carregarDadosAgenda()

        // Primeira vez na sessão: Atualiza automaticamente da rede
        // Vezes seguintes: Fica estático, só atualiza se o utilizador "puxar"
        if (!GlpiConfig.isAgendaFirstLoadDone) {
            carregarMensalidade()
            GlpiConfig.isAgendaFirstLoadDone = true
        }
    }

    private fun setupCalendar() {
        calendarAdapter = CalendarAdapter { date ->
            val clickedCal = Calendar.getInstance().apply { time = date }
            val currentMonth = currentCalendar.get(Calendar.MONTH)
            val currentYear = currentCalendar.get(Calendar.YEAR)
            
            val clickedMonth = clickedCal.get(Calendar.MONTH)
            val clickedYear = clickedCal.get(Calendar.YEAR)

            selectedDayCalendar.time = date
            selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
            tvDataTitulo.text = "Tickets de ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date)}:"

            // Se o dia clicado for de um mês diferente do atual, mudamos a página do calendário
            if (clickedMonth != currentMonth || clickedYear != currentYear) {
                currentCalendar.time = date
                ticketsDoMesCache = null
                carregarMensalidade()
            } else {
                renderCalendar()
            }
            
            carregarDadosAgenda()
        }
        
        rvCalendar.layoutManager = androidx.recyclerview.widget.GridLayoutManager(this, 7)
        rvCalendar.adapter = calendarAdapter

        findViewById<ImageView>(R.id.btn_prev_month)?.setOnClickListener {
            currentCalendar.add(Calendar.MONTH, -1)
            updateMonthYearDisplay()
            ticketsDoMesCache = null
            rvCalendar.visibility = View.INVISIBLE
            pbCalendarFullLoading.visibility = View.VISIBLE
            renderCalendar()
            carregarMensalidade()
        }
        findViewById<ImageView>(R.id.btn_next_month)?.setOnClickListener {
            currentCalendar.add(Calendar.MONTH, 1)
            updateMonthYearDisplay()
            ticketsDoMesCache = null
            rvCalendar.visibility = View.INVISIBLE
            pbCalendarFullLoading.visibility = View.VISIBLE
            renderCalendar()
            carregarMensalidade()
        }

        renderCalendar()
    }

    private fun updateMonthYearDisplay() {
        val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale("pt", "PT"))
        val monthYear = monthYearFormat.format(currentCalendar.time)
        tvMonthYear.text = if (monthYear.isNotEmpty()) {
            monthYear.substring(0, 1).uppercase() + monthYear.substring(1)
        } else ""
    }

    private fun renderCalendar(ticketsDoMes: List<Map<String, Any>>? = null) {
        val ticketsASeremExibidos = ticketsDoMes ?: ticketsDoMesCache
        
        updateMonthYearDisplay()

        if (ticketsASeremExibidos == null) {
            rvCalendar.visibility = View.INVISIBLE
            pbCalendarFullLoading.visibility = View.VISIBLE
            return
        } else {
            rvCalendar.visibility = View.VISIBLE
            pbCalendarFullLoading.visibility = View.GONE
        }

        val days = mutableListOf<CalendarDay>()
        val calendarCopy = currentCalendar.clone() as Calendar
        calendarCopy.set(Calendar.HOUR_OF_DAY, 0)
        calendarCopy.set(Calendar.MINUTE, 0)
        calendarCopy.set(Calendar.SECOND, 0)
        calendarCopy.set(Calendar.MILLISECOND, 0)
        calendarCopy.set(Calendar.DAY_OF_MONTH, 1)

        // Ajustar para segunda-feira como primeiro dia da semana (ISO)
        var firstDayOfWeek = calendarCopy.get(Calendar.DAY_OF_WEEK) - 2
        if (firstDayOfWeek < 0) firstDayOfWeek = 6

        calendarCopy.add(Calendar.DAY_OF_MONTH, -firstDayOfWeek)

        val sdfApi = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val selectedStr = sdfApi.format(selectedDayCalendar.time)

        // 6 semanas para cobrir todos os casos
        for (i in 0 until 42) {
            val date = calendarCopy.time
            val isCurrentMonth = calendarCopy.get(Calendar.MONTH) == currentCalendar.get(Calendar.MONTH)
            val dateStr = sdfApi.format(date)
            
            val ticketsDoDia = ticketsASeremExibidos.filter { ticket ->
                val statusRaw = ticket["12"]?.toString() ?: ""
                val statusId = extractId(statusRaw)
                
                // Filtro de Ativo robusto (por ID e por Nome comum)
                val isInactive = statusId == "5" || statusId == "6" || 
                                statusRaw.contains("Solucionado", ignoreCase = true) || 
                                statusRaw.contains("Fechado", ignoreCase = true)
                
                if (isInactive) return@filter false

                // Agora o ticket aparece em TODOS os dias relevantes (Criação, Prazo, TTR, TTO)
                val dataLimite = ticket["18"]?.toString() ?: ""
                val dataTTR = ticket["151"]?.toString() ?: ""
                val dataTTO = ticket["158"]?.toString() ?: ""
                val dataCriacao = ticket["15"]?.toString() ?: ""

                val dateParts = dateStr.split("-")
                val dateStrInvertida = "${dateParts[2]}/${dateParts[1]}/${dateParts[0]}"
                val dateStrInvertidaTraco = "${dateParts[2]}-${dateParts[1]}-${dateParts[0]}"

                fun matches(d: String) = d.contains(dateStr) || d.contains(dateStrInvertida) || d.contains(dateStrInvertidaTraco)

                matches(dataLimite) || matches(dataTTR) || matches(dataTTO) || matches(dataCriacao)
            }

            val hasTickets = ticketsDoDia.isNotEmpty()
            val isExpiredTicket = ticketsDoDia.any { ticket ->
                val statusRaw = ticket["12"]?.toString() ?: ""
                val statusId = extractId(statusRaw)
                val dataLimite = ticket["18"]?.toString() ?: ""
                isExpired(dataLimite, statusId)
            }

            days.add(CalendarDay(
                date = date,
                isCurrentMonth = isCurrentMonth,
                hasTickets = hasTickets,
                isSelected = dateStr == selectedStr,
                isExpiredTicket = isExpiredTicket
            ))
            calendarCopy.add(Calendar.DAY_OF_MONTH, 1)
        }
        calendarAdapter.updateDays(days)
    }

    private fun carregarMensalidade() {
        // Ao carregar, mantemos o calendário visível mas com alpha (escurecido/desvanecido)
        // e mostramos o loader por cima, para evitar o efeito de "piscar" (flickering).
        rvCalendar.alpha = 0.5f
        pbCalendarFullLoading.visibility = View.VISIBLE

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val sessionToken = PreferenceManager.getSessionToken(this@AgendaActivity)
                val appToken = PreferenceManager.getAppToken(this@AgendaActivity)
                
                // Em vez de filtrar por data no servidor (que causava perdas na fronteira),
                // carregamos todos os tickets ATIVOS (status < 5).
                val response = if (selectedUserId == null) {
                    GlpiRetrofit.api.getAgendaAtivos(sessionToken, appToken)
                } else {
                    val userId = selectedUserId!!
                    val loginBusca = GlpiConfig.USER_NAME.split(".", " ").firstOrNull() ?: ""
                    val nomeCompleto = GlpiConfig.USER_FULL_NAME
                    
                    GlpiRetrofit.api.getAgendaAtivosByUser(
                        sessionToken = sessionToken, 
                        appToken = appToken, 
                        userId1 = userId, 
                        userId2 = userId, 
                        userId3 = userId, 
                        userId4 = userId, 
                        userId5 = userId, 
                        userId6 = userId,
                        login1 = loginBusca, 
                        login2 = nomeCompleto.ifEmpty { loginBusca }
                    )
                }
                
                withContext(Dispatchers.Main) {
                    val newData = if (response.isSuccessful) response.body()?.data ?: emptyList() else emptyList()
                    ticketsDoMesCache = newData
                    saveAgendaToCache(newData)
                    
                    pbCalendarFullLoading.visibility = View.GONE
                    rvCalendar.alpha = 1.0f
                    rvCalendar.visibility = View.VISIBLE
                    swipeRefreshLayout.isRefreshing = false
                    renderCalendar(newData)
                    carregarDadosAgenda()
                }
            } catch (e: Exception) {
                Log.e("AgendaActivity", "Erro ao carregar mensalidade", e)
                withContext(Dispatchers.Main) {
                    if (ticketsDoMesCache == null) ticketsDoMesCache = emptyList()
                    pbCalendarFullLoading.visibility = View.GONE
                    rvCalendar.alpha = 1.0f
                    rvCalendar.visibility = View.VISIBLE
                    swipeRefreshLayout.isRefreshing = false
                    renderCalendar(ticketsDoMesCache)
                    carregarDadosAgenda()
                }
            }
        }
    }

    private fun saveAgendaToCache(data: List<Map<String, Any>>) {
        try {
            val json = Gson().toJson(data)
            if (selectedUserId == null) {
                PreferenceManager.setAgendaTeamCache(this, json)
            } else {
                PreferenceManager.setAgendaCache(this, json)
            }
        } catch (e: Exception) {
            Log.e("AgendaActivity", "Failed to save cache", e)
        }
    }

    private fun loadAgendaFromCache() {
        try {
            val json = if (selectedUserId == null) {
                PreferenceManager.getAgendaTeamCache(this)
            } else {
                PreferenceManager.getAgendaCache(this)
            }
            
            if (!json.isNullOrEmpty()) {
                val type = object : TypeToken<List<Map<String, Any>>>() {}.type
                ticketsDoMesCache = Gson().fromJson(json, type)
                Log.d("AgendaActivity", "Cache carregado com sucesso (${ticketsDoMesCache?.size} itens)")
            }
        } catch (e: Exception) {
            Log.e("AgendaActivity", "Failed to load cache", e)
        }
    }

    private fun configurarFiltros() {
        val chipEquipa = findViewById<View>(R.id.chip_equipa)
        val chipIndividual = findViewById<View>(R.id.chip_individual)
        val ivEquipa = findViewById<ImageView>(R.id.iv_chip_equipa)
        val ivIndividual = findViewById<ImageView>(R.id.iv_chip_individual)

        chipEquipa.setOnClickListener {
            if (filterType == "EQUIPA") return@setOnClickListener
            desmarcarTodos()
            it.isSelected = true
            ivEquipa.setImageResource(R.drawable.todos1)
            ivIndividual.setImageResource(R.drawable.meu)
            
            filterType = "EQUIPA"
            selectedUserId = null
            GlpiConfig.currentAgendaFilter = "EQUIPA"
            
            AlertHelper.exibirAlertaPremium(this, "Modo: Vista Geral")
            
            // 🔥 REATIVIDADE HÍBRIDA 🔥
            // Carregamos a cache para resposta instantânea na UI
            loadAgendaFromCache()    
            renderCalendar()
            carregarDadosAgenda()    
            
            // Mas também disparamos um refresh de rede para garantir que os dados estão atualizados
            carregarMensalidade()
        }
 
        chipIndividual.setOnClickListener {
            if (filterType == "INDIVIDUAL") return@setOnClickListener
            desmarcarTodos()
            it.isSelected = true
            ivIndividual.setImageResource(R.drawable.meu1)
            ivEquipa.setImageResource(R.drawable.todos)
            
            filterType = "INDIVIDUAL"
            selectedUserId = PreferenceManager.getUserId(this)
            GlpiConfig.currentAgendaFilter = "INDIVIDUAL"
            
            AlertHelper.exibirAlertaPremium(this, "Modo: Vista Pessoal")
            
            // 🔥 REATIVIDADE HÍBRIDA 🔥
            loadAgendaFromCache()
            renderCalendar()
            carregarDadosAgenda()
            
            // Refresh de rede automático
            carregarMensalidade()
        }
 

        // Estado inicial UI baseada no filtro guardado
        desmarcarTodos()
        if (filterType == "EQUIPA") {
            chipEquipa.isSelected = true
            ivEquipa.setImageResource(R.drawable.todos1)
            ivIndividual.setImageResource(R.drawable.meu)
        } else {
            chipIndividual.isSelected = true
            ivIndividual.setImageResource(R.drawable.meu1)
            ivEquipa.setImageResource(R.drawable.todos)
        }
    }

    private fun desmarcarTodos() {
        findViewById<View>(R.id.chip_equipa).isSelected = false
        findViewById<View>(R.id.chip_individual).isSelected = false
    }

    private fun carregarDadosAgenda() {
        val ticketsASeremFiltrados = ticketsDoMesCache
        
        if (ticketsASeremFiltrados == null) {
            // Se não temos o mês carregado, carregamos primeiro
            carregarMensalidade()
            return
        }

        val sdfApi = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateStr = selectedDate // Já está em yyyy-MM-dd

        val ticketsDoDia = ticketsASeremFiltrados.filter { ticket ->
            val statusRaw = ticket["12"]?.toString() ?: ""
            val statusId = extractId(statusRaw)
            
            val isInactive = statusId == "5" || statusId == "6" || 
                            statusRaw.contains("Solucionado", ignoreCase = true) || 
                            statusRaw.contains("Fechado", ignoreCase = true)
            
            if (isInactive) return@filter false

            val dataLimite = ticket["18"]?.toString() ?: ""
            val dataTTR = ticket["151"]?.toString() ?: ""
            val dataTTO = ticket["158"]?.toString() ?: ""
            val dataCriacao = ticket["15"]?.toString() ?: ""

            val dateParts = dateStr.split("-")
            val dateStrInvertida = "${dateParts[2]}/${dateParts[1]}/${dateParts[0]}"
            val dateStrInvertidaTraco = "${dateParts[2]}-${dateParts[1]}-${dateParts[0]}"

            fun matches(d: String) = d.contains(dateStr) || d.contains(dateStrInvertida) || d.contains(dateStrInvertidaTraco)

            matches(dataLimite) || matches(dataTTR) || matches(dataTTO) || matches(dataCriacao)
        }

        if (ticketsDoDia.isEmpty()) {
            rvTickets.adapter = TicketAdapter(emptyList(), showAgendaMode = true)
            tvSemTarefas.visibility = View.VISIBLE
            rvTickets.visibility = View.GONE
        } else {
            tvSemTarefas.visibility = View.GONE
            rvTickets.visibility = View.VISIBLE
            rvTickets.adapter = TicketAdapter(ticketsDoDia, showAgendaMode = true)
            rvTickets.scheduleLayoutAnimation()
        }
    }

    private fun isExpired(dueDateStr: String?, status: String?): Boolean {
        if (dueDateStr.isNullOrEmpty() || status == "5" || status == "6") return false
        return try {
            val sdfFull = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val dueDate = sdfFull.parse(dueDateStr)
            dueDate?.before(Date()) ?: false
        } catch (e: Exception) {
            try {
                val sdfShort = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val dueDate = sdfShort.parse(dueDateStr)
                dueDate?.before(Date()) ?: false
            } catch (e2: Exception) {
                false
            }
        }
    }

    private fun extractId(v: Any?): String {
        if (v == null) return ""
        if (v is Map<*, *>) return v["id"]?.toString()?.substringBefore(".") ?: ""
        if (v is List<*>) return (v.firstOrNull() as? Map<*, *>)?.get("id")?.toString()?.substringBefore(".") ?: ""
        val s = v.toString()
        return if (s.contains(".")) s.substringBefore(".") else s
    }

    private fun configurarNavegacao() {
        val navView = findViewById<BottomNavigationView>(R.id.bottom_navigation_agenda) ?: return
        navView.selectedItemId = R.id.nav_agenda
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
            if (item.itemId == R.id.nav_agenda) return@setOnItemSelectedListener true
            val intent = when (item.itemId) {
                R.id.nav_tickets -> Intent(this, DashboardActivity::class.java)
                R.id.nav_inventario -> Intent(this, InventoryActivity::class.java)
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
        val intent = Intent(this, DashboardActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        finish()
    }
}
