package com.example.remindersapp

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

// DTO del contrato remoto (Usamos el esquema estándar de JSONPlaceholder /todos para pruebas reales)
data class ReminderDto(
    val id: Int?,
    val userId: Int = 1,
    val title: String,
    val completed: Boolean
)

interface ReminderApiService {
    @GET("todos?_limit=5")
    suspend fun fetchRemoteReminders(): Response<List<ReminderDto>>

    @POST("todos")
    suspend fun createRemoteReminder(@Body dto: ReminderDto): Response<ReminderDto>

    @PUT("todos/{id}")
    suspend fun updateRemoteReminder(@Path("id") id: Int, @Body dto: ReminderDto): Response<ReminderDto>
}

object RetrofitClient {
    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    val apiService: ReminderApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ReminderApiService::class.java)
    }
}
