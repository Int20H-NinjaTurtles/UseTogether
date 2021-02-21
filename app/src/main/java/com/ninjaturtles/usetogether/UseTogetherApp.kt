package com.ninjaturtles.usetogether

import android.app.Application
import com.google.gson.Gson
import com.ninjaturtles.usetogether.ar_helper.PlateRecogniserService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class UseTogetherApp : Application() {

    private lateinit var retrofit: Retrofit
    private lateinit var plateRecogniserRetrofit: Retrofit
    lateinit var geoAPI: GeoAPI

    override fun onCreate() {
        super.onCreate()
        _instance = this
        retrofit = Retrofit.Builder()
            .baseUrl("http://161.35.218.3:8000/")
            .addConverterFactory(GsonConverterFactory.create(Gson()))
            .build()
        plateRecogniserRetrofit = Retrofit.Builder()
            .baseUrl("https://api.platerecognizer.com/")
            .addConverterFactory(GsonConverterFactory.create(Gson()))
            .build()
        geoAPI = retrofit.create(GeoAPI::class.java)
        plateRecogniserService = plateRecogniserRetrofit.create(PlateRecogniserService::class.java)
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
        lateinit var plateRecogniserService: PlateRecogniserService
    }
}