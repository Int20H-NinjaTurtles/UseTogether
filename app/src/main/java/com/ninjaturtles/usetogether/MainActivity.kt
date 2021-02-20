package com.ninjaturtles.usetogether

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.navigation.findNavController
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity() {

    object DeepLink {
        val REQUEST_RIDE = "actions.intent.REQUEST_RIDE"

        object Params {
            val START_POINT = "start_point"
            val END_POINT = "end_point"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        intent?.handleIntent()
    }


    private fun handleDeepLink(data: Uri?) {
        when (data?.path) {
            DeepLink.REQUEST_RIDE -> {
                // Get the parameter defined as "exerciseType" and add it to the fragment arguments
                val startPoint = data.getQueryParameter(DeepLink.Params.START_POINT).orEmpty()
                val endPoint = data.getQueryParameter(DeepLink.Params.END_POINT).orEmpty()
            }
            else -> {
                showDefaultView()
            }
        }
    }

    private fun Intent.handleIntent() {
        when (action) {
            Intent.ACTION_VIEW -> handleDeepLink(data)
            else -> showDefaultView()
        }
    }

    private fun showDefaultView() {
        Toast.makeText(this, "Unsupported operation", Toast.LENGTH_LONG).show()
    }
}