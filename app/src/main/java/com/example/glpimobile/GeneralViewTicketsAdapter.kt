package com.example.glpimobile

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class GeneralViewTicketsAdapter(
    private var tickets: List<Map<String, Any>>,
    private val hideStatusTag: Boolean = false,
    private val disableExpansion: Boolean = false
) : RecyclerView.Adapter<GeneralViewTicketsAdapter.GeneralViewHolder>() {

    fun updateList(newList: List<Map<String, Any>>) {
        tickets = newList
        notifyDataSetChanged()
    }

    class GeneralViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvId: TextView = itemView.findViewById(R.id.tv_ticket_id)
        val tvTitulo: TextView = itemView.findViewById(R.id.tv_ticket_assunto)
        val tvData: TextView = itemView.findViewById(R.id.tv_ticket_data)
        val tvStatus: TextView = itemView.findViewById(R.id.tv_ticket_status)
        val tvDescricao: TextView = itemView.findViewById(R.id.tv_ticket_descricao)
        var isExpanded: Boolean = false
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GeneralViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_ticket_history, parent, false)
        return GeneralViewHolder(view)
    }

    private fun extractId(valor: Any?): String {
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

    override fun onBindViewHolder(holder: GeneralViewHolder, position: Int) {
        val ticket = tickets[position]

        val rawId = ticket["2"]?.toString() ?: ticket["id"]?.toString() ?: ""
        val idLimpo = if (rawId.contains(".")) rawId.substringBefore(".") else rawId

        val titulo = ticket["1"]?.toString() ?: "Sem título"
        val dataAtualizacao = ticket["19"]?.toString() ?: ""
        val estado = extractId(ticket["12"])
        val descricaoHtml = ticket["21"]?.toString() ?: ""

        holder.tvId.text = "Ticket #$idLimpo"
        holder.tvTitulo.text = titulo
        holder.tvData.text = "Atualizado a: $dataAtualizacao"
        holder.tvDescricao.text = GlpiHtmlFixer.clean(descricaoHtml)

        val context = holder.itemView.context
        val txtStatus = when (estado) {
            "1" -> "NOVO"
            "2" -> "ATRIBUÍDO"
            "3" -> "PLANEADO"
            "4" -> "AGUARDANDO"
            "5" -> "RESOLVIDO"
            "6" -> "ENCERRADO"
            else -> {
                val upper = estado.uppercase()
                when {
                    upper.contains("ATRIBUÍDO") || upper.contains("ATRIBUIDO") -> "ATRIBUÍDO"
                    upper.contains("PLANEADO") -> "PLANEADO"
                    upper.contains("NOVO") -> "NOVO"
                    upper.contains("AGUARDANDO") -> "AGUARDANDO"
                    upper.contains("RESOLVIDO") -> "RESOLVIDO"
                    upper.contains("ENCERRADO") -> "ENCERRADO"
                    else -> if (estado.isNotEmpty()) estado else "ESTADO: $estado"
                }
            }
        }
        holder.tvStatus.text = txtStatus
        holder.tvStatus.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(context, R.color.azul_glpi)
        )
        
        if (hideStatusTag) {
            holder.tvStatus.visibility = View.GONE
        } else {
            holder.tvStatus.visibility = View.VISIBLE
        }

        // Garantir estado correto ao reciclar
        holder.isExpanded = false
        holder.tvDescricao.visibility = View.GONE

        if (disableExpansion) {
            holder.itemView.setOnClickListener(null)
        } else {
            holder.itemView.setOnClickListener {
                holder.isExpanded = !holder.isExpanded
                holder.tvDescricao.visibility = if (holder.isExpanded) View.VISIBLE else View.GONE
            }
        }
    }

    override fun getItemCount(): Int = tickets.size
}
