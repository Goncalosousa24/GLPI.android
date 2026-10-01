package com.example.glpimobile

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

interface GlpiApiService {

    @GET("apirest.php/initSession")
    suspend fun initSession(
        @Header("Authorization") authHeader: String,
        @Header("App-Token") appToken: String
    ): retrofit2.Response<SessionResponse>

    @GET("apirest.php/getFullSession")

    suspend fun getFullSession(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Query("_cb") cacheBuster: Long = System.currentTimeMillis()
    ): retrofit2.Response<Map<String, Any>>

    // Versão alternativa para alguns servidores que usam Header-App-Token
    @GET("apirest.php/getFullSession")
    suspend fun getFullSessionCustom(
        @Header("Session-Token") sessionToken: String,
        @Header("Header-App-Token") appToken: String
    ): retrofit2.Response<Map<String, Any>>

    @GET("apirest.php/getMyProfiles")
    suspend fun getMyProfiles(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String
    ): retrofit2.Response<Map<String, Any>>

    @POST("apirest.php/changeActiveProfile")
    suspend fun changeActiveProfile(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Body input: Map<String, Int>
    ): retrofit2.Response<Unit>

    @POST("apirest.php/changeActiveProfile")
    suspend fun changeActiveProfileQuery(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Query("profiles_id") profileId: Int
    ): retrofit2.Response<Unit>

    @POST("apirest.php/changeActiveProfile")
    suspend fun changeActiveProfileFull(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Body input: Map<String, @JvmSuppressWildcards Any>
    ): retrofit2.Response<Unit>

    @GET("apirest.php/User/{id}")
    suspend fun getUser(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Path("id") id: String
    ): Response<UserResponse>

    @GET("apirest.php/search/Ticket?range=0-1")
    suspend fun getEstatisticasMensais(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Query("criteria[0][field]") fieldDate: Int,
        @Query("criteria[0][searchtype]") searchType: String = "contains",
        @Query("criteria[0][value]") yearMonth: String,
        @Query("is_deleted") isDeleted: Int // 0 para ativos, 1 para eliminados
    ): SearchResponse

    @GET("apirest.php/search/Ticket?criteria[0][field]=4&criteria[0][searchtype]=equals&criteria[1][link]=OR&criteria[1][field]=5&criteria[1][searchtype]=equals&criteria[2][link]=OR&criteria[2][field]=22&criteria[2][searchtype]=equals&expand_dropdowns=true&forcedisplay[0]=2&forcedisplay[1]=1&forcedisplay[2]=15&forcedisplay[3]=151&forcedisplay[4]=158&forcedisplay[5]=19&forcedisplay[6]=15&forcedisplay[7]=12&forcedisplay[8]=21&forcedisplay[9]=17&forcedisplay[10]=16&forcedisplay[11]=4&forcedisplay[12]=5&forcedisplay[13]=22&forcedisplay[14]=8&forcedisplay[15]=14&forcedisplay[16]=70&forcedisplay[17]=71&forcedisplay[18]=6&forcedisplay[19]=7&forcedisplay[20]=3&forcedisplay[21]=151&forcedisplay[22]=158&forcedisplay[23]=66&range=0-999")
    suspend fun getTodosMeusTicketsStats(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Query("criteria[0][value]") userId1: Int,
        @Query("criteria[1][value]") userId2: Int,
        @Query("criteria[2][value]") userId3: Int,
        @Query("_unused1") userId4: Int = 0,
        @Query("_unused2") userId5: Int = 0,
        @Query("_unused3") userId6: Int = 0,
        @Query("_unused4") login1: String = "",
        @Query("_unused5") login2: String = ""
    ): TicketListResponse

    @GET("apirest.php/search/Ticket?sort=15&forcedisplay[0]=2&forcedisplay[1]=1&forcedisplay[3]=151&forcedisplay[4]=158&forcedisplay[5]=19&forcedisplay[6]=15&forcedisplay[7]=12&forcedisplay[8]=21&forcedisplay[9]=17&forcedisplay[10]=16&forcedisplay[11]=4&forcedisplay[12]=5&forcedisplay[13]=22&forcedisplay[14]=8&forcedisplay[15]=14&forcedisplay[16]=70&forcedisplay[17]=71&forcedisplay[18]=6&forcedisplay[19]=7&forcedisplay[20]=3&forcedisplay[21]=24&forcedisplay[22]=66&expand_dropdowns=true")
    suspend fun getMeusTicketsCriados(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Query("range") range: String = "0-1000",
        @Query("order") order: String,
        @Query("criteria[0][value]") userId1: Int,
        @Query("criteria[0][field]") field1: Int = 4,
        @Query("criteria[0][searchtype]") search1: String = "equals",
        @Query("criteria[1][link]") link: String = "OR",
        @Query("criteria[1][value]") userId2: Int,
        @Query("criteria[1][field]") field2: Int = 22,
        @Query("criteria[1][searchtype]") search2: String = "equals"
    ): TicketListResponse

    @GET("apirest.php/search/Ticket?criteria[0][criteria][0][field]=4&criteria[0][criteria][0][searchtype]=equals&criteria[0][criteria][1][link]=OR&criteria[0][criteria][1][field]=5&criteria[0][criteria][1][searchtype]=equals&criteria[0][criteria][2][link]=OR&criteria[0][criteria][2][field]=22&criteria[0][criteria][2][searchtype]=equals&criteria[1][link]=AND&criteria[1][field]=12&criteria[1][searchtype]=equals&criteria[1][value]=1&range=0-1")
    suspend fun getCountAbertosMeus(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Query("criteria[0][criteria][0][value]") userId1: String,
        @Query("criteria[0][criteria][1][value]") userId2: String,
        @Query("criteria[0][criteria][2][value]") userId3: String,
        @Query("_unused1") userId4: String,
        @Query("_unused2") login1: String,
        @Query("_unused3") login2: String
    ): retrofit2.Response<SearchResponse>

    @GET("apirest.php/search/Ticket?criteria[0][criteria][0][field]=4&criteria[0][criteria][0][searchtype]=equals&criteria[0][criteria][1][link]=OR&criteria[0][criteria][1][field]=5&criteria[0][criteria][1][searchtype]=equals&criteria[0][criteria][2][link]=OR&criteria[0][criteria][2][field]=22&criteria[0][criteria][2][searchtype]=equals&criteria[1][link]=AND&criteria[1][field]=12&criteria[1][searchtype]=equals&criteria[1][value]=1&expand_dropdowns=true&forcedisplay[0]=2&forcedisplay[1]=1&forcedisplay[3]=15&forcedisplay[4]=12&forcedisplay[5]=21&forcedisplay[6]=17&forcedisplay[7]=16&forcedisplay[8]=4&forcedisplay[9]=5&forcedisplay[10]=22&forcedisplay[11]=8&forcedisplay[12]=14&forcedisplay[13]=70&forcedisplay[14]=71&forcedisplay[15]=6&forcedisplay[16]=7&forcedisplay[17]=3&forcedisplay[18]=66")
    suspend fun getTicketsCriadosPorMim(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Query("criteria[0][criteria][0][value]") userId1: Int,
        @Query("criteria[0][criteria][1][value]") userId2: Int,
        @Query("criteria[0][criteria][2][value]") userId3: Int,
        @Query("_unused1") userId4: Int,
        @Query("_unused2") userId5: Int,
        @Query("_unused3") userId6: Int,
        @Query("_unused4") login1: String,
        @Query("_unused5") login2: String,
        @Query("order") order: String = "DESC",
        @Query("range") range: String = "0-149",
        @Query("sort") sort: Int = 19
    ): TicketListResponse

    @GET("apirest.php/search/Ticket?criteria[0][field]=4&criteria[0][searchtype]=equals&criteria[1][link]=OR&criteria[1][field]=5&criteria[1][searchtype]=equals&criteria[2][link]=OR&criteria[2][field]=22&criteria[2][searchtype]=equals&forcedisplay[0]=2&forcedisplay[1]=1&forcedisplay[3]=15&forcedisplay[4]=12&forcedisplay[5]=21&forcedisplay[6]=17&forcedisplay[7]=16&forcedisplay[8]=4&forcedisplay[9]=5&forcedisplay[10]=22&forcedisplay[11]=8&forcedisplay[12]=14&forcedisplay[13]=70&forcedisplay[14]=71&forcedisplay[15]=6&forcedisplay[16]=7&forcedisplay[17]=3&forcedisplay[18]=151&forcedisplay[19]=158&forcedisplay[20]=66&expand_dropdowns=true")
    suspend fun getTicketsCriadosPorMimTudo(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Query("criteria[0][value]") userId1: Int,
        @Query("criteria[1][value]") userId2: Int,
        @Query("criteria[2][value]") userId3: Int,
        @Query("_unused1") userId4: Int = 0,
        @Query("_unused2") userId5: Int = 0,
        @Query("_unused3") userId6: Int = 0,
        @Query("_unused4") login1: String = "",
        @Query("_unused5") login2: String = "",
        @Query("range") range: String = "0-1000",
        @Query("order") order: String = "DESC",
        @Query("sort") sort: Int = 19
    ): TicketListResponse

    @GET("apirest.php/search/Ticket?forcedisplay[0]=2&forcedisplay[1]=1&forcedisplay[3]=15&forcedisplay[4]=12&forcedisplay[5]=21&forcedisplay[6]=17&forcedisplay[7]=16&forcedisplay[8]=4&forcedisplay[9]=5&forcedisplay[10]=22&forcedisplay[11]=8&forcedisplay[12]=14&forcedisplay[13]=70&forcedisplay[14]=71&forcedisplay[15]=6&forcedisplay[16]=7&forcedisplay[17]=3&forcedisplay[18]=151&forcedisplay[19]=158&forcedisplay[20]=66&expand_dropdowns=true")
    suspend fun getTicketsCriadosGerais(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Query("criteria[0][field]") field: Int = 12,
        @Query("criteria[0][searchtype]") type: String = "equals",
        @Query("criteria[0][value]") value: Int = 1,
        @Query("range") range: String = "0-1000",
        @Query("order") order: String = "DESC",
        @Query("sort") sort: Int = 19
    ): TicketListResponse

    @GET("apirest.php/search/Ticket?forcedisplay[0]=2&forcedisplay[1]=1&forcedisplay[3]=15&forcedisplay[4]=12&forcedisplay[5]=21&forcedisplay[6]=17&forcedisplay[7]=16&forcedisplay[8]=4&forcedisplay[9]=5&forcedisplay[10]=22&forcedisplay[11]=8&forcedisplay[12]=14&forcedisplay[13]=70&forcedisplay[14]=71&forcedisplay[15]=6&forcedisplay[16]=7&forcedisplay[17]=3&forcedisplay[18]=151&forcedisplay[19]=158&forcedisplay[20]=66&expand_dropdowns=true")
    suspend fun getTicketsCriadosGeraisTudo(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Query("criteria[0][criteria][0][field]") f0: Int = 12,
        @Query("criteria[0][criteria][0][searchtype]") t0: String = "equals",
        @Query("criteria[0][criteria][0][value]") v0: Int = 1,
        @Query("criteria[0][criteria][1][link]") l1: String = "OR",
        @Query("criteria[0][criteria][1][field]") f1: Int = 12,
        @Query("criteria[0][criteria][1][searchtype]") t1: String = "equals",
        @Query("criteria[0][criteria][1][value]") v1: Int = 2,
        @Query("criteria[0][criteria][2][link]") l2: String = "OR",
        @Query("criteria[0][criteria][2][field]") f2: Int = 12,
        @Query("criteria[0][criteria][2][searchtype]") t2: String = "equals",
        @Query("criteria[0][criteria][2][value]") v2: Int = 3,
        @Query("criteria[0][criteria][3][link]") l3: String = "OR",
        @Query("criteria[0][criteria][3][field]") f3: Int = 12,
        @Query("criteria[0][criteria][3][searchtype]") t3: String = "equals",
        @Query("criteria[0][criteria][3][value]") v3: Int = 4,
        @Query("range") range: String = "0-1000",
        @Query("order") order: String = "DESC",
        @Query("sort") sort: Int = 19
    ): TicketListResponse

