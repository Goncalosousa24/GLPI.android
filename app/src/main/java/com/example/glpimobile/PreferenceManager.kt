package com.example.glpimobile

import android.content.Context
import android.content.SharedPreferences

object PreferenceManager {
    private const val PREFS_NAME = "glpi_prefs"
    private const val KEY_BASE_URL = "base_url"
    private const val KEY_APP_TOKEN = "app_token"
    private const val KEY_SESSION_TOKEN = "session_token"
    private const val KEY_DARK_MODE = "dark_mode_v2"
    private const val KEY_NOTIF_ENABLED = "notif_enabled"
    private const val KEY_NOTIF_OPEN = "notif_open"
    private const val KEY_NOTIF_CLOSED = "notif_closed"
    private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
    private const val KEY_LAST_OPEN_COUNT = "last_open_count"
    private const val KEY_LAST_CLOSED_COUNT = "last_closed_count"
    private const val KEY_USER_NAME = "cached_user_name"
    private const val KEY_USER_EMAIL = "cached_user_email"
    private const val KEY_USER_PROFILE = "cached_user_profile"
    private const val KEY_USER_ENTITY = "cached_user_entity"
    private const val KEY_PROFILE_ASSETS_COUNT = "cached_assets_count"
    private const val KEY_PROFILE_TICKETS_COUNT = "cached_tickets_count"
    private const val KEY_AGENDA_CACHE = "agenda_cache_v2"
    private const val KEY_AGENDA_TEAM_CACHE = "agenda_team_cache_v2"
    private const val KEY_AGENDA_FILTER = "agenda_filter_type"
    private const val KEY_USER_ID = "authenticated_user_id"
    private const val KEY_USER_FULL_NAME = "cached_user_full_name"
    private const val KEY_USER_LIST_CACHE = "user_list_cache_v1"
    
    // --- CACHE DASHBOARD ---
    private const val KEY_DASH_ABERTOS = "dash_abertos"
    private const val KEY_DASH_PROGRESSO = "dash_progresso"
    private const val KEY_DASH_RESOLVIDOS = "dash_resolvidos"
    private const val KEY_DASH_PRIORITARIOS = "dash_prioritarios"
    private const val KEY_DASH_ATIVIDADES = "dash_atividades_cache"
    private const val KEY_PERSONAL_VIEW = "personal_view_active" // Nova chave

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getBaseUrl(context: Context): String {
        val url = getPrefs(context).getString(KEY_BASE_URL, "http://glpi.ad.cm-vilaverde.pt/") ?: "http://glpi.ad.cm-vilaverde.pt/"
        if (url == "http://10.0.2.2:8080/") {
            setBaseUrl(context, "http://glpi.ad.cm-vilaverde.pt/")
            return "http://glpi.ad.cm-vilaverde.pt/"
        }
        return url
    }

    fun setBaseUrl(context: Context, url: String) {
        getPrefs(context).edit().putString(KEY_BASE_URL, url).apply()
    }

    fun getAppToken(context: Context): String {
        return getPrefs(context).getString(KEY_APP_TOKEN, "Kv6GgUHREqU0e35dKamiQSh5vjUYenPrqMItEeIh") ?: "Kv6GgUHREqU0e35dKamiQSh5vjUYenPrqMItEeIh"
    }

    fun setAppToken(context: Context, token: String) {
        getPrefs(context).edit().putString(KEY_APP_TOKEN, token).apply()
    }

    fun getSessionToken(context: Context): String {
        return getPrefs(context).getString(KEY_SESSION_TOKEN, "") ?: ""
    }

    fun setSessionToken(context: Context, token: String) {
        getPrefs(context).edit().putString(KEY_SESSION_TOKEN, token).apply()
    }

    fun getDarkModeState(context: Context): Int {
        return getPrefs(context).getInt(KEY_DARK_MODE, -1) // -1 = Seguir Sistema
    }

    fun setDarkModeState(context: Context, state: Int) {
        getPrefs(context).edit().putInt(KEY_DARK_MODE, state).apply()
    }

