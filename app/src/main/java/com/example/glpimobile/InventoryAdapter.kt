package com.example.glpimobile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class InventoryAdapter(private var inventoryList: List<Map<String, Any>>) :
    RecyclerView.Adapter<InventoryAdapter.InventoryViewHolder>() {

    class InventoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNome: TextView = view.findViewById(R.id.tv_equipamento_nome)
        val tvUtilizador: TextView = view.findViewById(R.id.tv_utilizador)
        val tvDetalhes: TextView = view.findViewById(R.id.tv_detalhes_serial)
    }

    fun updateList(newList: List<Map<String, Any>>) {
        this.inventoryList = newList
        notifyDataSetChanged()
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
                    if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() 
                }
            }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InventoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_inventory, parent, false)
        return InventoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: InventoryViewHolder, position: Int) {
        val item = inventoryList[position]

        // 1. Nome (Campo 1)
        val nome = item["1"]?.toString() ?: "Sem Nome"
        holder.tvNome.text = nome

        // 🔥 2. LÊ O CARIMBO QUE COLOCÁMOS NA ACTIVITY 🔥
        val tipoReal = item["TipoReal"]?.toString() ?: "Desconhecido"

        // 3. Tradução do Tipo para Português
        val tipoPt = when (tipoReal) {
            "Computer" -> "Computador"
            "Monitor" -> "Monitor"
            "NetworkEquipment" -> "Rede"
            "Printer" -> "Impressora"
            "Peripheral" -> "Periférico"
            else -> tipoReal
        }

        // 4. Departamento / Localização (Campo 3)
        val rawDep = item["3"]
        val departamento = when (rawDep) {
            is Map<*, *> -> rawDep["name"]?.toString() ?: rawDep["completename"]?.toString() ?: ""
            else -> rawDep?.toString() ?: ""
        }.let { if (it.isEmpty() || it == "null") "Sem Localização" else it }

        // 5. Busca Alargada de Utilizador (Campos 70, 8, 24, 15)
        fun extractName(raw: Any?): String {
            return when (raw) {
                is Map<*, *> -> raw["name"]?.toString() ?: raw["login"]?.toString() ?: ""
                else -> raw?.toString() ?: ""
            }.let { if (it == "null") "" else it }.trim()
        }

        val name70 = extractName(item["70"]) // Utilizador Principal
        val name8 = extractName(item["8"])   // Técnico Responsável
        val name24 = extractName(item["24"]) // Utilizador Alternativo
        val name15 = extractName(item["15"]) // Nome Alternativo

        val utilizadorFinal = when {
            name70.isNotEmpty() -> name70
            name8.isNotEmpty() -> name8
            name24.isNotEmpty() -> name24
            name15.isNotEmpty() -> name15
            else -> ""
        }

        val utilizadorExibicao = if (utilizadorFinal.isNotEmpty()) {
            formatarStringNome(utilizadorFinal) ?: "Sem Utilizador Atribuído"
        } else {
            "Sem Utilizador Atribuído"
        }

        if (tipoReal == "Computer" || tipoReal == "Monitor") {
            holder.tvUtilizador.visibility = View.VISIBLE
            holder.tvUtilizador.text = "Utilizador: $utilizadorExibicao"
        } else {
            holder.tvUtilizador.visibility = View.GONE
        }

        // 6. Serial Number (Campo 5)
        val rawSerial = item["5"]?.toString() ?: ""
        val serial = if (rawSerial.isEmpty() || rawSerial == "null") "---" else rawSerial

        // 🔥 CONFIGURAÇÃO VISUAL 🔥
        holder.tvDetalhes.text = "$tipoPt | $departamento | $serial"
        holder.tvDetalhes.visibility = View.VISIBLE

        holder.itemView.setOnClickListener {
            onItemClick?.invoke(item)
        }
    }

    var onItemClick: ((Map<String, Any>) -> Unit)? = null

    override fun getItemCount() = inventoryList.size
}