package com.ninjaturtles.usetogether

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions


class MainActivity : AppCompatActivity(), OnMapReadyCallback, BottomSheet.BottomSheetCallback {
    private val handler = Handler()
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var map: GoogleMap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)
        intent?.handleIntent()
    }

    private fun handleDeepLink(data: Uri?) {
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
        }, 2000)

        BottomSheet.newInstance(
            "A",
            "B",
            "Standart",
            "Alexand Vodila",
            150,
            5f
        ).show(supportFragmentManager, null)
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
                map?.clear()
                map?.addMarker(
                    MarkerOptions()
                        .position(LatLng(location.latitude, location.longitude))
                        .title("Marker in Sydney")
                )
                map?.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(
                            location.latitude,
                            location.longitude
                        ),
                        12f
                    )
                )
            }
        return
    }

    override fun onAccept() {
        startService(
            Intent(
                TaxoService.ON_WAY_ACTION,
                null,
                this,
                TaxoService::class.java
            )
        )
        val polyline = PolylineOptions().addAll(
            listOf(
                LatLng(12.0, 45.0),
                LatLng(13.0, 45.0),
                LatLng(14.0, 45.0),
                LatLng(15.0, 45.0)
            )
        )
        map?.addPolyline(polyline)
        startService(
            Intent(
                TaxoService.ON_WAY_ACTION,
                null,
                this,
                TaxoService::class.java
            )
        )
        handler.postDelayed({
            startService(
                Intent(
                    TaxoService.ARRIVED_ACTION,
                    null,
                    this,
                    TaxoService::class.java
                )
            )
        }, 4000)
    }

    override fun onDecline() {
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
                "A",
                "B",
                "Prime",
                "Rusik Vodila",
                200,
                10f
            ).show(supportFragmentManager, null)
        }, 2000)
    }
}