package com.example.glpimobile

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.glpimobile.databinding.ItemActivityLogBinding
import kotlinx.coroutines.*

class ActivityLogAdapter(
    private var equipamentos: List<GlpiEquipamento>,
    private val onAlterarClick: (GlpiEquipamento) -> Unit
) : RecyclerView.Adapter<ActivityLogAdapter.LogViewHolder>() {

    class LogViewHolder(val binding: ItemActivityLogBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ItemActivityLogBinding.inflate(layoutInflater, parent, false)
        return LogViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        val item = equipamentos[position]
        val context = holder.itemView.context

        with(holder.binding) {
            txtDeviceName.text = if (item.name.isNullOrBlank()) "Equipamento Sem Nome" else item.name
            txtDeviceSerial.text = if (item.serial.isNullOrBlank()) "S/N: Desconhecido" else "S/N: ${item.serial}"
            txtDeviceUser.text = if (item.utilizador.isNullOrBlank()) "Sem utilizador" else item.utilizador

            val estadoText = if (item.estadoStr.isNullOrBlank()) "Não definido" else item.estadoStr.replaceFirstChar { it.uppercase() }
            txtCurrentState.text = "Estado: $estadoText"

            // 🔥 LOGICA DE ALTERAÇÃO DE ESTADO DELEGADA À ACTIVITY 🔥
            btnAlterarEstado.setOnClickListener {
                onAlterarClick(item)
            }
        }
    }

    private fun atualizarEstadoNoGlpi(context: android.content.Context, item: GlpiEquipamento, novoId: Int, nomeEstado: String) {
        val TOKEN_SESSAO = GlpiConfig.SESSION_TOKEN
        val TOKEN_APP = GlpiConfig.APP_TOKEN

        // O GLPI quer os dados neste formato exato
        val inputData = mapOf("input" to mapOf("states_id" to novoId))

        // Usamos uma Coroutine simples para não travar a App
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = GlpiRetrofit.api.updateItem(
                    itemtype = item.itemtype, // "Computer"
                    id = item.id,
                    sessionToken = TOKEN_SESSAO,
                    appToken = TOKEN_APP,
                    input = inputData
                )

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(context, "Sucesso: $nomeEstado", Toast.LENGTH_SHORT).show()

                        // Opcional: Aqui podias chamar uma função na Activity para fazer refresh
                        // Mas para já, o utilizador já sabe que mudou!
                    } else {
                        Toast.makeText(context, "Erro ao atualizar no servidor", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Erro de ligação: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun getItemCount() = equipamentos.size

    fun updateData(novosEquipamentos: List<GlpiEquipamento>) {
        this.equipamentos = novosEquipamentos
        notifyDataSetChanged()
    }
}