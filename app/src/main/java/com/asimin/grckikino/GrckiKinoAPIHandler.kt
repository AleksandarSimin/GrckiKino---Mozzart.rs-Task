
package com.asimin.grckikino

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

object GrckiKinoAPIHandler {
    private const val BASE_URL = "https://api.opap.gr/"

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val service: GrckiKinoAPIService = retrofit.create(GrckiKinoAPIService::class.java)
}

interface GrckiKinoAPIService {
    @GET("draws/v3.0/1100/upcoming/20")
    suspend fun getUpcomingDraws(): List<Draw>
}
