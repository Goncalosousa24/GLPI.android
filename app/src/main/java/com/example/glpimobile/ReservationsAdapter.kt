package com.example.glpimobile

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class ReservationsAdapter(
    private var dataSet: List<GlpiResDevice>,
    private val onReserveClick: (GlpiResDevice) -> Unit,
    private val onDetailsClick: (GlpiResDevice) -> Unit
) : RecyclerView.Adapter<ReservationsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tv_res_item_name)
        val tvType: TextView = view.findViewById(R.id.tv_res_item_type)
        val tvStatus: TextView = view.findViewById(R.id.tv_res_status)
        val tvDetails: TextView = view.findViewById(R.id.tv_res_details)
        val btnReserve: Button = view.findViewById(R.id.btn_reservar_acao)
        val btnDetails: Button = view.findViewById(R.id.btn_res_detalhes)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reservation, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = dataSet[position]
        holder.tvName.text = item.name
        holder.tvType.text = "TIPO: ${item.itemType.uppercase()}"
        
        val context = holder.itemView.context
        if (item.isReservedNow) {
            holder.tvStatus.text = "RESERVADO"
            holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_estado) 
            holder.tvStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#888888")) // Cinza Neutro
            holder.btnReserve.isEnabled = true
            holder.btnReserve.alpha = 1.0f
            holder.btnReserve.text = "EFETUAR RESERVA"
            holder.btnReserve.backgroundTintList = android.content.res.ColorStateList.valueOf(ContextCompat.getColor(context, R.color.azul_glpi))
        } else {
            holder.tvStatus.text = "LIVRE"
            holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_estado)
            holder.tvStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(ContextCompat.getColor(context, R.color.azul_glpi))
            holder.btnReserve.isEnabled = true
            holder.btnReserve.alpha = 1.0f
            holder.btnReserve.text = "EFETUAR RESERVA"
            holder.btnReserve.backgroundTintList = android.content.res.ColorStateList.valueOf(ContextCompat.getColor(context, R.color.azul_glpi))
        }

        if (item.nextReservationDate != null) {
            holder.tvDetails.visibility = View.VISIBLE
            holder.tvDetails.text = "Próxima reserva: ${item.nextReservationDate}"
        } else {
            holder.tvDetails.visibility = View.GONE
        }

        holder.btnReserve.setOnClickListener {
            onReserveClick(item)
        }

        holder.btnDetails.setOnClickListener {
            onDetailsClick(item)
        }
    }

    override fun getItemCount() = dataSet.size

    fun updateData(newData: List<GlpiResDevice>) {
        dataSet = newData
        notifyDataSetChanged()
    }
}
