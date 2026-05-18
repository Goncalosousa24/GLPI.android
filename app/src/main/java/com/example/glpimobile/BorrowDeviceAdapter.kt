package com.example.glpimobile

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.glpimobile.databinding.ItemBorrowDeviceBinding
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class BorrowDeviceAdapter(
    private var equipamentos: List<GlpiEquipamento>,
    private val onBorrowSuccess: () -> Unit // Callback to refresh the list 
) : RecyclerView.Adapter<BorrowDeviceAdapter.BorrowViewHolder>() {

    class BorrowViewHolder(val binding: ItemBorrowDeviceBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BorrowViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ItemBorrowDeviceBinding.inflate(layoutInflater, parent, false)
        return BorrowViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BorrowViewHolder, position: Int) {
        val item = equipamentos[position]
        val context = holder.itemView.context

        with(holder.binding) {
            txtDeviceNameBorrow.text = if (item.name.isNullOrBlank()) "Equipamento Sem Nome" else item.name
            txtDeviceSerialBorrow.text = if (item.serial.isNullOrBlank()) "S/N: Desconhecido" else "S/N: ${item.serial}"
            txtDeviceUserBorrow.text = if (item.utilizador.isNullOrBlank()) "Sem utilizador" else item.utilizador

            val estadoText = if (item.estadoStr.isNullOrBlank()) "Não definido" else item.estadoStr.replaceFirstChar { it.uppercase() }
            txtCurrentStateBorrow.text = "Estado: $estadoText"

            btnEmprestarDevice.setOnClickListener {
                btnEmprestarDevice.isEnabled = false
                criarReservaNoGlpi(context, item) {
                    btnEmprestarDevice.isEnabled = true
                }
            }
        }
    }

    private fun criarReservaNoGlpi(context: android.content.Context, item: GlpiEquipamento, onComplete: () -> Unit) {
        val TOKEN_SESSAO = GlpiConfig.SESSION_TOKEN
        val TOKEN_APP = GlpiConfig.APP_TOKEN

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Passo 1: Verificar se já é reservável
                var resItemId: Int? = null
                val resItemResponse = GlpiRetrofit.api.getReservationItemForAsset(item.itemtype, item.id, TOKEN_SESSAO, TOKEN_APP)

                if (resItemResponse.isSuccessful) {
                    val resList = resItemResponse.body()
                    if (resList.isNullOrEmpty()) {
                        // Tornar reservável (Criar ReservationItem)
                        val payloadItem = mapOf("input" to mapOf("itemtype" to item.itemtype, "items_id" to item.id, "entities_id" to 0, "is_active" to 1))
                        val createResItem = GlpiRetrofit.api.createReservationItem(TOKEN_SESSAO, TOKEN_APP, payloadItem)
                        if (createResItem.isSuccessful) {
                            resItemId = (createResItem.body()?.get("id") as? Double)?.toInt()
                        }
                    } else {
                        resItemId = (resList[0]["id"] as? Double)?.toInt()
                    }
                }

                if (resItemId == null) {
                    withContext(Dispatchers.Main) { Toast.makeText(context, "Erro: GLPI não permite reservar este item.", Toast.LENGTH_SHORT).show(); onComplete() }
                    return@launch
                }

                // Passo 2: Validar se já existe reserva ativa
                val reservationsResponse = GlpiRetrofit.api.getReservationsForItem(resItemId, TOKEN_SESSAO, TOKEN_APP)
                if (reservationsResponse.isSuccessful) {
                    val res = reservationsResponse.body()
                    if (!res.isNullOrEmpty()) {
                        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        val now = Date()
                        val isReserved = res.any {
                            val endStr = it["end"] as? String
                            if (endStr != null) {
                                try {
                                    val endDate = format.parse(endStr)
                                    endDate != null && endDate.after(now)
                                } catch(e: Exception) { false }
                            } else false
                        }

                        if (isReserved) {
                            withContext(Dispatchers.Main) { Toast.makeText(context, "Atenção: Este equipamento já tem uma reserva ativa!", Toast.LENGTH_LONG).show(); onComplete() }
                            return@launch
                        }
                    }
                }

                // Passo 3: Efetuar Reserva
                val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val cal = Calendar.getInstance()
                val beginStr = format.format(cal.time)
                cal.add(Calendar.DAY_OF_YEAR, 7)
                val endStr = format.format(cal.time)

                val reqPayload = mapOf("input" to mapOf(
                    "reservationitems_id" to resItemId,
                    "begin" to beginStr,
                    "end" to endStr,
                    "comment" to "Empréstimo registado via App Mobile"
                ))

                val finalCreate = GlpiRetrofit.api.createReservation(TOKEN_SESSAO, TOKEN_APP, reqPayload)

                withContext(Dispatchers.Main) {
                    if (finalCreate.isSuccessful) {
                        Toast.makeText(context, "Reserva registada no calendário GLPI!", Toast.LENGTH_SHORT).show()
                        onBorrowSuccess() // Atualiza UI caso seja necessário
                    } else {
                        Toast.makeText(context, "Erro ao criar reserva no servidor", Toast.LENGTH_SHORT).show()
                    }
                    onComplete()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Erro de ligação API: ${e.message}", Toast.LENGTH_SHORT).show()
                    onComplete()
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
