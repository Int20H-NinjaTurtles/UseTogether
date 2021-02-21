package com.ninjaturtles.usetogether

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.fragment_find_path.*
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class FindTaxoBottomSheet : BottomSheetDialogFragment(), CoroutineScope {

    private lateinit var userLocation: LatLng
    private lateinit var taxoLocation: LatLng


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_find_path, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        userLocation = requireArguments().getParcelable("user")!!
        taxoLocation = requireArguments().getParcelable("taxo")!!
        updateDistance(userLocation, taxoLocation)
    }


    fun carUpdated(location: LatLng) {
        updateDistance(userLocation, location)
    }

    fun updateUser(location: LatLng) {
        updateDistance(location, taxoLocation)
    }

    private fun updateDistance(user: LatLng, taxo: LatLng) {
        val api = UseTogetherApp.instance.geoAPI
        launch {
            val _distance = withContext(Dispatchers.IO) {
                api.getDistance(Way(
                    listOf(user.latitude, user.longitude),
                    listOf(taxo.latitude, taxo.longitude)
                )).execute().body()
            }
            distance?.text = _distance.toString()
        }
    }


    companion object {
        fun newInstance(userLocation: LatLng, taxoLocation: LatLng): FindTaxoBottomSheet {
            return FindTaxoBottomSheet().apply {
                arguments = Bundle().apply {
                    putParcelable("user", userLocation)
                    putParcelable("taxo", taxoLocation)
                }
            }
        }
    }

    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main
}