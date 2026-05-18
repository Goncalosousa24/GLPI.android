package com.example.glpimobile

import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.CalendarView
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class CalendarActivity : AppCompatActivity() {

    private var rvTickets: RecyclerView? = null
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var tvDataSelecionada: TextView
    private lateinit var calendarView: CalendarView
    private var dataAtualPesquisa: String = ""

    private val TOKEN_SESSAO = GlpiConfig.SESSION_TOKEN
    private val TOKEN_APP = GlpiConfig.APP_TOKEN

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🔥 IDIOMA EM PORTUGUÊS 🔥
        val locale = Locale("pt", "PT")
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)

        setContentView(R.layout.activity_calendar)

        val azul = ContextCompat.getColor(this, R.color.azul_glpi)
        // 🔥 ADICIONEI A COR BRANCA AQUI 🔥
        val corBranca = ContextCompat.getColor(this, android.R.color.white)

        // 🔥 SETA PINTADA A BRANCO 🔥
        val lottieSeta = findViewById<LottieAnimationView>(R.id.lottie_seta)
        lottieSeta?.addValueCallback(KeyPath("**"), LottieProperty.COLOR_FILTER) {
            PorterDuffColorFilter(corBranca, PorterDuff.Mode.SRC_ATOP)
        }

        tvDataSelecionada = findViewById(R.id.tv_data_selecionada)
        calendarView = findViewById(R.id.calendarView)
        rvTickets = findViewById(R.id.rv_tickets_calendario)
        rvTickets?.layoutManager = LinearLayoutManager(this)

        swipeRefresh = findViewById(R.id.swipeRefresh)
        swipeRefresh.setColorSchemeColors(azul)
        swipeRefresh.setOnRefreshListener { carregarTicketsDaData(dataAtualPesquisa) }

        // 🔥 Clique para Voltar 🔥
        findViewById<FrameLayout>(R.id.btn_voltar).setOnClickListener {
            voltarParaMenu()
        }

        val hojeApi = SimpleDateFormat("yyyy-MM-dd", locale).format(Date())
        val hojeExibicao = SimpleDateFormat("dd/MM/yyyy", locale).format(Date())

        tvDataSelecionada.text = "Tickets abertos a $hojeExibicao"
        dataAtualPesquisa = hojeApi

        carregarTicketsDaData(dataAtualPesquisa)

        calendarView.setOnDateChangeListener { _, year, month, day ->
            val dataApi = String.format("%04d-%02d-%02d", year, month + 1, day)
            val dataExibicao = String.format("%02d/%02d/%04d", day, month + 1, year)

            tvDataSelecionada.text = "Tickets abertos a $dataExibicao"
            dataAtualPesquisa = dataApi
            carregarTicketsDaData(dataAtualPesquisa)
        }
    }

    private fun carregarTicketsDaData(data: String) {
        swipeRefresh.isRefreshing = true

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = GlpiRetrofit.api.getTicketsPorData(
                    sessionToken = TOKEN_SESSAO,
                    appToken = TOKEN_APP,
                    date = data
                )

                withContext(Dispatchers.Main) {
                    val lista = if (response.isSuccessful) response.body()?.data ?: emptyList() else emptyList()
                    if (lista.isEmpty()) {
                        rvTickets?.visibility = View.GONE
                        Toast.makeText(this@CalendarActivity, "Nenhum ticket aberto neste dia.", Toast.LENGTH_SHORT).show()
                    } else {
                        rvTickets?.visibility = View.VISIBLE
                        rvTickets?.adapter = TicketAdapter(lista)
                        rvTickets?.scheduleLayoutAnimation()
                    }
                    swipeRefresh.isRefreshing = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    swipeRefresh.isRefreshing = false
                    mostrarAlertaInfo("Aviso", "Não foi possível carregar os tickets deste dia.")
                }
            }
        }
    }

    private fun mostrarAlertaInfo(titulo: String, mensagem: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_custom_info, null)
        val dialog = AlertDialog.Builder(this, R.style.CustomAlertDialog).setView(dialogView).setCancelable(false).create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialogView.findViewById<TextView>(R.id.tv_titulo_dialog).text = titulo
        dialogView.findViewById<TextView>(R.id.tv_mensagem_dialog).text = mensagem
        dialogView.findViewById<Button>(R.id.btn_dialog_ok).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    // 🔥 Centraliza o retorno ao Dashboard 🔥
    private fun voltarParaMenu() {
        val intent = Intent(this, DashboardActivity::class.java)
        intent.putExtra("ABRIR_MENU", true)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        startActivity(intent)
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        finish()
    }

    // Quando clica no botão físico de voltar do telemóvel
    override fun onBackPressed() {
        voltarParaMenu()
    }
}