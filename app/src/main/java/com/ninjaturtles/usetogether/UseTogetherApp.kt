package com.ninjaturtles.usetogether

import android.app.Application
import com.google.gson.Gson
import com.mapbox.vision.VisionManager
import com.ninjaturtles.usetogether.ar_helper.PlateRecogniserService
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class UseTogetherApp : Application() {

    private lateinit var retrofit: Retrofit
    private lateinit var plateRecogniserRetrofit: Retrofit
    lateinit var geoAPI: GeoAPI

    override fun onCreate() {
        super.onCreate()
        VisionManager.init(this, BuildConfig.MAPBOX_DOWNLOADS_TOKEN)
        _instance = this
        retrofit = Retrofit.Builder()
            .baseUrl("http://161.35.218.3:8000/")
            .addConverterFactory(GsonConverterFactory.create(Gson()))
            .build()
        val okHttpClient = OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.MINUTES)
            .connectTimeout(60, TimeUnit.MINUTES)
            .addInterceptor(HeaderInterceptor())
            .build()
        plateRecogniserRetrofit = Retrofit.Builder()
            .baseUrl("https://api.platerecognizer.com/")
            .addConverterFactory(GsonConverterFactory.create(Gson()))
            .client(okHttpClient)
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