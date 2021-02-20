package com.ninjaturtles.usetogether

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions


class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private var map: GoogleMap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
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
        val sydney = LatLng(-34.0, 151.0)
        map?.addMarker(
            MarkerOptions()
                .position(sydney)
                .title("Marker in Sydney")
        )
        map?.moveCamera(CameraUpdateFactory.newLatLng(sydney))
    }
}