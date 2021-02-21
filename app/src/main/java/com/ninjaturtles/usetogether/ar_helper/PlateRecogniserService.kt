package com.ninjaturtles.usetogether.ar_helper

import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.POST

interface PlateRecogniserService {
    @POST("v1/plate-reader/")
    fun recognisePlate(
        @Field("upload") upload: String,
        @Field("mmc") mmc: Boolean = false
    ): Call<PlateRecogniserResponse>
}