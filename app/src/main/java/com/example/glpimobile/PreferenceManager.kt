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
    private const val KEY_NOTIF_REQUERENTE = "notif_requerente"
    private const val KEY_NOTIF_OBSERVADOR = "notif_observador"
    private const val KEY_NOTIF_ATRIBUIDO = "notif_atribuido"
    private const val KEY_NOTIF_FINALIZADO = "notif_finalizado"
    private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
    private const val KEY_LAST_REQUERENTE_COUNT = "last_requerente_count"
    private const val KEY_LAST_OBSERVADOR_COUNT = "last_observador_count"
    private const val KEY_LAST_ATRIBUIDO_COUNT = "last_atribuido_count"
    private const val KEY_LAST_FINALIZADO_COUNT = "last_finalizado_count"
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
        return getPrefs(context).getString(KEY_BASE_URL, "") ?: ""
    }

    fun setBaseUrl(context: Context, url: String) {
        getPrefs(context).edit().putString(KEY_BASE_URL, url).apply()
    }

    fun getAppToken(context: Context): String {
        return getPrefs(context).getString(KEY_APP_TOKEN, "") ?: ""
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

    fun isNotifRequerenteEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_NOTIF_REQUERENTE, true)
    }

    fun setNotifRequerenteEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_NOTIF_REQUERENTE, enabled).apply()
    }

    fun isNotifObservadorEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_NOTIF_OBSERVADOR, true)
    }

    fun setNotifObservadorEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_NOTIF_OBSERVADOR, enabled).apply()
    }

    fun isNotifAtribuidoEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_NOTIF_ATRIBUIDO, true)
    }

    fun setNotifAtribuidoEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_NOTIF_ATRIBUIDO, enabled).apply()
    }

    fun isNotifFinalizadoEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_NOTIF_FINALIZADO, true)
    }

    fun setNotifFinalizadoEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_NOTIF_FINALIZADO, enabled).apply()
    }

    fun isBiometricEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }

    fun setBiometricEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    fun getLastRequerenteCount(context: Context): Int {
        return getPrefs(context).getInt(KEY_LAST_REQUERENTE_COUNT, -1)
    }

    fun setLastRequerenteCount(context: Context, count: Int) {
        getPrefs(context).edit().putInt(KEY_LAST_REQUERENTE_COUNT, count).apply()
    }

    fun getLastObservadorCount(context: Context): Int {
        return getPrefs(context).getInt(KEY_LAST_OBSERVADOR_COUNT, -1)
    }

    fun setLastObservadorCount(context: Context, count: Int) {
        getPrefs(context).edit().putInt(KEY_LAST_OBSERVADOR_COUNT, count).apply()
    }

    fun getLastAtribuidoCount(context: Context): Int {
        return getPrefs(context).getInt(KEY_LAST_ATRIBUIDO_COUNT, -1)
    }

    fun setLastAtribuidoCount(context: Context, count: Int) {
        getPrefs(context).edit().putInt(KEY_LAST_ATRIBUIDO_COUNT, count).apply()
    }

    fun getLastFinalizadoCount(context: Context): Int {
        return getPrefs(context).getInt(KEY_LAST_FINALIZADO_COUNT, -1)
    }

    fun setLastFinalizadoCount(context: Context, count: Int) {
        getPrefs(context).edit().putInt(KEY_LAST_FINALIZADO_COUNT, count).apply()
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
    fun setUserListCache(context: Context, value: String?) {
        val editor = getPrefs(context).edit()
        if (value == null) editor.remove(KEY_USER_LIST_CACHE) else editor.putString(KEY_USER_LIST_CACHE, value)
        editor.apply()
    }

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

