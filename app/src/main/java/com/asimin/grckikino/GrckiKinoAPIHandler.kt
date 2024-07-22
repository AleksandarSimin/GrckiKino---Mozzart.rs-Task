package com.asimin.grckikino

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

object GrckiKinoAPIHandler {
    private const val BASE_URL = "https://api.opap.gr/"

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(logging)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val service: GrckiKinoAPIService = retrofit.create(GrckiKinoAPIService::class.java)
    val resultsService: GrckiKinoAPIService4Results = retrofit.create(GrckiKinoAPIService4Results::class.java)
}

interface GrckiKinoAPIService {
    @GET("draws/v3.0/1100/upcoming/20")
    suspend fun getUpcomingDraws(): List<Draw>
}

interface GrckiKinoAPIService4Results {
    @GET("draws/v3.0/1100/draw-date/{fromDate}/{toDate}")
    suspend fun getDrawResults(
        @Path("fromDate") fromDate: String,
        @Path("toDate") toDate: String
    ): DrawResultsResponse
}

data class DrawResultsResponse(
    val content: List<DrawResult>
)

data class DrawResult(
    val drawId: Int,
    val drawTime: Long,
    val winningNumbers: WinningNumbers
)

data class WinningNumbers(
    val list: List<Int>,
    val bonus: List<Int>
)
