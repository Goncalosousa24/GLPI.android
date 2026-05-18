package com.example.glpimobile

import com.google.gson.annotations.SerializedName

// --- Modelos de Resposta da API ---
data class TicketListResponse(
    @SerializedName("data") val data: List<Map<String, Any>>? = null,
    @SerializedName("totalcount") val totalcount: Int? = 0,
    @SerializedName("count") val count: Int? = 0
)

data class SearchResponse(
    @SerializedName("data") val data: List<Map<String, Any>>? = null,
    @SerializedName("totalcount") val totalcount: Int? = 0,
    @SerializedName("count") val count: Int? = 0
)
data class TicketCreateResponse(val id: Int, val message: String?)
data class UserResponse(val id: Int, val name: String, val realname: String? = null, val firstname: String? = null, val completename: String? = null)
data class SessionResponse(val session_token: String)


// --- Logs e Dispositivos ---
data class GLPILog(
    val id: Int,
    val itemtype: String,
    val items_id: Int,
    val user_name: String,
    val date_mod: String,
    val old_value: String?,
    val new_value: String?,
    val content: String?
)

data class DeviceTicket(val id: Int, val name: String, val date: String, val status: Int)

// 🔥 CLASSE QUE FALTAVA E QUEBROU OS LOGS 🔥
data class GlpiEquipamento(
    val id: Int,
    val name: String,
    val serial: String,
    val estadoStr: String?,
    val utilizador: String,
    val itemtype: String,
    val comment: String? = null
)

data class GlpiResDevice(
    val reservationItemsId: Int,
    val itemType: String,
    val itemsId: Int,
    val name: String,
    val isReservedNow: Boolean,
    val nextReservationDate: String?,
    val currentUser: String? = null,
    val activeComment: String? = null,
    val activeEndDate: String? = null,
    val serial: String = ""
)

// --- Pedido de Criação de Ticket ---
data class TicketRequest(val input: TicketInput)
data class TicketInput(
    val name: String,
    val content: String,
    val status: Int = 1,
    val type: Int = 1, // 1: Incidente, 2: Pedido
    val itilcategories_id: Int? = null,
    val requesttypes_id: Int = 4, // 4: Direct (Padrão)
    val urgency: Int = 3,
    val priority: Int = 3,
    val time_to_own: String? = null,
    val time_to_resolve: String? = null
)

data class DadosMensais(
    val nome: String,
    val nomeCurto: String,
    val criados: Int,
    val resolvidos: Int,
    val saldo: Int
)

// --- Criação de Dispositivos ---
data class DeviceRequest(val input: DeviceInput)
data class DeviceInput(
    val name: String,
    val serial: String? = null,
    val locations_id: Int? = null,
    val states_id: Int? = null,
    val users_id: Int? = null,
    val users_id_tech: Int? = null,
    val comment: String? = null
)
data class DeviceCreateResponse(val id: Int, val message: String?)

// --- Modelos para Upload de Documentos ---
data class DocumentRequest(val input: DocumentInput)
data class DocumentInput(
    val name: String,
    val items_id: Int,
    val itemtype: String = "Ticket"
)
data class DocumentCreateResponse(val id: Int, val message: String?)
