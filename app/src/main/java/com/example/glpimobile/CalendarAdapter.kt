package com.example.glpimobile

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.util.*

data class CalendarDay(
    val date: Date,
    val isCurrentMonth: Boolean,
    var hasTickets: Boolean = false,
    var isSelected: Boolean = false,
    var isExpiredTicket: Boolean = false
)

class CalendarAdapter(private val onDaySelected: (Date) -> Unit) :
    RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder>() {

    private var days: List<CalendarDay> = emptyList()

    fun updateDays(newDays: List<CalendarDay>) {
        days = newDays
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_calendar_day, parent, false)
        return CalendarViewHolder(view)
    }

    override fun onBindViewHolder(holder: CalendarViewHolder, position: Int) {
        val day = days[position]
        val cal = Calendar.getInstance().apply { time = day.date }
        holder.tvDay.text = cal.get(Calendar.DAY_OF_MONTH).toString()

        // Estilo conforme o mês
        if (!day.isCurrentMonth) {
            holder.tvDay.alpha = 0.3f
        } else {
            holder.tvDay.alpha = 1.0f
        }

        // Seleção
        holder.vSelection.visibility = if (day.isSelected) View.VISIBLE else View.INVISIBLE
        if (day.isSelected) {
            holder.tvDay.setTextColor(Color.WHITE)
            holder.tvDay.setTypeface(null, android.graphics.Typeface.BOLD)
        } else {
            holder.tvDay.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.texto_principal))
            holder.tvDay.setTypeface(null, android.graphics.Typeface.NORMAL)
        }

        // Indicador de tickets
        holder.vDot.visibility = if (day.hasTickets) View.VISIBLE else View.INVISIBLE
        if (day.hasTickets) {
            val context = holder.itemView.context
            val dotColor = if (day.isExpiredTicket) {
                ContextCompat.getColor(context, R.color.vermelho_forte) // Vermelho Elétrico
            } else {
                ContextCompat.getColor(context, R.color.azul_glpi) // Azul
            }
            holder.vDot.backgroundTintList = android.content.res.ColorStateList.valueOf(dotColor)
        }

        holder.itemView.setOnClickListener {
            onDaySelected(day.date)
        }
    }

    override fun getItemCount() = days.size

    class CalendarViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDay: TextView = view.findViewById(R.id.tv_day_number)
        val vSelection: View = view.findViewById(R.id.v_selection_circle)
        val vDot: View = view.findViewById(R.id.v_has_tickets_dot)
    }
}
