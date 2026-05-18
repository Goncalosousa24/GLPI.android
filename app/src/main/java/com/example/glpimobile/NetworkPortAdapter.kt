package com.example.glpimobile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class NetworkPortAdapter(
    private val portsList: List<Map<String, Any>>,
    private val parentName: String = "",
    private val parentSerial: String = ""
) : RecyclerView.Adapter<NetworkPortAdapter.PortViewHolder>() {

    class PortViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvConnectedDeviceName: TextView = view.findViewById(R.id.tv_connected_device_name)
        val tvPortIdName: TextView = view.findViewById(R.id.tv_port_id_name)
        val tvPortNumberBadge: TextView = view.findViewById(R.id.tv_port_number_badge)
        val tvPortMac: TextView = view.findViewById(R.id.tv_port_mac)
        val tvPortIp: TextView = view.findViewById(R.id.tv_port_ip)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PortViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_network_port, parent, false)
        return PortViewHolder(view)
    }

    override fun onBindViewHolder(holder: PortViewHolder, position: Int) {
        val item = portsList[position]

        // 1. Número da porta (Badge)
        val logicalNum = item["3"]?.toString()?.replace(".0", "")
            ?: item["logical_number"]?.toString()?.replace(".0", "") 
            ?: (position + 1).toString()
        holder.tvPortNumberBadge.text = logicalNum

        // 2. Local Port Name (Campo 1)
        var localPortName = item["1"]?.toString() ?: ""
        if (localPortName == "[]" || localPortName == "null") localPortName = ""

        // 3. Connected Device (Campo 39)
        var connectedTo = item["39"]?.toString() ?: ""
        if (connectedTo == "[]" || connectedTo == "null") connectedTo = ""

        // Se existir um nome injetado pelo scrapper (fallback ou override)
        val injectedConnected = item["39_injected"]?.toString() ?: ""
        if (injectedConnected.isNotEmpty() && injectedConnected != "null") {
            connectedTo = injectedConnected
        }

        // Definir Nome a apresentar
        // O utilizador quer o Dispositivo e o MAC. Se houver algo ligado, mostramos o que está ligado.
        // Se não, mostramos o nome da porta local.
        val displayTitle = if (connectedTo.isNotEmpty()) {
            connectedTo
        } else if (localPortName.isNotEmpty()) {
            localPortName
        } else {
            "SEM LIGAÇÃO"
        }

        // 4. Set Display Name (Uppercase)
        holder.tvConnectedDeviceName.text = displayTitle.uppercase(Locale.getDefault())
        
        holder.tvPortIdName.visibility = View.GONE

        // 5. MAC Address (Campo 4 nativo, ou injetado)
        var macRaw = ""
        val macNative = item["4"]
        if (macNative is Map<*, *>) {
            macRaw = macNative["name"]?.toString() ?: macNative["1"]?.toString() ?: ""
        } else {
            macRaw = macNative?.toString() ?: ""
        }
        
        val macInjected = item["6_injected"]?.toString() ?: ""
        if (macInjected.isNotEmpty() && macInjected != "null" && macInjected != "0") {
            macRaw = macInjected
        }
        
        macRaw = macRaw.replace("[]", "").replace("null", "").trim()
        if (macRaw == "0" || macRaw == "0.0") macRaw = ""
        
        val macLower = macRaw.lowercase(Locale.getDefault())
        if (macLower.isNotEmpty()) {
            holder.tvPortMac.text = "MAC: $macLower"
            holder.tvPortMac.setTextColor(holder.itemView.context.getColor(R.color.texto_secundario))
            holder.tvPortMac.visibility = View.VISIBLE
        } else {
            holder.tvPortMac.text = "MAC: INDISPONÍVEL"
            // Mantém cor cinzenta para não ser incomodativo, mas sim unificador de design
            holder.tvPortMac.setTextColor(holder.itemView.context.getColor(R.color.texto_secundario))
            holder.tvPortMac.visibility = View.VISIBLE
        }

        holder.tvPortIp.visibility = View.GONE
    }

    override fun getItemCount() = portsList.size
}