    @GET("apirest.php/search/Ticket?criteria[0][criteria][0][field]=12&criteria[0][criteria][0][searchtype]=equals&criteria[0][criteria][0][value]=2&criteria[0][criteria][1][link]=OR&criteria[0][criteria][1][field]=12&criteria[0][criteria][1][searchtype]=equals&criteria[0][criteria][1][value]=4&criteria[1][link]=AND&criteria[1][field]=5&criteria[1][searchtype]=equals&forcedisplay[0]=2&forcedisplay[1]=1&forcedisplay[3]=15&forcedisplay[4]=12&forcedisplay[5]=21&forcedisplay[6]=17&forcedisplay[7]=16&forcedisplay[8]=4&forcedisplay[9]=5&forcedisplay[10]=22&forcedisplay[11]=8&forcedisplay[12]=14&forcedisplay[13]=70&forcedisplay[14]=71&forcedisplay[15]=6&forcedisplay[16]=7&forcedisplay[17]=151&forcedisplay[18]=158&forcedisplay[19]=66&expand_dropdowns=true")
    suspend fun getTicketsAtribuidosAMim(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Query("criteria[1][value]") userId: Int,
        @Query("order") order: String,
        @Query("range") range: String = "0-1000"
    ): TicketListResponse

    @GET("apirest.php/search/Ticket?criteria[0][criteria][0][field]=12&criteria[0][criteria][0][searchtype]=equals&criteria[0][criteria][0][value]=5&criteria[0][criteria][1][link]=OR&criteria[0][criteria][1][field]=12&criteria[0][criteria][1][searchtype]=equals&criteria[0][criteria][1][value]=6&criteria[1][link]=AND&criteria[1][criteria][0][field]=5&criteria[1][criteria][0][searchtype]=equals&criteria[1][criteria][1][link]=OR&criteria[1][criteria][1][field]=6&criteria[1][criteria][1][searchtype]=equals&criteria[2][link]=AND&criteria[2][field]=19&criteria[2][searchtype]=morethan&forcedisplay[0]=2&forcedisplay[1]=1&forcedisplay[3]=15&forcedisplay[4]=12&forcedisplay[5]=21&forcedisplay[6]=17&forcedisplay[7]=16&forcedisplay[8]=4&forcedisplay[9]=5&forcedisplay[10]=22&forcedisplay[11]=8&forcedisplay[12]=14&forcedisplay[13]=70&forcedisplay[14]=71&forcedisplay[15]=6&forcedisplay[16]=7&forcedisplay[17]=3&forcedisplay[18]=24&forcedisplay[19]=151&forcedisplay[20]=158&forcedisplay[22]=13&forcedisplay[23]=7&forcedisplay[24]=10&forcedisplay[25]=11&forcedisplay[26]=66&expand_dropdowns=true")
    suspend fun getTicketsAtribuidosFinalizadosHistorico(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Query("criteria[1][criteria][0][value]") userId1: Int,
        @Query("criteria[1][criteria][1][value]") userId2: Int,
        @Query("criteria[2][value]") dataLimite: String, 
        @Query("order") order: String = "DESC",
        @Query("range") range: String = "0-100",
        @Query("sort") sort: Int = 19
    ): retrofit2.Response<TicketListResponse>
    
    @GET("apirest.php/search/Ticket?criteria[0][criteria][0][field]=12&criteria[0][criteria][0][searchtype]=equals&criteria[0][criteria][0][value]=5&criteria[0][criteria][1][link]=OR&criteria[0][criteria][1][field]=12&criteria[0][criteria][1][searchtype]=equals&criteria[0][criteria][1][value]=6&criteria[1][link]=AND&criteria[1][criteria][0][field]=2&criteria[1][criteria][0][searchtype]=equals&criteria[1][criteria][1][link]=OR&criteria[1][criteria][1][field]=4&criteria[1][criteria][1][searchtype]=equals&criteria[2][link]=AND&criteria[2][field]=19&criteria[2][searchtype]=morethan&forcedisplay[0]=2&forcedisplay[1]=1&forcedisplay[3]=15&forcedisplay[4]=12&forcedisplay[5]=21&forcedisplay[6]=17&forcedisplay[7]=16&forcedisplay[8]=4&forcedisplay[9]=5&forcedisplay[10]=22&forcedisplay[11]=8&forcedisplay[12]=14&forcedisplay[13]=70&forcedisplay[14]=71&forcedisplay[15]=6&forcedisplay[16]=7&forcedisplay[17]=3&forcedisplay[18]=24&forcedisplay[19]=151&forcedisplay[20]=158&forcedisplay[22]=13&forcedisplay[23]=7&forcedisplay[24]=10&forcedisplay[25]=11&forcedisplay[26]=66&expand_dropdowns=true")
    suspend fun getTicketsCriadosFinalizadosHistorico(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Query("criteria[1][criteria][0][value]") userId1: Int,
        @Query("criteria[1][criteria][1][value]") userId2: Int,
        @Query("criteria[2][value]") dataLimite: String, 
        @Query("order") order: String = "DESC",
        @Query("range") range: String = "0-100",
        @Query("sort") sort: Int = 19
    ): retrofit2.Response<TicketListResponse>


    @GET("apirest.php/search/Ticket?sort=17&forcedisplay[0]=2&forcedisplay[1]=1&forcedisplay[3]=15&forcedisplay[4]=12&forcedisplay[5]=21&forcedisplay[6]=17&forcedisplay[7]=16&forcedisplay[8]=4&forcedisplay[9]=5&forcedisplay[10]=22&forcedisplay[11]=8&forcedisplay[12]=14&forcedisplay[13]=70&forcedisplay[14]=71&forcedisplay[15]=6&forcedisplay[16]=7&forcedisplay[17]=151&forcedisplay[18]=158&forcedisplay[19]=66&expand_dropdowns=true")
    suspend fun getTicketsResolvidosPorMim(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Query("order") order: String,
        @Query("range") range: String = "0-100",
        @Query("criteria[0][value]") userId: Int,
        @Query("criteria[0][field]") fieldTecnico: Int = 5,
        @Query("criteria[0][searchtype]") searchTec: String = "equals",
        @Query("criteria[1][value]") statusValue: Int = 4,
        @Query("criteria[1][field]") fieldStatus: Int = 12,
        @Query("criteria[1][link]") link: String = "AND",
        @Query("criteria[1][searchtype]") searchStatus: String = "morethan"
    ): TicketListResponse

    @GET("apirest.php/search/Ticket?criteria[0][criteria][0][field]=12&criteria[0][criteria][0][searchtype]=equals&criteria[0][criteria][0][value]=5&criteria[0][criteria][1][link]=OR&criteria[0][criteria][1][field]=12&criteria[0][criteria][1][searchtype]=equals&criteria[0][criteria][1][value]=6&criteria[1][link]=AND&criteria[1][field]=19&criteria[1][searchtype]=morethan&forcedisplay[0]=2&forcedisplay[1]=1&forcedisplay[3]=15&forcedisplay[4]=12&forcedisplay[5]=21&forcedisplay[6]=17&forcedisplay[7]=16&forcedisplay[8]=4&forcedisplay[9]=5&forcedisplay[10]=22&forcedisplay[11]=8&forcedisplay[12]=14&forcedisplay[13]=70&forcedisplay[14]=71&forcedisplay[15]=6&forcedisplay[16]=7&forcedisplay[17]=3&forcedisplay[18]=24&forcedisplay[19]=151&forcedisplay[20]=158&forcedisplay[22]=13&forcedisplay[23]=7&forcedisplay[24]=10&forcedisplay[25]=11&forcedisplay[26]=66&expand_dropdowns=true")
    suspend fun getListaGeralFinalizadosHistorico(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Query("criteria[1][value]") dataLimite: String,
        @Query("order") order: String = "DESC",
        @Query("sort") sort: Int = 19,
        @Query("range") range: String = "0-299"
    ): retrofit2.Response<TicketListResponse>

    @GET("apirest.php/search/Ticket?criteria[0][criteria][0][field]=12&criteria[0][criteria][0][searchtype]=equals&criteria[0][criteria][0][value]=5&criteria[0][criteria][1][link]=OR&criteria[0][criteria][1][field]=12&criteria[0][criteria][1][searchtype]=equals&criteria[0][criteria][1][value]=6&criteria[1][link]=AND&criteria[1][criteria][0][field]=4&criteria[1][criteria][0][searchtype]=equals&criteria[1][criteria][1][link]=OR&criteria[1][criteria][1][field]=5&criteria[1][criteria][1][searchtype]=equals&criteria[1][criteria][2][link]=OR&criteria[1][criteria][2][field]=22&criteria[1][criteria][2][searchtype]=equals&criteria[2][link]=AND&criteria[2][field]=19&criteria[2][searchtype]=morethan&forcedisplay[0]=2&forcedisplay[1]=1&forcedisplay[3]=15&forcedisplay[4]=12&forcedisplay[5]=21&forcedisplay[6]=17&forcedisplay[7]=16&forcedisplay[8]=4&forcedisplay[9]=5&forcedisplay[10]=22&forcedisplay[11]=8&forcedisplay[12]=14&forcedisplay[13]=70&forcedisplay[14]=71&forcedisplay[15]=6&forcedisplay[16]=7&forcedisplay[17]=3&forcedisplay[18]=24&forcedisplay[19]=151&forcedisplay[20]=158&forcedisplay[22]=13&forcedisplay[23]=7&forcedisplay[24]=10&forcedisplay[25]=11&forcedisplay[26]=66&expand_dropdowns=true")
    suspend fun getTicketsMeusFinalizadosHistorico(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Query("criteria[1][criteria][0][value]") userId1: Int,
        @Query("criteria[1][criteria][1][value]") userId2: Int,
        @Query("criteria[1][criteria][2][value]") userId3: Int,
        @Query("_unused1") userId4: Int,
        @Query("_unused2") userId5: Int,
        @Query("_unused3") userId6: Int,
        @Query("_unused4") login1: String,
        @Query("_unused5") login2: String,
        @Query("criteria[2][value]") dataLimite: String,
        @Query("order") order: String = "DESC",
        @Query("sort") sort: Int = 19,
        @Query("range") range: String = "0-299"
    ): retrofit2.Response<TicketListResponse>

    @GET("apirest.php/search/Ticket?criteria[0][field]=12&criteria[0][searchtype]=equals&criteria[0][value]=1&forcedisplay[0]=2&forcedisplay[1]=1&forcedisplay[2]=9&forcedisplay[3]=151&forcedisplay[4]=158&forcedisplay[5]=19&forcedisplay[6]=15&forcedisplay[7]=12&forcedisplay[8]=21&forcedisplay[9]=17&forcedisplay[10]=16&forcedisplay[11]=4&forcedisplay[12]=5&forcedisplay[13]=22&forcedisplay[14]=8&forcedisplay[15]=14&forcedisplay[16]=70&forcedisplay[17]=71&forcedisplay[18]=6&forcedisplay[19]=7&forcedisplay[20]=3&forcedisplay[21]=24&forcedisplay[22]=66&expand_dropdowns=true")
    suspend fun getTicketsNovosEdit(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Query("range") range: String = "0-500",
        @Query("order") order: String = "DESC",
        @Query("sort") sort: Int = 19
    ): retrofit2.Response<TicketListResponse>

    @GET("apirest.php/search/Ticket?criteria[0][criteria][0][field]=12&criteria[0][criteria][0][searchtype]=equals&criteria[0][criteria][0][value]=2&criteria[0][criteria][1][link]=OR&criteria[0][criteria][1][field]=12&criteria[0][criteria][1][searchtype]=equals&criteria[0][criteria][1][value]=3&criteria[0][criteria][2][link]=OR&criteria[0][criteria][2][field]=12&criteria[0][criteria][2][searchtype]=equals&criteria[0][criteria][2][value]=4&forcedisplay[0]=2&forcedisplay[1]=1&forcedisplay[2]=9&forcedisplay[3]=151&forcedisplay[4]=158&forcedisplay[5]=19&forcedisplay[6]=15&forcedisplay[7]=12&forcedisplay[8]=21&forcedisplay[9]=17&forcedisplay[10]=16&forcedisplay[11]=4&forcedisplay[12]=5&forcedisplay[13]=22&forcedisplay[14]=8&forcedisplay[15]=14&forcedisplay[16]=70&forcedisplay[17]=71&forcedisplay[18]=6&forcedisplay[19]=7&forcedisplay[20]=3&forcedisplay[21]=24&forcedisplay[22]=66&expand_dropdowns=true")
    suspend fun getTicketsEmProgressoEdit(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Query("range") range: String = "0-500",
        @Query("order") order: String = "DESC",
        @Query("sort") sort: Int = 19
    ): retrofit2.Response<TicketListResponse>