    fun isNotifEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_NOTIF_ENABLED, true)
    }

    fun setNotifEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_NOTIF_ENABLED, enabled).apply()
    }

    fun isNotifOpenEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_NOTIF_OPEN, true)
    }

    fun setNotifOpenEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_NOTIF_OPEN, enabled).apply()
    }

    fun isNotifClosedEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_NOTIF_CLOSED, true)
    }

    fun setNotifClosedEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_NOTIF_CLOSED, enabled).apply()
    }

    fun isBiometricEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }

    fun setBiometricEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    fun getLastOpenCount(context: Context): Int {
        return getPrefs(context).getInt(KEY_LAST_OPEN_COUNT, -1)
    }

    fun setLastOpenCount(context: Context, count: Int) {
        getPrefs(context).edit().putInt(KEY_LAST_OPEN_COUNT, count).apply()
    }

    fun getLastClosedCount(context: Context): Int {
        return getPrefs(context).getInt(KEY_LAST_CLOSED_COUNT, -1)
    }

    fun setLastClosedCount(context: Context, count: Int) {
        getPrefs(context).edit().putInt(KEY_LAST_CLOSED_COUNT, count).apply()
    }

    // --- CACHE DE PERFIL ---
    fun getUserName(context: Context): String {
        return getPrefs(context).getString(KEY_USER_NAME, "Utilizador") ?: "Utilizador"
    }

    fun setUserName(context: Context, name: String) {
        getPrefs(context).edit().putString(KEY_USER_NAME, name).apply()
    }

    fun getUserFullName(context: Context): String {
        return getPrefs(context).getString(KEY_USER_FULL_NAME, "") ?: ""
    }

    fun setUserFullName(context: Context, name: String) {
        getPrefs(context).edit().putString(KEY_USER_FULL_NAME, name).apply()
    }

    fun getUserEmail(context: Context): String? = getPrefs(context).getString(KEY_USER_EMAIL, null)
    fun setUserEmail(context: Context, value: String) = getPrefs(context).edit().putString(KEY_USER_EMAIL, value).apply()

    fun getUserProfile(context: Context): String? = getPrefs(context).getString(KEY_USER_PROFILE, null)
    fun setUserProfile(context: Context, value: String) = getPrefs(context).edit().putString(KEY_USER_PROFILE, value).apply()

    fun getUserEntity(context: Context): String? = getPrefs(context).getString(KEY_USER_ENTITY, null)
    fun setUserEntity(context: Context, value: String) = getPrefs(context).edit().putString(KEY_USER_ENTITY, value).apply()

    fun getAssetsCount(context: Context): Int = getPrefs(context).getInt(KEY_PROFILE_ASSETS_COUNT, -1)
    fun setAssetsCount(context: Context, value: Int) = getPrefs(context).edit().putInt(KEY_PROFILE_ASSETS_COUNT, value).apply()

    fun getTicketsCount(context: Context): Int = getPrefs(context).getInt(KEY_PROFILE_TICKETS_COUNT, -1)
    fun setTicketsCount(context: Context, value: Int) = getPrefs(context).edit().putInt(KEY_PROFILE_TICKETS_COUNT, value).apply()

    /**
     * Verifica se o perfil atual é restrito (Observer ou Read-Only).
     * Perfis como 'Admin' ou 'Super-Admin' NÃO são restritos e têm acesso total.
     */
    fun isRestrictedProfile(context: Context): Boolean {
        val profile = getUserProfile(context) ?: ""
        return profile.contains("read-only", ignoreCase = true) || 
               profile.contains("leitura", ignoreCase = true) ||
               profile.contains("observador", ignoreCase = true) ||
               profile.contains("observer", ignoreCase = true)
    }

    /**
     * Verifica se o perfil atual é especificamente de Apenas Leitura (Read-Only).
     */
    fun isReadOnlyProfile(context: Context): Boolean {
        val profile = getUserProfile(context) ?: ""
        return profile.contains("read-only", ignoreCase = true) || 
               profile.contains("leitura", ignoreCase = true)
    }

    // --- CACHE DE AGENDA (JSON) ---
    fun getAgendaCache(context: Context): String? = getPrefs(context).getString(KEY_AGENDA_CACHE, null)
    fun setAgendaCache(context: Context, value: String) = getPrefs(context).edit().putString(KEY_AGENDA_CACHE, value).apply()

    fun getAgendaTeamCache(context: Context): String? = getPrefs(context).getString(KEY_AGENDA_TEAM_CACHE, null)
    fun setAgendaTeamCache(context: Context, value: String) = getPrefs(context).edit().putString(KEY_AGENDA_TEAM_CACHE, value).apply()

    fun getAgendaFilterType(context: Context): String = getPrefs(context).getString(KEY_AGENDA_FILTER, "INDIVIDUAL") ?: "INDIVIDUAL"
    fun setAgendaFilterType(context: Context, value: String) = getPrefs(context).edit().putString(KEY_AGENDA_FILTER, value).apply()

    // --- CACHE DE UTILIZADORES (JSON) ---
    fun getUserListCache(context: Context): String? = getPrefs(context).getString(KEY_USER_LIST_CACHE, null)
    fun setUserListCache(context: Context, value: String) = getPrefs(context).edit().putString(KEY_USER_LIST_CACHE, value).apply()

    // --- ID DO UTILIZADOR ---
    fun getUserId(context: Context): Int = getPrefs(context).getInt(KEY_USER_ID, 0)
    fun setUserId(context: Context, value: Int) = getPrefs(context).edit().putInt(KEY_USER_ID, value).apply()

    // --- MÉTODOS DASHBOARD ---
    fun getDashAbertos(context: Context): Int = getPrefs(context).getInt(KEY_DASH_ABERTOS, 0)
    fun setDashAbertos(context: Context, value: Int) = getPrefs(context).edit().putInt(KEY_DASH_ABERTOS, value).apply()

    fun getDashProgresso(context: Context): Int = getPrefs(context).getInt(KEY_DASH_PROGRESSO, 0)
    fun setDashProgresso(context: Context, value: Int) = getPrefs(context).edit().putInt(KEY_DASH_PROGRESSO, value).apply()

    fun getDashResolvidos(context: Context): Int = getPrefs(context).getInt(KEY_DASH_RESOLVIDOS, 0)
    fun setDashResolvidos(context: Context, value: Int) = getPrefs(context).edit().putInt(KEY_DASH_RESOLVIDOS, value).apply()

    fun getDashPrioritarios(context: Context): Int = getPrefs(context).getInt(KEY_DASH_PRIORITARIOS, 0)
    fun setDashPrioritarios(context: Context, value: Int) = getPrefs(context).edit().putInt(KEY_DASH_PRIORITARIOS, value).apply()

    fun getDashAtividades(context: Context): String? = getPrefs(context).getString(KEY_DASH_ATIVIDADES, null)
    fun setDashAtividades(context: Context, value: String?) = getPrefs(context).edit().putString(KEY_DASH_ATIVIDADES, value).apply()

    // --- VISTA PESSOAL ---
    fun isPersonalViewEnabled(context: Context): Boolean = getPrefs(context).getBoolean(KEY_PERSONAL_VIEW, false)
    fun setPersonalViewEnabled(context: Context, value: Boolean) = getPrefs(context).edit().putBoolean(KEY_PERSONAL_VIEW, value).apply()

    /**
     * Limpa todos os dados cacheados (contagens, listas, nomes) 
     * SEM remover configurações de sistema (URL, Tokens, Dark Mode).
     * Usado ao trocar de perfil para forçar refresh global.
     */
    fun clearAllDataCache(context: Context) {
        getPrefs(context).edit().apply {
            remove(KEY_USER_NAME)
            remove(KEY_USER_EMAIL)
            remove(KEY_USER_PROFILE)
            remove(KEY_USER_ENTITY)
            remove(KEY_PROFILE_ASSETS_COUNT)
            remove(KEY_PROFILE_TICKETS_COUNT)
            remove(KEY_AGENDA_CACHE)
            remove(KEY_AGENDA_TEAM_CACHE)
            remove(KEY_USER_LIST_CACHE)
            remove(KEY_DASH_ABERTOS)
            remove(KEY_DASH_PROGRESSO)
            remove(KEY_DASH_RESOLVIDOS)
            remove(KEY_DASH_PRIORITARIOS)
            remove(KEY_DASH_ATIVIDADES)
            // Mantemos Settings (URL, Token, DarkMode, Notificações, Biometria)
        }.apply()
    }
}

