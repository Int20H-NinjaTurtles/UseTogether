package com.ninjaturtles.usetogether

import android.app.Application
import com.google.gson.Gson
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class UseTogetherApp : Application() {

    lateinit var instance: UseTogetherApp
    private lateinit var retrofit: Retrofit

    override fun onCreate() {
        super.onCreate()
        instance = this
        retrofit = Retrofit.Builder()
            .baseUrl("http://161.35.218.3:8000/")
            .addConverterFactory(GsonConverterFactory.create(Gson()))
            .build()
    }
}