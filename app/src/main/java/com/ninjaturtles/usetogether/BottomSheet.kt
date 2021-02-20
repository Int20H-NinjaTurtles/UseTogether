package com.ninjaturtles.usetogether

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.fragment_request_ride.*

class BottomSheet : BottomSheetDialogFragment() {

    private var bottomSheetCallback: BottomSheetCallback? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        bottomSheetCallback =
            if (parentFragment is BottomSheetCallback) parentFragment as BottomSheetCallback
            else if (context is BottomSheetCallback) context
            else null
    }


    interface BottomSheetCallback {
        fun onAccept(pickupPoint: LatLng?, dropoffPoint: LatLng?)
        fun onDecline(
            pickupAdress: String,
            pickupPoint: LatLng?,
            dropoffAddress: String,
            dropoffPoint: LatLng?,
            category: String,
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_request_ride, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpViews()
    }

    private fun setUpViews() {
        pickupPoint.text = arguments?.getString("from")
        dropoffPoint.text = arguments?.getString("to")
        driver.text = arguments?.getString("driver")
        price.text = arguments?.getInt("price").toString()
        pickupPoint.text = arguments?.getString("from")
        decline.setOnClickListener {
            bottomSheetCallback?.onDecline(
                arguments?.getString("from") ?: "",
                arguments?.getParcelable("from_point"),
                arguments?.getString("to") ?: "",
                arguments?.getParcelable("to_point"),
                arguments?.getString("category") ?: ""
            )
            dismissAllowingStateLoss()
        }

        accept.setOnClickListener {
            bottomSheetCallback?.onAccept(
                arguments?.getParcelable("from_point"),
                arguments?.getParcelable("to_point")
            )
            dismissAllowingStateLoss()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(
            pickupAdress: String,
            pickupPoint: LatLng?,
            dropoffAddress: String,
            dropoffPoint: LatLng?,
            category: String,
            driver: String,
            price: Int,
            estimate: Float,
            ): BottomSheet {
            val fragment = BottomSheet()
            fragment.arguments = Bundle().apply {
                putString("from", pickupAdress)
                putString("to", dropoffAddress)
                putString("category", category)
                putString("driver", driver)
                putInt("price", price)
                putFloat("estimate", estimate)
                putParcelable("from_point", pickupPoint)
                putParcelable("to_point", dropoffPoint)
            }
            return fragment
        }
    }
}