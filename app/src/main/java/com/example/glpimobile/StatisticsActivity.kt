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
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class StatisticsActivity : AppCompatActivity() {

    private val DURACAO_ANIMACAO = 1500
    private lateinit var lineChartDesempenho: LineChart
    private lateinit var pieChartBalanco: PieChart
    private lateinit var barChartCategorias: HorizontalBarChart
    
    private lateinit var tvKpiMediaMensal: TextView
    private lateinit var tvKpiSaldo: TextView
    private lateinit var tvKpiMediaDiaria: TextView
    private lateinit var rvEstatisticas: RecyclerView
    private lateinit var progressLoading: ProgressBar
    private lateinit var nestedScrollView: NestedScrollView
    
    private var mesesSelecionados = 3
    private val TOKEN_SESSAO = GlpiConfig.SESSION_TOKEN
    private val TOKEN_APP = GlpiConfig.APP_TOKEN

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_statistics)
            inicializarUI()
            configurarFiltros()
        } catch (e: Exception) {
            Toast.makeText(this, "Erro ao abrir: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun inicializarUI() {
        lineChartDesempenho = findViewById(R.id.lineChart_desempenho)
        pieChartBalanco = findViewById(R.id.pieChart_balanco)
        barChartCategorias = findViewById(R.id.bar_chart_categorias)
        
        tvKpiMediaMensal = findViewById(R.id.tv_kpi_media_mensal)
        tvKpiSaldo = findViewById(R.id.tv_kpi_saldo)
        tvKpiMediaDiaria = findViewById(R.id.tv_kpi_media_diaria)
        rvEstatisticas = findViewById(R.id.rv_lista_estatisticas)
        progressLoading = findViewById<ProgressBar>(R.id.progress_loading)
        nestedScrollView = findViewById(R.id.nested_scroll_view)
        
        rvEstatisticas.layoutManager = LinearLayoutManager(this)
        rvEstatisticas.isNestedScrollingEnabled = false

        findViewById<TextView>(R.id.tv_header_title).text = "ESTATÍSTICAS GERAIS"
        findViewById<FrameLayout>(R.id.btn_voltar).setOnClickListener { finish() }

        val lottieSeta = findViewById<LottieAnimationView>(R.id.lottie_seta)
        lottieSeta?.addValueCallback(KeyPath("**"), LottieProperty.COLOR_FILTER) {
            PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP)
        }
        lottieSeta?.setAnimation(R.raw.esquerda)
        lottieSeta?.playAnimation()

        findViewById<TextView>(R.id.btn_toggle_grafico).setOnClickListener { view ->
            val btn = view as TextView
            if (lineChartDesempenho.visibility == View.VISIBLE) {
                lineChartDesempenho.visibility = View.GONE
                pieChartBalanco.visibility = View.VISIBLE
                btn.text = "VER DESEMPENHO"
                
                // Desativar legenda no balanço para maximizar o tamanho
                pieChartBalanco.legend.isEnabled = false
                pieChartBalanco.setExtraOffsets(0f, 0f, 0f, 0f)
                
                pieChartBalanco.animateY(DURACAO_ANIMACAO, Easing.EaseInOutQuart)
            } else {
                lineChartDesempenho.visibility = View.VISIBLE
                pieChartBalanco.visibility = View.GONE
                btn.text = "VER BALANÇO"
                lineChartDesempenho.animateY(DURACAO_ANIMACAO, Easing.EaseInOutQuart)
            }
        }

        configurarEstiloGraficos()
    }

    private fun configurarFiltros() {
        val btn3 = findViewById<View>(R.id.filter_3_meses)
        val btn6 = findViewById<View>(R.id.filter_6_meses)
        val rb3 = findViewById<RadioButton>(R.id.rb_3_meses)
        val rb6 = findViewById<RadioButton>(R.id.rb_6_meses)

        fun selecionar(btn: View, meses: Int) {
            btn3.isSelected = false
            btn6.isSelected = false
            rb3.isChecked = false
            rb6.isChecked = false

            btn.isSelected = true
            if (btn == btn3) rb3.isChecked = true else rb6.isChecked = true
            
            mesesSelecionados = meses
            carregarEstatisticasGlobais(meses)
        }

        btn3.setOnClickListener { selecionar(btn3, 3) }
        btn6.setOnClickListener { selecionar(btn6, 6) }
        selecionar(btn3, 3)
    }

    private fun configurarEstiloGraficos() {
        val corTexto = ContextCompat.getColor(this, R.color.texto_principal)
        
        // Estilo Comum
        listOf(lineChartDesempenho, pieChartBalanco, barChartCategorias).forEach { chart ->
            chart.description.isEnabled = false
            chart.legend.textColor = corTexto
        }

        // Line Chart (Sincronizado com Perfil)
        lineChartDesempenho.apply {
            extraBottomOffset = 25f
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            axisRight.isEnabled = false
            axisLeft.textColor = corTexto
            xAxis.textColor = corTexto
            legend.apply {
                form = Legend.LegendForm.CIRCLE
                horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                orientation = Legend.LegendOrientation.HORIZONTAL
                xEntrySpace = 20f
            }
        }

        // Pie Chart Balanço
        pieChartBalanco.apply {
            isDrawHoleEnabled = true
            setHoleColor(Color.TRANSPARENT)
            holeRadius = 50f
            transparentCircleRadius = 0f
            setDrawCenterText(false)
            setDrawEntryLabels(false)
            minOffset = 0f
            setExtraOffsets(0f, 0f, 0f, 0f)
            legend.isEnabled = false // Removido conforme solicitado
        }

        // Bar Chart Categorias
        barChartCategorias.apply {
            extraLeftOffset = 0f
            extraRightOffset = 25f
            setDrawBorders(false)
            setScaleEnabled(false)
            setPinchZoom(false)
            axisRight.isEnabled = false
            axisLeft.apply {
                isEnabled = true
                setDrawLabels(false)
                setDrawGridLines(false)
                setDrawAxisLine(false)
                axisMinimum = 0f
            }
            xAxis.apply {
                setDrawGridLines(false)
                position = XAxis.XAxisPosition.BOTTOM
                textColor = corTexto
                textSize = 12f
                granularity = 1f
                setDrawAxisLine(false)
            }
            legend.isEnabled = false
        }
    }

    private fun carregarEstatisticasGlobais(qtdMeses: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main) { 
                progressLoading.visibility = View.VISIBLE 
            }
            try {
                val localePT = Locale("pt", "PT")
                val formatoApi = SimpleDateFormat("yyyy-MM", Locale.US)
                val formatoFull = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val formatoMesLongo = SimpleDateFormat("MMMM yyyy", localePT)
                val formatoMesCurto = SimpleDateFormat("MMM", localePT)

                // Cálculo da Data Limite (Threshold) para o período
                val calLimite = Calendar.getInstance().apply { add(Calendar.MONTH, -qtdMeses) }
                val dataLimiteStr = formatoFull.format(calLimite.time)

                val deferreds: List<Deferred<DadosMensais>> = (qtdMeses downTo 1).map { mesesAtras ->
                    val cal = Calendar.getInstance().apply { add(Calendar.MONTH, -mesesAtras) }
                    val mesConsulta = formatoApi.format(cal.time)
                    val nomeLongo = formatoMesLongo.format(cal.time).replaceFirstChar { it.uppercase() }
                    val nomeCurto = formatoMesCurto.format(cal.time).replaceFirstChar { it.uppercase() }

                    async<DadosMensais> {
                        val criadosRes = GlpiRetrofit.api.getEstatisticasMensais(TOKEN_SESSAO, TOKEN_APP, fieldDate = 15, yearMonth = mesConsulta, isDeleted = 0)
                        val resolvidosRes = GlpiRetrofit.api.getEstatisticasMensais(TOKEN_SESSAO, TOKEN_APP, fieldDate = 17, yearMonth = mesConsulta, isDeleted = 0)
                        DadosMensais(nomeLongo, nomeCurto, criadosRes.totalcount ?: 0, resolvidosRes.totalcount ?: 0, 0)
                    }
                }
                
                val resultados = deferreds.awaitAll()

                // BUSCA OTIMIZADA PARA CATEGORIAS: Query única exata igual ao iOS
                val categoriasReq = async { GlpiRetrofit.api.getCategoriasEstatisticas(TOKEN_SESSAO, TOKEN_APP, dataLimite = dataLimiteStr, range = "0-3000") }
                
                val resCategorias = categoriasReq.await().body()?.data ?: emptyList()

                // A API já filtrou por data >= dataLimiteStr, portanto todos os resultados são válidos
                val ticketsNoPeriodo = resCategorias

                withContext(Dispatchers.Main) {
                    // Verifica se o utilizador está no fundo ou muito perto dele
                    val diff = (nestedScrollView.getChildAt(0).height - nestedScrollView.height)
                    val isAtBottom = nestedScrollView.scrollY >= diff - 50 // Tolerância de 50px
                    val currentScrollY = nestedScrollView.scrollY
                    
                    progressLoading.visibility = View.GONE
                    exibirDadosGlobais(resultados, ticketsNoPeriodo, qtdMeses)
                    
                    // Restaura a posição de scroll de forma inteligente
                    nestedScrollView.post {
                        if (isAtBottom && qtdMeses == 6) {
                            // Se estava no fundo e mudou para 6 meses (onde a página cresce), segue para o novo fundo
                            nestedScrollView.fullScroll(View.FOCUS_DOWN)
                        } else {
                            // Caso contrário, mantém a distância exata ao topo
                            nestedScrollView.scrollTo(0, currentScrollY)
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressLoading.visibility = View.GONE
                    Toast.makeText(this@StatisticsActivity, "Erro ao carregar dados: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun exibirDadosGlobais(resultados: List<DadosMensais>, todosTickets: List<Map<String, Any>>, qtdMeses: Int) {
        var somaCriados = 0
        var somaResolvidos = 0
        resultados.forEach {
            somaCriados += it.criados
            somaResolvidos += it.resolvidos
        }

        animarNumero(tvKpiMediaMensal, (somaCriados.toFloat() / qtdMeses), false)
        animarNumero(tvKpiSaldo, (somaResolvidos - somaCriados).toFloat(), true, showPlus = true)
        animarNumero(tvKpiMediaDiaria, (somaCriados.toFloat() / (qtdMeses * 30)), false)

        // --- Gráfico de Linha (Estilo Perfil) ---
        val entriesCriados = resultados.mapIndexed { i, d -> Entry(i.toFloat(), d.criados.toFloat()) }
        val entriesResolvidos = resultados.mapIndexed { i, d -> Entry(i.toFloat(), d.resolvidos.toFloat()) }
        val labels = resultados.map { it.nomeCurto.substring(0, 1).uppercase() }

        val maxVal = maxOf(entriesCriados.maxOfOrNull { it.y } ?: 0f, entriesResolvidos.maxOfOrNull { it.y } ?: 0f)
        lineChartDesempenho.axisLeft.apply {
            axisMaximum = if (maxVal == 0f) 5f else maxVal * 1.5f
            axisMinimum = 0f
            textColor = ContextCompat.getColor(this@StatisticsActivity, R.color.texto_principal)
        }

        val setCriados = LineDataSet(entriesCriados, "ATRIBUÍDOS").apply { 
            color = Color.parseColor("#89CFF0"); setCircleColor(color); lineWidth = 4f; circleRadius = 5f; setDrawCircles(true); setDrawValues(false); isHighlightEnabled = false
        }
        val setResolvidos = LineDataSet(entriesResolvidos, "RESOLVIDOS").apply { 
            color = ContextCompat.getColor(this@StatisticsActivity, R.color.azul_glpi); setCircleColor(color); lineWidth = 4f; circleRadius = 5f; setDrawCircles(true); setDrawValues(false); isHighlightEnabled = false
        }
        
        lineChartDesempenho.data = null
        lineChartDesempenho.notifyDataSetChanged()
        lineChartDesempenho.data = LineData(setCriados, setResolvidos)
        lineChartDesempenho.xAxis.apply {
            valueFormatter = IndexAxisValueFormatter(labels)
            labelCount = qtdMeses; granularity = 1f
            textColor = ContextCompat.getColor(this@StatisticsActivity, R.color.texto_principal)
        }
        lineChartDesempenho.legend.apply {
            textSize = 12f
            form = Legend.LegendForm.CIRCLE
        }
        lineChartDesempenho.animateY(DURACAO_ANIMACAO, Easing.EaseInOutQuart)

        // --- Gráfico de Pizza Balanço ---
        val dataSetPie = PieDataSet(listOf(PieEntry(somaCriados.toFloat(), "ATRIBUÍDOS"), PieEntry(somaResolvidos.toFloat(), "RESOLVIDOS")), "").apply {
            colors = listOf(Color.parseColor("#89CFF0"), ContextCompat.getColor(this@StatisticsActivity, R.color.azul_glpi))
            sliceSpace = 3f; valueTextColor = Color.WHITE; valueTextSize = 15f; setValueTypeface(Typeface.DEFAULT_BOLD)
            valueFormatter = object : ValueFormatter() { override fun getFormattedValue(v: Float) = v.toInt().toString() }
        }
        pieChartBalanco.data = null
        pieChartBalanco.notifyDataSetChanged()
        pieChartBalanco.data = PieData(dataSetPie)
        pieChartBalanco.animateY(DURACAO_ANIMACAO, Easing.EaseInOutQuart)

        // --- Categorias (Horizontal Bar) ---
        processarCategorias(todosTickets)

        rvEstatisticas.adapter = GeneralStatisticsAdapter(resultados)
        rvEstatisticas.scheduleLayoutAnimation()
    }

    private fun processarCategorias(tickets: List<Map<String, Any>>) {
        if (tickets.isEmpty()) return
        
        val topCat = tickets.groupBy { it["7"]?.toString() ?: "N/A" }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
            .take(5)
            .reversed() // Reverse for horizontal layout order

        val entries = topCat.mapIndexed { i, p -> BarEntry(i.toFloat(), p.second.toFloat()) }
        
        val dataSet = BarDataSet(entries, "").apply {
            color = ContextCompat.getColor(this@StatisticsActivity, R.color.azul_glpi)
            valueTextColor = ContextCompat.getColor(this@StatisticsActivity, R.color.texto_principal)
            valueTextSize = 13f
            valueTypeface = Typeface.DEFAULT_BOLD
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(v: Float) = v.toInt().toString()
            }
        }

        barChartCategorias.apply {
            data = null
            notifyDataSetChanged()
            data = BarData(dataSet).apply { barWidth = 0.6f }
            xAxis.valueFormatter = IndexAxisValueFormatter(topCat.map { it.first })
            setFitBars(true)
            notifyDataSetChanged()
            invalidate()
            animateY(DURACAO_ANIMACAO, Easing.EaseInOutQuart)
        }
    }

    private fun animarNumero(textView: TextView, valorFinal: Float, isInt: Boolean, suffix: String = "", showPlus: Boolean = false) {
        val animator = if (isInt) ValueAnimator.ofInt(0, valorFinal.toInt()) else ValueAnimator.ofFloat(0f, valorFinal)
        animator.duration = DURACAO_ANIMACAO.toLong()
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.addUpdateListener { animation ->
            val pref = if (showPlus && valorFinal > 0) "+" else ""
            if (isInt) textView.text = "$pref${animation.animatedValue}$suffix"
            else textView.text = String.format("%s%.1f%s", pref, animation.animatedValue as Float, suffix)
        }
        animator.start()
    }

    class GeneralStatisticsAdapter(private val lista: List<DadosMensais>) : RecyclerView.Adapter<GeneralStatisticsAdapter.VH>() {
        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val nome: TextView = v.findViewById(R.id.tv_estatistica_nome)
            val info: TextView = v.findViewById(R.id.tv_estatistica_total)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(LayoutInflater.from(parent.context).inflate(R.layout.item_estatistica, parent, false))
        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = lista[position]
            holder.nome.text = item.nome.uppercase()
            holder.nome.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.texto_dica))
            holder.info.text = "ATRIBUÍDOS: ${item.criados}  •  RESOLVIDOS: ${item.resolvidos}"
        }
        override fun getItemCount() = lista.size
    }
}
