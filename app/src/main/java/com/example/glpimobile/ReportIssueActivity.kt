package com.example.glpimobile

import android.content.Intent
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath
import kotlinx.coroutines.*

class ReportIssueActivity : AppCompatActivity() {

    private lateinit var autoLocal: AutoCompleteTextView
    private lateinit var autoTipo: AutoCompleteTextView
    private lateinit var autoSerial: AutoCompleteTextView
    private lateinit var autoMotivo: AutoCompleteTextView
    private lateinit var etDescricao: EditText
    private lateinit var btnEnviar: Button

    private val TOKEN_SESSAO = GlpiConfig.SESSION_TOKEN
    private val TOKEN_APP = GlpiConfig.APP_TOKEN

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report_issue)

        autoLocal = findViewById(R.id.auto_localizacao)
        autoTipo = findViewById(R.id.auto_tipo_equipamento)
        autoSerial = findViewById(R.id.auto_serial_number)
        autoMotivo = findViewById(R.id.auto_motivo_problema)
        etDescricao = findViewById(R.id.et_problem_description)
        btnEnviar = findViewById(R.id.btn_send_report)

        configurarVisualSeta()
        configurarDadosEstaticos()

        // Selecção via dropdown
        autoSerial.setOnItemClickListener { parent, _, position, _ ->
            val snSelecionado = parent.getItemAtPosition(position).toString()
            buscarDadosAutomaticos(snSelecionado)
        }

        // Digitação manual: debounce de 600ms para não fazer chamadas a cada tecla
        var searchJob: kotlinx.coroutines.Job? = null
        autoSerial.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val texto = s?.toString()?.trim() ?: ""
                if (texto.length >= 4) {
                    searchJob?.cancel()
                    searchJob = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                        kotlinx.coroutines.delay(600)
                        buscarDadosAutomaticos(texto)
                    }
                }
            }
        })

        carregarSugestoesSeriais()
        btnEnviar.setOnClickListener { enviarTicket() }
    }

    private fun configurarVisualSeta() {
        val corBranca = ContextCompat.getColor(this, android.R.color.white)
        val lottieSeta = findViewById<LottieAnimationView>(R.id.lottie_seta)

        lottieSeta?.addValueCallback(KeyPath("**"), LottieProperty.COLOR_FILTER) {
            PorterDuffColorFilter(corBranca, PorterDuff.Mode.SRC_ATOP)
        }

        findViewById<FrameLayout>(R.id.btn_voltar_report).setOnClickListener {
            voltarParaInventarioComSinal()
        }
    }

    private fun voltarParaInventarioComSinal() {
        val intent = Intent(this, InventoryActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        intent.putExtra("ABRIR_MENU", true)
        startActivity(intent)
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        finish()
    }

    override fun onBackPressed() {
        voltarParaInventarioComSinal()
    }

    private fun configurarDadosEstaticos() {
        // 🔥 ADICIONADO: "Impressoras" à lista de exibição
        val categorias = listOf("Computador", "Dispositivo de Rede", "Monitor", "Impressora")
        autoTipo.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categorias))

        val locais = resources.getStringArray(R.array.locais_glpi).toList().filter { it != "Selecione a Localização..." }
        autoLocal.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, locais))

        val motivos = listOf("Não liga", "Sem Internet", "Erro de Software", "Ecrã partido", "Encravamento de papel", "Outro")
        autoMotivo.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, motivos))
    }

    private fun buscarDadosAutomaticos(sn: String) {
        if (sn.isEmpty()) return

        // Limpar imediatamente antes de cada nova busca
        autoTipo.setText("", false)
        autoLocal.setText("", false)

        CoroutineScope(Dispatchers.IO).launch {
            val categoriasApi = listOf("Computer", "NetworkEquipment", "Monitor", "Printer")
            for (tipo in categoriasApi) {
                try {
                    val resp = GlpiRetrofit.api.searchBySerialWithLocation(tipo, TOKEN_SESSAO, TOKEN_APP, value = sn)
                    val bodyData = if (resp.isSuccessful) resp.body()?.data else null
                    if (!bodyData.isNullOrEmpty()) {
                        val dados = bodyData[0]
                        Log.d("AUTO_FILL", "Dados encontrados para $tipo: $dados")

                        // Campo 3 = localização (forcedisplay[2]=3)
                        val localNome = dados["3"]?.toString()?.trim()
                            ?.takeIf { it.isNotEmpty() && it != "null" } ?: ""

                        withContext(Dispatchers.Main) {
                            val catExibicao = when (tipo) {
                                "Computer" -> "Computadores"
                                "NetworkEquipment" -> "Rede"
                                "Monitor" -> "Monitores"
                                "Printer" -> "Impressoras"
                                else -> ""
                            }
                            autoTipo.setText(catExibicao, false)
                            // Preenche a localização se existir, ou deixa vazio (já foi limpo antes)
                            autoLocal.setText(localNome, false)
                            Log.d("AUTO_FILL", "Categoria: $catExibicao | Localização: ${if (localNome.isEmpty()) "(vazia)" else localNome}")
                        }
                        break
                    }
                } catch (e: Exception) {
                    Log.e("AUTO_FILL", "Erro ao buscar $tipo: ${e.message}")
                }
            }
        }
    }

    private fun carregarSugestoesSeriais() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val tipos = listOf("Computer", "Monitor", "NetworkEquipment", "Printer")

                // Busca todos em paralelo
                val jobs = tipos.map { tipo ->
                    async {
                        try {
                            val resp = GlpiRetrofit.api.searchItems(tipo, TOKEN_SESSAO, TOKEN_APP, "0-200")
                            if (resp.isSuccessful) {
                                resp.body()?.data
                                    ?.map { it["5"]?.toString() ?: "" }
                                    ?.filter { it.isNotEmpty() && it != "null" }
                                    ?: emptyList()
                            } else emptyList()
                        } catch (e: Exception) { emptyList() }
                    }
                }

                val listaCombinada = jobs.awaitAll()
                    .flatten()
                    .distinct()
                    .sorted()

                withContext(Dispatchers.Main) {
                    autoSerial.setAdapter(
                        ArrayAdapter(this@ReportIssueActivity, android.R.layout.simple_dropdown_item_1line, listaCombinada)
                    )
                }
            } catch (e: Exception) { }
        }
    }

    private fun enviarTicket() {
        if (autoLocal.text.isEmpty() || autoTipo.text.isEmpty() || autoSerial.text.isEmpty()) {
            AlertHelper.exibirAlertaPremium(this, "Preencha os campos obrigatórios", isError = true)
            return
        }
        AlertHelper.exibirAlertaPremium(this, "Ticket enviado!", isError = false)
        voltarParaInventarioComSinal()
    }
}