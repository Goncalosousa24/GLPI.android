package com.example.glpimobile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class ActionHistoryAdapter(private var list: List<Map<String, Any>>) : RecyclerView.Adapter<ActionHistoryAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val title: TextView = v.findViewById(R.id.tv_action_title)
        val detail: TextView = v.findViewById(R.id.tv_action_detail)
        val time: TextView = v.findViewById(R.id.tv_action_time)
        val icon: ImageView = v.findViewById(R.id.iv_action_icon)
        val timelineTop: View = v.findViewById(R.id.view_timeline_top)
        val timelineBottom: View = v.findViewById(R.id.view_timeline_bottom)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_action_log, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = list[position]
        
        // Esconder linhas da timeline no topo e fundo para estética
        holder.timelineTop.visibility = if (position == 0) View.INVISIBLE else View.VISIBLE
        holder.timelineBottom.visibility = if (position == itemCount - 1) View.INVISIBLE else View.VISIBLE

        // Tradução amigável de Itemtypes
        val technicalType = (item["itemtype"] ?: item["3"])?.toString() ?: "Item"
        val itemtype = when(technicalType) {
            "Computer" -> "Computador"
            "Monitor" -> "Monitor"
            "Ticket" -> "Ticket"
            "Software" -> "Software"
            "NetworkEquipment" -> "Equip. Rede"
            "Printer" -> "Impressora"
            "Peripheral" -> "Periférico"
            "Phone" -> "Telefone"
            "User" -> "Utilizador"
            "MailCollector" -> "Serviço Correio (Sistema)"
            else -> technicalType
        }

        val itemId = (item["items_id"] ?: item["4"])?.toString() ?: "?"
        
        // Tradução amigável de Campos (id_search_option)
        val rawField = (item["id_search_option"] ?: item["6"])?.toString() ?: ""
        val fieldName = when(rawField) {
            "1" -> "ID"
            "2" -> "Nome"
            "12" -> "Estado"
            "70", "5" -> "Técnico/Utilizador"
            "80" -> "Nº Série"
            "31" -> "Estado (Inventário)"
            "18" -> "Localização"
            "19" -> "Categoria"
            else -> rawField
        }

        val dateMod = (item["date_mod"] ?: item["2"])?.toString() ?: ""
        val newValue = (item["new_value"] ?: item["8"])?.toString() ?: ""

        val acaoTraduzida: String
        val detalheTraduzido: String
        var iconRes = android.R.drawable.ic_menu_edit

        // Mapeamento Humano das Ações do GLPI
        when {
            rawField == "1" || rawField.isEmpty() && newValue.isNotEmpty() -> {
                acaoTraduzida = "Criação de $itemtype"
                detalheTraduzido = "Criaste o $itemtype #$itemId"
                iconRes = android.R.drawable.ic_menu_add
            }
            rawField == "12" || fieldName.contains("status", ignoreCase = true) -> {
                acaoTraduzida = "Mudança de Estado"
                detalheTraduzido = "Alteraste o estado do $itemtype #$itemId para '$newValue'"
                iconRes = android.R.drawable.ic_popup_sync
            }
            fieldName.contains("solve", ignoreCase = true) || newValue == "5" -> {
                acaoTraduzida = "Resolução"
                detalheTraduzido = "Resolveste o $itemtype #$itemId"
                iconRes = android.R.drawable.checkbox_on_background
            }
            else -> {
                acaoTraduzida = "Atualização em $itemtype"
                val campoExibicao = if (fieldName.isNotEmpty()) "o campo '$fieldName'" else "o item"
                detalheTraduzido = "Modificaste $campoExibicao no $itemtype #$itemId"
            }
        }

        holder.title.text = acaoTraduzida.uppercase()
        holder.detail.text = detalheTraduzido
        holder.time.text = formatarDataRelativa(dateMod)
        holder.icon.setImageResource(iconRes)
    }

    override fun getItemCount() = list.size

    fun updateList(newList: List<Map<String, Any>>) {
        list = newList
        notifyDataSetChanged()
    }

    private fun formatarDataRelativa(dataString: String): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val data = sdf.parse(dataString)
            val agora = Date()
            val diff = agora.time - (data?.time ?: agora.time)
            
            val segundos = diff / 1000
            val minutos = segundos / 60
            val horas = minutos / 60
            val dias = horas / 24

            when {
                segundos < 60 -> "agora mesmo"
                minutos < 60 -> "há $minutos min"
                horas < 24 -> "há $horas h"
                dias < 7 -> "há $dias dias"
                else -> dataString.substring(0, 10)
            }
        } catch (e: Exception) {
            dataString
        }
    }
}