    @GET("apirest.php/search/Ticket?sort=17&order=DESC&forcedisplay[0]=2&forcedisplay[1]=1&forcedisplay[3]=151&forcedisplay[4]=158&forcedisplay[5]=19&forcedisplay[6]=15&forcedisplay[7]=12&forcedisplay[8]=21&forcedisplay[9]=17&forcedisplay[10]=16&forcedisplay[11]=4&forcedisplay[12]=5&forcedisplay[13]=22&forcedisplay[14]=8&forcedisplay[15]=14&forcedisplay[16]=70&forcedisplay[17]=71&forcedisplay[18]=6&forcedisplay[19]=7&forcedisplay[20]=3&forcedisplay[21]=24&forcedisplay[22]=66&expand_dropdowns=true")
    suspend fun getListaTicketsRecentPorEstado(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Query("criteria[0][field]") field: Int,
        @Query("criteria[0][searchtype]") type: String = "equals",
        @Query("criteria[0][value]") value: Int,
        @Query("criteria[1][value]") dataLimite: String,
        @Query("criteria[1][field]") fieldDate: Int = 15,
        @Query("criteria[1][searchtype]") typeDate: String = "morethan",
        @Query("range") range: String = "0-500"
    ): retrofit2.Response<TicketListResponse>

    @GET("apirest.php/search/Ticket?sort=19&order=DESC&forcedisplay[0]=2&forcedisplay[1]=15&forcedisplay[2]=7&expand_dropdowns=true")
    suspend fun getTicketsParaEstatisticas(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Query("criteria[0][field]") field: Int,
        @Query("criteria[0][searchtype]") type: String = "equals",
        @Query("criteria[0][value]") value: Int,
        @Query("criteria[1][value]") dataLimite: String,
        @Query("criteria[1][field]") fieldDate: Int = 15,
        @Query("criteria[1][searchtype]") typeDate: String = "morethan",
        @Query("range") range: String = "0-500"
    ): retrofit2.Response<TicketListResponse>

    @GET("apirest.php/search/Ticket?forcedisplay[0]=7&expand_dropdowns=true")
    suspend fun getCategoriasEstatisticas(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Query("criteria[0][value]") dataLimite: String,
        @Query("criteria[0][field]") fieldDate: Int = 15,
        @Query("criteria[0][searchtype]") typeDate: String = "morethan",
        @Query("range") range: String = "0-3000"
    ): retrofit2.Response<TicketListResponse>

    @GET("apirest.php/search/Ticket?criteria[0][field]=4&criteria[0][searchtype]=equals&criteria[1][link]=OR&criteria[1][field]=5&criteria[1][searchtype]=equals&criteria[2][link]=OR&criteria[2][field]=22&criteria[2][searchtype]=equals&sort=19&order=DESC&forcedisplay[0]=2&forcedisplay[1]=1&forcedisplay[3]=15&forcedisplay[4]=12&forcedisplay[5]=21&forcedisplay[6]=17&forcedisplay[7]=16&forcedisplay[8]=4&forcedisplay[9]=5&forcedisplay[10]=22&forcedisplay[11]=8&forcedisplay[12]=14&forcedisplay[13]=70&forcedisplay[14]=71&forcedisplay[15]=6&forcedisplay[16]=7&forcedisplay[17]=3&forcedisplay[18]=66&expand_dropdowns=true")
    suspend fun getUltimasAtividades(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Query("criteria[0][value]") userId1: Int,
        @Query("criteria[1][value]") userId2: Int,
        @Query("criteria[2][value]") userId3: Int,
        @Query("_unused1") userId4: Int = 0,
        @Query("_unused2") userId5: Int = 0,
        @Query("_unused3") userId6: Int = 0,
        @Query("_unused4") login1: String = "",
        @Query("_unused5") login2: String = "",
        @Query("range") range: String = "0-9"
    ): retrofit2.Response<TicketListResponse>

    @GET("apirest.php/search/Ticket?sort=19&order=DESC&forcedisplay[0]=2&forcedisplay[1]=1&forcedisplay[3]=15&forcedisplay[4]=12&forcedisplay[5]=21&forcedisplay[6]=17&forcedisplay[7]=16&forcedisplay[8]=4&forcedisplay[9]=5&forcedisplay[10]=22&forcedisplay[11]=8&forcedisplay[12]=14&forcedisplay[13]=70&forcedisplay[14]=71&forcedisplay[15]=6&forcedisplay[16]=7&forcedisplay[17]=66&expand_dropdowns=true")
    suspend fun getTicketsPorData(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Query("criteria[0][value]") date: String,
        @Query("criteria[0][field]") field: Int = 15,
        @Query("criteria[0][searchtype]") type: String = "contains"
    ): retrofit2.Response<TicketListResponse>
    @GET("apirest.php/search/Ticket?sort=19&order=DESC&forcedisplay[0]=2&forcedisplay[1]=1&forcedisplay[3]=15&forcedisplay[4]=12&forcedisplay[5]=21&forcedisplay[6]=17&forcedisplay[7]=16&forcedisplay[8]=4&forcedisplay[9]=5&forcedisplay[10]=22&forcedisplay[11]=8&forcedisplay[12]=14&forcedisplay[13]=70&forcedisplay[14]=71&forcedisplay[15]=6&forcedisplay[16]=7&forcedisplay[17]=66&expand_dropdowns=true")
    suspend fun getHistoricoTicketsPaginado(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Query("range") range: String = "0-150",
        @Query("criteria[0][value]") userId1: Int,
        @Query("criteria[0][field]") field1: Int = 4, // Requester
        @Query("criteria[0][searchtype]") search1: String = "equals",
        @Query("criteria[1][link]") link: String = "OR",
        @Query("criteria[1][value]") userId2: Int,
        @Query("criteria[1][field]") field2: Int = 5, // Assigned Technician
        @Query("criteria[1][searchtype]") search2: String = "equals",
        @Query("criteria[2][link]") link3: String = "OR",
        @Query("criteria[2][value]") userId3: Int,
        @Query("criteria[2][field]") field3: Int = 22, // Author
        @Query("criteria[2][searchtype]") search3: String = "equals",
        @Query("criteria[3][link]") link4: String = "OR",
        @Query("criteria[3][value]") userId4: Int,
        @Query("criteria[3][field]") field4: Int = 66, // Observer
        @Query("criteria[3][searchtype]") search4: String = "equals",
        @Query("_cache_buster") cacheBuster: Long = System.currentTimeMillis()
    ): retrofit2.Response<TicketListResponse>

    // Endpoint limpo para filtrar o histórico por um único papel (CRIADOS, REQUERENTE, OBSERVADOR ou ATRIBUÍDO)
    @GET("apirest.php/search/Ticket?sort=19&order=DESC&forcedisplay[0]=2&forcedisplay[1]=1&forcedisplay[3]=15&forcedisplay[4]=12&forcedisplay[5]=21&forcedisplay[6]=17&forcedisplay[7]=16&forcedisplay[8]=4&forcedisplay[9]=5&forcedisplay[10]=22&forcedisplay[11]=8&forcedisplay[12]=14&forcedisplay[13]=70&forcedisplay[14]=71&forcedisplay[15]=6&forcedisplay[16]=7&forcedisplay[17]=66&expand_dropdowns=true")
    suspend fun getHistoricoTicketsPorPapel(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Query("range") range: String = "0-150",
        @Query("criteria[0][field]") field: Int,       // 4=Requerente, 5=Atribuído, 22=Criados, 66=Observador
        @Query("criteria[0][searchtype]") searchType: String = "equals",
        @Query("criteria[0][value]") userId: Int,
        @Query("_cache_buster") cacheBuster: Long = System.currentTimeMillis()
    ): retrofit2.Response<TicketListResponse>

    @GET("apirest.php/search/Ticket?sort=19&order=DESC&forcedisplay[0]=2&forcedisplay[1]=1&forcedisplay[3]=15&forcedisplay[4]=12&forcedisplay[5]=21&forcedisplay[6]=17&forcedisplay[7]=16&forcedisplay[8]=4&forcedisplay[9]=5&forcedisplay[10]=22&forcedisplay[11]=18&forcedisplay[12]=14&forcedisplay[13]=70&forcedisplay[14]=71&forcedisplay[15]=6&forcedisplay[16]=7&forcedisplay[17]=3&forcedisplay[18]=151&forcedisplay[19]=158&forcedisplay[20]=66&expand_dropdowns=true")
    suspend fun getAgendaAtivos(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Query("is_deleted") isDeleted: Int = 0,
        @Query("range") range: String = "0-500"
    ): retrofit2.Response<TicketListResponse>

    @GET("apirest.php/search/Ticket?criteria[0][field]=12&criteria[0][searchtype]=equals&criteria[0][value]=2&criteria[1][link]=OR&criteria[1][field]=12&criteria[1][searchtype]=equals&criteria[1][value]=4&criteria[2][link]=OR&criteria[2][field]=12&criteria[2][searchtype]=equals&criteria[2][value]=3&forcedisplay[0]=2&forcedisplay[1]=1&forcedisplay[3]=15&forcedisplay[4]=12&forcedisplay[5]=21&forcedisplay[6]=17&forcedisplay[7]=16&forcedisplay[8]=4&forcedisplay[9]=5&forcedisplay[10]=22&forcedisplay[11]=8&forcedisplay[12]=14&forcedisplay[13]=70&forcedisplay[14]=71&forcedisplay[15]=6&forcedisplay[16]=7&forcedisplay[17]=3&forcedisplay[18]=151&forcedisplay[19]=158&forcedisplay[20]=66&expand_dropdowns=true")
    suspend fun getListaProgressoTotal(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Query("order") order: String = "DESC",
        @Query("sort") sort: Int = 15,
        @Query("range") range: String = "0-149"
    ): retrofit2.Response<TicketListResponse>

    @GET("apirest.php/search/Ticket?forcedisplay[0]=2&forcedisplay[1]=1&forcedisplay[3]=15&forcedisplay[4]=12&forcedisplay[5]=21&forcedisplay[6]=17&forcedisplay[7]=16&forcedisplay[8]=4&forcedisplay[9]=5&forcedisplay[10]=22&forcedisplay[11]=8&forcedisplay[12]=14&forcedisplay[13]=70&forcedisplay[14]=71&forcedisplay[15]=6&forcedisplay[16]=7&forcedisplay[17]=3&forcedisplay[18]=151&forcedisplay[19]=158&forcedisplay[20]=66&expand_dropdowns=true")
    suspend fun getListaTicketsPorEstadoTotal(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Query("criteria[0][field]") field: Int,
        @Query("criteria[0][searchtype]") type: String = "equals",
        @Query("criteria[0][value]") value: Int,
        @Query("order") order: String = "DESC",
        @Query("sort") sort: Int = 15,
        @Query("range") range: String = "0-149"
    ): retrofit2.Response<TicketListResponse>

