package com.example.glpimobile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ProfileAdapter(
    private var userList: List<Map<String, Any>>,
    private val showEmail: Boolean = false,
    private val onItemClick: ((Map<String, Any>) -> Unit)? = null
) :
    RecyclerView.Adapter<ProfileAdapter.ProfileViewHolder>() {

    class ProfileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvUsername: TextView = itemView.findViewById(R.id.tvUsername)
        val tvPerfil: TextView = itemView.findViewById(R.id.tvPerfil)
        val tvExtra: TextView = itemView.findViewById(R.id.tvExtra)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfileViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)
        return ProfileViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProfileViewHolder, position: Int) {
        val user = userList[position]

        // 1. Nome do Utilizador
        holder.tvUsername.text = user["1"]?.toString() ?: "Utilizador Desconhecido"

        // 2. Tratamento e Tradução do Perfil ou Mostrar Email
        if (showEmail) {
            holder.tvPerfil.text = user["5"]?.toString() ?: ""
        } else {
            val rawPerfil = user["20"]?.toString()
            holder.tvPerfil.text = when {
                rawPerfil == null || rawPerfil == "null" || rawPerfil.isEmpty() -> "Sem Perfil"
                rawPerfil.contains("Super-Admin", ignoreCase = true) -> "Super-Administrador"
                rawPerfil.contains("Admin", ignoreCase = true) -> "Administrador"
                rawPerfil.contains("Supervisor", ignoreCase = true) -> "Supervisor"
                rawPerfil.contains("Technician", ignoreCase = true) -> "Técnico"
                rawPerfil.contains("Hotliner", ignoreCase = true) -> "Hotliner"
                rawPerfil.contains("Observer", ignoreCase = true) -> "Observador"
                rawPerfil.contains("Read-Only", ignoreCase = true) -> "Apenas Leitura"
                else -> rawPerfil
            }
        }

        // Esconder o campo extra
        holder.tvExtra.text = ""
        holder.tvExtra.visibility = View.GONE

        holder.itemView.setOnClickListener {
            onItemClick?.invoke(user)
        }
    }

    override fun getItemCount(): Int = userList.size

    fun updateList(newList: List<Map<String, Any>>) {
        userList = newList
        notifyDataSetChanged()
    }
}