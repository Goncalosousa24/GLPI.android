package com.example.glpimobile

import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RadioButton
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
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.LegendEntry
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class MyStatisticsActivity : AppCompatActivity() {

    private val DURACAO_SINCRONIZADA = 1200
    private lateinit var lineChart: LineChart
    private lateinit var pieChart: PieChart
    private lateinit var btnToggleGrafico: TextView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var rvEstatisticas: RecyclerView
    private lateinit var tvPeriodoGrafico: TextView
    private lateinit var tvKpiMediaMensal: TextView
    private lateinit var tvKpiSaldo: TextView
    private lateinit var tvKpiMediaDiaria: TextView
    private lateinit var tvLabelMesPassado: TextView
    private lateinit var tvLabelMesAtual: TextView
    private lateinit var tvCountPassadoRes: TextView
    private lateinit var tvCountAtualRes: TextView
    private lateinit var tvCountHojeRes: TextView

    private var mesesSelecionados = 3
    private val USER_ID_ALVO get() = GlpiConfig.USER_ID
    private val TOKEN_SESSAO get() = GlpiConfig.SESSION_TOKEN
    private val TOKEN_APP get() = GlpiConfig.APP_TOKEN

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val locale = Locale("pt", "PT")
            Locale.setDefault(locale)
            val config = resources.configuration
            config.setLocale(locale)
            @Suppress("DEPRECATION")
            resources.updateConfiguration(config, resources.displayMetrics)

            setContentView(R.layout.activity_my_statistics)

            inicializarUI()
            configurarEstiloLineChart()
            configurarEstiloPieChart()
            configurarFiltros()

            swipeRefresh.setOnRefreshListener { carregarDadosIndividuais(mesesSelecionados) }
        } catch (e: Exception) {
            Toast.makeText(this, "ERRO: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun inicializarUI() {
        lineChart = findViewById(R.id.lineChart_estatisticas)
        pieChart = findViewById(R.id.pieChart_estatisticas)
        btnToggleGrafico = findViewById(R.id.btn_toggle_grafico)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        rvEstatisticas = findViewById(R.id.rv_lista_estatisticas)
        tvPeriodoGrafico = findViewById(R.id.tv_periodo_estatisticas)

        tvKpiMediaMensal = findViewById(R.id.tv_kpi_media_mensal)
        tvKpiSaldo = findViewById(R.id.tv_kpi_saldo)
        tvKpiMediaDiaria = findViewById(R.id.tv_kpi_media_diaria)

        tvPeriodoGrafico.visibility = View.GONE
        btnToggleGrafico.setTextColor(Color.WHITE)

        val corDinamica = ContextCompat.getColor(this, R.color.texto_principal)
        tvKpiMediaMensal.setTextColor(corDinamica)
        tvKpiSaldo.setTextColor(corDinamica)
        tvKpiMediaDiaria.setTextColor(corDinamica)

        tvLabelMesPassado = findViewById(R.id.tv_label_mes_passado)
        tvLabelMesAtual = findViewById(R.id.tv_label_mes_atual)
        tvCountPassadoRes = findViewById(R.id.tv_count_passado_res)
        tvCountAtualRes = findViewById(R.id.tv_count_atual_res)
        tvCountHojeRes = findViewById(R.id.tv_count_hoje_res)

        rvEstatisticas.layoutManager = LinearLayoutManager(this)
        findViewById<FrameLayout>(R.id.btn_voltar)?.setOnClickListener { finish() }

        val lottieSeta = findViewById<LottieAnimationView>(R.id.lottie_seta)
        lottieSeta?.addValueCallback(KeyPath("**"), LottieProperty.COLOR_FILTER) {
            PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP)
        }

        btnToggleGrafico.setOnClickListener {
            if (lineChart.visibility == View.VISIBLE) {
                lineChart.visibility = View.GONE
                pieChart.visibility = View.VISIBLE
                btnToggleGrafico.text = "VER EVOLUÇÃO"
                pieChart.legend.isEnabled = false
                pieChart.setExtraOffsets(0f, 0f, 0f, 0f)
                pieChart.animateY(DURACAO_SINCRONIZADA, Easing.EaseInOutQuart)
            } else {
                pieChart.visibility = View.GONE
                lineChart.visibility = View.VISIBLE
                btnToggleGrafico.text = "VER BALANÇO"
                lineChart.animateY(DURACAO_SINCRONIZADA, Easing.EaseInOutQuart)
            }
        }
    }

    private fun carregarDadosIndividuais(qtdMeses: Int) {
        swipeRefresh.isRefreshing = true
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val localePT = Locale("pt", "PT")
                val formatoApi = SimpleDateFormat("yyyy-MM", localePT)
                val formatoMesLongo = SimpleDateFormat("MMMM yyyy", localePT)
                val formatoMesCurto = SimpleDateFormat("MMM", localePT)

                val loginBusca = GlpiConfig.USER_NAME
                val nomeCompleto = GlpiConfig.USER_FULL_NAME

                // 🚀 BUSCAR TUDO DE UMA VEZ 🚀
                val response = GlpiRetrofit.api.getTodosMeusTicketsStats(
                    sessionToken = TOKEN_SESSAO, appToken = TOKEN_APP, 
                    userId1 = USER_ID_ALVO, userId2 = USER_ID_ALVO, userId3 = USER_ID_ALVO, 
                    userId4 = USER_ID_ALVO, userId5 = USER_ID_ALVO, userId6 = USER_ID_ALVO,
                    login1 = loginBusca, login2 = nomeCompleto.ifEmpty { loginBusca }
                )

                val allTickets = response.data ?: emptyList()
                val calHoje = Calendar.getInstance()
                val dataHojeStr = SimpleDateFormat("yyyy-MM-dd", localePT).format(calHoje.time)

                val resultados = mutableListOf<DadosMensais>()
                
                for (mesesAtras in qtdMeses downTo 1) {
                    val cal = Calendar.getInstance().apply { add(Calendar.MONTH, -mesesAtras) }
                    val mesChave = SimpleDateFormat("yyyy-MM", localePT).format(cal.time)
                    val nomeLongo = formatoMesLongo.format(cal.time).replaceFirstChar { it.uppercase() }
                    val nomeCurto = formatoMesCurto.format(cal.time).replaceFirstChar { it.uppercase() }

                    var contCriados = 0
                    var contResolvidos = 0

                    for (ticket in allTickets) {
                        val dataCriacao = ticket["15"]?.toString() ?: "" // Campo 15: Data Criação
                        val dataUpdate = ticket["19"]?.toString() ?: ""   // Campo 19: Data Atualização
                        val status = ticket["12"]?.toString()?.toDoubleOrNull()?.toInt() ?: 0 // Campo 12: Status

                        if (dataCriacao.startsWith(mesChave)) contCriados++
                        if ((status == 5 || status == 6) && dataUpdate.startsWith(mesChave)) contResolvidos++
                    }
                    resultados.add(DadosMensais(nomeLongo, nomeCurto, contCriados, contResolvidos, 0))
                }

                // Dados do mês atual para os KPI cards
                val mesAtualChave = SimpleDateFormat("yyyy-MM", localePT).format(calHoje.time)
                var resAtribuidosAtual = 0
                var resResolvidosAtual = 0
                var resResolvidosHoje = 0

                for (ticket in allTickets) {
                    val dataCriacao = ticket["15"]?.toString() ?: ""
                    val dataUpdate = ticket["19"]?.toString() ?: ""
                    val status = ticket["12"]?.toString()?.toDoubleOrNull()?.toInt() ?: 0

                    if (dataCriacao.startsWith(mesAtualChave)) resAtribuidosAtual++
                    if ((status == 5 || status == 6) && dataUpdate.startsWith(mesAtualChave)) resResolvidosAtual++
                    if ((status == 5 || status == 6) && dataUpdate.startsWith(dataHojeStr)) resResolvidosHoje++
                }

                // Mês passado é o último da lista (mesesAtras = 1)
                val dadosMesPassado = resultados.last()
                val entriesAtribuidos = mutableListOf<Entry>()
                val entriesResolvidos = mutableListOf<Entry>()
                val iniciaisMeses = mutableListOf<String>()

                var somaTotalAtribuidos = 0
                var somaTotalResolvidos = 0

                resultados.forEachIndexed { index, data ->
                    somaTotalAtribuidos += data.criados
                    somaTotalResolvidos += data.resolvidos
                    iniciaisMeses.add(data.nomeCurto.substring(0, 1).uppercase())
                    entriesAtribuidos.add(Entry(index.toFloat(), data.criados.toFloat()))
                    entriesResolvidos.add(Entry(index.toFloat(), data.resolvidos.toFloat()))
                }

                withContext(Dispatchers.Main) {
                    exibirLineChart(entriesAtribuidos, entriesResolvidos, iniciaisMeses, qtdMeses)
                    exibirPieChart(somaTotalAtribuidos, somaTotalResolvidos)
                    rvEstatisticas.adapter = MyStatisticsAdapter(resultados)
                    rvEstatisticas.scheduleLayoutAnimation()

                    animarNumero(tvKpiMediaMensal, (somaTotalAtribuidos.toFloat() / qtdMeses.toFloat()), false)
                    val saldoTotal = (somaTotalResolvidos - somaTotalAtribuidos).toFloat()
                    animarNumero(tvKpiSaldo, saldoTotal, true, showPlus = true)
                    animarNumero(tvKpiMediaDiaria, somaTotalAtribuidos.toFloat() / (qtdMeses * 30), false)

                    tvCountHojeRes.text = resResolvidosHoje.toString()
                    tvCountAtualRes.text = resResolvidosAtual.toString()
                    // Nota: tvMesAtribuidos removido pois não existe no layout atual para este card
                    
                    animarNumero(tvCountHojeRes, resResolvidosHoje.toFloat(), false)
                    animarNumero(tvCountAtualRes, resResolvidosAtual.toFloat(), false)

                    configurarComparacao(resAtribuidosAtual, resResolvidosAtual, dadosMesPassado.criados, dadosMesPassado.resolvidos, resResolvidosHoje)

                    swipeRefresh.isRefreshing = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    swipeRefresh.isRefreshing = false
                    Toast.makeText(this@MyStatisticsActivity, "Erro ao carregar os meus dados.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun exibirLineChart(entriesCriados: List<Entry>, entriesResolvidos: List<Entry>, iniciais: List<String>, qtdMeses: Int) {
        val corAzulBebe = Color.parseColor("#89CFF0")
        val corAzulGlpi = ContextCompat.getColor(this, R.color.azul_glpi)
        val corTextoLabels = ContextCompat.getColor(this, R.color.texto_principal)

        val maxVal = maxOf(entriesCriados.maxOfOrNull { it.y } ?: 0f, entriesResolvidos.maxOfOrNull { it.y } ?: 0f)
        lineChart.axisLeft.apply {
            axisMaximum = if (maxVal == 0f) 5f else maxVal * 1.5f
            axisMinimum = 0f
            textColor = corTextoLabels
        }
        lineChart.xAxis.apply {
            valueFormatter = IndexAxisValueFormatter(iniciais)
            labelCount = qtdMeses
            granularity = 1f
            textColor = corTextoLabels
        }
        val setCriados = LineDataSet(entriesCriados, "ATRIBUÍDOS").apply { color = corAzulBebe; setCircleColor(corAzulBebe); lineWidth = 4f; setDrawValues(false); setDrawCircles(true); circleRadius = 5f; isHighlightEnabled = false }
        val setResolvidos = LineDataSet(entriesResolvidos, "RESOLVIDOS").apply { color = corAzulGlpi; setCircleColor(corAzulGlpi); lineWidth = 4f; setDrawValues(false); setDrawCircles(true); circleRadius = 5f; isHighlightEnabled = false }

        lineChart.data = null
        lineChart.notifyDataSetChanged()
        lineChart.data = LineData(setCriados, setResolvidos)
        lineChart.animateY(DURACAO_SINCRONIZADA, Easing.EaseInOutQuart)
    }

    private fun configurarEstiloLineChart() {
        val corTextoLabels = ContextCompat.getColor(this, R.color.texto_principal)
        lineChart.description.isEnabled = false
        lineChart.extraBottomOffset = 25f
        lineChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        lineChart.axisRight.isEnabled = false
        lineChart.legend.apply {
            isEnabled = true
            textColor = corTextoLabels
            textSize = 12f
            form = Legend.LegendForm.CIRCLE
            horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
            verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
            orientation = Legend.LegendOrientation.HORIZONTAL
            xEntrySpace = 20f
        }
    }

    private fun exibirPieChart(totalAtribuidos: Int, totalResolvidos: Int) {
        val entries = ArrayList<PieEntry>().apply {
            add(PieEntry(totalAtribuidos.toFloat(), ""))
            add(PieEntry(totalResolvidos.toFloat(), ""))
        }

        val dataSet = PieDataSet(entries, "").apply {
            colors = listOf(Color.parseColor("#89CFF0"), ContextCompat.getColor(this@MyStatisticsActivity, R.color.azul_glpi))
            sliceSpace = 3f
            valueTextColor = Color.WHITE
            valueTextSize = 15f
            setValueTypeface(Typeface.DEFAULT_BOLD)
            valueFormatter = object : ValueFormatter() { override fun getFormattedValue(value: Float): String = value.toInt().toString() }
        }

        pieChart.data = null
        pieChart.notifyDataSetChanged()
        pieChart.data = PieData(dataSet)
        pieChart.apply {
            isDrawHoleEnabled = true
            setHoleColor(Color.TRANSPARENT)
            holeRadius = 50f
            transparentCircleRadius = 0f
            setDrawCenterText(false)
            description.isEnabled = false
            setDrawEntryLabels(false)
            animateY(DURACAO_SINCRONIZADA, Easing.EaseInOutQuart)
        }
    }

    private fun configurarEstiloPieChart() {
        pieChart.minOffset = 0f
        pieChart.setExtraOffsets(0f, 0f, 0f, 0f)
        pieChart.legend.isEnabled = false
    }

    private fun configurarFiltros() {
        val btn3Meses = findViewById<View>(R.id.filter_3_meses)
        val btn6Meses = findViewById<View>(R.id.filter_6_meses)
        val rb3 = findViewById<RadioButton>(R.id.rb_3_meses)
        val rb6 = findViewById<RadioButton>(R.id.rb_6_meses)

        fun atualizarUIFiltro(selecionado: View, meses: Int) {
            btn3Meses.isSelected = false
            btn6Meses.isSelected = false
            rb3.isChecked = false
            rb6.isChecked = false

            selecionado.isSelected = true
            if (selecionado == btn3Meses) rb3.isChecked = true else rb6.isChecked = true
            
            mesesSelecionados = meses
            carregarDadosIndividuais(meses)
        }

        btn3Meses.setOnClickListener { atualizarUIFiltro(btn3Meses, 3) }
        btn6Meses.setOnClickListener { atualizarUIFiltro(btn6Meses, 6) }
        atualizarUIFiltro(btn3Meses, 3)
    }

    class MyStatisticsAdapter(private val lista: List<DadosMensais>) : RecyclerView.Adapter<MyStatisticsAdapter.VH>() {
        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val nomeMes: TextView = v.findViewById(R.id.tv_estatistica_nome)
            val totalTickets: TextView = v.findViewById(R.id.tv_estatistica_total)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_estatistica, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = lista[position]
            val context = holder.itemView.context
            holder.nomeMes.text = item.nome.uppercase()
            holder.nomeMes.setTextColor(ContextCompat.getColor(context, R.color.texto_dica))
            holder.totalTickets.text = "ATRIBUÍDOS: ${item.criados}  •  RESOLVIDOS: ${item.resolvidos}"
            holder.totalTickets.setTextColor(ContextCompat.getColor(context, R.color.texto_principal))
        }

        override fun getItemCount() = lista.size
    }

    private fun configurarComparacao(atribAtual: Int, resAtual: Int, atribPassado: Int, resPassado: Int, resHoje: Int) {
        val localePT = Locale("pt", "PT")
        val formatoMes = SimpleDateFormat("MMMM", localePT)
        val cal = Calendar.getInstance()
        val mesAtualNome = formatoMes.format(cal.time).uppercase()
        cal.add(Calendar.MONTH, -1)
        val mesPassadoNome = formatoMes.format(cal.time).uppercase()

        tvLabelMesPassado.text = mesPassadoNome
        tvLabelMesAtual.text = mesAtualNome
 
        animarNumero(tvCountPassadoRes, resPassado.toFloat(), true)
        animarNumero(tvCountAtualRes, resAtual.toFloat(), true)
        animarNumero(tvCountHojeRes, resHoje.toFloat(), true)
    }

    private fun animarNumero(textView: TextView, valorFinal: Float, isInt: Boolean, suffix: String = "", showPlus: Boolean = false) {
        val animator = if (isInt) ValueAnimator.ofInt(0, valorFinal.toInt()) else ValueAnimator.ofFloat(0f, valorFinal)
        animator.duration = DURACAO_SINCRONIZADA.toLong()
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.addUpdateListener { animation ->
            val prefixo = if (showPlus && valorFinal > 0) "+" else ""
            if (isInt) textView.text = "$prefixo${animation.animatedValue}$suffix"
            else textView.text = String.format("%s%.1f%s", prefixo, animation.animatedValue as Float, suffix)
        }
        animator.start()
    }
}


