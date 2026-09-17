package com.example.data.remote

import com.example.data.local.DrowsinessEventEntity
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

@JsonClass(generateAdapter = true)
data class SheetPayload(
    val timestamp: Long,
    val driverName: String,
    val vehicleId: String,
    val state: String,
    val score: Int,
    val perclos: Float,
    val ear: Float,
    val mar: Float,
    val speedKmh: Float,
    val latitude: Double?,
    val longitude: Double?
)

@JsonClass(generateAdapter = true)
data class SheetSyncResponse(
    val status: String? = null,
    val message: String? = null
)

interface GoogleSheetsApi {
    @POST
    suspend fun logDrowsinessEvent(
        @Url webhookUrl: String,
        @Body payload: SheetPayload
    ): Response<SheetSyncResponse>
}

class GoogleSheetsSyncService {

    private val api: GoogleSheetsApi

    init {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://script.google.com/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()

        api = retrofit.create(GoogleSheetsApi::class.java)
    }

    suspend fun syncEvent(
        webhookUrl: String,
        driverName: String,
        vehicleId: String,
        event: DrowsinessEventEntity
    ): Boolean {
        if (webhookUrl.isBlank()) return false
        return try {
            val payload = SheetPayload(
                timestamp = event.timestamp,
                driverName = driverName,
                vehicleId = vehicleId,
                state = event.state,
                score = event.score,
                perclos = event.perclos,
                ear = event.ear,
                mar = event.mar,
                speedKmh = event.speedKmh,
                latitude = event.latitude,
                longitude = event.longitude
            )
            val response = api.logDrowsinessEvent(webhookUrl, payload)
            response.isSuccessful
        } catch (_: Exception) {
            false
        }
    }
}
