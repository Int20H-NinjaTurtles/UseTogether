package com.ninjaturtles.usetogether

import android.app.Application
import com.google.gson.Gson
import com.ninjaturtles.usetogether.ar_helper.PlateRecogniserService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class UseTogetherApp : Application() {

    lateinit var instance: UseTogetherApp

    override fun onCreate() {
        super.onCreate()
        instance = this
        retrofit = Retrofit.Builder()
            .baseUrl("http://161.35.218.3:8000/")
            .addConverterFactory(GsonConverterFactory.create(Gson()))
            .build()
        plateRecogniserRetrofit = Retrofit.Builder()
            .baseUrl("https://api.platerecognizer.com/")
            .addConverterFactory(GsonConverterFactory.create(Gson()))
            .build()
        plateRecogniserService = plateRecogniserRetrofit.create(PlateRecogniserService::class.java)
    }

    companion object {
        lateinit var retrofit: Retrofit
        lateinit var plateRecogniserRetrofit: Retrofit
        lateinit var plateRecogniserService: PlateRecogniserService
    }
}