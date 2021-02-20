package com.ninjaturtles.usetogether

import android.app.Application
import com.google.gson.Gson
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class UseTogetherApp : Application() {

    private lateinit var retrofit: Retrofit
    lateinit var geoAPI: GeoAPI

    override fun onCreate() {
        super.onCreate()
        _instance = this
        retrofit = Retrofit.Builder()
            .baseUrl("http://161.35.218.3:8000/")
            .addConverterFactory(GsonConverterFactory.create(Gson()))
            .build()
        geoAPI = retrofit.create(GeoAPI::class.java)
    }


    companion object {
        private var _instance: UseTogetherApp? = null
        val instance: UseTogetherApp
            get() {
                if(_instance == null) {
                    _instance = UseTogetherApp()
                }
                return _instance!!
            }
    }
}