    @GET("apirest.php/search/Ticket?criteria[0][criteria][0][field]=12&criteria[0][criteria][0][searchtype]=equals&criteria[0][criteria][0][value]=2&criteria[0][criteria][1][link]=OR&criteria[0][criteria][1][field]=12&criteria[0][criteria][1][searchtype]=equals&criteria[0][criteria][1][value]=3&criteria[0][criteria][2][link]=OR&criteria[0][criteria][2][field]=12&criteria[0][criteria][2][searchtype]=equals&criteria[0][criteria][2][value]=4&criteria[1][link]=AND&criteria[1][criteria][0][field]=4&criteria[1][criteria][0][searchtype]=equals&criteria[1][criteria][1][link]=OR&criteria[1][criteria][1][field]=5&criteria[1][criteria][1][searchtype]=equals&criteria[1][criteria][2][link]=OR&criteria[1][criteria][2][field]=22&criteria[1][criteria][2][searchtype]=equals&forcedisplay[0]=2&forcedisplay[1]=1&forcedisplay[3]=15&forcedisplay[4]=12&forcedisplay[5]=21&forcedisplay[6]=17&forcedisplay[7]=16&forcedisplay[8]=4&forcedisplay[9]=5&forcedisplay[10]=22&forcedisplay[11]=8&forcedisplay[12]=14&forcedisplay[13]=70&forcedisplay[14]=71&forcedisplay[15]=6&forcedisplay[16]=7&forcedisplay[17]=3&forcedisplay[18]=151&forcedisplay[19]=158&forcedisplay[20]=66&expand_dropdowns=true")
    suspend fun getTicketsMeusEmResolucao(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Query("criteria[1][criteria][0][value]") userId1: Int,
        @Query("criteria[1][criteria][1][value]") userId2: Int,
        @Query("criteria[1][criteria][2][value]") userId3: Int,
        @Query("_unused1") userId4: Int,
        @Query("_unused2") userId5: Int,
        @Query("_unused3") userId6: Int,
        @Query("_unused4") login1: String,
        @Query("_unused5") login2: String,
        @Query("order") order: String = "DESC",
        @Query("sort") sort: Int = 15,
        @Query("range") range: String = "0-149"
    ): retrofit2.Response<TicketListResponse>

    @GET("apirest.php/search/Ticket?criteria[0][field]=12&criteria[0][searchtype]=equals&criteria[1][link]=AND&criteria[1][criteria][0][field]=4&criteria[1][criteria][0][searchtype]=equals&criteria[1][criteria][1][link]=OR&criteria[1][criteria][1][field]=5&criteria[1][criteria][1][searchtype]=equals&criteria[1][criteria][2][link]=OR&criteria[1][criteria][2][field]=22&criteria[1][criteria][2][searchtype]=equals&forcedisplay[0]=2&forcedisplay[1]=1&forcedisplay[3]=15&forcedisplay[4]=12&forcedisplay[5]=21&forcedisplay[6]=17&forcedisplay[7]=16&forcedisplay[8]=4&forcedisplay[9]=5&forcedisplay[10]=22&forcedisplay[11]=8&forcedisplay[12]=14&forcedisplay[13]=70&forcedisplay[14]=71&forcedisplay[15]=6&forcedisplay[16]=7&forcedisplay[17]=3&forcedisplay[18]=151&forcedisplay[19]=158&forcedisplay[20]=66&expand_dropdowns=true")
    suspend fun getTicketsMeusEmProgressoPorEstado(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Query("criteria[0][value]") statusValue: Int,
        @Query("criteria[1][criteria][0][value]") userId1: Int,
        @Query("criteria[1][criteria][1][value]") userId2: Int,
        @Query("criteria[1][criteria][2][value]") userId3: Int,
        @Query("_unused1") userId4: Int,
        @Query("_unused2") userId5: Int,
        @Query("_unused3") userId6: Int,
        @Query("_unused4") login1: String,
        @Query("_unused5") login2: String,
        @Query("order") order: String = "DESC",
        @Query("sort") sort: Int = 15,
        @Query("range") range: String = "0-149"
    ): retrofit2.Response<TicketListResponse>

    @GET("apirest.php/search/Ticket?sort=19&order=DESC&forcedisplay[0]=2&forcedisplay[1]=1&forcedisplay[3]=15&forcedisplay[4]=12&forcedisplay[5]=21&forcedisplay[6]=17&forcedisplay[7]=16&forcedisplay[8]=4&forcedisplay[9]=5&forcedisplay[10]=22&forcedisplay[11]=18&forcedisplay[12]=14&forcedisplay[13]=70&forcedisplay[14]=71&forcedisplay[15]=6&forcedisplay[16]=7&forcedisplay[17]=3&forcedisplay[18]=151&forcedisplay[19]=158&forcedisplay[20]=66&expand_dropdowns=true")
    suspend fun getAgendaAtivosByUser(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        // Status 1,2,3,4 (Ativos)
        @Query("criteria[0][criteria][0][field]") fS1: Int = 12,
        @Query("criteria[0][criteria][0][searchtype]") sS1: String = "equals",
        @Query("criteria[0][criteria][0][value]") vS1: Int = 1,
        @Query("criteria[0][criteria][1][link]") lS1: String = "OR",
        @Query("criteria[0][criteria][1][field]") fS2: Int = 12,
        @Query("criteria[0][criteria][1][searchtype]") sS2: String = "equals",
        @Query("criteria[0][criteria][1][value]") vS2: Int = 2,
        @Query("criteria[0][criteria][2][link]") lS2: String = "OR",
        @Query("criteria[0][criteria][2][field]") fS3: Int = 12,
        @Query("criteria[0][criteria][2][searchtype]") sS3: String = "equals",
        @Query("criteria[0][criteria][2][value]") vS3: Int = 3,
        @Query("criteria[0][criteria][3][link]") lS3: String = "OR",
        @Query("criteria[0][criteria][3][field]") fS4: Int = 12,
        @Query("criteria[0][criteria][3][searchtype]") sS4: String = "equals",
        @Query("criteria[0][criteria][3][value]") vS4: Int = 4,
        
        // Identidade (3 campos)
        @Query("criteria[1][link]") linkAND: String = "AND",
        @Query("criteria[1][criteria][0][field]") field1: Int = 4,
        @Query("criteria[1][criteria][0][searchtype]") search1: String = "equals",
        @Query("criteria[1][criteria][0][value]") userId1: Int,
        @Query("criteria[1][criteria][1][link]") link1: String = "OR",
        @Query("criteria[1][criteria][1][field]") field2: Int = 5,
        @Query("criteria[1][criteria][1][searchtype]") search2: String = "equals",
        @Query("criteria[1][criteria][1][value]") userId2: Int,
        @Query("criteria[1][criteria][2][link]") link2: String = "OR",
        @Query("criteria[1][criteria][2][field]") field3: Int = 22,
        @Query("criteria[1][criteria][2][searchtype]") search3: String = "equals",
        @Query("criteria[1][criteria][2][value]") userId3: Int,
        
        // Unused params
        @Query("_unused1") field4: Int = 0,
        @Query("_unused2") search4: String = "",
        @Query("_unused3") userId4: Int = 0,
        @Query("_unused4") link4: String = "",
        @Query("_unused5") field5: Int = 0,
        @Query("_unused6") search5: String = "",
        @Query("_unused7") userId5: Int = 0,
        @Query("_unused8") link5: String = "",
        @Query("_unused9") field6: Int = 0,
        @Query("_unused10") search6: String = "",
        @Query("_unused11") userId6: Int = 0,
        @Query("_unused12") link6: String = "",
        @Query("_unused13") field7: Int = 0,
        @Query("_unused14") search7: String = "",
        @Query("_unused15") login1: String = "",
        @Query("_unused16") link7: String = "",
        @Query("_unused17") field8: Int = 0,
        @Query("_unused18") search8: String = "",
        @Query("_unused19") login2: String = "",
        
        @Query("range") range: String = "0-500"
    ): retrofit2.Response<TicketListResponse>

    @GET("apirest.php/search/Ticket?sort=18&order=ASC&forcedisplay[0]=2&forcedisplay[1]=1&forcedisplay[3]=15&forcedisplay[4]=12&forcedisplay[5]=21&forcedisplay[6]=17&forcedisplay[7]=16&forcedisplay[8]=4&forcedisplay[9]=5&forcedisplay[10]=22&forcedisplay[11]=18&forcedisplay[12]=14&forcedisplay[13]=70&forcedisplay[14]=71&forcedisplay[15]=6&forcedisplay[16]=7&forcedisplay[17]=66&expand_dropdowns=true")
    suspend fun getTicketsExpirados(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Query("criteria[0][field]") field1: Int = 12,
        @Query("criteria[0][searchtype]") search1: String = "lessthan",
        @Query("criteria[0][value]") statusLimite: Int = 5,
        @Query("criteria[1][link]") link1: String = "AND",
        @Query("criteria[1][field]") field2: Int = 5,
        @Query("criteria[1][searchtype]") search2: String = "equals",
        @Query("criteria[1][value]") userId: Int,
        @Query("range") range: String = "0-500"
    ): TicketListResponse

    @GET("apirest.php/search/Ticket?sort=18&order=ASC&forcedisplay[0]=2&forcedisplay[1]=1&forcedisplay[3]=15&forcedisplay[4]=12&forcedisplay[5]=21&forcedisplay[6]=17&forcedisplay[7]=16&forcedisplay[8]=4&forcedisplay[9]=5&forcedisplay[10]=22&forcedisplay[11]=18&forcedisplay[12]=14&forcedisplay[13]=70&forcedisplay[14]=71&forcedisplay[15]=6&forcedisplay[16]=7&forcedisplay[17]=66&expand_dropdowns=true")
    suspend fun getTodosTicketsExpirados(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Query("criteria[0][field]") field1: Int = 12,
        @Query("criteria[0][searchtype]") search1: String = "lessthan",
        @Query("criteria[0][value]") statusLimite: Int = 5,
        @Query("range") range: String = "0-500"
    ): TicketListResponse


    @GET("apirest.php/search/Ticket?forcedisplay[0]=2&forcedisplay[1]=1&forcedisplay[3]=151&forcedisplay[4]=158&forcedisplay[5]=19&forcedisplay[6]=15&forcedisplay[7]=12&forcedisplay[8]=21&forcedisplay[9]=17&forcedisplay[10]=16&forcedisplay[11]=4&forcedisplay[12]=5&forcedisplay[13]=22&forcedisplay[14]=8&forcedisplay[15]=14&forcedisplay[16]=70&forcedisplay[17]=71&forcedisplay[18]=6&forcedisplay[19]=7&forcedisplay[20]=3&forcedisplay[21]=24&forcedisplay[22]=66&expand_dropdowns=true")
    suspend fun pesquisarTickets(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Query("criteria[0][field]") field: Int,
        @Query("criteria[0][searchtype]") searchType: String,
        @Query("criteria[0][value]") value: String,
        @Query("range") range: String = "0-49",
        @Query("is_deleted") isDeleted: Int = 0
    ): retrofit2.Response<TicketListResponse>

    @GET("apirest.php/search/Ticket?forcedisplay[0]=2&forcedisplay[1]=1&forcedisplay[3]=151&forcedisplay[4]=158&forcedisplay[5]=19&forcedisplay[6]=15&forcedisplay[7]=12&forcedisplay[8]=21&forcedisplay[9]=17&forcedisplay[10]=16&forcedisplay[11]=4&forcedisplay[12]=5&forcedisplay[13]=22&forcedisplay[14]=8&forcedisplay[15]=14&forcedisplay[16]=70&forcedisplay[17]=71&forcedisplay[18]=6&forcedisplay[19]=7&forcedisplay[20]=3&forcedisplay[21]=24&forcedisplay[22]=66&expand_dropdowns=true")
    suspend fun pesquisarTicketsDinamico(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Query("range") range: String = "0-149",
        @Query("is_deleted") isDeleted: Int = 0,
        @QueryMap criteria: Map<String, String>
    ): retrofit2.Response<TicketListResponse>


    @GET("apirest.php/search/Ticket?sort=17&order=DESC&forcedisplay[0]=2&forcedisplay[1]=1&forcedisplay[3]=15&forcedisplay[4]=12&forcedisplay[5]=21&forcedisplay[6]=17&forcedisplay[7]=16&forcedisplay[8]=4&forcedisplay[9]=5&forcedisplay[10]=22&forcedisplay[11]=8&forcedisplay[12]=14&forcedisplay[13]=70&forcedisplay[14]=71&forcedisplay[15]=6&forcedisplay[16]=7&forcedisplay[17]=3&forcedisplay[18]=66&expand_dropdowns=true")
    suspend fun pesquisarTicketsResolvidos(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Query("criteria[0][field]") field: Int,
        @Query("criteria[0][searchtype]") searchType: String,
        @Query("criteria[0][value]") value: String,
        @Query("criteria[1][field]") field2: Int? = null,
        @Query("criteria[1][searchtype]") searchType2: String? = null,
        @Query("criteria[1][value]") value2: String? = null,
        @Query("range") range: String = "0-1000"
    ): TicketListResponse
    @GET("apirest.php/search/Ticket?criteria[0][field]=12&criteria[0][searchtype]=equals&criteria[0][value]=1&range=0-1")
    suspend fun getCountAbertosTotal(@Header("Session-Token") sessionToken: String, @Header("App-Token") appToken: String): retrofit2.Response<SearchResponse>

