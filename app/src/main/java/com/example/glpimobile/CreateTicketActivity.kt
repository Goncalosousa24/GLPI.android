package com.example.glpimobile

import android.content.Context
import android.content.Intent
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.*

class CreateTicketActivity : AppCompatActivity() {

    private val categoryMap = mutableMapOf<String, String>()
    private val FONTES_PEDIDO = arrayOf("Direct", "E-Mail", "Formcreator", "Helpdesk", "Phone", "Written", "Other")
    private val PRIORIDADES = arrayOf("Muito Baixo", "Baixo", "Médio", "Alto", "Muito Alto", "Principal")
    
    private val selectedUris = mutableListOf<Uri>()
    private var cameraImageUri: Uri? = null
    private val cardsList = mutableListOf<View>()

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { addAnexo(it) }
    }

    private val pickFileLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { addAnexo(it) }
    }

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            cameraImageUri?.let { addAnexo(it) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 🔥 IDIOMA EM PORTUGUÊS (Consistente com CalendarActivity para evitar crashes) 🔥
        val localePT = Locale("pt", "PT")
        Locale.setDefault(localePT)
        val config = resources.configuration
        config.setLocale(localePT)
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)

        setContentView(R.layout.activity_create_ticket)

        val etAssunto = findViewById<EditText>(R.id.et_assunto)
        val etDescricao = findViewById<EditText>(R.id.et_descricao)
        val btnEnviar = findViewById<View>(R.id.btn_abrir_ticket)
        val btnVoltar = findViewById<FrameLayout>(R.id.btn_voltar)
        val lottieSeta = findViewById<LottieAnimationView>(R.id.lottie_seta)

        // Novos campos UI
        val rbIncidente = findViewById<RadioButton>(R.id.rb_incidente)
        val tvCategoriaSel = findViewById<TextView>(R.id.tv_categoria_selecionada)
        val ivCategoriaSeta = findViewById<ImageView>(R.id.iv_categoria_seta)
        val llCategoriaOpcoes = findViewById<LinearLayout>(R.id.ll_categoria_opcoes)
        val tvFonteSel = findViewById<TextView>(R.id.tv_fonte_selecionada)
        val ivFonteSeta = findViewById<ImageView>(R.id.iv_fonte_seta)
        val llFonteOpcoes = findViewById<LinearLayout>(R.id.ll_fonte_opcoes)
        val tvPrioridadeSel = findViewById<TextView>(R.id.tv_prioridade_selecionada)
        val ivPrioridadeSeta = findViewById<ImageView>(R.id.iv_prioridade_seta)
        val llPrioridadeOpcoes = findViewById<LinearLayout>(R.id.ll_prioridade_opcoes)
        
        val tvTTO = findViewById<TextView>(R.id.tv_tto_selecionado)
        val tvTTR = findViewById<TextView>(R.id.tv_ttr_selecionado)

        // Toolbar de Anexos removida a pedido do utilizador


        // Estilo visual
        val corBranca = ContextCompat.getColor(this, android.R.color.white)
        lottieSeta?.addValueCallback(KeyPath("**"), LottieProperty.COLOR_FILTER) {
            PorterDuffColorFilter(corBranca, PorterDuff.Mode.SRC_ATOP)
        }

        btnVoltar.setOnClickListener { voltarParaMenu(abrirMenu = true) }

        // 11. Orchestrar Focos
        configurarBordasDinamicas(
            listOf(
                findViewById(R.id.layout_tipo),
                findViewById(R.id.layout_categoria),
                findViewById(R.id.layout_fonte),
                findViewById(R.id.layout_prioridade),
                findViewById(R.id.layout_tto),
                findViewById(R.id.layout_ttr),
                findViewById(R.id.layout_descricao)
            ),
            listOf(etAssunto, etDescricao)
        )

        // 12. Inicializar Menus
        setupExpandableCard(findViewById(R.id.layout_fonte), llFonteOpcoes, tvFonteSel, ivFonteSeta, FONTES_PEDIDO)
        setupExpandableCard(findViewById(R.id.layout_prioridade), llPrioridadeOpcoes, tvPrioridadeSel, ivPrioridadeSeta, PRIORIDADES)
        carregarCategorias(llCategoriaOpcoes, tvCategoriaSel, ivCategoriaSeta)

        btnEnviar.setOnClickListener {
            val assunto = etAssunto.text.toString().trim()
            val descricao = etDescricao.text.toString().trim()

            if (assunto.isEmpty() || descricao.isEmpty()) {
                AlertHelper.exibirAlertaPremium(this, "Preencha todos os campos.", isError = true)
                return@setOnClickListener
            }

            val tipo = if (rbIncidente.isChecked) 1 else 2
            val categoriaId = categoryMap[tvCategoriaSel.text.toString()]?.toIntOrNull()
            val fonteId = when(tvFonteSel.text.toString()) {
                "Helpdesk" -> 1; "E-Mail" -> 2; "Phone" -> 3; "Direct" -> 4; "Written" -> 5; "Other" -> 6; "Formcreator" -> 7; else -> 4
            }
            val priorityId = when(tvPrioridadeSel.text.toString()) {
                "Muito Baixo" -> 1; "Baixo" -> 2; "Médio" -> 3; "Alto" -> 4; "Muito Alto" -> 5; "Principal" -> 6; else -> 3
            }
            val tto = if (tvTTO.text.contains("-")) tvTTO.text.toString() else null
            val ttr = if (tvTTR.text.contains("-")) tvTTR.text.toString() else null

            enviarTicketParaApi(assunto, descricao, tipo, categoriaId, fonteId, priorityId, tto, ttr)
        }

        // 13. Limpar Foco ao clicar no fundo
        val rootLayout = findViewById<View>(R.id.root_layout)
        val scrollView = findViewById<View>(R.id.scroll_view)
        val innerLayout = findViewById<View>(R.id.inner_content_layout)
        
        val limparTudo: () -> Unit = {
            etAssunto.clearFocus()
            etDescricao.clearFocus()
            resetarCardsGlobal()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(rootLayout.windowToken, 0)
        }
        
        rootLayout.setOnClickListener { substituirPorLimparTudo(limparTudo) }
        scrollView.setOnClickListener { substituirPorLimparTudo(limparTudo) }
        innerLayout.setOnClickListener { substituirPorLimparTudo(limparTudo) }
    }

    private fun substituirPorLimparTudo(limparTudo: () -> Unit) {
        limparTudo()
    }

    private fun configurarBordasDinamicas(cards: List<View>, editTexts: List<EditText>) {
        cardsList.clear()
        cardsList.addAll(cards)

        cards.forEach { card ->
            card.setOnClickListener {
                editTexts.forEach { et -> 
                    et.clearFocus()
                    (et.parent.parent as? TextInputLayout)?.let { til ->
                        til.isFocusedByDefault = false
                        til.isSelected = false
                    }
                }
                val menuToToggle = when(card.id) {
                    R.id.layout_categoria -> findViewById<View>(R.id.ll_categoria_opcoes)
                    R.id.layout_fonte -> findViewById<View>(R.id.ll_fonte_opcoes)
                    R.id.layout_prioridade -> findViewById<View>(R.id.ll_prioridade_opcoes)
                    else -> null
                }
                val wasOpen = menuToToggle?.visibility == View.VISIBLE

                resetarCardsGlobal()
                card.setBackgroundResource(R.drawable.bg_entrada_focada)

                if (!wasOpen) {
                    when(card.id) {
                        R.id.layout_categoria -> toggleMenu(findViewById(R.id.ll_categoria_opcoes), findViewById(R.id.iv_categoria_seta))
                        R.id.layout_fonte -> toggleMenu(findViewById(R.id.ll_fonte_opcoes), findViewById(R.id.iv_fonte_seta))
                        R.id.layout_prioridade -> toggleMenu(findViewById(R.id.ll_prioridade_opcoes), findViewById(R.id.iv_prioridade_seta))
                        R.id.layout_tto, R.id.layout_ttr -> {
                            val isTTO = card.id == R.id.layout_tto
                            showDateTimePicker(if (isTTO) findViewById<TextView>(R.id.tv_tto_selecionado) else findViewById<TextView>(R.id.tv_ttr_selecionado))
                        }
                        R.id.layout_descricao -> findViewById<EditText>(R.id.et_descricao).requestFocus()
                    }
                }
                
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(card.windowToken, 0)
            }
        }

        editTexts.forEach { et ->
            et.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    resetarCardsGlobal()
                    // Se for a descrição, ativamos a borda no cartão pai (que contém a toolbar)
                    if (et.id == R.id.et_descricao) {
                        findViewById<View>(R.id.layout_descricao).setBackgroundResource(R.drawable.bg_entrada_focada)
                    }
                }
            }
        }
    }

    private fun resetarCardsGlobal() {
        cardsList.forEach { it.setBackgroundResource(R.drawable.bg_cartao_ticket) }
        findViewById<View>(R.id.ll_categoria_opcoes).visibility = View.GONE
        findViewById<ImageView>(R.id.iv_categoria_seta).rotation = 0f
        findViewById<View>(R.id.ll_fonte_opcoes).visibility = View.GONE
        findViewById<ImageView>(R.id.iv_fonte_seta).rotation = 0f
        findViewById<View>(R.id.ll_prioridade_opcoes).visibility = View.GONE
        findViewById<ImageView>(R.id.iv_prioridade_seta).rotation = 0f
    }

    private fun toggleMenu(container: LinearLayout, icon: ImageView) {
        if (container.visibility == View.GONE) {
            container.visibility = View.VISIBLE
            icon.rotation = 180f
        } else {
            container.visibility = View.GONE
            icon.rotation = 0f
        }
    }

    private fun getTmpFileUri(): Uri {
        val tmpFile = java.io.File.createTempFile("tmp_image_file", ".png", cacheDir).apply {
            createNewFile()
            deleteOnExit()
        }
        return androidx.core.content.FileProvider.getUriForFile(applicationContext, "${packageName}.fileprovider", tmpFile)
    }

    private fun addAnexo(uri: Uri) {
        selectedUris.add(uri)
        updateAnexosUI()
    }

    private fun updateAnexosUI() {
        val container = findViewById<LinearLayout>(R.id.ll_anexos_selecionados)
        container.removeAllViews()

        selectedUris.forEach { uri ->
            val fileName = getFileName(uri)
            val bubble = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                background = ContextCompat.getDrawable(context, R.drawable.bg_cartao_brilhante)
                setPadding(dpToPx(12), dpToPx(8), dpToPx(8), dpToPx(8))
                gravity = android.view.Gravity.CENTER_VERTICAL
                val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                params.setMargins(0, 0, dpToPx(8), 0)
                layoutParams = params
            }

            val tvName = TextView(this).apply {
                text = fileName; textSize = 12f; maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END; maxWidth = dpToPx(120)
                setTextColor(ContextCompat.getColor(context, R.color.texto_principal))
            }

            val btnRemove = ImageView(this).apply {
                setImageResource(R.drawable.ic_close)
                setPadding(dpToPx(8), dpToPx(4), dpToPx(4), dpToPx(4))
                layoutParams = LinearLayout.LayoutParams(dpToPx(32), dpToPx(32))
                setColorFilter(ContextCompat.getColor(context, R.color.texto_dica), android.graphics.PorterDuff.Mode.SRC_IN)
                setOnClickListener {
                    selectedUris.remove(uri)
                    updateAnexosUI()
                }
            }

            bubble.addView(tvName); bubble.addView(btnRemove)
            container.addView(bubble)
        }
    }

    private fun getFileName(uri: Uri): String {
        var result = "ficheiro"
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.let {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) result = it.getString(index)
                }
                it.close()
            }
        }
        if (result == "ficheiro") result = uri.path?.substringAfterLast('/') ?: "ficheiro"
        return result
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()
 
    private fun setupExpandableCard(card: View, optionsContainer: LinearLayout, tvSelected: TextView, ivSeta: ImageView, options: Array<String>) {
        val outValue = android.util.TypedValue()
        theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true)
        val selectableResource = outValue.resourceId
 
        options.forEach { option ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
                gravity = android.view.Gravity.CENTER_VERTICAL
                setBackgroundResource(selectableResource)
                setOnClickListener {
                    tvSelected.text = option
                    optionsContainer.visibility = View.GONE
                    ivSeta.rotation = 0f
                }
            }
            val tvOption = TextView(this).apply {
                text = option; textSize = 16f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setTextColor(ContextCompat.getColor(context, R.color.texto_principal))
            }
            row.addView(tvOption); optionsContainer.addView(row)
            val separator = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1).apply { setMargins(dpToPx(16), 0, dpToPx(16), 0) }
                setBackgroundColor(ContextCompat.getColor(context, R.color.linha_separadora))
            }
            optionsContainer.addView(separator)
        }
    }

    private fun showDateTimePicker(textView: TextView) {
        val localePT = Locale("pt", "PT")
        val cal = Calendar.getInstance(localePT)
        
        android.app.DatePickerDialog(this, R.style.GLPI_PickerTheme, { _, year, month, day ->
            cal.set(year, month, day)
            android.app.TimePickerDialog(this, R.style.GLPI_PickerTheme, { _, hour, minute ->
                cal.set(Calendar.HOUR_OF_DAY, hour)
                cal.set(Calendar.MINUTE, minute)
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", localePT)
                textView.text = sdf.format(cal.time)
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun carregarCategorias(container: LinearLayout, tvSelected: TextView, ivSeta: ImageView) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val categories = GlpiRetrofit.api.getITILCategories(GlpiConfig.SESSION_TOKEN, GlpiConfig.APP_TOKEN)
                withContext(Dispatchers.Main) {
                    categories.forEach { cat ->
                        val id = cat["id"]?.toString()?.substringBefore(".") ?: ""
                        val name = cat["completename"]?.toString() ?: cat["name"]?.toString() ?: ""
                        if (id.isNotEmpty() && name.isNotEmpty()) categoryMap[name] = id
                    }
                    updateCategoryUI(container, tvSelected, ivSeta)
                }
            } catch (e: Exception) {}
        }
    }

    private fun updateCategoryUI(container: LinearLayout, tvSelected: TextView, ivSeta: ImageView) {
        val outValue = android.util.TypedValue()
        theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true)
        val selectableResource = outValue.resourceId

        container.removeAllViews()
        categoryMap.keys.sorted().forEach { name ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
                gravity = android.view.Gravity.CENTER_VERTICAL; setBackgroundResource(selectableResource)
                setOnClickListener {
                    tvSelected.text = name; container.visibility = View.GONE; ivSeta.rotation = 0f
                }
            }
            val tvOption = TextView(this).apply {
                text = name; textSize = 16f
                setTextColor(ContextCompat.getColor(context, R.color.texto_principal))
            }
            row.addView(tvOption); container.addView(row)
            val separator = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1).apply { setMargins(dpToPx(16), 0, dpToPx(16), 0) }
                setBackgroundColor(ContextCompat.getColor(context, R.color.linha_separadora))
            }
            container.addView(separator)
        }
    }

    private fun enviarTicketParaApi(assunto: String, descricao: String, tipo: Int, catId: Int?, fonteId: Int, priorityId: Int, tto: String?, ttr: String?) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Obter a data e hora local do dispositivo Android formatadas para o GLPI
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                val currentDateTime = sdf.format(java.util.Date())

                val input = TicketInput(
                    name = assunto,
                    content = descricao,
                    status = 1,
                    type = tipo,
                    itilcategories_id = catId,
                    requesttypes_id = fonteId,
                    priority = priorityId,
                    time_to_own = tto,
                    time_to_resolve = ttr,
                    date = currentDateTime,
                    date_mod = currentDateTime
                )
                val response = GlpiRetrofit.api.criarTicket(GlpiConfig.SESSION_TOKEN, GlpiConfig.APP_TOKEN, TicketRequest(input))
                withContext(Dispatchers.Main) {
                    if (response.id != null) {
                        if (selectedUris.isNotEmpty()) {
                            AlertHelper.exibirAlertaPremium(this@CreateTicketActivity, "A enviar anexos...", isError = false)
                            uploadDocumentos(response.id)
                        } else {
                            AlertHelper.exibirAlertaPremium(this@CreateTicketActivity, "Ticket #${response.id} criado!", isError = false)
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                voltarParaMenu(abrirMenu = false)
                            }, 1500)
                        }
                    } else {
                        AlertHelper.exibirAlertaPremium(this@CreateTicketActivity, "Erro ao criar ticket.", isError = true)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    AlertHelper.exibirAlertaPremium(this@CreateTicketActivity, "Erro de ligação.", isError = true)
                }
            }
        }
    }

    private fun uploadDocumentos(ticketId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            selectedUris.forEach { uri ->
                try {
                    val fileName = getFileName(uri)
                    val inputStream = contentResolver.openInputStream(uri)
                    val bytes = inputStream?.readBytes() ?: return@forEach
                    inputStream.close()
                    val requestFile = bytes.toRequestBody("application/octet-stream".toMediaTypeOrNull(), 0, bytes.size)
                    val body = MultipartBody.Part.createFormData("filename[0]", fileName, requestFile)
                    val manifestJson = Gson().toJson(DocumentRequest(DocumentInput(name = fileName, items_id = ticketId)))
                    val manifestPart = manifestJson.toRequestBody("application/json".toMediaTypeOrNull())
                    GlpiRetrofit.api.addDocument(GlpiConfig.SESSION_TOKEN, GlpiConfig.APP_TOKEN, manifestPart, body)
                } catch (e: Exception) {}
            }
            withContext(Dispatchers.Main) {
                AlertHelper.exibirAlertaPremium(this@CreateTicketActivity, "Ticket criado com sucesso!", isError = false)
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    voltarParaMenu(abrirMenu = false)
                }, 1500)
            }
        }
    }

    private fun voltarParaMenu(abrirMenu: Boolean = true) {
        val intent = Intent(this, DashboardActivity::class.java)
        intent.putExtra("ABRIR_MENU", abrirMenu)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        startActivity(intent)
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        finish()
    }

    override fun onBackPressed() { voltarParaMenu(abrirMenu = true) }
}