package com.example.glpimobile

import android.app.TimePickerDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AssetReservationActivity : AppCompatActivity() {

    // Models for our screen
    data class ExistingReservation(
        val id: Int,
        val beginDate: Date,
        val endDate: Date,
        val user: String,
        val comment: String
    )

    data class DayReservation(
        val date: Date,
        var startHour: Int = 9,
        var startMinute: Int = 0,
        var endHour: Int = 18,
        var endMinute: Int = 0
    )

    data class CalendarDay(
        val date: Date,
        val isCurrentMonth: Boolean,
        var isSelected: Boolean = false,
        var existingReservation: ExistingReservation? = null
    )

    // UI Bindings
    private lateinit var tvAssetNameHeader: TextView
    private lateinit var tvAssetSerialHeader: TextView
    private lateinit var ivAssetTypeIcon: ImageView
    private lateinit var btnPrevMonth: View
    private lateinit var btnNextMonth: View
    private lateinit var tvCalendarMonth: TextView
    private lateinit var rvCalendarDays: RecyclerView
    private lateinit var layoutTimePickersSection: View
    private lateinit var rvDayTimePickers: RecyclerView
    private lateinit var layoutAssetResPor: View
    private lateinit var tvAssetResPorValor: TextView
    private lateinit var etAssetResComentarios: EditText
    private lateinit var pbAssetRes: View
    private lateinit var btnConfirmarAssetReservas: Button
    private lateinit var btnVoltarAssetRes: View

    // State Variables
    private var assetName: String = ""
    private var assetSerial: String = ""
    private var assetType: String = ""
    private var itemsId: Int = -1
    private var reservationItemsId: Int = -1

    private val currentCalendar = Calendar.getInstance()
    private val selectedDays = mutableListOf<Calendar>()
    private val dayReservations = mutableListOf<DayReservation>()
    private val existingReservations = mutableListOf<ExistingReservation>()

    private var selectedUserId: Int = -1
    private var selectedUserName: String = ""

    private lateinit var tvResDataTitulo: TextView
    private lateinit var tvSemReservas: TextView
    private lateinit var rvReservasDoDia: RecyclerView
    private lateinit var existingReservationsAdapter: ExistingReservationsAdapter
    private val selectedDateCalendar = Calendar.getInstance()

    private lateinit var calendarDaysAdapter: CalendarDaysAdapter
    private lateinit var dayTimePickersAdapter: DayTimePickersAdapter

    private val sdfMonthYear = SimpleDateFormat("MMMM yyyy", Locale("pt", "PT"))
    private val sdfDateHeader = SimpleDateFormat("dd/MM", Locale("pt", "PT"))
    private val sdfTimeFormat = SimpleDateFormat("HH:mm", Locale("pt", "PT"))

    override fun onCreate(savedInstanceState: Bundle?) {
        // Enforce localized PT date/time formatting rules
        Locale.setDefault(Locale("pt", "PT"))
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_asset_reservation)

        // Read intent extras
        assetName = intent.getStringExtra("EXTRA_NAME") ?: "Equipamento"
        assetSerial = intent.getStringExtra("EXTRA_SERIAL") ?: ""
        assetType = intent.getStringExtra("EXTRA_TYPE") ?: "Item"
        itemsId = intent.getIntExtra("EXTRA_ITEMS_ID", -1)
        reservationItemsId = intent.getIntExtra("EXTRA_RESERVATION_ITEMS_ID", -1)

        selectedUserId = PreferenceManager.getUserId(this)
        val cachedFullName = PreferenceManager.getUserFullName(this)
        selectedUserName = if (cachedFullName.isNotBlank()) cachedFullName else PreferenceManager.getUserName(this)

        initViews()
        setupAssetCard()
        setupCalendar()
        setupTimePickers()
        setupListeners()
        
        carregarReservasExistentes()
        updateBottomButtonState()
    }

    private fun initViews() {
        tvAssetNameHeader = findViewById(R.id.tv_asset_name_header)
        tvAssetSerialHeader = findViewById(R.id.tv_asset_serial_header)
        ivAssetTypeIcon = findViewById(R.id.iv_asset_type_icon)
        btnPrevMonth = findViewById(R.id.btn_prev_month)
        btnNextMonth = findViewById(R.id.btn_next_month)
        tvCalendarMonth = findViewById(R.id.tv_calendar_month)
        rvCalendarDays = findViewById(R.id.rv_calendar_days)
        layoutTimePickersSection = findViewById(R.id.layout_time_pickers_section)
        rvDayTimePickers = findViewById(R.id.rv_day_time_pickers)
        layoutAssetResPor = findViewById(R.id.layout_asset_res_por)
        tvAssetResPorValor = findViewById(R.id.tv_asset_res_por_valor)
        etAssetResComentarios = findViewById(R.id.et_asset_res_comentarios)
        pbAssetRes = findViewById(R.id.pb_asset_res)
        (pbAssetRes as? LottieAnimationView)?.let { lottie ->
            lottie.addValueCallback(KeyPath("**"), LottieProperty.COLOR_FILTER) {
                PorterDuffColorFilter(android.graphics.Color.parseColor("#0047FF"), PorterDuff.Mode.SRC_ATOP)
            }
        }
        btnConfirmarAssetReservas = findViewById(R.id.btn_confirmar_asset_reservas)
        btnVoltarAssetRes = findViewById(R.id.btn_voltar_asset_res)

        tvResDataTitulo = findViewById(R.id.tv_res_data_titulo)
        tvSemReservas = findViewById(R.id.tv_sem_reservas)
        rvReservasDoDia = findViewById(R.id.rv_reservas_do_dia)

        val lottieSeta = findViewById<LottieAnimationView>(R.id.lottie_seta_asset_res)
        val corBranca = ContextCompat.getColor(this, android.R.color.white)
        lottieSeta?.addValueCallback(KeyPath("**"), LottieProperty.COLOR_FILTER) {
            PorterDuffColorFilter(corBranca, PorterDuff.Mode.SRC_ATOP)
        }

        tvAssetResPorValor.text = selectedUserName
    }

    private fun setupAssetCard() {
        tvAssetNameHeader.text = assetName.replace("-emprestimo", "", ignoreCase = true).uppercase()
        tvAssetSerialHeader.text = if (assetSerial.isBlank()) "Sem número de série" else assetSerial.uppercase()

        if (assetType.contains("computer", ignoreCase = true) || assetType.contains("computador", ignoreCase = true)) {
            ivAssetTypeIcon.setImageResource(R.drawable.ic_laptop)
        } else {
            ivAssetTypeIcon.setImageResource(R.drawable.ic_network)
        }
    }

    private fun setupCalendar() {
        // Enforce 1st day of month for current month calendar view
        currentCalendar.set(Calendar.DAY_OF_MONTH, 1)
        currentCalendar.set(Calendar.HOUR_OF_DAY, 0)
        currentCalendar.set(Calendar.MINUTE, 0)
        currentCalendar.set(Calendar.SECOND, 0)
        currentCalendar.set(Calendar.MILLISECOND, 0)

        tvCalendarMonth.text = sdfMonthYear.format(currentCalendar.time).uppercase()

        rvCalendarDays.layoutManager = GridLayoutManager(this, 7)
        calendarDaysAdapter = CalendarDaysAdapter(emptyList())
        rvCalendarDays.adapter = calendarDaysAdapter

        // Set up existing reservations RecyclerView
        existingReservationsAdapter = ExistingReservationsAdapter(emptyList())
        rvReservasDoDia.layoutManager = LinearLayoutManager(this)
        rvReservasDoDia.adapter = existingReservationsAdapter

        // Set initial viewed day to today
        selectedDateCalendar.time = Date()
        atualizarReservasDoDiaSelecionado()

        renderCalendar()
    }

    private fun setupTimePickers() {
        rvDayTimePickers.layoutManager = LinearLayoutManager(this)
        dayTimePickersAdapter = DayTimePickersAdapter()
        rvDayTimePickers.adapter = dayTimePickersAdapter
    }

    private fun setupListeners() {
        btnVoltarAssetRes.setOnClickListener {
            finish()
        }

        btnPrevMonth.setOnClickListener {
            currentCalendar.add(Calendar.MONTH, -1)
            tvCalendarMonth.text = sdfMonthYear.format(currentCalendar.time).uppercase()
            selectedDateCalendar.time = currentCalendar.time
            atualizarReservasDoDiaSelecionado()
            renderCalendar()
        }

        btnNextMonth.setOnClickListener {
            currentCalendar.add(Calendar.MONTH, 1)
            tvCalendarMonth.text = sdfMonthYear.format(currentCalendar.time).uppercase()
            selectedDateCalendar.time = currentCalendar.time
            atualizarReservasDoDiaSelecionado()
            renderCalendar()
        }

        layoutAssetResPor.setOnClickListener {
            mostrarDialogBuscaUsuario { nome, id ->
                selectedUserName = nome
                selectedUserId = id
                tvAssetResPorValor.text = nome
                updateBottomButtonState()
            }
        }

        tvCalendarMonth.setOnClickListener {
            mostrarDialogDebugReservas()
        }

        btnConfirmarAssetReservas.setOnClickListener {
            efetuarReservas()
        }
    }

    private fun mostrarDialogDebugReservas() {
        val sb = StringBuilder()
        sb.append("Total de reservas na memória: ${existingReservations.size}\n\n")
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        for ((index, res) in existingReservations.withIndex()) {
            sb.append("${index + 1}. POR: ${res.user}\n")
            sb.append("   INÍCIO: ${sdf.format(res.beginDate)}\n")
            sb.append("   FIM:    ${sdf.format(res.endDate)}\n")
            sb.append("   OBS:    ${res.comment}\n\n")
        }

        val textView = TextView(this).apply {
            text = sb.toString()
            setPadding(48, 48, 48, 48)
            textSize = 14f
            setTextColor(getColor(R.color.texto_principal))
        }

        val scrollView = android.widget.ScrollView(this).apply {
            addView(textView)
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("DEBUG - RESERVAS NA MEMÓRIA")
            .setView(scrollView)
            .setPositiveButton("FECHAR", null)
            .show()
    }

    private fun renderCalendar() {
        val days = mutableListOf<CalendarDay>()
        val calendarCopy = currentCalendar.clone() as Calendar
        calendarCopy.set(Calendar.HOUR_OF_DAY, 0)
        calendarCopy.set(Calendar.MINUTE, 0)
        calendarCopy.set(Calendar.SECOND, 0)
        calendarCopy.set(Calendar.MILLISECOND, 0)
        calendarCopy.set(Calendar.DAY_OF_MONTH, 1)

        // Monday-first offset adjustment:
        // Sunday is 1, Monday is 2, ..., Saturday is 7
        var firstWeekday = calendarCopy.get(Calendar.DAY_OF_WEEK) - 2
        if (firstWeekday < 0) firstWeekday += 7 // Monday is 0, Sunday is 6

        calendarCopy.add(Calendar.DAY_OF_MONTH, -firstWeekday)

        // Generate exactly 42 days (6 weeks)
        for (i in 0 until 42) {
            val date = calendarCopy.time
            val isCurrentMonth = calendarCopy.get(Calendar.MONTH) == currentCalendar.get(Calendar.MONTH)
            
            // Check if occupied/reserved (inclusive of multi-day ranges)
            val existing = existingReservations.firstOrNull { isDateWithinReservation(date, it) }
            
            // Check if selected
            val isSelected = selectedDays.any { isSameDay(it.time, date) }

            days.add(CalendarDay(
                date = date,
                isCurrentMonth = isCurrentMonth,
                isSelected = isSelected,
                existingReservation = existing
            ))
            calendarCopy.add(Calendar.DAY_OF_MONTH, 1)
        }
        calendarDaysAdapter.updateDays(days)
    }

    private fun isDateWithinReservation(dayDate: Date, reservation: ExistingReservation): Boolean {
        // Define o início do dia (00:00:00.000) no fuso horário local
        val dayStart = Calendar.getInstance().apply {
            time = dayDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        // Define o fim do dia (23:59:59.999) no fuso horário local
        val dayEnd = Calendar.getInstance().apply {
            time = dayDate
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.time

        // Um dia está ocupado se a reserva inicia antes do fim do dia E termina após o início do dia
        return reservation.beginDate.before(dayEnd) && reservation.endDate.after(dayStart)
    }

    private fun isSameDay(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = date1 }
        val cal2 = Calendar.getInstance().apply { time = date2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
                cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH)
    }

    private fun toggleDaySelection(day: Calendar) {
        val matchedIdx = selectedDays.indexOfFirst {
            it.get(Calendar.YEAR) == day.get(Calendar.YEAR) &&
            it.get(Calendar.MONTH) == day.get(Calendar.MONTH) &&
            it.get(Calendar.DAY_OF_MONTH) == day.get(Calendar.DAY_OF_MONTH)
        }

        if (matchedIdx >= 0) {
            selectedDays.removeAt(matchedIdx)
            val resIdx = dayReservations.indexOfFirst {
                val c = Calendar.getInstance().apply { time = it.date }
                c.get(Calendar.YEAR) == day.get(Calendar.YEAR) &&
                c.get(Calendar.MONTH) == day.get(Calendar.MONTH) &&
                c.get(Calendar.DAY_OF_MONTH) == day.get(Calendar.DAY_OF_MONTH)
            }
            if (resIdx >= 0) {
                dayReservations.removeAt(resIdx)
            }
        } else {
            selectedDays.add(day)
            dayReservations.add(DayReservation(day.time))
            // Sort selected reservations by date chronologically
            dayReservations.sortBy { it.date }
        }

        renderCalendar()
        dayTimePickersAdapter.notifyDataSetChanged()

        layoutTimePickersSection.visibility = if (dayReservations.isEmpty()) View.GONE else View.VISIBLE
        updateBottomButtonState()
    }

    private fun updateBottomButtonState() {
        if (selectedDays.isEmpty()) {
            btnConfirmarAssetReservas.text = "SELECIONE UM DIA"
            btnConfirmarAssetReservas.isEnabled = false
        } else if (selectedUserName.isBlank()) {
            btnConfirmarAssetReservas.text = "FALTA O NOME"
            btnConfirmarAssetReservas.isEnabled = false
        } else {
            btnConfirmarAssetReservas.text = "CONFIRMAR ${selectedDays.size} RESERVAS"
            btnConfirmarAssetReservas.isEnabled = true
        }
    }

    private fun carregarReservasExistentes() {
        android.util.Log.d("GLPI_RES_ACT", "carregarReservasExistentes() iniciada com reservationItemsId: $reservationItemsId")
        if (reservationItemsId == -1) {
            android.util.Log.e("GLPI_RES_ACT", "reservationItemsId eh -1. Abortando carregamento da API.")
            android.widget.Toast.makeText(this, "Erro: ID do dispositivo inválido (-1)", android.widget.Toast.LENGTH_LONG).show()
            return
        }

        val sessionToken = GlpiConfig.SESSION_TOKEN
        val appToken = GlpiConfig.APP_TOKEN

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val list = mutableListOf<ExistingReservation>()
                android.util.Log.d("GLPI_RES_ACT", "A enviar pedido getReservationsForItem para ID: $reservationItemsId")

                // Fetch real reservations from API
                val response = GlpiRetrofit.api.getReservationsForItem(reservationItemsId, sessionToken, appToken)
                android.util.Log.d("GLPI_RES_ACT", "Resposta HTTP da API: ${response.code()}")

                if (response.isSuccessful && response.body() != null) {
                    val apiDataList = response.body()!!
                    android.util.Log.d("GLPI_RES_ACT", "Reservas cruas encontradas: ${apiDataList.size} items.")
                    val sdfAPI = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

                    for (apiRes in apiDataList) {
                        val resId = apiRes["id"]?.toString()?.toDoubleOrNull()?.toInt() ?: -1
                        val beginStr = apiRes["begin"]?.toString() ?: ""
                        val endStr = apiRes["end"]?.toString() ?: ""
                        val commentStr = apiRes["comment"]?.toString() ?: ""
                        android.util.Log.d("GLPI_RES_ACT", "Processando reserva crua: id=$resId, begin=$beginStr, end=$endStr, coment=$commentStr")
                        
                        var usersName = "Utilizador GLPI"
                        val usersIdObj = apiRes["users_id"]
                        if (usersIdObj != null) {
                            when (usersIdObj) {
                                is List<*> -> {
                                    if (usersIdObj.size > 1) {
                                        usersName = usersIdObj[1]?.toString() ?: "Utilizador GLPI"
                                    }
                                }
                                is Map<*, *> -> {
                                    usersName = usersIdObj["name"]?.toString()
                                        ?: usersIdObj["completename"]?.toString()
                                        ?: "Utilizador GLPI"
                                }
                                else -> {
                                    val strVal = usersIdObj.toString().trim()
                                    if (strVal.toDoubleOrNull() != null || strVal.all { it.isDigit() }) {
                                        usersName = apiRes["users_name"]?.toString()
                                            ?: apiRes["user_name"]?.toString()
                                            ?: "Utilizador #$strVal"
                                    } else {
                                        usersName = strVal
                                    }
                                }
                            }
                        }
                        
                        // Formatar o nome do utilizador com as letras maiúsculas e sem pontos
                        usersName = formatarStringNome(usersName) ?: usersName

                        if (beginStr.isNotEmpty() && endStr.isNotEmpty()) {
                            try {
                                val begDate = sdfAPI.parse(beginStr)
                                val endDate = sdfAPI.parse(endStr)
                                if (begDate != null && endDate != null) {
                                    list.add(ExistingReservation(resId, begDate, endDate, usersName, commentStr))
                                    android.util.Log.d("GLPI_RES_ACT", "Reserva adicionada com sucesso: id=$resId, $begDate ate $endDate por $usersName")
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("GLPI_RES_ACT", "Erro parse datas '$beginStr' / '$endStr'", e)
                            }
                        }
                    }
                } else {
                    android.util.Log.e("GLPI_RES_ACT", "API de reservas nao retornou sucesso ou body nulo. Codigo: ${response.code()}")
                }

                withContext(Dispatchers.Main) {
                    existingReservations.clear()
                    existingReservations.addAll(list)
                    android.util.Log.d("GLPI_RES_ACT", "Lista final existingReservations atualizada. Total: ${existingReservations.size} reservas.")
                    renderCalendar()
                    atualizarReservasDoDiaSelecionado()
                }
            } catch (e: Exception) {
                android.util.Log.e("GLPI_RES_ACT", "Excepcao fatal ao carregar reservas da API. Usando mocks como fallback.", e)
                // Fallback mock reservations if API is offline
                val fallbackList = mutableListOf<ExistingReservation>()
                val cal1 = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, 2)
                    set(Calendar.HOUR_OF_DAY, 9)
                    set(Calendar.MINUTE, 0)
                }
                fallbackList.add(ExistingReservation(99901, cal1.time, cal1.apply { set(Calendar.HOUR_OF_DAY, 13) }.time, "Maria Silva", "Necessário para inventário local."))

                val cal2 = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, 5)
                    set(Calendar.HOUR_OF_DAY, 14)
                    set(Calendar.MINUTE, 30)
                }
                fallbackList.add(ExistingReservation(99902, cal2.time, cal2.apply { set(Calendar.HOUR_OF_DAY, 18) }.time, "João Mendes", "Apresentação na sala de reuniões."))

                withContext(Dispatchers.Main) {
                    existingReservations.clear()
                    existingReservations.addAll(fallbackList)
                    android.util.Log.d("GLPI_RES_ACT", "Lista fallback Mocks atualizada. Total: ${existingReservations.size}")
                    renderCalendar()
                    atualizarReservasDoDiaSelecionado()
                    
                    android.widget.Toast.makeText(
                        this@AssetReservationActivity, 
                        "Ligação offline. Modo demonstração ativado.", 
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
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

    private fun mostrarDialogDetalhesExistente(res: ExistingReservation) {
        val builder = AlertDialog.Builder(this, R.style.CustomAlertDialog)
        val dialogView = layoutInflater.inflate(R.layout.dialog_reserva_detalhes_eliminar, null)
        builder.setView(dialogView)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val tvHeader = dialogView.findViewById<TextView>(R.id.tv_dialog_header)
        val tvContent = dialogView.findViewById<TextView>(R.id.tv_dialog_content)
        val btnFechar = dialogView.findViewById<Button>(R.id.btn_dialog_fechar)
        val btnEliminar = dialogView.findViewById<Button>(R.id.btn_dialog_eliminar)

        val sdfDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault())

        tvHeader.text = "Visualizar Reserva"

        val sb = java.lang.StringBuilder()
        sb.append("DATA:\n${sdfDate.format(res.beginDate)}\n\n")
        sb.append("HORÁRIO:\n${sdfTime.format(res.beginDate)} - ${sdfTime.format(res.endDate)}\n\n")
        sb.append("RESERVADO POR:\n${res.user}\n\n")
        if (res.comment.isNotBlank()) {
            sb.append("OBSERVAÇÕES:\n${res.comment}")
        } else {
            sb.append("OBSERVAÇÕES:\nSem observações.")
        }

        tvContent.text = sb.toString()
        btnFechar.setOnClickListener { dialog.dismiss() }
        
        btnEliminar.setOnClickListener {
            dialog.dismiss()
            // Alert dialog de confirmação com visual premium antes de eliminar a reserva
            val confirmDialog = AlertDialog.Builder(this, R.style.CustomAlertDialog)
                .setTitle("Eliminar Reserva?")
                .setMessage("Tem a certeza que deseja eliminar esta reserva definitivamente?")
                .setPositiveButton("ELIMINAR") { _, _ ->
                    eliminarReserva(res.id)
                }
                .setNegativeButton("CANCELAR", null)
                .create()
            
            confirmDialog.setOnShowListener {
                val colorBlue = android.graphics.Color.parseColor("#0047FF")
                confirmDialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(colorBlue)
                confirmDialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(colorBlue)
            }
            confirmDialog.show()
        }

        dialog.show()
    }

    private fun eliminarReserva(resId: Int) {
        if (resId == -1 || resId >= 99900) {
            existingReservations.removeAll { it.id == resId }
            renderCalendar()
            atualizarReservasDoDiaSelecionado()
            setResult(RESULT_OK)
            AlertHelper.exibirAlertaPremium(this, "Reserva eliminada com sucesso!", false)
            return
        }

        pbAssetRes.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val sessionToken = GlpiConfig.SESSION_TOKEN
                val appToken = GlpiConfig.APP_TOKEN
                val response = GlpiRetrofit.api.deleteReservation(resId, sessionToken, appToken)
                withContext(Dispatchers.Main) {
                    pbAssetRes.visibility = View.GONE
                    if (response.isSuccessful) {
                        setResult(RESULT_OK)
                        AlertHelper.exibirAlertaPremium(this@AssetReservationActivity, "Reserva eliminada com sucesso!", false)
                        carregarReservasExistentes()
                    } else {
                        AlertHelper.exibirAlertaPremium(this@AssetReservationActivity, "Erro ao eliminar reserva", true)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    pbAssetRes.visibility = View.GONE
                    AlertHelper.exibirAlertaPremium(this@AssetReservationActivity, "Falha de ligação", true)
                }
            }
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
        val sessionToken = GlpiConfig.SESSION_TOKEN
        val appToken = GlpiConfig.APP_TOKEN

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

                val response = GlpiRetrofit.api.pesquisarUsuarios(sessionToken, appToken, criteria)
                withContext(Dispatchers.Main) {
                    pb.visibility = View.GONE
                    if (response.isSuccessful) {
                        val usersList = response.body()?.data ?: emptyList()
                        adapter.updateList(usersList)
                        emptyView.visibility = if (usersList.isEmpty()) View.VISIBLE else View.GONE
                    }
                }
            } catch (e: java.lang.Exception) {
                withContext(Dispatchers.Main) {
                    pb.visibility = View.GONE
                }
            }
        }
    }

    private fun efetuarReservas() {
        if (dayReservations.isEmpty()) return

        pbAssetRes.visibility = View.VISIBLE
        btnConfirmarAssetReservas.isEnabled = false

        val sessionToken = GlpiConfig.SESSION_TOKEN
        val appToken = GlpiConfig.APP_TOKEN
        val comments = etAssetResComentarios.text.toString()

        CoroutineScope(Dispatchers.IO).launch {
            var successCount = 0
            var errorCount = 0
            val sdfAPI = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

            for (res in dayReservations) {
                val calBegin = Calendar.getInstance().apply {
                    time = res.date
                    set(Calendar.HOUR_OF_DAY, res.startHour)
                    set(Calendar.MINUTE, res.startMinute)
                    set(Calendar.SECOND, 0)
                }

                val calEnd = Calendar.getInstance().apply {
                    time = res.date
                    set(Calendar.HOUR_OF_DAY, res.endHour)
                    set(Calendar.MINUTE, res.endMinute)
                    set(Calendar.SECOND, 0)
                }

                val beginStr = sdfAPI.format(calBegin.time)
                val endStr = sdfAPI.format(calEnd.time)

                val reqPayload = mutableMapOf<String, Any>(
                    "input" to mutableMapOf(
                        "reservationitems_id" to reservationItemsId,
                        "begin" to beginStr,
                        "end" to endStr,
                        "comment" to if (comments.isBlank()) "Reserva efetuada via App GLPIMobile" else comments,
                        "users_id" to selectedUserId
                    )
                )

                try {
                    val response = GlpiRetrofit.api.createReservation(sessionToken, appToken, reqPayload)
                    if (response.isSuccessful) {
                        successCount++
                    } else {
                        errorCount++
                    }
                } catch (e: Exception) {
                    errorCount++
                }
            }

            withContext(Dispatchers.Main) {
                pbAssetRes.visibility = View.GONE
                btnConfirmarAssetReservas.isEnabled = true

                if (successCount > 0) {
                    AlertHelper.exibirAlertaPremium(this@AssetReservationActivity, "Reserva efetuada com sucesso!", false)
                    Handler(Looper.getMainLooper()).postDelayed({
                        setResult(RESULT_OK)
                        finish()
                    }, 1500)
                } else {
                    AlertHelper.exibirAlertaPremium(this@AssetReservationActivity, "Erro: Não foi possível efetuar reservas", true)
                }
            }
        }
    }

    private fun atualizarReservasDoDiaSelecionado() {
        val dayDate = selectedDateCalendar.time
        val resDoDia = existingReservations.filter { isDateWithinReservation(dayDate, it) }

        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        tvResDataTitulo.text = "RESERVAS DE ${sdf.format(dayDate)}:"

        if (resDoDia.isEmpty()) {
            tvSemReservas.visibility = View.VISIBLE
            rvReservasDoDia.visibility = View.GONE
            existingReservationsAdapter.updateList(emptyList())
        } else {
            tvSemReservas.visibility = View.GONE
            rvReservasDoDia.visibility = View.VISIBLE
            existingReservationsAdapter.updateList(resDoDia)
        }
    }

    private inner class ExistingReservationsAdapter(private var list: List<ExistingReservation>) :
        RecyclerView.Adapter<ExistingReservationsAdapter.ResViewHolder>() {

        inner class ResViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvUserName: TextView = view.findViewById(R.id.tv_res_user_name)
            val tvTimeBadge: TextView = view.findViewById(R.id.tv_res_time_badge)
            val tvComment: TextView = view.findViewById(R.id.tv_res_comment)
            val clCommentContainer: View = view.findViewById(R.id.cl_comment_container)
        }

        fun updateList(newList: List<ExistingReservation>) {
            list = newList
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_reserva_dia, parent, false)
            return ResViewHolder(view)
        }

        override fun getItemCount(): Int = list.size

        override fun onBindViewHolder(holder: ResViewHolder, position: Int) {
            val res = list[position]
            holder.tvUserName.text = res.user.uppercase()

            val sdfHour = SimpleDateFormat("HH:mm", Locale.getDefault())
            val timeRange = "${sdfHour.format(res.beginDate)} - ${sdfHour.format(res.endDate)}"
            holder.tvTimeBadge.text = timeRange

            if (res.comment.isBlank()) {
                holder.clCommentContainer.visibility = View.GONE
            } else {
                holder.clCommentContainer.visibility = View.VISIBLE
                holder.tvComment.text = res.comment
            }

            holder.itemView.setOnClickListener {
                mostrarDialogDetalhesExistente(res)
            }
        }
    }

    // --- Adapter for Calendar Days Grid ---
    private inner class CalendarDaysAdapter(private var days: List<CalendarDay>) :
        RecyclerView.Adapter<CalendarDaysAdapter.DayViewHolder>() {

        inner class DayViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvDayNumber: TextView = view.findViewById(R.id.tv_day_number)
            val vSelectionCircle: View = view.findViewById(R.id.v_selection_circle)
            val vReservedCircle: View = view.findViewById(R.id.v_reserved_circle)
            val vDot: View = view.findViewById(R.id.v_has_tickets_dot)
        }

        fun updateDays(newDays: List<CalendarDay>) {
            days = newDays
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_calendar_day, parent, false)
            return DayViewHolder(view)
        }

        override fun getItemCount(): Int = days.size

        override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
            val day = days[position]
            val cal = Calendar.getInstance().apply { time = day.date }
            holder.tvDayNumber.text = cal.get(Calendar.DAY_OF_MONTH).toString()

            // Estilo conforme o mês (dia desvanecido se for do mês adjacente)
            if (!day.isCurrentMonth) {
                holder.tvDayNumber.alpha = 0.3f
                holder.vDot.alpha = 0.3f
            } else {
                holder.tvDayNumber.alpha = 1.0f
                holder.vDot.alpha = 1.0f
            }

            val existing = day.existingReservation
            val isSelected = day.isSelected

            // Reset visual states
            holder.vSelectionCircle.visibility = View.INVISIBLE
            holder.vReservedCircle.visibility = View.INVISIBLE
            holder.vDot.visibility = View.INVISIBLE
            holder.tvDayNumber.setTypeface(null, android.graphics.Typeface.NORMAL)

            when {
                existing != null -> {
                    // Dia ocupado / reservado no GLPI (Círculo Cinzento)
                    holder.vReservedCircle.visibility = View.VISIBLE
                    holder.tvDayNumber.setTextColor(getColor(R.color.texto_principal))
                    holder.tvDayNumber.setTypeface(null, android.graphics.Typeface.BOLD)
                    
                    holder.itemView.setOnClickListener {
                        selectedDateCalendar.time = day.date
                        atualizarReservasDoDiaSelecionado()
                        renderCalendar()
                    }
                }
                isSelected -> {
                    // Dia selecionado pelo usuário para nova reserva
                    holder.vSelectionCircle.visibility = View.VISIBLE
                    holder.tvDayNumber.setTextColor(getColor(android.R.color.white))
                    holder.tvDayNumber.setTypeface(null, android.graphics.Typeface.BOLD)
                    
                    holder.itemView.setOnClickListener {
                        selectedDateCalendar.time = day.date
                        atualizarReservasDoDiaSelecionado()
                        toggleDaySelection(cal)
                    }
                }
                else -> {
                    // Dia livre normal
                    holder.tvDayNumber.setTextColor(getColor(R.color.texto_principal))
                    
                    holder.itemView.setOnClickListener {
                        selectedDateCalendar.time = day.date
                        atualizarReservasDoDiaSelecionado()
                        toggleDaySelection(cal)
                    }
                }
            }
        }
    }

    // --- Adapter for Time Pickers List ---
    private inner class DayTimePickersAdapter :
        RecyclerView.Adapter<DayTimePickersAdapter.PickerViewHolder>() {

        inner class PickerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvPickerDate: TextView = view.findViewById(R.id.tv_picker_date)
            val btnSelectStartTime: View = view.findViewById(R.id.btn_select_start_time)
            val tvStartTimeVal: TextView = view.findViewById(R.id.tv_start_time_val)
            val btnSelectEndTime: View = view.findViewById(R.id.btn_select_end_time)
            val tvEndTimeVal: TextView = view.findViewById(R.id.tv_end_time_val)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PickerViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_day_time_picker, parent, false)
            return PickerViewHolder(view)
        }

        override fun getItemCount(): Int = dayReservations.size

        override fun onBindViewHolder(holder: PickerViewHolder, position: Int) {
            val res = dayReservations[position]

            holder.tvPickerDate.text = sdfDateHeader.format(res.date)
            holder.tvStartTimeVal.text = String.format("%02d:%02d", res.startHour, res.startMinute)
            holder.tvEndTimeVal.text = String.format("%02d:%02d", res.endHour, res.endMinute)

            holder.btnSelectStartTime.setOnClickListener {
                TimePickerDialog(
                    this@AssetReservationActivity,
                    R.style.GLPI_PickerTheme,
                    { _, hour, minute ->
                        res.startHour = hour
                        res.startMinute = minute
                        holder.tvStartTimeVal.text = String.format("%02d:%02d", hour, minute)
                    },
                    res.startHour,
                    res.startMinute,
                    true
                ).show()
            }

            holder.btnSelectEndTime.setOnClickListener {
                TimePickerDialog(
                    this@AssetReservationActivity,
                    R.style.GLPI_PickerTheme,
                    { _, hour, minute ->
                        res.endHour = hour
                        res.endMinute = minute
                        holder.tvEndTimeVal.text = String.format("%02d:%02d", hour, minute)
                    },
                    res.endHour,
                    res.endMinute,
                    true
                ).show()
            }
        }
    }
}