    @GET("apirest.php/search/Ticket?criteria[0][criteria][0][field]=4&criteria[0][criteria][0][searchtype]=equals&criteria[0][criteria][1][link]=OR&criteria[0][criteria][1][field]=5&criteria[0][criteria][1][searchtype]=equals&criteria[0][criteria][2][link]=OR&criteria[0][criteria][2][field]=22&criteria[0][criteria][2][searchtype]=equals&criteria[1][link]=AND&criteria[1][field]=12&criteria[1][value]=1&range=0-1")
    suspend fun getCountAbertosMeus(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Query("criteria[0][criteria][0][value]") userId1: Int,
        @Query("criteria[0][criteria][1][value]") userId2: Int,
        @Query("criteria[0][criteria][2][value]") userId3: Int,
        @Query("_unused1") userId4: Int,
        @Query("_unused2") userId5: Int,
        @Query("_unused3") userId6: Int,
        @Query("_unused4") login1: String,
        @Query("_unused5") login2: String
    ): retrofit2.Response<SearchResponse>

    @GET("apirest.php/search/Ticket?criteria[0][field]=12&criteria[0][searchtype]=equals&criteria[0][value]=2&criteria[1][link]=OR&criteria[1][field]=12&criteria[1][searchtype]=equals&criteria[1][value]=3&criteria[2][link]=OR&criteria[2][field]=12&criteria[2][searchtype]=equals&criteria[2][value]=4&range=0-1")
    suspend fun getCountProgressoTotal(@Header("Session-Token") sessionToken: String, @Header("App-Token") appToken: String): retrofit2.Response<SearchResponse>

    // In Progress: (U=Me AND Status=2) OR (U=Me AND Status=3) OR (U=Me AND Status=4) for each Role
    @GET("apirest.php/search/Ticket?criteria[0][criteria][0][field]=4&criteria[0][criteria][0][searchtype]=equals&criteria[0][criteria][1][link]=OR&criteria[0][criteria][1][field]=5&criteria[0][criteria][1][searchtype]=equals&criteria[0][criteria][2][link]=OR&criteria[0][criteria][2][field]=22&criteria[0][criteria][2][searchtype]=equals&criteria[1][link]=AND&criteria[1][criteria][0][field]=12&criteria[1][criteria][0][value]=2&criteria[1][criteria][1][link]=OR&criteria[1][criteria][1][field]=12&criteria[1][criteria][1][value]=3&criteria[1][criteria][2][link]=OR&criteria[1][criteria][2][field]=12&criteria[1][criteria][2][value]=4&range=0-1")
    suspend fun getCountProgressoMeus(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Query("criteria[0][criteria][0][value]") userId1: Int,
        @Query("criteria[0][criteria][1][value]") userId2: Int,
        @Query("criteria[0][criteria][2][value]") userId3: Int,
        @Query("_unused1") userId4: Int,
        @Query("_unused2") userId5: Int,
        @Query("_unused3") userId6: Int,
        @Query("_unused4") login1: String,
        @Query("_unused5") login2: String
    ): retrofit2.Response<SearchResponse>

    @GET("apirest.php/search/Ticket?criteria[0][criteria][0][field]=4&criteria[0][criteria][0][searchtype]=equals&criteria[0][criteria][1][link]=OR&criteria[0][criteria][1][field]=5&criteria[0][criteria][1][searchtype]=equals&criteria[0][criteria][2][link]=OR&criteria[0][criteria][2][field]=22&criteria[0][criteria][2][searchtype]=equals&criteria[1][link]=AND&criteria[1][criteria][0][field]=12&criteria[1][criteria][0][value]=5&criteria[1][criteria][1][link]=OR&criteria[1][criteria][1][field]=12&criteria[1][criteria][1][value]=6&criteria[2][link]=AND&criteria[2][field]=19&criteria[2][searchtype]=morethan&range=0-1")
    suspend fun getCountResolvidosRecentes(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Query("criteria[0][criteria][0][value]") userId1: Int,
        @Query("criteria[0][criteria][1][value]") userId2: Int,
        @Query("criteria[0][criteria][2][value]") userId3: Int,
        @Query("_unused1") userId4: Int,
        @Query("_unused2") userId5: Int,
        @Query("_unused3") userId6: Int,
        @Query("_unused4") login1: String,
        @Query("_unused5") login2: String,
        @Query("criteria[2][value]") dataLimite: String
    ): retrofit2.Response<SearchResponse>

    @GET("apirest.php/search/Ticket?criteria[0][criteria][0][field]=12&criteria[0][criteria][0][searchtype]=equals&criteria[0][criteria][0][value]=5&criteria[0][criteria][1][link]=OR&criteria[0][criteria][1][field]=12&criteria[0][criteria][1][searchtype]=equals&criteria[0][criteria][1][value]=6&criteria[1][link]=AND&criteria[1][field]=19&criteria[1][searchtype]=morethan&range=0-1")
    suspend fun getCountResolvidosGlobal(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Query("criteria[1][value]") dataLimite: String
    ): retrofit2.Response<SearchResponse>

    @GET("apirest.php/search/Ticket?criteria[0][field]=4&criteria[0][searchtype]=equals&criteria[1][link]=AND&criteria[1][field]=12&criteria[1][searchtype]=lessthan&criteria[1][value]=5&range=0-1")
    suspend fun getCountRequerenteAtivos(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Query("criteria[0][value]") userId: Int
    ): retrofit2.Response<SearchResponse>

    @GET("apirest.php/search/Ticket?criteria[0][field]=66&criteria[0][searchtype]=equals&criteria[1][link]=AND&criteria[1][field]=12&criteria[1][searchtype]=lessthan&criteria[1][value]=5&range=0-1")
    suspend fun getCountObservadorAtivos(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Query("criteria[0][value]") userId: Int
    ): retrofit2.Response<SearchResponse>

    @GET("apirest.php/search/Ticket?criteria[0][field]=5&criteria[0][searchtype]=equals&criteria[1][link]=AND&criteria[1][field]=12&criteria[1][searchtype]=lessthan&criteria[1][value]=5&range=0-1")
    suspend fun getCountAtribuidoAtivos(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Query("criteria[0][value]") userId: Int
    ): retrofit2.Response<SearchResponse>

    @GET("apirest.php/search/Ticket?criteria[0][criteria][0][field]=4&criteria[0][criteria][0][searchtype]=equals&criteria[0][criteria][1][link]=OR&criteria[0][criteria][1][field]=5&criteria[0][criteria][1][searchtype]=equals&criteria[0][criteria][2][link]=OR&criteria[0][criteria][2][field]=66&criteria[0][criteria][2][searchtype]=equals&criteria[0][criteria][3][link]=OR&criteria[0][criteria][3][field]=22&criteria[0][criteria][3][searchtype]=equals&criteria[1][link]=AND&criteria[1][criteria][0][field]=12&criteria[1][criteria][0][value]=5&criteria[1][criteria][1][link]=OR&criteria[1][criteria][1][field]=12&criteria[1][criteria][1][value]=6&criteria[2][link]=AND&criteria[2][field]=19&criteria[2][searchtype]=morethan&range=0-1")
    suspend fun getCountFinalizadosRecentes(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Query("criteria[0][criteria][0][value]") userId1: Int,
        @Query("criteria[0][criteria][1][value]") userId2: Int,
        @Query("criteria[0][criteria][2][value]") userId3: Int,
        @Query("criteria[0][criteria][3][value]") userId4: Int,
        @Query("criteria[2][value]") dataLimite: String
    ): retrofit2.Response<SearchResponse>


    @GET("apirest.php/search/Ticket?criteria[0][criteria][0][field]=12&criteria[0][criteria][0][searchtype]=equals&criteria[0][criteria][0][value]=1&criteria[0][criteria][1][link]=OR&criteria[0][criteria][1][field]=12&criteria[0][criteria][1][searchtype]=equals&criteria[0][criteria][1][value]=2&criteria[0][criteria][2][link]=OR&criteria[0][criteria][2][field]=12&criteria[0][criteria][2][searchtype]=equals&criteria[0][criteria][2][value]=3&criteria[0][criteria][3][link]=OR&criteria[0][criteria][3][field]=12&criteria[0][criteria][3][searchtype]=equals&criteria[0][criteria][3][value]=4&criteria[1][link]=AND&criteria[1][criteria][0][field]=3&criteria[1][criteria][0][searchtype]=equals&criteria[1][criteria][0][value]=4&criteria[1][criteria][1][link]=OR&criteria[1][criteria][1][field]=3&criteria[1][criteria][1][searchtype]=equals&criteria[1][criteria][1][value]=5&criteria[1][criteria][2][link]=OR&criteria[1][criteria][2][field]=3&criteria[1][criteria][2][searchtype]=equals&criteria[1][criteria][2][value]=6&range=0-1")
    suspend fun getCountPrioritariosDashboard(@Header("Session-Token") sessionToken: String, @Header("App-Token") appToken: String): retrofit2.Response<SearchResponse>

    @GET("apirest.php/search/Ticket?criteria[0][criteria][0][field]=4&criteria[0][criteria][0][searchtype]=equals&criteria[0][criteria][1][link]=OR&criteria[0][criteria][1][field]=5&criteria[0][criteria][1][searchtype]=equals&criteria[0][criteria][2][link]=OR&criteria[0][criteria][2][field]=22&criteria[0][criteria][2][searchtype]=equals&criteria[1][link]=AND&criteria[1][criteria][0][field]=3&criteria[1][criteria][0][searchtype]=equals&criteria[1][criteria][0][value]=4&criteria[1][criteria][1][link]=OR&criteria[1][criteria][1][field]=3&criteria[1][criteria][1][searchtype]=equals&criteria[1][criteria][1][value]=5&criteria[1][criteria][2][link]=OR&criteria[1][criteria][2][field]=3&criteria[1][criteria][2][searchtype]=equals&criteria[1][criteria][2][value]=6&criteria[2][link]=AND&criteria[2][criteria][0][field]=12&criteria[2][criteria][0][value]=1&criteria[2][criteria][1][link]=OR&criteria[2][criteria][1][field]=12&criteria[2][criteria][1][value]=2&criteria[2][criteria][2][link]=OR&criteria[2][criteria][2][field]=12&criteria[2][criteria][2][value]=3&criteria[2][criteria][3][link]=OR&criteria[2][criteria][3][field]=12&criteria[2][criteria][3][value]=4&range=0-1")
    suspend fun getCountPrioritariosMeus(
        @Header("Session-Token") sessionToken: String, 
        @Header("App-Token") appToken: String,
        @Query("criteria[0][criteria][0][value]") userId1: Int,
        @Query("criteria[0][criteria][1][value]") userId2: Int,
        @Query("criteria[0][criteria][2][value]") userId3: Int,
        @Query("_unused1") userId4: Int,
        @Query("_unused2") userId5: Int,
        @Query("_unused3") userId6: Int,
        @Query("_unused4") login1: String,
        @Query("_unused5") login2: String
    ): retrofit2.Response<SearchResponse>

