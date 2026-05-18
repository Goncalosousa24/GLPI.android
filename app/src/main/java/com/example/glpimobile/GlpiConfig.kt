package com.example.glpimobile

object GlpiConfig {
    val SESSION_TOKEN: String
        get() = PreferenceManager.getSessionToken(GlpiApp.instance)
    
    val APP_TOKEN: String
        get() = PreferenceManager.getAppToken(GlpiApp.instance)
        
    val BASE_URL: String
        get() = PreferenceManager.getBaseUrl(GlpiApp.instance)

    val USER_ID: Int
        get() = PreferenceManager.getUserId(GlpiApp.instance)

    val USER_NAME: String
        get() = PreferenceManager.getUserName(GlpiApp.instance)

    val USER_FULL_NAME: String
        get() = PreferenceManager.getUserFullName(GlpiApp.instance)

    // 🔥 MODO OFFLINE: Ative para trabalhar no design sem acesso à API
    const val OFFLINE_MODE = false

    // Estado da Agenda para a sessão atual (Reinicia quando a app fecha)
    var currentAgendaFilter: String = "INDIVIDUAL"
    var isAgendaFirstLoadDone: Boolean = false
}
