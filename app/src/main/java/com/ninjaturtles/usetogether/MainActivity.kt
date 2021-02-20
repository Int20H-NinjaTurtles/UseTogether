package com.ninjaturtles.usetogether

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.navigation.findNavController
import com.google.android.material.navigation.NavigationView
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        intent?.handleIntent()
    }


    private fun handleDeepLink(data: Uri?) {
        info.text = data.toString()
//        val startPoint = data?.getQueryParameter(DeepLink.Params.START_POINT).orEmpty()
//        val endPoint = data?.getQueryParameter(DeepLink.Params.END_POINT).orEmpty()
    }

    private fun Intent.handleIntent() {
        when (action) {
            Intent.ACTION_VIEW -> handleDeepLink(data)
            else -> {

            }
        }
    }
}