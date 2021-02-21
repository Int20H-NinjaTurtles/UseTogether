package com.ninjaturtles.usetogether

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface GeoAPI {

    @POST("build-route/")
    fun getRoute(@Body way: Way): Call<List<Point>>

    @POST("calc-distance-route")
    fun getDistance(@Body way: Way): Call<Int>
}

data class Way(
    val start: List<Double>,
    val destination: List<Double>
)

data class Point(
    val latitude: Double,
    val longitude: Double
)