    @GET("apirest.php/search/Ticket?criteria[0][criteria][0][field]=12&criteria[0][criteria][0][searchtype]=equals&criteria[0][criteria][0][value]=1&criteria[0][criteria][1][link]=OR&criteria[0][criteria][1][field]=12&criteria[0][criteria][1][searchtype]=equals&criteria[0][criteria][1][value]=2&criteria[0][criteria][2][link]=OR&criteria[0][criteria][2][field]=12&criteria[0][criteria][2][searchtype]=equals&criteria[0][criteria][2][value]=3&criteria[0][criteria][3][link]=OR&criteria[0][criteria][3][field]=12&criteria[0][criteria][3][searchtype]=equals&criteria[0][criteria][3][value]=4&criteria[1][link]=AND&criteria[1][criteria][0][field]=3&criteria[1][criteria][0][searchtype]=equals&criteria[1][criteria][0][value]=4&criteria[1][criteria][1][link]=OR&criteria[1][criteria][1][field]=3&criteria[1][criteria][1][searchtype]=equals&criteria[1][criteria][1][value]=5&criteria[1][criteria][2][link]=OR&criteria[1][criteria][2][field]=3&criteria[1][criteria][2][searchtype]=equals&criteria[1][criteria][2][value]=6&expand_dropdowns=true&forcedisplay[0]=2&forcedisplay[1]=1&forcedisplay[3]=15&forcedisplay[4]=12&forcedisplay[5]=21&forcedisplay[6]=17&forcedisplay[7]=16&forcedisplay[8]=4&forcedisplay[9]=5&forcedisplay[10]=22&forcedisplay[11]=8&forcedisplay[12]=14&forcedisplay[13]=70&forcedisplay[14]=71&forcedisplay[15]=6&forcedisplay[16]=7&forcedisplay[17]=3&forcedisplay[18]=66&expand_dropdowns=true")
    suspend fun getListaPrioritariosTotal(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Query("range") range: String = "0-1000",
        @Query("order") order: String = "DESC",
        @Query("sort") sort: Int = 19
    ): TicketListResponse

    @GET("apirest.php/search/Ticket?criteria[0][criteria][0][field]=4&criteria[0][criteria][0][searchtype]=equals&criteria[0][criteria][1][link]=OR&criteria[0][criteria][1][field]=5&criteria[0][criteria][1][searchtype]=equals&criteria[0][criteria][2][link]=OR&criteria[0][criteria][2][field]=22&criteria[0][criteria][2][searchtype]=equals&criteria[1][link]=AND&criteria[1][criteria][0][field]=3&criteria[1][criteria][0][searchtype]=equals&criteria[1][criteria][0][value]=4&criteria[1][criteria][1][link]=OR&criteria[1][criteria][1][field]=3&criteria[1][criteria][1][searchtype]=equals&criteria[1][criteria][1][value]=5&criteria[1][criteria][2][link]=OR&criteria[1][criteria][2][field]=3&criteria[1][criteria][2][searchtype]=equals&criteria[1][criteria][2][value]=6&criteria[2][link]=AND&criteria[2][criteria][0][field]=12&criteria[2][criteria][0][value]=1&criteria[2][criteria][1][link]=OR&criteria[2][criteria][1][field]=12&criteria[2][criteria][1][value]=2&criteria[2][criteria][2][link]=OR&criteria[2][criteria][2][field]=12&criteria[2][criteria][2][value]=3&criteria[2][criteria][3][link]=OR&criteria[2][criteria][3][field]=12&criteria[2][criteria][3][value]=4&expand_dropdowns=true&forcedisplay[0]=2&forcedisplay[1]=1&forcedisplay[3]=15&forcedisplay[4]=12&forcedisplay[5]=21&forcedisplay[6]=17&forcedisplay[7]=16&forcedisplay[8]=4&forcedisplay[9]=5&forcedisplay[10]=22&forcedisplay[11]=8&forcedisplay[12]=14&forcedisplay[13]=70&forcedisplay[14]=71&forcedisplay[15]=6&forcedisplay[16]=7&forcedisplay[17]=3&forcedisplay[18]=66&expand_dropdowns=true")
    suspend fun getListaPrioritariosMeus(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Query("criteria[0][criteria][0][value]") userId1: Int,
        @Query("criteria[0][criteria][1][value]") userId2: Int,
        @Query("criteria[0][criteria][2][value]") userId3: Int,
        @Query("_unused1") userId4: Int,
        @Query("_unused2") userId5: Int,
        @Query("_unused3") userId6: Int,
        @Query("_unused4") login1: String,
        @Query("_unused5") login2: String,
        @Query("order") order: String = "DESC",
        @Query("range") range: String = "0-149",
        @Query("sort") sort: Int = 19
    ): TicketListResponse


    @GET("apirest.php/search/Ticket?criteria[0][criteria][0][field]=12&criteria[0][criteria][0][searchtype]=equals&criteria[0][criteria][0][value]=5&criteria[0][criteria][1][link]=OR&criteria[0][criteria][1][field]=12&criteria[0][criteria][1][searchtype]=equals&criteria[0][criteria][1][value]=6&criteria[1][link]=AND&criteria[1][criteria][0][field]=3&criteria[1][criteria][0][searchtype]=equals&criteria[1][criteria][1][value]=4&criteria[1][criteria][1][link]=OR&criteria[1][criteria][1][field]=3&criteria[1][criteria][1][searchtype]=equals&criteria[1][criteria][1][value]=5&criteria[1][criteria][2][link]=OR&criteria[1][criteria][2][field]=3&criteria[1][criteria][2][searchtype]=equals&criteria[1][criteria][2][value]=6&criteria[2][link]=AND&criteria[2][field]=17&criteria[2][searchtype]=morethan&forcedisplay[0]=2&forcedisplay[1]=1&forcedisplay[3]=15&forcedisplay[4]=12&forcedisplay[5]=21&forcedisplay[6]=17&forcedisplay[7]=16&forcedisplay[8]=4&forcedisplay[9]=5&forcedisplay[10]=22&forcedisplay[11]=8&forcedisplay[12]=14&forcedisplay[13]=70&forcedisplay[14]=71&forcedisplay[15]=6&forcedisplay[16]=7&forcedisplay[17]=3&forcedisplay[18]=66&expand_dropdowns=true")
    suspend fun getListaPrioritariosFinalizadosHistorico(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Query("criteria[2][value]") dataLimite: String,
        @Query("range") range: String = "0-100",
        @Query("order") order: String = "DESC",
        @Query("sort") sort: Int = 17
    ): TicketListResponse

    @GET("apirest.php/search/Ticket?criteria[0][criteria][0][field]=12&criteria[0][criteria][0][searchtype]=equals&criteria[0][criteria][0][value]=1&criteria[0][criteria][1][link]=OR&criteria[0][criteria][1][field]=12&criteria[0][criteria][1][searchtype]=equals&criteria[0][criteria][1][value]=2&criteria[0][criteria][2][link]=OR&criteria[0][criteria][2][field]=12&criteria[0][criteria][2][searchtype]=equals&criteria[0][criteria][2][value]=3&criteria[0][criteria][3][link]=OR&criteria[0][criteria][3][field]=12&criteria[0][criteria][3][searchtype]=equals&criteria[0][criteria][3][value]=4&criteria[1][link]=AND&criteria[1][field]=3&criteria[1][searchtype]=equals&forcedisplay[0]=2&forcedisplay[1]=1&forcedisplay[3]=15&forcedisplay[4]=12&forcedisplay[5]=21&forcedisplay[6]=17&forcedisplay[7]=16&forcedisplay[8]=4&forcedisplay[9]=5&forcedisplay[10]=22&forcedisplay[11]=8&forcedisplay[12]=14&forcedisplay[13]=70&forcedisplay[14]=71&forcedisplay[15]=6&forcedisplay[16]=7&forcedisplay[17]=3&forcedisplay[18]=66&expand_dropdowns=true")
    suspend fun getListaPrioritariosPorPrioridade(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Query("criteria[1][value]") priorityValue: Int,
        @Query("range") range: String = "0-1000",
        @Query("order") order: String = "DESC",
        @Query("sort") sort: Int = 19
    ): TicketListResponse

    @GET("apirest.php/search/Ticket?criteria[0][criteria][0][field]=4&criteria[0][criteria][0][searchtype]=equals&criteria[0][criteria][1][link]=OR&criteria[0][criteria][1][field]=5&criteria[0][criteria][1][searchtype]=equals&criteria[0][criteria][2][link]=OR&criteria[0][criteria][2][field]=22&criteria[0][criteria][2][searchtype]=equals&criteria[1][link]=AND&criteria[1][field]=3&criteria[1][searchtype]=equals&criteria[2][link]=AND&criteria[2][criteria][0][field]=12&criteria[2][criteria][0][value]=1&criteria[2][criteria][1][link]=OR&criteria[2][criteria][1][field]=12&criteria[2][criteria][1][value]=2&criteria[2][criteria][2][link]=OR&criteria[2][criteria][2][field]=12&criteria[2][criteria][2][value]=3&criteria[2][criteria][3][link]=OR&criteria[2][criteria][3][field]=12&criteria[2][criteria][3][value]=4&forcedisplay[0]=2&forcedisplay[1]=1&forcedisplay[3]=15&forcedisplay[4]=12&forcedisplay[5]=21&forcedisplay[6]=17&forcedisplay[7]=16&forcedisplay[8]=4&forcedisplay[9]=5&forcedisplay[10]=22&forcedisplay[11]=8&forcedisplay[12]=14&forcedisplay[13]=70&forcedisplay[14]=71&forcedisplay[15]=6&forcedisplay[16]=7&forcedisplay[17]=3&forcedisplay[18]=66&expand_dropdowns=true")
    suspend fun getListaPrioritariosPorPrioridadeMeus(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Query("criteria[0][criteria][0][value]") userId1: Int,
        @Query("criteria[0][criteria][1][value]") userId2: Int,
        @Query("criteria[0][criteria][2][value]") userId3: Int,
        @Query("_unused1") userId4: Int,
        @Query("_unused2") userId5: Int,
        @Query("_unused3") userId6: Int,
        @Query("_unused4") login1: String,
        @Query("_unused5") login2: String,
        @Query("criteria[1][value]") priorityValue1: Int,
        @Query("order") order: String = "DESC",
        @Query("range") range: String = "0-149",
        @Query("sort") sort: Int = 19
    ): TicketListResponse
    @POST("apirest.php/Ticket")
    suspend fun criarTicket(@Header("Session-Token") sessionToken: String, @Header("App-Token") appToken: String, @Body ticket: TicketRequest): TicketCreateResponse

    @POST("apirest.php/PluginFcmToken")
    suspend fun registerFcmToken(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Body tokenData: @JvmSuppressWildcards Map<String, Any>
    ): Response<Unit>

    @GET("apirest.php/search/{itemtype}?expand_dropdowns=true")
    suspend fun searchItems(
        @Path("itemtype") itemtype: String,
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Query("range") range: String,
        @QueryMap criteria: Map<String, String> = emptyMap()
    ): Response<SearchResponse>

    @GET("apirest.php/search/{itemtype}?expand_dropdowns=true")
    suspend fun searchInventory(
        @Path("itemtype") itemtype: String,
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Query("range") range: String = "0-20",
        @QueryMap criteria: Map<String, String> = emptyMap()
    ): Response<SearchResponse>

    @GET("apirest.php/search/{itemtype}")
    suspend fun searchByCriteria(
        @Path("itemtype") itemtype: String,
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Query("criteria[0][field]") field: Int = 5,
        @Query("criteria[0][searchtype]") searchType: String = "equals",
        @Query("criteria[0][value]") value: String,
        @Query("range") range: String = "0-1",
        @Query("forcedisplay[0]") f0: Int = 1,
        @Query("forcedisplay[1]") f1: Int = 2,
        @Query("forcedisplay[2]") f2: Int = 5,
        @Query("forcedisplay[3]") f3: Int = 20,
        @Query("forcedisplay[4]") f4: Int = 80
    ): retrofit2.Response<SearchResponse>

    // Pesquisa por utilizador (campo 70) OU técnico responsável (campo 24) — usada em Meus Dispositivos
    @GET("apirest.php/search/{itemtype}")
    suspend fun searchByUserOrTech(
        @Path("itemtype") itemtype: String,
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        // Critério 0: utilizador (campo 70)
        @Query("criteria[0][field]") field0: Int = 70,
        @Query("criteria[0][searchtype]") searchType0: String = "equals",
        @Query("criteria[0][value]") value0: String,
        // Critério 1: técnico responsável (campo 24) com OR
        @Query("criteria[1][link]") link1: String = "OR",
        @Query("criteria[1][field]") field1: Int = 24,
        @Query("criteria[1][searchtype]") searchType1: String = "equals",
        @Query("criteria[1][value]") value1: String,
        @Query("range") range: String = "0-999",
        @Query("forcedisplay[0]") f0: Int = 1,
        @Query("forcedisplay[1]") f1: Int = 2,
        @Query("forcedisplay[2]") f2: Int = 5,
        @Query("forcedisplay[3]") f3: Int = 70,
        @Query("forcedisplay[4]") f4: Int = 24
    ): retrofit2.Response<SearchResponse>

