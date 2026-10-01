package com.example.glpimobile

import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.text.Editable
import android.text.Html
import android.graphics.Typeface
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class TicketAdapter(
    private var tickets: List<Map<String, Any>>,
    private val mostrarMotivo: Boolean = false,
    private val showCreationDate: Boolean = false,
    private val showResolutionDate: Boolean = false,
    private val showEditButton: Boolean = false,
    private val showAgendaMode: Boolean = false,
    private val initialExpandedTicketId: String? = null,
    private val showDeleteButton: Boolean = false,
    private val showTrashActions: Boolean = false,
    private val showResponderButton: Boolean = true,
    private val onDeleteClick: ((String, Map<String, Any>) -> Unit)? = null,
    private val onRestoreClick: ((String, Map<String, Any>) -> Unit)? = null,
    private val onPurgeClick: ((String, Map<String, Any>) -> Unit)? = null
) : RecyclerView.Adapter<TicketAdapter.TicketViewHolder>() {

    private var expandedPosition: Int = -1
    private var selectedPosition: Int = -1
    private var extractIdFunc: ((Any?) -> String)? = null

    companion object {
        val UNAME_CACHE = ConcurrentHashMap<String, String>()
        private val PENDING_FETCHES = java.util.Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())
        private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    }

    fun updateList(newList: List<Map<String, Any>>) {
        tickets = newList
        notifyDataSetChanged()
    }

    class TicketViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvId: TextView = itemView.findViewById(R.id.tv_ticket_id)
        val tvTitulo: TextView = itemView.findViewById(R.id.tv_ticket_assunto)
        val tvData: TextView = itemView.findViewById(R.id.tv_ticket_data)
        val tvRequester: TextView = itemView.findViewById(R.id.tv_ticket_requester)
        val tvTechnician: TextView = itemView.findViewById(R.id.tv_ticket_technician)
        val tvDescricao: TextView = itemView.findViewById(R.id.tv_ticket_descricao)
        val vSeparador: View = itemView.findViewById(R.id.v_separador_descricao)
        val tvPriority: TextView = itemView.findViewById(R.id.tv_ticket_priority)
        val btnAtoresContainer: View = itemView.findViewById(R.id.btn_atores_container)
        val lottieSucesso: com.airbnb.lottie.LottieAnimationView = itemView.findViewById(R.id.lottie_sucesso_atribuicao)
        val llRespostasContainer: LinearLayout = itemView.findViewById(R.id.ll_respostas_container)
        val btnDeleteContainer: View = itemView.findViewById(R.id.btn_delete_container)
        val layoutTrashActions: View = itemView.findViewById(R.id.layout_trash_actions)
        val btnRestore: View = itemView.findViewById(R.id.btn_restore_container)
        val btnPurge: View = itemView.findViewById(R.id.btn_purge_container)
        
        // Novos botões de fundo (Expansão)
        val layoutBottomTrashActions: View = itemView.findViewById(R.id.layout_bottom_trash_actions)
        val btnBottomRestore: View = itemView.findViewById(R.id.btn_bottom_restore)
        val btnBottomPurge: View = itemView.findViewById(R.id.btn_bottom_purge)
        
        var isExpanded: Boolean = false
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TicketViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_ticket, parent, false)
        return TicketViewHolder(view)
    }

    private fun formatarStringNome(raw: String?): String? {
        if (raw == null) return null
        var s = raw.trim()
        if (s.isEmpty() || s == "null") return null
        
        // Substituir pontos por espaços
        s = s.replace(".", " ")
        

        // Capitalizar cada palavra
        return s.split(" ")
            .filter { it.isNotEmpty() }
            .joinToString(" ") { word ->
                word.lowercase().replaceFirstChar { 
                    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() 
                }
            }
    }

    private fun formatarNome(valor: Any?, viewToUpdate: TextView? = null): String? {
        if (valor == null) return null
        
        fun processarItem(item: Any?): String? {
            if (item == null) return null
            if (item is Map<*, *>) {
                val fname = item["firstname"]?.toString() ?: ""
                val rname = item["realname"]?.toString() ?: ""
                val cname = item["completename"]?.toString() ?: ""
                val name = item["name"]?.toString() ?: ""
                val rawName = if (fname.isNotEmpty() || rname.isNotEmpty()) {
                    "$fname $rname".trim()
                } else {
                    cname.ifEmpty { name }
                }
                return formatarStringNome(rawName)
            }
            
            val s = item.toString().trim()
            if (s == "null" || s.isEmpty()) return null
            
            val idPotencial = s.toDoubleOrNull()?.toInt()?.toString()
            if (idPotencial != null && idPotencial.isNotEmpty()) {
                if (idPotencial == "11") return "Gonçalo Sousa"
                val cached = UNAME_CACHE[idPotencial]
                if (cached != null) return formatarStringNome(cached)
                if (viewToUpdate != null && !PENDING_FETCHES.contains(idPotencial)) {
                    fetchUserName(idPotencial, viewToUpdate)
                }
                return "[ID: $idPotencial]"
            }
            
            if (s.contains("-") && s.length > 8) return null
            if (s.contains(">") || listOf("Software", "Hardware", "null", "Novo", "Incidente").any { s.equals(it, ignoreCase = true) }) return null
            return formatarStringNome(s)
        }

        if (valor is List<*>) {
            val nomes = valor.mapNotNull { processarItem(it) }
            if (nomes.isEmpty()) return null
            return nomes.joinToString(" & ")
        }
        
        return processarItem(valor)
    }

    override fun onBindViewHolder(holder: TicketViewHolder, position: Int) {
        val context = holder.itemView.context
        val ticket = tickets[position]
        val rawId = ticket["2"]?.toString() ?: ticket["id"]?.toString() ?: ""
        val idLimpo = if (rawId.contains(".")) rawId.substringBefore(".") else rawId
        val titulo = ticket["1"]?.toString() ?: "Sem título"
        val dataAtualizacao = (ticket["19"] ?: ticket["date_mod"] ?: "").toString()
        val dataCriacao = (ticket["15"] ?: ticket["date"] ?: "").toString()
        val dataEncerramento = (ticket["16"] ?: ticket["closedate"] ?: "").toString()
        val dataResolucao = (ticket["17"] ?: ticket["solvedate"] ?: "").toString()
        val dataLimite = (ticket["18"] ?: ticket["due_date"] ?: "").toString()
        fun extractId(valor: Any?): String {
            if (valor == null) return ""
            if (valor is Map<*, *>) return valor["id"]?.toString()?.substringBefore(".") ?: ""
            if (valor is List<*>) {
                val primeiro = valor.firstOrNull()
                if (primeiro is Map<*, *>) return primeiro["id"]?.toString()?.substringBefore(".") ?: ""
                return primeiro?.toString()?.substringBefore(".") ?: ""
            }
            val s = valor.toString()
            return if (s.contains(".")) s.substringBefore(".") else s
        }
        this.extractIdFunc = ::extractId

        val estado = extractId(ticket["12"])
        val prioridadeId = extractId(ticket["3"])
        val descricaoHtml = ticket["21"]?.toString() ?: ""

        val prioridadeNome = when(prioridadeId) {
            "1" -> "MUITO BAIXA"
            "2" -> "BAIXA"
            "3" -> "MÉDIA"
            "4" -> "ALTA"
            "5" -> "MUITO ALTA"
            "6" -> "PRINCIPAL"
            else -> ""
        }

        val prioridadeCor = when(prioridadeId) {
            "4", "5", "6" -> R.color.vermelho_forte // ALTA / MUITO ALTA / PRINCIPAL
            else -> R.color.azul_glpi
        }

        holder.tvPriority.text = prioridadeNome
        holder.tvPriority.visibility = if (prioridadeNome.isNotEmpty()) View.VISIBLE else View.GONE
        holder.tvPriority.setTextColor(ContextCompat.getColor(holder.itemView.context, prioridadeCor))

        holder.tvId.text = "Ticket #$idLimpo"
        holder.tvTitulo.text = titulo
        holder.tvDescricao.text = GlpiHtmlFixer.clean(descricaoHtml)

        fun isExpired(dueDateStr: String?, status: String?): Boolean {
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

        if (mostrarMotivo) {
            val statusNome = when(estado) {
                "1" -> "Aberto"
                "2" -> "Atribuído"
                "3" -> "Planeado"
                "4" -> "Aguardando"
                "5" -> "Resolvido"
                "6" -> "Fechado"
                else -> ""
            }
            val statusSufixo = if (statusNome.isNotEmpty()) " ($statusNome)" else ""
            
            val motivo = when {
                dataCriacao.isNotEmpty() && dataCriacao == dataAtualizacao -> "NOVO TICKET$statusSufixo • "
                estado == "5" || estado == "6" -> "RESOLVIDO • "
                else -> "ATUALIZADO$statusSufixo • "
            }
            holder.tvData.text = "$motivo$dataAtualizacao"
        } else if (showResolutionDate) {
            val resolucao = if (dataResolucao.isNotBlank()) dataResolucao else if (dataEncerramento.isNotBlank()) dataEncerramento else dataAtualizacao
            holder.tvData.text = "Resolvido em: $resolucao"
        } else if (showCreationDate) {
            holder.tvData.text = "Criado em: $dataCriacao"
        } else if (showAgendaMode) {
            val sb = StringBuilder()
            sb.append("<b>CRIADO EM:</b> $dataCriacao")
            if (dataLimite.isNotEmpty()) {
                sb.append("<br>")
                if (estado != "5" && estado != "6") {
                    val expirado = isExpired(dataLimite, estado)
                    if (expirado) {
                        sb.append("<b>EXPIRADO:</b> $dataLimite")
                        holder.tvData.setTextColor(ContextCompat.getColor(context, R.color.vermelho_forte))
                    } else {
                        sb.append("<b>LIMITE:</b> $dataLimite")
                        holder.tvData.setTextColor(ContextCompat.getColor(context, R.color.cor_info_card_destaque))
                    }
                } else {
                    sb.append("<b>CONCLUÍDO</b>")
                    holder.tvData.setTextColor(ContextCompat.getColor(context, R.color.cor_info_card_destaque))
                }
            } else {
                sb.append(" (SEM DATA LIMITE)")
                holder.tvData.setTextColor(ContextCompat.getColor(context, R.color.cor_info_card_destaque))
            }
            holder.tvData.text = Html.fromHtml(sb.toString(), Html.FROM_HTML_MODE_LEGACY)
        } else {
            holder.tvData.text = "Atualizado a: $dataAtualizacao"
            holder.tvData.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.texto_dica))
        }

        val autor = formatarNome(ticket["22"], holder.tvRequester) ?: "Desconhecido"
        holder.tvRequester.text = Html.fromHtml("<b>Criado por:</b> $autor", Html.FROM_HTML_MODE_LEGACY)
        
        val tecnicoFinal = formatarNome(ticket["5"], holder.tvTechnician)
        val tecnicoExibicao = tecnicoFinal ?: "Não atribuído"
        holder.tvTechnician.text = Html.fromHtml("<b>Atribuído a:</b> $tecnicoExibicao", Html.FROM_HTML_MODE_LEGACY)

        // Botão EDITAR (Apenas se showEditButton for true)
        if (showEditButton) {
            holder.btnAtoresContainer.visibility = View.VISIBLE
            holder.lottieSucesso.visibility = View.GONE
            if (estado == "6") {
                holder.btnAtoresContainer.setBackgroundResource(R.drawable.bg_button_closed_oval)
            } else {
                holder.btnAtoresContainer.setBackgroundResource(R.drawable.bg_button_atores_oval)
            }
            holder.btnAtoresContainer.setOnClickListener {
                val ticketId = ticket["2"]?.toString()?.replace(".0", "") ?: return@setOnClickListener
                showAtoresManagementDialog(ticketId, ticket, autor, tecnicoExibicao, holder)
            }
        } else {
            holder.btnAtoresContainer.visibility = View.GONE
        }

        // Botão ELIMINAR (Custom Red Trash for Recycling)
        if (showDeleteButton) {
            holder.tvPriority.visibility = View.GONE
            holder.btnDeleteContainer.visibility = View.VISIBLE
            holder.btnDeleteContainer.setOnClickListener {
                val ticketId = ticket["2"]?.toString()?.replace(".0", "") ?: ticket["id"]?.toString()?.replace(".0", "") ?: return@setOnClickListener
                onDeleteClick?.invoke(ticketId, ticket)
            }
        } else {
            holder.tvPriority.visibility = View.VISIBLE
            holder.btnDeleteContainer.visibility = View.GONE
        }

        // Ações de Reciclagem (Restaurar / Purgar com X)
        if (showTrashActions) {
            holder.tvPriority.visibility = View.GONE
            holder.layoutTrashActions.visibility = View.VISIBLE
            
            holder.btnRestore.setOnClickListener {
                val ticketId = ticket["2"]?.toString()?.replace(".0", "") ?: ticket["id"]?.toString()?.replace(".0", "") ?: return@setOnClickListener
                onRestoreClick?.invoke(ticketId, ticket)
            }
            
            holder.btnPurge.setOnClickListener {
                val ticketId = ticket["2"]?.toString()?.replace(".0", "") ?: ticket["id"]?.toString()?.replace(".0", "") ?: return@setOnClickListener
                onPurgeClick?.invoke(ticketId, ticket)
            }
        } else {
            holder.layoutTrashActions.visibility = View.GONE
        }

        val deveExpandir = (expandedPosition == position) || (initialExpandedTicketId != null && idLimpo == initialExpandedTicketId)
        
        val targetVisibility = if (deveExpandir) View.VISIBLE else View.GONE
        
        holder.tvTitulo.maxLines = if (deveExpandir) 10 else 1
        holder.tvRequester.maxLines = if (deveExpandir) 10 else 1
        holder.tvTechnician.maxLines = if (deveExpandir) 10 else 1
        holder.tvDescricao.visibility = targetVisibility
        holder.vSeparador.visibility = targetVisibility
        
        // Os botões grandes de fundo da reciclagem aparecem apenas se estivermos na reciclagem
        holder.layoutBottomTrashActions.visibility = if (deveExpandir && showTrashActions) View.VISIBLE else View.GONE
        
        if (deveExpandir && showTrashActions) {
            holder.btnBottomRestore.setOnClickListener {
                val ticketId = ticket["2"]?.toString()?.replace(".0", "") ?: ticket["id"]?.toString()?.replace(".0", "") ?: return@setOnClickListener
                onRestoreClick?.invoke(ticketId, ticket)
            }
            holder.btnBottomPurge.setOnClickListener {
                val ticketId = ticket["2"]?.toString()?.replace(".0", "") ?: ticket["id"]?.toString()?.replace(".0", "") ?: return@setOnClickListener
                onPurgeClick?.invoke(ticketId, ticket)
            }
        }

        holder.llRespostasContainer.visibility = targetVisibility
        
        // Se este item deve expandir e ainda não carregámos as respostas, fazemo-lo agora
        if (deveExpandir) {
            val ticketId = ticket["2"]?.toString()?.replace(".0", "") ?: ""
            if (ticketId.isNotEmpty()) {
                carregarRespostas(holder, ticketId)
            }
        }

        holder.itemView.setOnClickListener {
            val prevExpanded = expandedPosition
            
            // Desmarcar seleção ao abrir/fechar expansão
            val prevSelected = selectedPosition
            selectedPosition = -1
            if (prevSelected != -1) notifyItemChanged(prevSelected)

            if (expandedPosition == position) {
                // Se já está aberto, fecha
                expandedPosition = -1
            } else {
                // Abre este e fecha o anterior
                expandedPosition = position
            }
            
            // Notificar instantaneamente sem animações de transição
            notifyDataSetChanged()
        }

        // --- FOCO EM TELA CHEIA (MENU FLUTUANTE) ---
        holder.itemView.setOnLongClickListener {
            showTicketFocusMode(ticket, holder, position)
            true
        }
    }

    private fun showAddFollowupDialog(ticketId: String, holder: TicketViewHolder) {
        val context = holder.itemView.context
        val dialogView = LayoutInflater.from(holder.itemView.context).inflate(R.layout.dialog_add_followup, null)
        
        val dialog = AlertDialog.Builder(holder.itemView.context, R.style.CustomAlertDialog).setView(dialogView).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val etContent = dialogView.findViewById<EditText>(R.id.et_followup_content)
        val btnEnviar = dialogView.findViewById<View>(R.id.btn_enviar_resposta)
        val btnClose = dialogView.findViewById<View>(R.id.iv_close_dialog)

        btnClose.setOnClickListener { dialog.dismiss() }

        btnEnviar.setOnClickListener {
            if (isRestricted(context)) {
                exibirAlertaPremium(context, "Sem permissão para responder.", dialogView)
                return@setOnClickListener
            }
            val content = etContent.text.toString().trim()
            if (content.isNotEmpty()) {
                enviarResposta(ticketId, content, holder, dialog)
            } else {
                exibirAlertaPremium(context, "Por favor, escreva uma mensagem.", dialogView, isError = true)
            }
        }

        dialog.show()
    }

    private fun enviarResposta(ticketId: String, content: String, holder: TicketViewHolder, dialog: AlertDialog) {
        val context = holder.itemView.context
        scope.launch {
            try {
                val sessionToken = PreferenceManager.getSessionToken(holder.itemView.context)
                val appToken = PreferenceManager.getAppToken(holder.itemView.context)

                val input = mapOf(
                    "input" to mapOf(
                        "itemtype" to "Ticket",
                        "items_id" to ticketId,
                        "content" to content
                    )
                )

                val response = GlpiRetrofit.api.addFollowup(sessionToken, appToken, input)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        exibirAlertaPremium(context, "Resposta enviada com sucesso!", null, isError = false)
                        dialog.dismiss()
                        carregarRespostas(holder, ticketId)
                    } else {
                        exibirAlertaPremium(context, "Erro ao enviar resposta: ${response.code()}", null, isError = true)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    exibirAlertaPremium(context, "Erro de rede: ${e.message}", null, isError = true)
                }
            }
        }
    }



    private val categoryMap = mutableMapOf<String, String>()

    private fun isRestricted(context: android.content.Context): Boolean {
        return PreferenceManager.isRestrictedProfile(context)
    }

    private fun isReadOnly(context: android.content.Context): Boolean {
        return PreferenceManager.isReadOnlyProfile(context)
    }

    private fun findActivity(context: android.content.Context): androidx.appcompat.app.AppCompatActivity? {
        var ctx = context
        while (ctx is android.content.ContextWrapper) {
            if (ctx is androidx.appcompat.app.AppCompatActivity) {
                return ctx
            }
            ctx = ctx.baseContext
        }
        return null
    }

    private fun exibirAlertaPremium(context: android.content.Context, mensagem: String, specificView: View? = null, isError: Boolean = false) {
        if (specificView != null) {
            AlertHelper.exibirAlertaPremium(specificView, mensagem, isError)
        } else if (context is android.app.Activity) {
            AlertHelper.exibirAlertaPremium(context, mensagem, isError)
        }
    }

    private val FONTES_PEDIDO = arrayOf("Direct", "E-Mail", "Formcreator", "Helpdesk", "Phone", "Written", "Other")
    private val PRIORIDADES = arrayOf("Muito Baixo", "Baixo", "Médio", "Alto", "Muito Alto", "Principal")
    private val ESTADOS = arrayOf("Novo", "A processar (atribuído)", "A processar (planeado)", "Aguardando", "Resolvido", "Encerrado")

    private fun showAtoresManagementDialog(ticketId: String, ticket: Map<String, Any?>, reqName: String, techName: String, holder: TicketViewHolder) {
        val context = holder.itemView.context
        val dialogView = LayoutInflater.from(holder.itemView.context).inflate(R.layout.dialog_manage_atores, null)
        val dialog = AlertDialog.Builder(context, R.style.CustomAlertDialog)
            .setView(dialogView)
            .create()

        val tvReq = dialogView.findViewById<TextView>(R.id.tv_current_requester)
        val btnEditReq = dialogView.findViewById<com.airbnb.lottie.LottieAnimationView>(R.id.btn_edit_requester)
        
        val llObserversContainer = dialogView.findViewById<LinearLayout>(R.id.ll_observers_container)
        val btnAddObserverField = dialogView.findViewById<View>(R.id.btn_add_observer_field)

        val llTechniciansContainer = dialogView.findViewById<LinearLayout>(R.id.ll_technicians_container)
        val btnAddTechField = dialogView.findViewById<View>(R.id.btn_add_technician_field)

        val layoutEstado = dialogView.findViewById<View>(R.id.layout_estado)
        val llEstadoOpcoes = dialogView.findViewById<LinearLayout>(R.id.ll_estado_opcoes)
        val tvEstadoSel = dialogView.findViewById<TextView>(R.id.tv_estado_selecionado)
        val ivEstadoSeta = dialogView.findViewById<ImageView>(R.id.iv_estado_seta)

        val rgTipo = dialogView.findViewById<RadioGroup>(R.id.rg_tipo)
        val rbIncidente = dialogView.findViewById<RadioButton>(R.id.rb_incidente)
        val rbPedido = dialogView.findViewById<RadioButton>(R.id.rb_pedido)
        
        val layoutCategoria = dialogView.findViewById<View>(R.id.layout_categoria)
        val llCategoriaOpcoes = dialogView.findViewById<LinearLayout>(R.id.ll_categoria_opcoes)
        val tvCategoriaSel = dialogView.findViewById<TextView>(R.id.tv_categoria_selecionada)
        val ivCategoriaSeta = dialogView.findViewById<ImageView>(R.id.iv_categoria_seta)

        val layoutFonte = dialogView.findViewById<View>(R.id.layout_fonte)
        val llFonteOpcoes = dialogView.findViewById<LinearLayout>(R.id.ll_fonte_opcoes)
        val tvFonteSel = dialogView.findViewById<TextView>(R.id.tv_fonte_selecionada)
        val ivFonteSeta = dialogView.findViewById<ImageView>(R.id.iv_fonte_seta)

        val layoutPrioridade = dialogView.findViewById<View>(R.id.layout_prioridade)
        val llPrioridadeOpcoes = dialogView.findViewById<LinearLayout>(R.id.ll_prioridade_opcoes)
        val tvPrioridadeSel = dialogView.findViewById<TextView>(R.id.tv_prioridade_selecionada)
        val ivPrioridadeSeta = dialogView.findViewById<ImageView>(R.id.iv_prioridade_seta)

        val btnSalvar = dialogView.findViewById<Button>(R.id.btn_salvar_ticket)
        val ivClose = dialogView.findViewById<ImageView>(R.id.iv_close_dialog)

        val tvTTO = dialogView.findViewById<TextView>(R.id.tv_tto_selecionado)
        val ivTTOCal = dialogView.findViewById<ImageView>(R.id.iv_tto_calendario)
        val tvTTR = dialogView.findViewById<TextView>(R.id.tv_ttr_selecionado)
        val ivTTRCal = dialogView.findViewById<ImageView>(R.id.iv_ttr_calendario)

        ivClose.setOnClickListener {
            dialog.dismiss()
        }

        val layoutRequester = dialogView.findViewById<View>(R.id.layout_requester)
        val llRequesterSearch = dialogView.findViewById<LinearLayout>(R.id.ll_requester_search_container)
        val etSearchReq = dialogView.findViewById<EditText>(R.id.et_search_requester)
        val rvReqResults = dialogView.findViewById<RecyclerView>(R.id.rv_requester_results)
        val pbReq = dialogView.findViewById<ProgressBar>(R.id.pb_search_requester)
        val ivReqSeta = dialogView.findViewById<ImageView>(R.id.iv_requester_seta)

        val isBlocked = isRestricted(context)
        val lockdownMsg = "Sem permissão para alterar tickets."

        fun showLockdownAlert() {
            exibirAlertaPremium(context, lockdownMsg, dialogView, isError = true)
        }

        Log.d("TicketAdapterDebug", "showAtoresManagementDialog: ticketId = $ticketId, ticket keys = ${ticket.keys}, ticket = $ticket")

        // 🔥 LÓGICA INTELIGENTE: Detetar valores atuais do ticket 🔥
        val typeValue = extractIdFunc?.invoke(ticket["14"]) ?: "1"
        if (typeValue == "2") {
            rbPedido.isChecked = true
            rbIncidente.isChecked = false
        } else {
            rbIncidente.isChecked = true
            rbPedido.isChecked = false
        }

        val priorityId = extractIdFunc?.invoke(ticket["3"]) ?: "3"
        tvPrioridadeSel.text = when(priorityId) {
            "1" -> "Muito Baixo"
            "2" -> "Baixo"
            "3" -> "Médio"
            "4" -> "Alto"
            "5" -> "Muito Alto"
            "6" -> "Principal"
            else -> "Médio"
        }

        tvFonteSel.text = "A carregar..."


        val rawCategory = ticket["7"]?.toString() ?: ""
        tvCategoriaSel.text = if (rawCategory.isNotEmpty() && rawCategory != "null") rawCategory else "Pesquisar categoria..."
        
        val realRequester = formatarNome(ticket["4"], tvReq)
        tvReq.text = realRequester ?: "Não definido"

        // TTO e TTR
        val currentTTO = ticket["158"]?.toString() ?: ""
        val currentTTR = ticket["151"]?.toString() ?: ""
        tvTTO.text = if (currentTTO.isNotEmpty() && currentTTO != "null") currentTTO else "---- / -- / --"
        tvTTR.text = if (currentTTR.isNotEmpty() && currentTTR != "null") currentTTR else "---- / -- / --"

        fun showDateTimePicker(textView: TextView) {
            if (isBlocked) {
                showLockdownAlert()
                return
            }

            val localePT = java.util.Locale("pt", "PT")
            java.util.Locale.setDefault(localePT)
            val config = context.resources.configuration
            config.setLocale(localePT)
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)

            val calendar = java.util.Calendar.getInstance()
            
            android.app.DatePickerDialog(context, R.style.GLPI_PickerTheme, { _, year, month, day ->
                calendar.set(java.util.Calendar.YEAR, year)
                calendar.set(java.util.Calendar.MONTH, month)
                calendar.set(java.util.Calendar.DAY_OF_MONTH, day)
                
                android.app.TimePickerDialog(context, R.style.GLPI_PickerTheme, { _, hour, minute ->
                    calendar.set(java.util.Calendar.HOUR_OF_DAY, hour)
                    calendar.set(java.util.Calendar.MINUTE, minute)
                    calendar.set(java.util.Calendar.SECOND, 0)
                    
                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", localePT)
                    textView.text = sdf.format(calendar.time)
                }, calendar.get(java.util.Calendar.HOUR_OF_DAY), calendar.get(java.util.Calendar.MINUTE), true).show()
            }, calendar.get(java.util.Calendar.YEAR), calendar.get(java.util.Calendar.MONTH), calendar.get(java.util.Calendar.DAY_OF_MONTH)).show()
        }

        rbIncidente.setOnClickListener { if (isBlocked) { showLockdownAlert(); rbPedido.isChecked = (typeValue == "2"); rbIncidente.isChecked = (typeValue != "2") } }
        rbPedido.setOnClickListener { if (isBlocked) { showLockdownAlert(); rbIncidente.isChecked = (typeValue != "2"); rbPedido.isChecked = (typeValue == "2") } }
        btnEditReq.setOnClickListener { if (isBlocked) showLockdownAlert() }

        ivTTOCal.setOnClickListener { showDateTimePicker(tvTTO) }
        ivTTRCal.setOnClickListener { showDateTimePicker(tvTTR) }
        tvTTO.setOnClickListener { showDateTimePicker(tvTTO) }
        tvTTR.setOnClickListener { showDateTimePicker(tvTTR) }

        fun setupExpandableCard(card: View, optionsContainer: LinearLayout, tvSelected: TextView, ivSeta: ImageView, options: Array<String>) {
            optionsContainer.removeAllViews()
            options.forEach { option ->
                val row = LinearLayout(holder.itemView.context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    setPadding(16, 32, 16, 32)
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    setOnClickListener {
                        tvSelected.text = option
                        for (i in 0 until optionsContainer.childCount) {
                            val child = optionsContainer.getChildAt(i)
                            if (child is LinearLayout) {
                                for (j in 0 until child.childCount) {
                                    val grandChild = child.getChildAt(j)
                                    if (grandChild is android.widget.RadioButton) {
                                        grandChild.isChecked = (child.getChildAt(0) as? TextView)?.text == option
                                    }
                                }
                            }
                        }
                        optionsContainer.visibility = View.GONE
                        ivSeta.rotation = 0f
                    }
                }

                val tvOption = TextView(holder.itemView.context).apply {
                    text = option
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    textSize = 14f
                    setTextColor(holder.itemView.context.getColor(R.color.texto_principal))
                }

                val rbSelection = android.widget.RadioButton(holder.itemView.context).apply {
                    buttonTintList = androidx.core.content.ContextCompat.getColorStateList(holder.itemView.context, R.color.radio_button_tint_selector)
                    isChecked = (option == tvSelected.text.toString())
                    isClickable = false
                    isFocusable = false
                }

                row.addView(tvOption)
                row.addView(rbSelection)
                optionsContainer.addView(row)

                val separator = View(holder.itemView.context).apply {
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1).apply { setMargins(16, 0, 16, 0) }
                    setBackgroundColor(androidx.core.content.ContextCompat.getColor(context, R.color.linha_separadora))
                }
                optionsContainer.addView(separator)
            }
            if (optionsContainer.childCount > 0) optionsContainer.removeViewAt(optionsContainer.childCount - 1)
            card.setOnClickListener {
                if (isBlocked) {
                    showLockdownAlert()
                    return@setOnClickListener
                }
                if (optionsContainer.visibility == View.GONE) {
                    optionsContainer.visibility = View.VISIBLE
                    ivSeta.rotation = 180f
                } else {
                    optionsContainer.visibility = View.GONE
                    ivSeta.rotation = 0f
                }
            }
        }

        fun setupExpandableUserSearch(
            card: View,
            container: LinearLayout,
            editText: EditText,
            recyclerView: RecyclerView,
            progressBar: ProgressBar,
            ivSeta: ImageView,
            tvSelected: TextView,
            roleType: Int
        ) {
            recyclerView.layoutManager = LinearLayoutManager(holder.itemView.context)
            val adapter = UserSearchAdapter { selectedUser ->
                val userIdStr = selectedUser["2"]?.toString()?.replace(".0", "") ?: ""
                val fName = selectedUser["34"]?.toString() ?: ""
                val lName = selectedUser["9"]?.toString() ?: ""
                val fullName = "$fName $lName".trim().ifEmpty { selectedUser["1"]?.toString() ?: "Utilizador" }
                atribuirTicketAoUsuario(ticketId, userIdStr, fullName, holder, roleType)
                tvSelected.text = fullName
                container.visibility = View.GONE
                ivSeta.rotation = 0f
            }
            recyclerView.adapter = adapter

            var searchJob: Job? = null
            editText.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    val q = s.toString().trim()
                    if (q.length < 2) { adapter.updateList(emptyList()); return }
                    searchJob?.cancel()
                    searchJob = scope.launch {
                        delay(400)
                        withContext(Dispatchers.Main) { progressBar.visibility = View.VISIBLE }
                        try {
                            val sessionToken = PreferenceManager.getSessionToken(holder.itemView.context)
                            val appToken = PreferenceManager.getAppToken(holder.itemView.context)
                            val criteria = mapOf(
                                "forcedisplay[0]" to "1",
                                "forcedisplay[1]" to "2",
                                "forcedisplay[2]" to "34",
                                "forcedisplay[3]" to "9",
                                "criteria[0][field]" to "1",
                                "criteria[0][searchtype]" to "contains",
                                "criteria[0][value]" to q,
                                "criteria[1][link]" to "OR",
                                "criteria[1][field]" to "9",
                                "criteria[1][searchtype]" to "contains",
                                "criteria[1][value]" to q,
                                "criteria[2][link]" to "OR",
                                "criteria[2][field]" to "34",
                                "criteria[2][searchtype]" to "contains",
                                "criteria[2][value]" to q
                            )
                            val resp = GlpiRetrofit.api.pesquisarUsuarios(sessionToken, appToken, criteria)

                            val users = if (resp.isSuccessful) resp.body()?.data ?: emptyList() else emptyList()
                            
                            val usersWithClear = mutableListOf<Map<String, Any>>()
                            usersWithClear.add(mapOf("1" to "Ninguém (Limpar)", "2" to "-1", "34" to "Ninguém", "9" to "(Limpar)"))
                            usersWithClear.addAll(users)

                            withContext(Dispatchers.Main) { adapter.updateList(usersWithClear) }
                        } catch (e: Exception) { Log.e("TicketAdapter", "In-place search failed", e) }
                        finally { withContext(Dispatchers.Main) { progressBar.visibility = View.GONE } }
                    }
                }
                override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
                override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            })

            card.setOnClickListener {
                if (isBlocked) {
                    showLockdownAlert()
                    return@setOnClickListener
                }
                if (container.visibility == View.GONE) {
                    container.visibility = View.VISIBLE
                    ivSeta.rotation = 180f
                    editText.requestFocus()
                } else {
                    container.visibility = View.GONE
                    ivSeta.rotation = 0f
                }
            }
        }

        fun updateCategoryUI() {
            llCategoriaOpcoes.removeAllViews()
            val sortedNames = categoryMap.keys.sorted()
            sortedNames.forEach { name ->
                val row = LinearLayout(holder.itemView.context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    setPadding(16, 32, 16, 32)
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    setOnClickListener {
                        tvCategoriaSel.text = name
                        for (i in 0 until llCategoriaOpcoes.childCount) {
                            val child = llCategoriaOpcoes.getChildAt(i)
                            if (child is LinearLayout) {
                                for (j in 0 until child.childCount) {
                                    val grandChild = child.getChildAt(j)
                                    if (grandChild is android.widget.RadioButton) {
                                        grandChild.isChecked = (child.getChildAt(0) as? TextView)?.text == name
                                    }
                                }
                            }
                        }
                        llCategoriaOpcoes.visibility = View.GONE
                        ivCategoriaSeta.rotation = 0f
                    }
                }

                val tvOption = TextView(holder.itemView.context).apply {
                    text = name
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    textSize = 14f
                    setTextColor(holder.itemView.context.getColor(R.color.texto_principal))
                }

                val rbSelection = android.widget.RadioButton(holder.itemView.context).apply {
                    buttonTintList = androidx.core.content.ContextCompat.getColorStateList(holder.itemView.context, R.color.radio_button_tint_selector)
                    isChecked = (name == tvCategoriaSel.text.toString())
                    isClickable = false
                    isFocusable = false
                }

                row.addView(tvOption)
                row.addView(rbSelection)
                llCategoriaOpcoes.addView(row)

                val separator = View(holder.itemView.context).apply {
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1).apply { setMargins(16, 0, 16, 0) }
                    setBackgroundColor(androidx.core.content.ContextCompat.getColor(context, R.color.linha_separadora))
                }
                llCategoriaOpcoes.addView(separator)
            }
        }

        if (categoryMap.isEmpty()) {
            scope.launch {
                try {
                    val sessionToken = PreferenceManager.getSessionToken(holder.itemView.context)
                    val appToken = PreferenceManager.getAppToken(holder.itemView.context)
                    val categories = GlpiRetrofit.api.getITILCategories(sessionToken, appToken)
                    withContext(Dispatchers.Main) {
                        categories.forEach { cat ->
                            val id = cat["id"]?.toString()?.substringBefore(".") ?: ""
                            val name = cat["completename"]?.toString() ?: cat["name"]?.toString() ?: ""
                            if (id.isNotEmpty() && name.isNotEmpty()) {
                                categoryMap[name] = id
                            }
                        }
                        updateCategoryUI()
                    }
                } catch (e: Exception) { Log.e("TicketAdapter", "Failed to fetch categories", e) }
            }
        } else {
            updateCategoryUI()
        }

        layoutCategoria.setOnClickListener {
            if (isBlocked) {
                showLockdownAlert()
                return@setOnClickListener
            }
            if (llCategoriaOpcoes.visibility == View.GONE) {
                llCategoriaOpcoes.visibility = View.VISIBLE
                ivCategoriaSeta.rotation = 180f
            } else {
                llCategoriaOpcoes.visibility = View.GONE
                ivCategoriaSeta.rotation = 0f
            }
        }

        // Setup Dropdowns
        setupExpandableCard(layoutFonte, llFonteOpcoes, tvFonteSel, ivFonteSeta, FONTES_PEDIDO)
        setupExpandableCard(layoutPrioridade, llPrioridadeOpcoes, tvPrioridadeSel, ivPrioridadeSeta, PRIORIDADES)
        setupExpandableUserSearch(layoutRequester, llRequesterSearch, etSearchReq, rvReqResults, pbReq, ivReqSeta, tvReq, 1)

        val blueFilter = PorterDuffColorFilter(holder.itemView.context.getColor(R.color.azul_glpi), PorterDuff.Mode.SRC_ATOP)
        btnEditReq.addValueCallback(com.airbnb.lottie.model.KeyPath("**"), com.airbnb.lottie.LottieProperty.COLOR_FILTER) { blueFilter }

        // Setup Estado (Status) Selector dropdown
        val currentStatusId = extractIdFunc?.invoke(ticket["12"]) ?: "1"
        tvEstadoSel.text = when(currentStatusId) {
            "1" -> "Novo"
            "2" -> "A processar (atribuído)"
            "3" -> "A processar (planeado)"
            "4" -> "Aguardando"
            "5" -> "Resolvido"
            "6" -> "Encerrado"
            else -> "Novo"
        }
        setupExpandableCard(layoutEstado, llEstadoOpcoes, tvEstadoSel, ivEstadoSeta, ESTADOS)

        // Multiple Observers Logic
        class ObserverCardState(
            var userId: String,
            var userName: String,
            val view: View
        )
        val observerCards = mutableListOf<ObserverCardState>()

        fun addObserverField(initialId: String, initialName: String) {
            val cardView = LayoutInflater.from(context).inflate(R.layout.item_observer_edit, llObserversContainer, false)
            val tvCurrentObserver = cardView.findViewById<TextView>(R.id.tv_current_observer)
            val btnRemoveObserver = cardView.findViewById<ImageView>(R.id.btn_remove_observer)
            val ivObserverSeta = cardView.findViewById<ImageView>(R.id.iv_observer_seta)
            val layoutRoot = cardView.findViewById<View>(R.id.layout_observer_root)
            val llObserverSearch = cardView.findViewById<LinearLayout>(R.id.ll_observer_search_container)
            val etSearchObserver = cardView.findViewById<EditText>(R.id.et_search_observer)
            val rvObserverResults = cardView.findViewById<RecyclerView>(R.id.rv_observer_results)
            val pbObserver = cardView.findViewById<ProgressBar>(R.id.pb_search_observer)
            val btnEditObserver = cardView.findViewById<com.airbnb.lottie.LottieAnimationView>(R.id.btn_edit_observer)

            tvCurrentObserver.text = initialName
            val cardState = ObserverCardState(initialId, initialName, cardView)
            observerCards.add(cardState)

            val filter = PorterDuffColorFilter(context.getColor(R.color.azul_glpi), PorterDuff.Mode.SRC_ATOP)
            btnEditObserver.addValueCallback(com.airbnb.lottie.model.KeyPath("**"), com.airbnb.lottie.LottieProperty.COLOR_FILTER) { filter }

            rvObserverResults.layoutManager = LinearLayoutManager(context)
            val adapter = UserSearchAdapter { selectedUser ->
                val userIdStr = selectedUser["2"]?.toString()?.replace(".0", "") ?: ""
                val fName = selectedUser["34"]?.toString() ?: ""
                val lName = selectedUser["9"]?.toString() ?: ""
                val fullName = "$fName $lName".trim().ifEmpty { selectedUser["1"]?.toString() ?: "Utilizador" }
                
                if (userIdStr == "-1") {
                    cardState.userId = ""
                    cardState.userName = "Sem observador"
                    tvCurrentObserver.text = "Sem observador"
                } else {
                    cardState.userId = userIdStr
                    cardState.userName = fullName
                    tvCurrentObserver.text = fullName
                }
                llObserverSearch.visibility = View.GONE
                ivObserverSeta.rotation = 0f
            }
            rvObserverResults.adapter = adapter

            var searchJob: Job? = null
            etSearchObserver.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    val q = s.toString().trim()
                    if (q.length < 2) { adapter.updateList(emptyList()); return }
                    searchJob?.cancel()
                    searchJob = scope.launch {
                        delay(400)
                        withContext(Dispatchers.Main) { pbObserver.visibility = View.VISIBLE }
                        try {
                            val sessionToken = PreferenceManager.getSessionToken(context)
                            val appToken = PreferenceManager.getAppToken(context)
                            val criteria = mapOf(
                                "forcedisplay[0]" to "1",
                                "forcedisplay[1]" to "2",
                                "forcedisplay[2]" to "34",
                                "forcedisplay[3]" to "9",
                                "criteria[0][field]" to "1",
                                "criteria[0][searchtype]" to "contains",
                                "criteria[0][value]" to q,
                                "criteria[1][link]" to "OR",
                                "criteria[1][field]" to "9",
                                "criteria[1][searchtype]" to "contains",
                                "criteria[1][value]" to q,
                                "criteria[2][link]" to "OR",
                                "criteria[2][field]" to "34",
                                "criteria[2][searchtype]" to "contains",
                                "criteria[2][value]" to q
                            )
                            val resp = GlpiRetrofit.api.pesquisarUsuarios(sessionToken, appToken, criteria)
                            val users = if (resp.isSuccessful) resp.body()?.data ?: emptyList() else emptyList()
                            
                            val usersWithClear = mutableListOf<Map<String, Any>>()
                            usersWithClear.add(mapOf("1" to "Ninguém (Limpar)", "2" to "-1", "34" to "Ninguém", "9" to "(Limpar)"))
                            usersWithClear.addAll(users)

                            withContext(Dispatchers.Main) { adapter.updateList(usersWithClear) }
                        } catch (e: Exception) { Log.e("TicketAdapter", "In-place search failed", e) }
                        finally { withContext(Dispatchers.Main) { pbObserver.visibility = View.GONE } }
                    }
                }
                override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
                override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            })

            layoutRoot.setOnClickListener {
                if (isBlocked) {
                    showLockdownAlert()
                    return@setOnClickListener
                }
                if (llObserverSearch.visibility == View.GONE) {
                    llObserverSearch.visibility = View.VISIBLE
                    ivObserverSeta.rotation = 180f
                    etSearchObserver.requestFocus()
                } else {
                    llObserverSearch.visibility = View.GONE
                    ivObserverSeta.rotation = 0f
                }
            }

            btnRemoveObserver.setOnClickListener {
                if (isBlocked) {
                    showLockdownAlert()
                    return@setOnClickListener
                }
                llObserversContainer.removeView(cardView)
                observerCards.remove(cardState)
                if (observerCards.isEmpty()) {
                    addObserverField("", "Sem observador")
                }
            }

            llObserversContainer.addView(cardView)
        }

        // Multiple Technicians Logic
        class TechCardState(
            var userId: String,
            var userName: String,
            val view: View
        )
        val techCards = mutableListOf<TechCardState>()

        fun addTechnicianField(initialId: String, initialName: String) {
            val cardView = LayoutInflater.from(context).inflate(R.layout.item_technician_edit, llTechniciansContainer, false)
            val tvCurrentTech = cardView.findViewById<TextView>(R.id.tv_current_technician)
            val btnRemoveTech = cardView.findViewById<ImageView>(R.id.btn_remove_technician)
            val ivTechSeta = cardView.findViewById<ImageView>(R.id.iv_technician_seta)
            val layoutRoot = cardView.findViewById<View>(R.id.layout_technician_root)
            val llTechSearch = cardView.findViewById<LinearLayout>(R.id.ll_technician_search_container)
            val etSearchTech = cardView.findViewById<EditText>(R.id.et_search_technician)
            val rvTechResults = cardView.findViewById<RecyclerView>(R.id.rv_technician_results)
            val pbTech = cardView.findViewById<ProgressBar>(R.id.pb_search_technician)
            val btnEditTech = cardView.findViewById<com.airbnb.lottie.LottieAnimationView>(R.id.btn_edit_technician)

            tvCurrentTech.text = initialName
            val cardState = TechCardState(initialId, initialName, cardView)
            techCards.add(cardState)

            val filter = PorterDuffColorFilter(context.getColor(R.color.azul_glpi), PorterDuff.Mode.SRC_ATOP)
            btnEditTech.addValueCallback(com.airbnb.lottie.model.KeyPath("**"), com.airbnb.lottie.LottieProperty.COLOR_FILTER) { filter }

            rvTechResults.layoutManager = LinearLayoutManager(context)
            val adapter = UserSearchAdapter { selectedUser ->
                val userIdStr = selectedUser["2"]?.toString()?.replace(".0", "") ?: ""
                val fName = selectedUser["34"]?.toString() ?: ""
                val lName = selectedUser["9"]?.toString() ?: ""
                val fullName = "$fName $lName".trim().ifEmpty { selectedUser["1"]?.toString() ?: "Utilizador" }
                
                if (userIdStr == "-1") {
                    cardState.userId = ""
                    cardState.userName = "Não atribuído"
                    tvCurrentTech.text = "Não atribuído"
                } else {
                    cardState.userId = userIdStr
                    cardState.userName = fullName
                    tvCurrentTech.text = fullName
                }
                llTechSearch.visibility = View.GONE
                ivTechSeta.rotation = 0f
            }
            rvTechResults.adapter = adapter

            var searchJob: Job? = null
            etSearchTech.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    val q = s.toString().trim()
                    if (q.length < 2) { adapter.updateList(emptyList()); return }
                    searchJob?.cancel()
                    searchJob = scope.launch {
                        delay(400)
                        withContext(Dispatchers.Main) { pbTech.visibility = View.VISIBLE }
                        try {
                            val sessionToken = PreferenceManager.getSessionToken(context)
                            val appToken = PreferenceManager.getAppToken(context)
                            val criteria = mapOf(
                                "forcedisplay[0]" to "1",
                                "forcedisplay[1]" to "2",
                                "forcedisplay[2]" to "34",
                                "forcedisplay[3]" to "9",
                                "criteria[0][field]" to "1",
                                "criteria[0][searchtype]" to "contains",
                                "criteria[0][value]" to q,
                                "criteria[1][link]" to "OR",
                                "criteria[1][field]" to "9",
                                "criteria[1][searchtype]" to "contains",
                                "criteria[1][value]" to q,
                                "criteria[2][link]" to "OR",
                                "criteria[2][field]" to "34",
                                "criteria[2][searchtype]" to "contains",
                                "criteria[2][value]" to q
                            )
                            val resp = GlpiRetrofit.api.pesquisarUsuarios(sessionToken, appToken, criteria)
                            val users = if (resp.isSuccessful) resp.body()?.data ?: emptyList() else emptyList()
                            
                            val usersWithClear = mutableListOf<Map<String, Any>>()
                            usersWithClear.add(mapOf("1" to "Ninguém (Limpar)", "2" to "-1", "34" to "Ninguém", "9" to "(Limpar)"))
                            usersWithClear.addAll(users)

                            withContext(Dispatchers.Main) { adapter.updateList(usersWithClear) }
                        } catch (e: Exception) { Log.e("TicketAdapter", "In-place search failed", e) }
                        finally { withContext(Dispatchers.Main) { pbTech.visibility = View.GONE } }
                    }
                }
                override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
                override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            })

            layoutRoot.setOnClickListener {
                if (isBlocked) {
                    showLockdownAlert()
                    return@setOnClickListener
                }
                if (llTechSearch.visibility == View.GONE) {
                    llTechSearch.visibility = View.VISIBLE
                    ivTechSeta.rotation = 180f
                    etSearchTech.requestFocus()
                } else {
                    llTechSearch.visibility = View.GONE
                    ivTechSeta.rotation = 0f
                }
            }

            btnRemoveTech.setOnClickListener {
                if (isBlocked) {
                    showLockdownAlert()
                    return@setOnClickListener
                }
                llTechniciansContainer.removeView(cardView)
                techCards.remove(cardState)
                if (techCards.isEmpty()) {
                    addTechnicianField("", "Não atribuído")
                }
            }

            llTechniciansContainer.addView(cardView)
        }

        fun extractTechnicians(value: Any?): List<Pair<String, String>> {
            if (value == null) return emptyList()
            
            fun processSingleTech(item: Any?): Pair<String, String>? {
                if (item == null) return null
                if (item is Map<*, *>) {
                    val id = item["id"]?.toString()?.substringBefore(".") ?: ""
                    val fname = item["firstname"]?.toString() ?: ""
                    val rname = item["realname"]?.toString() ?: ""
                    val cname = item["completename"]?.toString() ?: ""
                    val name = item["name"]?.toString() ?: ""
                    val rawName = if (fname.isNotEmpty() || rname.isNotEmpty()) {
                        "$fname $rname".trim()
                    } else {
                        cname.ifEmpty { name }
                    }
                    val processedName = formatarStringNome(rawName) ?: "[ID: $id]"
                    if (id.isNotEmpty()) {
                        return Pair(id, processedName)
                    }
                }
                val s = item.toString().trim()
                if (s == "null" || s.isEmpty()) return null
                if (s.contains("-") && s.length > 8) return null
                if (s.contains(">") || listOf("Software", "Hardware", "null", "Novo", "Incidente").any { s.equals(it, ignoreCase = true) }) return null
                val idPotencial = s.toDoubleOrNull()?.toInt()?.toString()
                if (idPotencial != null) {
                    val cached = UNAME_CACHE[idPotencial] ?: "[ID: $idPotencial]"
                    return Pair(idPotencial, formatarStringNome(cached) ?: cached)
                }
                return Pair("", formatarStringNome(s) ?: s)
            }

            if (value is List<*>) {
                return value.mapNotNull { processSingleTech(it) }
            }
            val single = processSingleTech(value)
            if (single != null) {
                return listOf(single)
            }
            return emptyList()
        }

        // Initialize technicians cards
        val initialTechs = extractTechnicians(ticket["5"])
        if (initialTechs.isEmpty()) {
            addTechnicianField("", "Não atribuído")
        } else {
            initialTechs.forEach { tech ->
                addTechnicianField(tech.first, tech.second)
            }
        }

        btnAddTechField.setOnClickListener {
            if (isBlocked) {
                showLockdownAlert()
                return@setOnClickListener
            }
            addTechnicianField("", "Não atribuído")
        }

        // Initialize observer cards
        val initialObservers = extractTechnicians(ticket["66"])
        if (initialObservers.isEmpty()) {
            addObserverField("", "Sem observador")
        } else {
            initialObservers.forEach { obs ->
                addObserverField(obs.first, obs.second)
            }
        }

        btnAddObserverField.setOnClickListener {
            if (isBlocked) {
                showLockdownAlert()
                return@setOnClickListener
            }
            addObserverField("", "Sem observador")
        }

        btnSalvar.setOnClickListener {
            if (isRestricted(context)) {
                exibirAlertaPremium(context, "Sem permissão para alterar tickets.", dialogView)
                return@setOnClickListener
            }
            val initialStatusId = extractIdFunc?.invoke(ticket["12"]) ?: "1"
            if (initialStatusId == "6") {
                exibirAlertaPremium(context, "Este ticket encontra-se encerrado e não pode ser editado.", dialogView, isError = true)
                return@setOnClickListener
            }
            val selectedType = if (rbIncidente.isChecked) 1 else 2
            val selectedCategory = tvCategoriaSel.text.toString()
            val selectedFonteName = tvFonteSel.text.toString()
            val selectedPrioridadeName = tvPrioridadeSel.text.toString()
            
            // Mapeamento de Fonte para requesttypes_id
            val selectedSourceId = when {
                selectedFonteName.equals("Helpdesk", ignoreCase = true) -> 1
                selectedFonteName.contains("Email", ignoreCase = true) || selectedFonteName.contains("E-Mail", ignoreCase = true) -> 2
                selectedFonteName.equals("Phone", ignoreCase = true) || selectedFonteName.equals("Telefone", ignoreCase = true) -> 3
                selectedFonteName.equals("Direct", ignoreCase = true) || selectedFonteName.equals("Directo", ignoreCase = true) || selectedFonteName.equals("Direto", ignoreCase = true) -> 4
                selectedFonteName.equals("Written", ignoreCase = true) || selectedFonteName.equals("Escrito", ignoreCase = true) -> 5
                selectedFonteName.equals("Other", ignoreCase = true) || selectedFonteName.equals("Outro", ignoreCase = true) -> 6
                selectedFonteName.equals("Formcreator", ignoreCase = true) -> 7
                else -> 4
            }

            // Mapeamento de Prioridade
            val selectedPriorityId = when(selectedPrioridadeName) {
                "Muito Baixo" -> 1
                "Baixo" -> 2
                "Médio" -> 3
                "Alto" -> 4
                "Muito Alto" -> 5
                "Principal" -> 6
                else -> 3
            }

            val selectedStatusName = tvEstadoSel.text.toString()
            val selectedStatusId = when (selectedStatusName) {
                "Novo" -> 1
                "A processar (atribuído)" -> 2
                "A processar (planeado)" -> 3
                "Aguardando" -> 4
                "Resolvido" -> 5
                "Encerrado" -> 6
                else -> 1
            }
            
            scope.launch {
                try {
                    val sessionToken = PreferenceManager.getSessionToken(holder.itemView.context)
                    val appToken = PreferenceManager.getAppToken(holder.itemView.context)
                    
                    val selectedCategoryName = tvCategoriaSel.text.toString()
                    val selectedCategoryId = categoryMap[selectedCategoryName]

                    val innerMap = mutableMapOf<String, Any>(
                        "id" to ticketId,
                        "type" to selectedType,
                        "requesttypes_id" to selectedSourceId,
                        "priority" to selectedPriorityId,
                        "status" to selectedStatusId
                    )
                    
                    if (selectedCategoryId != null) {
                        innerMap["itilcategories_id"] = selectedCategoryId
                    }
                    
                    val ttoStr = tvTTO.text.toString().trim()
                    if (ttoStr.contains("-") && ttoStr.length >= 10) {
                        innerMap["time_to_own"] = ttoStr
                    }
                    
                    val ttrStr = tvTTR.text.toString().trim()
                    if (ttrStr.contains("-") && ttrStr.length >= 10) {
                        innerMap["time_to_resolve"] = ttrStr
                    }

                    val input = mapOf("input" to (innerMap as Map<String, Any>))
                    
                    val resp = GlpiRetrofit.api.updateTicketStatus(ticketId, sessionToken, appToken, input)
                    
                    if (resp.isSuccessful) {
                        // Synchronize technicians
                        val respActors = GlpiRetrofit.api.getTicketActors(ticketId, sessionToken, appToken)
                        if (respActors.isSuccessful) {
                            val actors = respActors.body() ?: emptyList()
                            
                            // Technicians (Type = 2)
                            val serverTechs = actors.filter {
                                (it["type"]?.toString() ?: "").substringBefore(".") == "2"
                            }
                            val serverUserIds = serverTechs.map { (it["users_id"]?.toString() ?: "").substringBefore(".") }
                            val selectedUserIds = techCards.map { it.userId }.filter { it.isNotEmpty() }
                            
                            // Delete technicians removed in UI
                            serverTechs.forEach { actor ->
                                val userId = (actor["users_id"]?.toString() ?: "").substringBefore(".")
                                if (!selectedUserIds.contains(userId)) {
                                    val linkId = (actor["id"]?.toString() ?: "").substringBefore(".").toIntOrNull()
                                    if (linkId != null) {
                                        GlpiRetrofit.api.deleteTicketActor(linkId, sessionToken, appToken)
                                    }
                                }
                            }
                            
                            // Add technicians newly assigned in UI
                            selectedUserIds.forEach { userId ->
                                if (!serverUserIds.contains(userId)) {
                                    val tId = ticketId.toIntOrNull() ?: ticketId
                                    val uId = userId.toIntOrNull() ?: userId
                                    val inputUser = mapOf("input" to mapOf("tickets_id" to tId, "users_id" to uId, "type" to 2))
                                    val resUser = GlpiRetrofit.api.atribuirTicket(sessionToken, appToken, inputUser)
                                    if (resUser.isSuccessful) {
                                        val card = techCards.find { it.userId == userId }
                                        if (card != null) {
                                            UNAME_CACHE[userId] = card.userName
                                        }
                                    }
                                }
                            }

                            // Observers (Type = 3)
                            val serverObservers = actors.filter {
                                (it["type"]?.toString() ?: "").substringBefore(".") == "3"
                            }
                            val serverObserverUserIds = serverObservers.map { (it["users_id"]?.toString() ?: "").substringBefore(".") }
                            val selectedObserverUserIds = observerCards.map { it.userId }.filter { it.isNotEmpty() }
                            
                            // Delete observers removed in UI
                            serverObservers.forEach { actor ->
                                val userId = (actor["users_id"]?.toString() ?: "").substringBefore(".")
                                if (!selectedObserverUserIds.contains(userId)) {
                                    val linkId = (actor["id"]?.toString() ?: "").substringBefore(".").toIntOrNull()
                                    if (linkId != null) {
                                        GlpiRetrofit.api.deleteTicketActor(linkId, sessionToken, appToken)
                                    }
                                }
                            }
                            
                            // Add observers newly assigned in UI
                            selectedObserverUserIds.forEach { userId ->
                                if (!serverObserverUserIds.contains(userId)) {
                                    val tId = ticketId.toIntOrNull() ?: ticketId
                                    val uId = userId.toIntOrNull() ?: userId
                                    val inputUser = mapOf("input" to mapOf("tickets_id" to tId, "users_id" to uId, "type" to 3))
                                    val resUser = GlpiRetrofit.api.atribuirTicket(sessionToken, appToken, inputUser)
                                    if (resUser.isSuccessful) {
                                        val card = observerCards.find { it.userId == userId }
                                        if (card != null) {
                                            UNAME_CACHE[userId] = card.userName
                                        }
                                    }
                                }
                            }
                        }

                        // Update local dataset
                        val index = tickets.indexOfFirst { 
                            val tid = it["2"]?.toString()?.replace(".0", "") ?: it["id"]?.toString()?.replace(".0", "")
                            tid == ticketId 
                        }
                        if (index != -1) {
                            val mutableTicket = tickets[index].toMutableMap()
                            mutableTicket["12"] = selectedStatusId.toString()
                            
                            val activeTechs = techCards.filter { it.userId.isNotEmpty() }
                            if (activeTechs.isEmpty()) {
                                mutableTicket.remove("5")
                            } else {
                                mutableTicket["5"] = activeTechs.map { card ->
                                    mapOf(
                                        "id" to card.userId,
                                        "name" to card.userName
                                    )
                                }
                            }
                            
                            val activeObservers = observerCards.filter { it.userId.isNotEmpty() }
                            if (activeObservers.isEmpty()) {
                                mutableTicket.remove("66")
                            } else {
                                mutableTicket["66"] = activeObservers.map { card ->
                                    mapOf(
                                        "id" to card.userId,
                                        "name" to card.userName
                                    )
                                }
                            }
                            
                            val newList = tickets.toMutableList()
                            newList[index] = mutableTicket
                            tickets = newList
                        }

                        withContext(Dispatchers.Main) {
                            exibirAlertaPremium(context, "Ticket gravado com sucesso!", null, isError = false)
                            dialog.dismiss()
                            this@TicketAdapter.notifyDataSetChanged()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            exibirAlertaPremium(context, "Erro ao gravar alterações", null, isError = true)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("TicketAdapter", "Failed to save ticket", e)
                    withContext(Dispatchers.Main) {
                        exibirAlertaPremium(context, "Erro de conexão", null, isError = true)
                    }
                }
            }
        }

        // Fetch ticket details and ticket actors (technicians and observers) in parallel
        scope.launch(Dispatchers.IO) {
            try {
                val sessionToken = PreferenceManager.getSessionToken(context)
                val appToken = PreferenceManager.getAppToken(context)
                
                val (resp, respActors) = supervisorScope {
                    val ticketDeferred = async { GlpiRetrofit.api.getTicketById(ticketId, sessionToken, appToken) }
                    val actorsDeferred = async { GlpiRetrofit.api.getTicketActors(ticketId, sessionToken, appToken) }
                    
                    val r = try { ticketDeferred.await() } catch (e: Exception) {
                        Log.e("TicketAdapter", "Failed to fetch ticket $ticketId", e)
                        null
                    }
                    val ra = try { actorsDeferred.await() } catch (e: Exception) {
                        Log.e("TicketAdapter", "Failed to fetch actors for ticket $ticketId", e)
                        null
                    }
                    Pair(r, ra)
                }
                
                val fullTicket: Map<String, Any> = if (resp != null && resp.isSuccessful) resp.body() ?: emptyMap() else emptyMap()
                
                fun resolveSourceName(value: Any?): String {
                    if (value == null) return "Direct"
                    val str = when (value) {
                        is Map<*, *> -> value["name"]?.toString() ?: value["id"]?.toString() ?: ""
                        else -> value.toString()
                    }.trim()
                    return when {
                        str.isEmpty() || str == "null" || str == "0" -> "Direct"
                        str.contains("Helpdesk", ignoreCase = true) -> "Helpdesk"
                        str.contains("E-Mail", ignoreCase = true) || str.contains("Email", ignoreCase = true) -> "E-Mail"
                        str.contains("Phone", ignoreCase = true) || str.contains("Telefone", ignoreCase = true) -> "Phone"
                        str.contains("Written", ignoreCase = true) || str.contains("Escrito", ignoreCase = true) -> "Written"
                        str.contains("Other", ignoreCase = true) || str.contains("Outro", ignoreCase = true) -> "Other"
                        str.contains("Formcreator", ignoreCase = true) -> "Formcreator"
                        str.contains("Direct", ignoreCase = true) || str.contains("Direto", ignoreCase = true) -> "Direct"
                        str == "1" -> "Helpdesk"
                        str == "2" -> "E-Mail"
                        str == "3" -> "Phone"
                        str == "4" -> "Direct"
                        str == "5" -> "Written"
                        str == "6" -> "Other"
                        str == "7" -> "Formcreator"
                        str.toDoubleOrNull() != null -> "Direct"
                        else -> str
                    }
                }
                val sourceText = resolveSourceName(fullTicket["requesttypes_id"])
                
                val apiTTO = fullTicket["time_to_own"]?.toString() ?: ""
                val apiTTR = fullTicket["time_to_resolve"]?.toString() ?: ""
                
                val cleanTto = if (apiTTO.isNotEmpty() && apiTTO != "null") apiTTO else "---- / -- / --"
                val cleanTtr = if (apiTTR.isNotEmpty() && apiTTR != "null") apiTTR else "---- / -- / --"

                val techList = mutableListOf<Pair<String, String>>()
                val observerList = mutableListOf<Pair<String, String>>()
                var requesterId = ""
                var requesterName = ""
                var hasActors = false

                if (respActors != null && respActors.isSuccessful) {
                    val actors = respActors.body() ?: emptyList()
                    hasActors = true
                    actors.forEach { actor ->
                        val userId = (actor["users_id"]?.toString() ?: "").substringBefore(".")
                        val type = (actor["type"]?.toString() ?: "").substringBefore(".")
                        if (userId.isNotEmpty()) {
                            var name = UNAME_CACHE[userId]
                            if (name == null) {
                                try {
                                    val userResp = GlpiRetrofit.api.getUser(sessionToken, appToken, userId)
                                    if (userResp.isSuccessful) {
                                        val u = userResp.body()
                                        if (u != null) {
                                            val fname = u.firstname ?: ""
                                            val rname = u.realname ?: ""
                                            val uname = u.name ?: ""
                                            val cname = u.completename ?: ""
                                            val rawName = if (fname.isNotEmpty() || rname.isNotEmpty()) {
                                                "$fname $rname".trim()
                                            } else {
                                                cname.ifEmpty { uname.ifEmpty { "ID: $userId" } }
                                            }
                                            name = formatarStringNome(rawName) ?: "[ID: $userId]"
                                            UNAME_CACHE[userId] = name
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("TicketAdapter", "Failed to resolve user $userId name", e)
                                }
                            }
                            val resolvedName = name ?: "[ID: $userId]"
                            when (type) {
                                "1" -> {
                                    requesterId = userId
                                    requesterName = resolvedName
                                }
                                "2" -> techList.add(Pair(userId, resolvedName))
                                "3" -> observerList.add(Pair(userId, resolvedName))
                            }
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    tvFonteSel.text = sourceText
                    tvTTO.text = cleanTto
                    tvTTR.text = cleanTtr
                    
                    if (hasActors) {
                        if (requesterName.isNotEmpty()) {
                            tvReq.text = requesterName
                        }
                        
                        llTechniciansContainer.removeAllViews()
                        techCards.clear()
                        if (techList.isEmpty()) {
                            addTechnicianField("", "Não atribuído")
                        } else {
                            techList.forEach { tech ->
                                addTechnicianField(tech.first, tech.second)
                            }
                        }

                        llObserversContainer.removeAllViews()
                        observerCards.clear()
                        if (observerList.isEmpty()) {
                            addObserverField("", "Sem observador")
                        } else {
                            observerList.forEach { obs ->
                                addObserverField(obs.first, obs.second)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("TicketAdapter", "Parallel fetch failed", e)
                withContext(Dispatchers.Main) {
                    tvFonteSel.text = "Direct"
                }
            }
        }

        dialog.show()
        dialogView.clipToOutline = true

        val displayMetrics = context.resources.displayMetrics
        val maxHeight = (displayMetrics.heightPixels * 0.82).toInt()

        dialog.window?.let { window ->
            window.setLayout(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            )
            window.setBackgroundDrawableResource(android.R.color.transparent)

            val decorView = window.decorView
            decorView.post {
                try {
                    if (decorView.height > maxHeight) {
                        window.setLayout(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            maxHeight
                        )
                    }
                } catch (e: Exception) {
                    Log.e("DIALOG_LAYOUT", "Falha ao definir dimensões do diálogo", e)
                }
            }
        }
    }


    private fun atribuirTicketAoUsuario(ticketId: String, userIdString: String, name: String, holder: TicketViewHolder, roleType: Int = 2) {
        val btn = holder.btnAtoresContainer
        val ctx = btn.context
        
        if (isRestricted(ctx)) {
            exibirAlertaPremium(ctx, "Sem permissão para alterar responsáveis.", null, isError = true)
            return
        }
        
        val lottie = holder.lottieSucesso
        
        scope.launch {
            try {
                val ctx = btn.context
                val sToken = PreferenceManager.getSessionToken(ctx)
                val aToken = PreferenceManager.getAppToken(ctx)
                
                val tId = ticketId.toIntOrNull() ?: ticketId
                val uId = userIdString.toIntOrNull() ?: userIdString
                
                if (userIdString == "-1") {
                    // Lógica para LIMPAR: Buscar atribuições atuais e apagar a do tipo correspondente
                    val respActors = GlpiRetrofit.api.getTicketActors(ticketId, sToken, aToken)
                    if (respActors.isSuccessful) {
                        val actors = respActors.body() ?: emptyList()
                        actors.forEach { actor ->
                            val actorType = (actor["type"]?.toString() ?: "").substringBefore(".")
                            if (actorType == roleType.toString()) {
                                val actorId = (actor["id"]?.toString() ?: "").substringBefore(".").toIntOrNull()
                                if (actorId != null) {
                                    GlpiRetrofit.api.deleteTicketActor(actorId, sToken, aToken)
                                }
                            }
                        }
                    }
                    
                    withContext(Dispatchers.Main) {
                        lottie.visibility = View.VISIBLE
                        lottie.playAnimation()
                        val msg = if (roleType == 1) "Requerente removido!" else "Técnico removido!"
                        exibirAlertaPremium(ctx, msg, null, isError = false)
                        
                        // Atualizar visual local
                        atualizarDadosLocais(ticketId, "Não atribuído", "", roleType, holder)
                        delay(1000)
                        this@TicketAdapter.notifyDataSetChanged()
                    }
                    return@launch
                }

                // 🔥 NOVO: Antes de adicionar, remover os atores atuais do mesmo tipo (para ser uma substituição real)
                val respActors = GlpiRetrofit.api.getTicketActors(ticketId, sToken, aToken)
                if (respActors.isSuccessful) {
                    val actors = respActors.body() ?: emptyList()
                    actors.forEach { actor ->
                        val actorType = (actor["type"]?.toString() ?: "").substringBefore(".")
                        if (actorType == roleType.toString()) {
                            val actorId = (actor["id"]?.toString() ?: "").substringBefore(".").toIntOrNull()
                            if (actorId != null) {
                                GlpiRetrofit.api.deleteTicketActor(actorId, sToken, aToken)
                            }
                        }
                    }
                }

                Log.d("TicketAdapter", "Adding Role $roleType. Tkt $tId, User $uId ($name)")

                val inputUser = mapOf("input" to mapOf("tickets_id" to tId, "users_id" to uId, "type" to roleType))
                val res = GlpiRetrofit.api.atribuirTicket(sToken, aToken, inputUser)
                
                if (res.isSuccessful) {
                    if (roleType == 2) {
                        GlpiRetrofit.api.updateTicketStatus(ticketId, sToken, aToken, mapOf("input" to mapOf("status" to 2)))
                    }
                    UNAME_CACHE[userIdString] = name
                    withContext(Dispatchers.Main) {
                        lottie.visibility = View.VISIBLE
                        lottie.playAnimation()
                        val msg = if (roleType == 1) "Requerente associado!" else "Técnico associado!"
                        exibirAlertaPremium(ctx, msg, null, isError = false)
                        
                        atualizarDadosLocais(ticketId, name, userIdString, roleType, holder)
                        
                        delay(1500)
                        this@TicketAdapter.notifyDataSetChanged()
                    }
                } else {
                    val code = res.code()
                    val errorBody = res.errorBody()?.string() ?: "Sem detalhes"
                    Log.e("TicketAdapter", "API Fail: $code - $errorBody")
                    withContext(Dispatchers.Main) {
                        exibirAlertaPremium(ctx, "Erro $code: $errorBody", null, isError = true)
                    }
                }
            } catch (e: Exception) { 
                Log.e("TicketAdapter", "Action CRASHED", e)
                withContext(Dispatchers.Main) { 
                    exibirAlertaPremium(btn.context, "Erro: ${e.message}", null, isError = true)
                }
            }
        }
    }

    private fun atualizarDadosLocais(ticketId: String, name: String, userIdString: String, roleType: Int, holder: TicketViewHolder) {
        val index = tickets.indexOfFirst { 
            val tid = it["2"]?.toString()?.replace(".0", "") ?: it["id"]?.toString()?.replace(".0", "")
            tid == ticketId 
        }
        
        if (index != -1) {
            val mutableTicket = tickets[index].toMutableMap()
            if (roleType == 1) {
                if (userIdString == "-1") {
                    mutableTicket.remove("4")
                } else {
                    mutableTicket["4"] = name
                }
            } else {
                if (userIdString == "-1") {
                    mutableTicket.remove("5")
                } else {
                    mutableTicket["5"] = name
                }
            }
            val newList = tickets.toMutableList()
            newList[index] = mutableTicket
            tickets = newList
        }

        // Re-renderizar dinamicamente os textos com base nos dados reais atualizados do ticket
        val ticket = tickets.find { 
            (it["2"]?.toString()?.replace(".0", "") ?: it["id"]?.toString()?.replace(".0", "")) == ticketId 
        }
        if (ticket != null) {
            val autor = formatarNome(ticket["22"], holder.tvRequester) ?: "Desconhecido"
            holder.tvRequester.text = Html.fromHtml("<b>Criado por:</b> $autor", Html.FROM_HTML_MODE_LEGACY)

            val tecnicoFinal = formatarNome(ticket["5"], holder.tvTechnician)
            holder.tvTechnician.text = Html.fromHtml("<b>Atribuído a:</b> ${tecnicoFinal ?: "Não atribuído"}", Html.FROM_HTML_MODE_LEGACY)
        }
    }

    private fun fetchUserName(id: String, viewToUpdate: TextView?, onFetched: ((String) -> Unit)? = null) {
        if (id.isEmpty()) return
        PENDING_FETCHES.add(id)
        scope.launch {
            try {
                val ctx = viewToUpdate?.context ?: return@launch
                val response = GlpiRetrofit.api.getUser(PreferenceManager.getSessionToken(ctx), PreferenceManager.getAppToken(ctx), id)
                if (response.isSuccessful) {
                    val user = response.body() ?: return@launch
                    val fname = user.firstname ?: ""
                    val rname = user.realname ?: ""
                    val uname = user.name ?: ""
                    val cname = user.completename ?: ""
                    
                    val fullName = if (fname.isNotEmpty() || rname.isNotEmpty()) {
                        "$fname $rname".trim()
                    } else {
                        cname.ifEmpty { uname.ifEmpty { "ID: $id" } }
                    }
                    UNAME_CACHE[id] = fullName
                    
                    withContext(Dispatchers.Main) { 
                        onFetched?.invoke(fullName)
                        // Garantir que a view enviada é atualizada
                        if (viewToUpdate?.text?.contains(id) == true) {
                            val original = viewToUpdate.text.toString()
                            val updated = original.replace(id, fullName).replace("[ID: ", "").replace("]", "").replace("ID: ", "")
                            viewToUpdate.text = Html.fromHtml(updated, Html.FROM_HTML_MODE_LEGACY)
                        }
                        this@TicketAdapter.notifyDataSetChanged() 
                    }
                }
            } catch (e: Exception) { Log.e("TicketAdapter", "Fetch name failed for $id", e) }
            finally { PENDING_FETCHES.remove(id) }
        }
    }


    private fun carregarRespostas(holder: TicketViewHolder, ticketId: String) {
        val context = holder.itemView.context
        holder.llRespostasContainer.removeAllViews()

        // Animação Lottie de carregamento mais discreta e na cor da marca
        val lottieLoading = com.airbnb.lottie.LottieAnimationView(holder.itemView.context).apply {
            setAnimation(R.raw.loading)
            repeatCount = com.airbnb.lottie.LottieDrawable.INFINITE
            
            addValueCallback(
                com.airbnb.lottie.model.KeyPath("**"),
                com.airbnb.lottie.LottieProperty.COLOR_FILTER,
                com.airbnb.lottie.value.LottieValueCallback(android.graphics.PorterDuffColorFilter(holder.itemView.context.getColor(R.color.azul_glpi), android.graphics.PorterDuff.Mode.SRC_ATOP))
            )
            
            layoutParams = LinearLayout.LayoutParams(
                (80 * holder.itemView.context.resources.displayMetrics.density).toInt(),
                (80 * holder.itemView.context.resources.displayMetrics.density).toInt()
            ).apply {
                gravity = android.view.Gravity.CENTER_HORIZONTAL
                setMargins(0, 32, 0, 32)
            }
            playAnimation()
        }
        holder.llRespostasContainer.addView(lottieLoading)

        scope.launch {
            try {
                val sessionToken = PreferenceManager.getSessionToken(holder.itemView.context)
                val appToken = PreferenceManager.getAppToken(holder.itemView.context)

                val followups = try { GlpiRetrofit.api.getTicketFollowups(ticketId, sessionToken, appToken) } catch (e: Exception) { emptyList<Map<String, Any>>() }
                val solutions = try { GlpiRetrofit.api.getTicketSolutions(ticketId, sessionToken, appToken) } catch (e: Exception) { emptyList<Map<String, Any>>() }

                withContext(Dispatchers.Main) {
                    val allInteractions = (followups.map { it + ("type" to "followup") } +
                                          solutions.map { it + ("type" to "solution") })
                        .sortedByDescending { it["date_creation"]?.toString() ?: "" }

                    // Lista temporária para guardar as vistas antes de as injetar no layout
                    val viewsToShow = mutableListOf<View>()

                    if (allInteractions.isEmpty()) {
                        val tvNoData = TextView(holder.itemView.context).apply {
                            text = "Sem respostas adicionais."
                            setPadding(16, 16, 16, 16)
                            textSize = 13f
                            alpha = 0.5f
                            gravity = android.view.Gravity.CENTER_HORIZONTAL
                        }
                        viewsToShow.add(tvNoData)
                    } else {
                        // Título
                        val tvTitle = TextView(holder.itemView.context).apply {
                            text = "INTERAÇÕES"
                            setPadding(16, 24, 16, 8)
                            textSize = 13f
                            typeface = Typeface.DEFAULT_BOLD
                            setTextColor(holder.itemView.context.getColor(R.color.texto_principal))
                        }
                        viewsToShow.add(tvTitle)

                        allInteractions.forEach { item ->
                            val layoutRes = LinearLayout(holder.itemView.context).apply {
                                orientation = LinearLayout.VERTICAL
                                setPadding(24, 16, 24, 16)
                                background = holder.itemView.context.getDrawable(R.drawable.bg_cartao_brilhante)
                                elevation = 2f
                                val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                                lp.setMargins(0, 8, 0, 8)
                                layoutParams = lp
                            }

                            // Cabeçalho (Autor e Data)
                            val tvHeader = TextView(holder.itemView.context).apply {
                                val data = item["date_creation"]?.toString() ?: ""
                                val tipoStr = if (item["type"] == "solution") "SOLUÇÃO" else "RESPOSTA"
                                val userId = item["users_id"]?.toString()?.substringBefore(".") ?: ""
                                
                                val authorPart = if (userId.isNotEmpty()) {
                                    val cachedName = UNAME_CACHE[userId]
                                    if (cachedName != null) " - POR: $cachedName" else " - POR: [ID: $userId]"
                                } else ""
                                
                                text = "$tipoStr - $data$authorPart"
                                textSize = 14f
                                isAllCaps = true
                                setTextColor(holder.itemView.context.getColor(R.color.texto_principal))
                                typeface = Typeface.DEFAULT_BOLD
                                
                                if (userId.isNotEmpty() && UNAME_CACHE[userId] == null) {
                                    fetchUserName(userId, this)
                                }
                            }
                            layoutRes.addView(tvHeader)

                            // Conteúdo
                            val tvContent = TextView(holder.itemView.context).apply {
                                val rawContent = item["content"]?.toString() ?: ""
                                text = GlpiHtmlFixer.clean(rawContent)
                                textSize = 16f
                                setTextColor(holder.itemView.context.getColor(R.color.texto_principal))
                                setPadding(0, 8, 0, 0)
                            }
                            layoutRes.addView(tvContent)
                            viewsToShow.add(layoutRes)
                        }
                    }

                    // AGORA SIM: Injeção instantânea sem animações
                    holder.llRespostasContainer.removeAllViews()
                    viewsToShow.forEach { holder.llRespostasContainer.addView(it) }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    holder.llRespostasContainer.removeAllViews()
                    val tvError = TextView(holder.itemView.context).apply {
                        text = "Erro ao carregar respostas."
                        setPadding(16, 16, 16, 16)
                            textSize = 12f
                        alpha = 0.6f
                        gravity = android.view.Gravity.CENTER_HORIZONTAL
                    }
                    holder.llRespostasContainer.addView(tvError)
                    exibirAlertaPremium(context, "Erro de ligação", null, isError = true)
                }
            }
        }
    }

    private fun showTicketFocusMode(ticket: Map<String, Any>, originalHolder: TicketViewHolder, position: Int) {
        val context = originalHolder.itemView.context
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_ticket_focus_mode, null)
        
        // Criar Diálogo Full Screen adaptativo com centramento forçado
        val dialog = android.app.Dialog(context, android.R.style.Theme_NoTitleBar_Fullscreen)
        dialog.setContentView(dialogView)
        val isDarkMode = (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        val themeColor = context.getColor(R.color.fundo_app)

        dialog.window?.let { window ->
            window.setBackgroundDrawableResource(R.color.fundo_app)
            window.setLayout(android.view.WindowManager.LayoutParams.MATCH_PARENT, android.view.WindowManager.LayoutParams.MATCH_PARENT)
            window.setGravity(android.view.Gravity.CENTER)
            
            // Forçar que o conteúdo preencha as barras de sistema
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            
            // Barra de Navegação Adaptativa
            window.navigationBarColor = themeColor
            val controller = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
            controller.isAppearanceLightStatusBars = !isDarkMode
            controller.isAppearanceLightNavigationBars = !isDarkMode
        }

        // Inflar o ticket preservando as dimensões originais (passando o container como parent)
        val container = dialogView.findViewById<ViewGroup>(R.id.ticket_container)
        val ticketView = LayoutInflater.from(context).inflate(R.layout.item_ticket, container, false)
        container.addView(ticketView)
        
        // Aplicar o fundo e elevação originais para paridade exata
        ticketView.setBackgroundResource(R.drawable.bg_cartao_brilhante)
        ticketView.elevation = 4f

        // Popular dados (Sincronizado com o Holder original para manter fidelidade)
        val rawId = ticket["2"]?.toString() ?: ticket["id"]?.toString() ?: ""
        val idLimpo = if (rawId.contains(".")) rawId.substringBefore(".") else rawId
        ticketView.findViewById<TextView>(R.id.tv_ticket_id).text = "Ticket #$idLimpo"
        ticketView.findViewById<TextView>(R.id.tv_ticket_assunto).text = ticket["1"]?.toString() ?: "Sem título"
        ticketView.findViewById<TextView>(R.id.tv_ticket_assunto).maxLines = 1 // Restaurar tamanho original
        ticketView.findViewById<TextView>(R.id.tv_ticket_assunto).ellipsize = android.text.TextUtils.TruncateAt.END
        
        val prioridadeId = (ticket["3"] ?: "").toString().substringBefore(".")
        val prioridadeNome = when(prioridadeId) {
            "1" -> "MUITO BAIXA"
            "2" -> "BAIXA"
            "3" -> "MÉDIA"
            "4" -> "ALTA"
            "5" -> "MUITO ALTA"
            "6" -> "PRINCIPAL"
            else -> ""
        }
        val tvPrio = ticketView.findViewById<TextView>(R.id.tv_ticket_priority)
        tvPrio.text = prioridadeNome
        tvPrio.visibility = if (prioridadeNome.isNotEmpty()) View.VISIBLE else View.GONE
        tvPrio.setTextColor(ContextCompat.getColor(context, if (prioridadeId.toIntOrNull() ?: 0 >= 4) R.color.vermelho_forte else R.color.azul_glpi))

        val tvFocusReq = ticketView.findViewById<TextView>(R.id.tv_ticket_requester)
        tvFocusReq.text = originalHolder.tvRequester.text
        tvFocusReq.maxLines = 10

        val tvFocusTech = ticketView.findViewById<TextView>(R.id.tv_ticket_technician)
        tvFocusTech.text = originalHolder.tvTechnician.text
        tvFocusTech.maxLines = 10

        ticketView.findViewById<TextView>(R.id.tv_ticket_data).text = originalHolder.tvData.text

        // Garantir que a descrição não aparece se estiver colapsado no original
        ticketView.findViewById<View>(R.id.v_separador_descricao).visibility = View.GONE
        ticketView.findViewById<View>(R.id.tv_ticket_descricao).visibility = View.GONE

        // Ações
        val ticketId = idLimpo
        val reqStr = originalHolder.tvRequester.text.toString().substringAfter("Criado por: ").trim()
        val techStr = originalHolder.tvTechnician.text.toString().substringAfter("Atribuído a: ").trim()
 
        dialogView.findViewById<View>(R.id.btn_menu_responder).setOnClickListener {
            dialog.dismiss()
            showAddFollowupDialog(ticketId, originalHolder)
        }

        dialogView.findViewById<View>(R.id.btn_menu_editar).setOnClickListener {
            dialog.dismiss()
            showAtoresManagementDialog(ticketId, ticket, reqStr, techStr, originalHolder)
        }
        dialogView.findViewById<View>(R.id.btn_menu_reciclagem).setOnClickListener {
            val builder = AlertDialog.Builder(context)
            val confirmView = LayoutInflater.from(context).inflate(R.layout.dialog_delete_confirmation, null)
            builder.setView(confirmView)
            val confirmDialog = builder.create()
            confirmDialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))

            confirmView.findViewById<TextView>(R.id.tv_dialog_title).text = "MOVER PARA A RECICLAGEM"
            confirmView.findViewById<TextView>(R.id.tv_dialog_message).text = "Tem a certeza que deseja mover o ticket #$ticketId para a reciclagem?"
            
            confirmView.findViewById<View>(R.id.btn_cancelar_delete).setOnClickListener { confirmDialog.dismiss() }
            
            val btnConfirmar = confirmView.findViewById<Button>(R.id.btn_confirmar_delete)
            btnConfirmar.text = "CONFIRMAR"
            btnConfirmar.setBackgroundResource(R.drawable.bg_botao_vermelho)

            btnConfirmar.setOnClickListener {
                confirmDialog.dismiss()
                dialog.dismiss()
                if (onDeleteClick != null) onDeleteClick.invoke(ticketId, ticket)
                else enviarParaReciclagem(ticketId, context)
            }
            
            confirmDialog.show()
        }
        dialogView.findViewById<View>(R.id.btn_back_container).setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    private fun enviarParaReciclagem(ticketId: String, context: android.content.Context) {
        scope.launch {
            try {
                val sToken = PreferenceManager.getSessionToken(context)
                val aToken = PreferenceManager.getAppToken(context)
                val input = mapOf("input" to mapOf("id" to ticketId, "is_deleted" to 1))
                val res = GlpiRetrofit.api.updateTicketStatus(ticketId, sToken, aToken, input)
                withContext(Dispatchers.Main) {
                    if (res.isSuccessful) {
                        exibirAlertaPremium(context, "Ticket enviado para a reciclagem!", null, isError = false)
                    } else {
                        exibirAlertaPremium(context, "Erro ao remover ticket", null, isError = true)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    exibirAlertaPremium(context, "Erro de ligação", null, isError = true)
                }
            }
        }
    }

    override fun getItemCount(): Int = tickets.size
}