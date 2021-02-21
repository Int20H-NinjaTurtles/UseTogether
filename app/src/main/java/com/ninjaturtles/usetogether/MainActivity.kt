package com.ninjaturtles.usetogether

import android.content.Intent
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.ninjaturtles.usetogether.ar_helper.ARActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext


class MainActivity : AppCompatActivity(), OnMapReadyCallback, BottomSheet.BottomSheetCallback,
    CoroutineScope {
    private val handler = Handler()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var lastLocation: LatLng? = null

    private var map: GoogleMap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)
        intent?.handleIntent()
        find.isVisible = true
        find.setOnClickListener {
            val intent = Intent(this@MainActivity, ARActivity::class.java)
            intent.putExtra("originLongitude", 30.49912f)
            intent.putExtra("originLatitude", 50.4716497f)
            intent.putExtra("plate", "КА7120ВА")
            startActivity(intent)
        }
    }

    private fun handleDeepLink(data: Uri?) {
        val category = data?.getQueryParameter("category") ?: ""
        val pickupName = data?.getQueryParameter("pickupLocationName")
        val dropoffName = data?.getQueryParameter("dropoffLocationName")
        val pickupAddress = data?.getQueryParameter("pickupLocationAddress") ?: ""
        val dropoffAddress = data?.getQueryParameter("dropoffLocationAddress") ?: ""
        val pickupLatitude = data?.getQueryParameter("pickupLocationGeolatitude")?.toDouble() ?: 0.0
        val dropoffLatitude =
            data?.getQueryParameter("dropoffLocationGeolatitude")?.toDouble() ?: 0.0
        val pickupLongtitude =
            data?.getQueryParameter("pickupLocationGeolongitude")?.toDouble() ?: 0.0
        val dropoffLongtitude =
            data?.getQueryParameter("dropoffLocationGeolongitude")?.toDouble() ?: 0.0
        startService(
            Intent(
                TaxoService.FINDING_ACTION,
                data,
                this,
                TaxoService::class.java
            )
        )

        handler.postDelayed({
            startService(
                Intent(
                    TaxoService.APPROVE_ACTION,
                    data,
                    this,
                    TaxoService::class.java
                )
            )
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location == null) return@addOnSuccessListener
                    lastLocation = LatLng(location.latitude, location.longitude)
                    BottomSheet.newInstance(
                        dropoffAddress,
                        lastLocation,
                        pickupAddress,
                        LatLng(dropoffLatitude, dropoffLongtitude),
                        category,
                        "Alexand Vodila",
                        150,
                        5f
                    ).show(supportFragmentManager, null)
                }
        }, 5000)


    }

    private fun Intent.handleIntent() {
        when (action) {
            Intent.ACTION_VIEW -> handleDeepLink(data)
            else -> {

            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap?) {
        map = googleMap
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location == null) return@addOnSuccessListener
                lastLocation = LatLng(location.latitude, location.longitude)
                map?.clear()
                map?.addMarker(
                    MarkerOptions()
                        .position(lastLocation!!)
                        .title("Marker in Sydney")
                )
                map?.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        lastLocation,
                        12f
                    )
                )
            }
        return
    }

    override fun onAccept(pickupPoint: LatLng?, dropoffPoint: LatLng?) {
        launch {
            val pointsList = withContext(Dispatchers.IO) {
                UseTogetherApp.instance.geoAPI.getRoute(
                    Way(
                        listOf(pickupPoint!!.latitude, pickupPoint.longitude),
                        listOf(dropoffPoint!!.latitude, dropoffPoint.longitude)
                    )
                ).execute()
            }
            startService(
                Intent(
                    TaxoService.ON_WAY_ACTION,
                    null,
                    this@MainActivity,
                    TaxoService::class.java
                )
            )
            val polyline = PolylineOptions().apply {
                add(pickupPoint)
                addAll(pointsList?.body()?.map {
                    LatLng(it.latitude, it.longitude)
                })
                add(dropoffPoint)
            }
            map?.addPolyline(polyline)
            startService(
                Intent(
                    TaxoService.ON_WAY_ACTION,
                    null,
                    this@MainActivity,
                    TaxoService::class.java
                )
            )
            val num = polyline.points.size
            for (i in 0 until num) {
                delay(50)
                map?.clear()
                map?.addMarker(MarkerOptions().position(lastLocation!!))
                map?.addPolyline(PolylineOptions().addAll(polyline.points.subList(0, polyline.points.size-i)))
                polyline.points.subList(0, polyline.points.size-i).lastOrNull()?.let {
//                    map?.addMarker(MarkerOptions()
//                        .position(it)
//                        .icon(BitmapDescriptorFactory.fromBitmap(R.drawable.ic_car))
//                    )
                }

            }
            startService(
                Intent(
                    TaxoService.ARRIVED_ACTION,
                    null,
                    this@MainActivity,
                    TaxoService::class.java
                )
            )
        }
    }

    override fun onDecline(
        pickupAdress: String,
        pickupPoint: LatLng?,
        dropoffAddress: String,
        dropoffPoint: LatLng?,
        category: String
    ) {
        find.isVisible = false
        startService(
            Intent(
                TaxoService.FINDING_ACTION,
                null,
                this,
                TaxoService::class.java
            )
        )

        handler.postDelayed({
            startService(
                Intent(
                    TaxoService.FINDING_ACTION,
                    null,
                    this,
                    TaxoService::class.java
                )
            )

            BottomSheet.newInstance(
                pickupAdress,
                pickupPoint,
                dropoffAddress,
                dropoffPoint,
                category,
                "Rusik Vodila",
                200,
                10f
            ).show(supportFragmentManager, null)
        }, 2000)
    }

    override fun onDestroy() {
        job.cancel()
        super.onDestroy()
    }


    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main
}