    // Pesquisa por número de série incluindo campo 3 (localização) — usada em Reportar Problema
    @GET("apirest.php/search/{itemtype}")
    suspend fun searchBySerialWithLocation(
        @Path("itemtype") itemtype: String,
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Query("criteria[0][field]") field: Int = 5,
        @Query("criteria[0][searchtype]") searchType: String = "equals",
        @Query("criteria[0][value]") value: String,
        @Query("range") range: String = "0-1",
        @Query("forcedisplay[0]") f0: Int = 1,
        @Query("forcedisplay[1]") f1: Int = 2,
        @Query("forcedisplay[2]") f2: Int = 3,
        @Query("forcedisplay[3]") f3: Int = 5,
        @Query("forcedisplay[4]") f4: Int = 31
    ): retrofit2.Response<SearchResponse>

    @GET("apirest.php/search/{itemtype}")
    suspend fun searchItemsWithTickets(
        @Path("itemtype") itemtype: String,
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Query("range") range: String,
        @Query("criteria[0][field]") filterField: Int = 60,
        @Query("criteria[0][searchtype]") filterType: String = "notcontains",
        @Query("criteria[0][value]") filterValue: String = "0",
        @Query("forcedisplay[0]") f0: Int = 1,
        @Query("forcedisplay[1]") f1: Int = 2,
        @Query("forcedisplay[2]") f2: Int = 5,
        @Query("forcedisplay[3]") f3: Int = 70,
        @Query("forcedisplay[4]") f4: Int = 16,
        @Query("forcedisplay[5]") f5: Int = 31
    ): Response<SearchResponse>

    @GET("apirest.php/search/{itemtype}")
    suspend fun searchByCriteriaWithTickets(
        @Path("itemtype") itemtype: String,
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Query("criteria[0][field]") field: Int = 1,
        @Query("criteria[0][searchtype]") searchType: String = "contains",
        @Query("criteria[0][value]") value: String,
        @Query("criteria[1][link]") link: String = "AND",
        @Query("criteria[1][field]") filterField: Int = 60,
        @Query("criteria[1][searchtype]") filterType: String = "notcontains",
        @Query("criteria[1][value]") filterValue: String = "0"
    ): Response<SearchResponse>

    @PUT("apirest.php/{itemtype}/{id}")
    suspend fun updateItem(
        @Path("itemtype") itemtype: String,
        @Path("id") id: Int,
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Body input: @JvmSuppressWildcards Map<String, @JvmSuppressWildcards Map<String, Any>>
    ): Response<Unit>

    @GET("apirest.php/Ticket/{id}")
    suspend fun getTicketById(
        @Path("id") id: String,
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Query("expand_dropdowns") expandDropdowns: Boolean = true
    ): retrofit2.Response<Map<String, Any>>

    @PUT("apirest.php/Ticket/{id}")
    suspend fun updateTicketStatus(
        @Path("id") id: String,
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Body input: @JvmSuppressWildcards Map<String, @JvmSuppressWildcards Map<String, @JvmSuppressWildcards Any>>
    ): retrofit2.Response<Unit>

    @POST("apirest.php/Ticket_User")
    suspend fun atribuirTicket(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Body input: @JvmSuppressWildcards Map<String, @JvmSuppressWildcards Map<String, @JvmSuppressWildcards Any>>
    ): retrofit2.Response<okhttp3.ResponseBody>
    
    @GET("apirest.php/Ticket/{id}/Ticket_User")
    suspend fun getTicketActors(
        @Path("id") id: String,
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String
    ): retrofit2.Response<List<Map<String, Any>>>

    @DELETE("apirest.php/Ticket_User/{id}")
    suspend fun deleteTicketActor(
        @Path("id") id: Int,
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String
    ): retrofit2.Response<Unit>

    @DELETE("apirest.php/Reservation/{id}")
    suspend fun deleteReservation(
        @Path("id") id: Int,
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String
    ): Response<Unit>

    @GET("apirest.php/ReservationItem")
    suspend fun getAllReservationItems(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Query("range") range: String = "0-100"
    ): Response<List<Map<String, Any>>>

    @GET("apirest.php/{itemtype}/{id}")
    suspend fun getGenericItem(
        @Path("itemtype") itemtype: String,
        @Path("id") id: Int,
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String
    ): Response<Map<String, Any>>

    @GET("apirest.php/{itemtype}/{id}/ReservationItem")
    suspend fun getReservationItemForAsset(
        @Path("itemtype") itemtype: String,
        @Path("id") id: Int,
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String
    ): Response<List<Map<String, Any>>>

    @POST("apirest.php/ReservationItem")
    suspend fun createReservationItem(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Body input: @JvmSuppressWildcards Map<String, Any>
    ): Response<Map<String, Any>>

    @POST("apirest.php/Reservation")
    suspend fun createReservation(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Body input: @JvmSuppressWildcards Map<String, Any>
    ): Response<Map<String, Any>>

    @GET("apirest.php/ReservationItem/{resItemId}/Reservation")
    suspend fun getReservationsForItem(
        @Path("resItemId") resItemId: Int,
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Query("expand_dropdowns") expand: Boolean = true,
        @Query("range") range: String = "0-100"
    ): Response<List<Map<String, Any>>>

    @GET("apirest.php/{itemtype}/{id}/Item_Ticket")
    suspend fun getItemTicketsRelation(
        @Path("itemtype") itemtype: String,
        @Path("id") id: Int,
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String
    ): retrofit2.Response<okhttp3.ResponseBody>

    @GET("apirest.php/search/Ticket?sort=19&order=DESC&forcedisplay[0]=2&forcedisplay[1]=1&forcedisplay[3]=15&forcedisplay[4]=12&forcedisplay[5]=4&forcedisplay[6]=5&forcedisplay[7]=22&forcedisplay[8]=8&forcedisplay[9]=14&forcedisplay[10]=70&forcedisplay[11]=71&forcedisplay[12]=6&forcedisplay[13]=7&forcedisplay[14]=66&expand_dropdowns=true")
    suspend fun getTicketsByIds(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @QueryMap criteria: Map<String, String>
    ): retrofit2.Response<okhttp3.ResponseBody>

    @GET("apirest.php/search/Ticket?forcedisplay[0]=2&forcedisplay[1]=1&forcedisplay[3]=15&forcedisplay[4]=12&forcedisplay[5]=21&forcedisplay[6]=17&forcedisplay[7]=16&forcedisplay[8]=4&forcedisplay[9]=5&forcedisplay[10]=22&forcedisplay[11]=8&forcedisplay[12]=14&forcedisplay[13]=70&forcedisplay[14]=71&forcedisplay[15]=6&forcedisplay[16]=7&forcedisplay[17]=3&forcedisplay[18]=66&sort=19&order=DESC&expand_dropdowns=true")
    suspend fun getUltimasAtividadesGeral(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Query("range") range: String = "0-4"
    ): retrofit2.Response<TicketListResponse>

    @GET("apirest.php/{itemtype}/{id}/Log")
    suspend fun getLogsForItem(
        @Path("itemtype") itemtype: String,
        @Path("id") id: Int,
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String
    ): Response<List<Map<String, Any>>>

    @GET("apirest.php/Log")
    suspend fun getGeneralLogs(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Query("order") order: String = "DESC",
        @Query("range") range: String = "0-100"
    ): List<GLPILog>

    @GET("apirest.php/Log")
    suspend fun getLogsByUser(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Query("users_id") userId: Int,
        @Query("range") range: String = "0-149",
        @Query("sort") sort: String = "id",
        @Query("order") order: String = "DESC"
    ): List<Map<String, Any>>

    // Pesquisa de utilizadores para atribuição
    @GET("apirest.php/search/User")
    suspend fun pesquisarUsuarios(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @QueryMap criteria: Map<String, String> = emptyMap(),
        @Query("range") range: String = "0-1000"
    ): Response<SearchResponse>

    @GET("apirest.php/ITILCategory")
    suspend fun getITILCategories(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Query("range") range: String = "0-200"
    ): List<Map<String, Any>>

    @GET("apirest.php/Location")
    suspend fun getLocationsList(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Query("range") range: String = "0-300"
    ): List<Map<String, Any>>

    @GET("apirest.php/State")
    suspend fun getStatesList(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Query("range") range: String = "0-100"
    ): List<Map<String, Any>>

    @POST("apirest.php/{itemtype}")
    suspend fun addDevice(
        @Path("itemtype") itemtype: String,
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Body request: @JvmSuppressWildcards Map<String, Any>
    ): retrofit2.Response<DeviceCreateResponse>

    @PUT("apirest.php/{itemtype}/{id}")
    suspend fun updateDevice(
        @Path("itemtype") itemtype: String,
        @Path("id") id: Int,
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Body request: @JvmSuppressWildcards Map<String, Any>
    ): retrofit2.Response<ResponseBody>

    @GET("apirest.php/Ticket/{id}/ITILFollowup")
    suspend fun getTicketFollowups(
        @Path("id") ticketId: String,
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String
    ): List<Map<String, Any>>

    @GET("apirest.php/Ticket/{id}/ITILSolution")
    suspend fun getTicketSolutions(
        @Path("id") ticketId: String,
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String
    ): List<Map<String, Any>>

    @POST("apirest.php/ITILFollowup")
    suspend fun addFollowup(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Body input: Map<String, @JvmSuppressWildcards Any?>
    ): Response<Map<String, @JvmSuppressWildcards Any?>>

    @Multipart
    @POST("apirest.php/Document")
    suspend fun addDocument(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Part("uploadManifest") manifest: okhttp3.RequestBody,
        @Part file: okhttp3.MultipartBody.Part
    ): Response<DocumentCreateResponse>

    // Endpoint mais robusto para buscar portas ligadas diretamente a um item sem depender de IDs de critérios de pesquisa
    // get_full_objects=true garante que o GLPI devolva o objeto item { name: "APC" } completo
    @GET("apirest.php/{itemtype}/{id}/NetworkPort?expand_dropdowns=true&get_full_objects=true")
    suspend fun getNetworkPorts(
        @Path("itemtype") itemType: String,
        @Path("id") itemId: Int,
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String
    ): List<Map<String, Any>>

    // NOVO: Endpoint de pesquisa robusta para NetworkPort confirmada com o environment GLPI do localhost
    @GET("apirest.php/search/NetworkPort?expand_dropdowns=true")
    suspend fun getNetworkPortsSearch(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Query("range") range: String,
        @Query("criteria[0][field]") field20: Int = 20, // Itemtype (na NetworkPort é 20, e não 12)
        @Query("criteria[0][searchtype]") type20: String = "equals",
        @Query("criteria[0][value]") value20: String,   // ex: NetworkEquipment
        @Query("criteria[1][link]") link1: String = "AND",
        @Query("criteria[1][field]") field21: Int = 21, // Items_id (na NetworkPort é 21, e não 13)
        @Query("criteria[1][searchtype]") type21: String = "equals",
        @Query("criteria[1][value]") value21: String,   // ex: 10
        @Query("forcedisplay[0]") f0: Int = 1,  // Nome da Porta local
        @Query("forcedisplay[1]") f1: Int = 3,  // Logical Number
        @Query("forcedisplay[2]") f2: Int = 4,  // MAC Address nativo (Field 4)
        @Query("forcedisplay[3]") f3: Int = 39, // LIGADO A (Field _virtual_connected_to ex: x616 > Jacinta Macedo)
        @Query("forcedisplay[4]") f4: Int = 126, // IP (Field da tab IPAddress.name)
        @Query("forcedisplay[5]") f5: Int = 2,   // ID base da porta (Crucial para fazer ligações recursivas!)
        @Query("sort") sort: Int = 3,           // Ordenar por Numero Logico da Porta
        @Query("order") order: String = "ASC"   // Ordem Ascendente
    ): Response<SearchResponse>

    // NOVO: Pesquisar na Pivot Table por ligações
    @GET("apirest.php/search/NetworkPort_NetworkPort?range=0-1")
    suspend fun getPortConnection(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Query("criteria[0][field]") searchField: Int, // Pass 3 (id_1) or 4 (id_2)
        @Query("criteria[0][searchtype]") type: String = "equals",
        @Query("criteria[0][value]") portId: String,
        @Query("forcedisplay[0]") f0: Int = 3, // id_1 
        @Query("forcedisplay[1]") f1: Int = 4  // id_2
    ): Response<SearchResponse>

    @GET("apirest.php/search/NetworkPort?range=0-1")
    suspend fun getRemotePortDetails(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Query("criteria[0][field]") field2: Int = 2,
        @Query("criteria[0][searchtype]") type: String = "equals",
        @Query("criteria[0][value]") remotePortId: String,
        @Query("forcedisplay[0]") f0: Int = 1,
        @Query("forcedisplay[1]") f1: Int = 2,
        @Query("forcedisplay[2]") f2: Int = 3,
        @Query("forcedisplay[3]") f3: Int = 4,
        @Query("forcedisplay[4]") f4: Int = 5,
        @Query("forcedisplay[5]") f5: Int = 6,
        @Query("forcedisplay[6]") f6: Int = 20,
        @Query("forcedisplay[7]") f7: Int = 21,
        @Query("forcedisplay[8]") f8: Int = 126
    ): Response<SearchResponse>

    // NOVO: Buscar o nome real de um dispositivo diretamente pelo seu endpoint estrito
    @GET("apirest.php/{itemtype}/{id}")
    suspend fun getDeviceById(
        @Path("itemtype") itemtype: String,
        @Path("id") id: String,
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String
    ): Map<String, Any>

    @DELETE("apirest.php/Ticket/{id}")
    suspend fun deleteTicket(
        @Path("id") id: String,
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Query("force_purge") forcePurge: Boolean = false
    ): Response<Unit>

    @PUT("apirest.php/Ticket/{id}")
    suspend fun updateTicket(
        @Path("id") id: String,
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Body input: @JvmSuppressWildcards Map<String, Any>
    ): Response<Unit>

    @GET("apirest.php/search/Ticket?forcedisplay[0]=2&forcedisplay[1]=1&forcedisplay[3]=15&forcedisplay[4]=12&forcedisplay[5]=21&forcedisplay[6]=17&forcedisplay[7]=16&forcedisplay[8]=4&forcedisplay[9]=5&forcedisplay[10]=22&forcedisplay[11]=8&forcedisplay[12]=14&forcedisplay[13]=70&forcedisplay[14]=71&forcedisplay[15]=6&forcedisplay[16]=7&forcedisplay[17]=3&forcedisplay[18]=66&expand_dropdowns=true")
    suspend fun getTicketsByItemSearch(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Query("criteria[0][field]") fieldId: Int = 13, // Items_id
        @Query("criteria[0][searchtype]") typeId: String = "equals",
        @Query("criteria[0][value]") valueId: Int,
        @Query("criteria[1][link]") linkType: String = "AND",
        @Query("criteria[1][field]") fieldType: Int = 14, // Itemtype
        @Query("criteria[1][searchtype]") typeType: String = "equals",
        @Query("criteria[1][value]") valueType: String,
        @Query("range") range: String = "0-100"
    ): retrofit2.Response<TicketListResponse>



    @GET("apirest.php/search/Ticket?forcedisplay[0]=2&forcedisplay[1]=1&forcedisplay[3]=15&forcedisplay[4]=12&forcedisplay[5]=21&forcedisplay[6]=17&forcedisplay[7]=16&forcedisplay[8]=4&forcedisplay[9]=5&forcedisplay[10]=22&forcedisplay[11]=8&forcedisplay[12]=14&forcedisplay[13]=70&forcedisplay[14]=71&forcedisplay[15]=6&forcedisplay[16]=7&forcedisplay[17]=3&forcedisplay[18]=66&expand_dropdowns=true")
    suspend fun getTodosTicketsAtivos(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Query("range") range: String = "0-300",
        @Query("criteria[0][field]") field: Int = 19,
        @Query("criteria[0][searchtype]") type: String = "morethan",
        @Query("criteria[0][value]") value: String = "2000-01-01",
        @Query("order") order: String = "DESC",
        @Query("sort") sort: Int = 19,
        @Query("is_deleted") isDeleted: Int = 0
    ): retrofit2.Response<TicketListResponse>
}

object GlpiRetrofit {
    private var retrofit: Retrofit? = null

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .addInterceptor { chain ->
            // 🔥 MODO OFFLINE: Se ativado, devolvemos uma resposta mockada sem bater na rede
            if (GlpiConfig.OFFLINE_MODE) {
                val request = chain.request()
                val url = request.url.toString()
                
                val mockResponse = when {
                    url.contains("initSession") -> "{\"session_token\":\"mock_token_offline\"}"
                    url.contains("getFullSession") -> "{\"session\":{\"glpiID\":11,\"glpi_id\":11,\"glpiactiveprofile\":{\"name\":\"Admin\"}}}"
                    url.contains("Location") -> "[{\"id\":1,\"name\":\"Edifício Principal\"},{\"id\":2,\"name\":\"Polo Tecnológico\"},{\"id\":3,\"name\":\"Sala de Reuniões\"}]"
                    url.contains("State") -> "[{\"id\":1,\"name\":\"Em uso\"},{\"id\":2,\"name\":\"Em stock\"},{\"id\":3,\"name\":\"Avariado\"}]"
                    url.contains("search/User") || url.contains("User") -> {
                        val names = listOf("Gonçalo Sousa", "Maria Santos", "Rui Costa", "Ana Ferreira", "João Silva", "Tiago Oliveira", "Catarina Martins", "Pedro Ribeiro", "Sofia Almeida", "Miguel Pereira", "Gonçalo Sousa")
                        val start = request.url.queryParameter("range")?.split("-")?.getOrNull(0)?.toIntOrNull() ?: 0
                        val end = request.url.queryParameter("range")?.split("-")?.getOrNull(1)?.toIntOrNull() ?: 9
                        val mockList = mutableListOf<String>()
                        for (i in 1..11) {
                            val parts = names[i-1].split(" ")
                            val email = if (i == 11) "goncalosp2408@gmail.com" else "${parts[0].lowercase()}.${parts[1].lowercase()}@empresa.pt"
                            mockList.add("""
                                {
                                    "2": $i,
                                    "1": "${parts[0].lowercase()}.${parts[1].lowercase()}",
                                    "9": "${parts[1]}",
                                    "34": "${parts[0]}",
                                    "5": "$email",
                                    "8": 1,
                                    "user_id": $i
                                }
                            """.trimIndent())
                        }
                        val subList = if (start < mockList.size) mockList.subList(start, minOf(end + 1, mockList.size)) else emptyList()
                        "{\"totalcount\": 10, \"count\": ${subList.size}, \"data\": [${subList.joinToString(",")}]}"
                    }
                    url.contains("search/Ticket") || url.contains("search/?") && request.url.queryParameter("criteria[0][field]") == null -> {
                        val rangeStr = request.url.queryParameter("range") ?: ""
                        if (rangeStr == "0-1") {
                            // É um pedido de contagem para o Dashboard
                            val count = when {
                                url.contains("field]=3") -> 2
                                url.contains("value]=5") -> 48
                                url.contains("value]=2") -> 14
                                url.contains("value]=1") -> 7
                                else -> 5
                            }
                            "{\"totalcount\": $count, \"count\": $count, \"data\": []}"
                        } else {
                            val titles = listOf("Rato não funciona", "PC não liga", "Acesso à rede bloqueado", "Impressora sem tinteiro", "Teclado com teclas soltas")
                            val authors = listOf("Ana Ferreira", "João Silva", "Tiago Oliveira", "Gonçalo Sousa", "Catarina Martins")
                            val techs = listOf("Maria Santos", "Rui Costa", "Miguel Pereira")
                            val mockList = mutableListOf<String>()
                            for (i in 1..5) {
                                val status = if (i % 3 == 0) 5 else if (i % 2 == 0) 2 else 1
                                val prioridade = if (i % 4 == 0) 5 else 3
                                val date = "2024-05-${10+i} 10:00:00"
                                val author = authors[(i-1)%5]
                                val tech = techs[(i-1)%3]
                                mockList.add("""
                                    {
                                        "1": "${titles[i-1]}",
                                        "2": ${1000 + i},
                                        "3": "$prioridade",
                                        "4": "Gonçalo Sousa",
                                        "5": "$tech",
                                        "7": "Descrição do problema relatado pelo utilizador.",
                                        "12": "$status",
                                        "15": "$date",
                                        "16": "$date",
                                        "17": "$date",
                                        "21": "$status",
                                        "22": "$author",
                                        "id": ${1000 + i}
                                    }
                                """.trimIndent())
                            }
                            "{\"totalcount\": 5, \"count\": 5, \"data\": [${mockList.joinToString(",")}]}"
                        }
                    }
                    url.contains("search") -> {
                        val itemtype = when {
                            url.contains("Computer") -> "Computer"
                            url.contains("Monitor") -> "Monitor"
                            url.contains("Printer") -> "Printer"
                            url.contains("NetworkEquipment") -> "NetworkEquipment"
                            url.contains("Peripheral") -> "Peripheral"
                            else -> "Device"
                        }
                        // Gerar apenas 3 dispositivos para a secção 'Meus Dispositivos'
                        val items = when (itemtype) {
                            "Computer" -> listOf("MacBook Pro M2")
                            "Monitor" -> listOf("Dell UltraSharp 27")
                            "Peripheral" -> listOf("Rato Logitech Master")
                            else -> emptyList()
                        }
                        
                        val mockList = mutableListOf<String>()
                        for (i in 1..items.size) {
                            val model = items[i-1]
                            mockList.add("""
                                {
                                    "1": "$model",
                                    "2": ${100 + i},
                                    "3": {"name": "Edifício Principal"},
                                    "5": "SN-${model.replace(" ", "").uppercase()}-$i",
                                    "70": "Gonçalo Sousa",
                                    "31": "Em uso"
                                }
                            """.trimIndent())
                        }
                        "{\"totalcount\": ${items.size}, \"count\": ${items.size}, \"data\": [${mockList.joinToString(",")}]}"
                    }
                    url.contains("Ticket_User") || url.contains("ITILFollowup") || url.contains("ITILSolution") || url.contains("Log") -> "[]"
                    url.contains("Ticket") -> "{\"totalcount\":0,\"data\":[],\"count\":0}"
                    else -> "[]"
                }

                return@addInterceptor okhttp3.Response.Builder()
                    .request(request)
                    .protocol(okhttp3.Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK (Offline Mode)")
                    .body(mockResponse.toResponseBody("application/json".toMediaTypeOrNull()))
                    .build()
            }

            val request = chain.request().newBuilder()
                .header("Accept", "application/json")
                .build()
            chain.proceed(request)
        }
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
        .build()

    val api: GlpiApiService
        get() {
            var currentBaseUrl = GlpiConfig.BASE_URL
            if (!currentBaseUrl.endsWith("/")) currentBaseUrl += "/"
            if (!currentBaseUrl.startsWith("http")) currentBaseUrl = "http://$currentBaseUrl"
            
            val r = retrofit
            if (r == null || r.baseUrl().toString() != currentBaseUrl) {
                try {
                    retrofit = Retrofit.Builder()
                        .baseUrl(currentBaseUrl)
                        .client(client)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()
                } catch (e: Exception) {
                    Log.e("RETROFIT_ERROR", "Erro ao inicializar Retrofit com $currentBaseUrl. Usando fallback.", e)
                    retrofit = Retrofit.Builder()
                        .baseUrl("http://127.0.0.1/")
                        .client(client)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()
                }
            }
            return retrofit!!.create(GlpiApiService::class.java)
        }
}
