package com.ninjaturtles.usetogether.ar_helper

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.mapbox.android.core.location.*
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.Point
import com.mapbox.geojson.utils.PolylineUtils
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigationOptions
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute
import com.mapbox.services.android.navigation.v5.offroute.OffRouteListener
import com.mapbox.services.android.navigation.v5.route.RouteFetcher
import com.mapbox.services.android.navigation.v5.route.RouteListener
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement
import com.mapbox.vision.VisionManager
import com.mapbox.vision.ar.LaneVisualParams
import com.mapbox.vision.ar.VisionArManager
import com.mapbox.vision.ar.core.models.ManeuverType
import com.mapbox.vision.ar.core.models.Route
import com.mapbox.vision.ar.core.models.RoutePoint
import com.mapbox.vision.mobile.core.interfaces.VisionEventsListener
import com.mapbox.vision.mobile.core.models.detection.Detection
import com.mapbox.vision.mobile.core.models.detection.DetectionClass
import com.mapbox.vision.mobile.core.models.detection.FrameDetections
import com.mapbox.vision.mobile.core.models.frame.Image
import com.mapbox.vision.mobile.core.models.position.GeoCoordinate
import com.mapbox.vision.utils.VisionLogger
import com.ninjaturtles.usetogether.BuildConfig
import com.ninjaturtles.usetogether.R
import com.ninjaturtles.usetogether.UseTogetherApp
import kotlinx.android.synthetic.main.fragment_a_r.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.nio.ByteBuffer


class ARActivity : AppCompatActivity(), ProgressChangeListener, OffRouteListener {
    private val locationEngine: LocationEngine by lazy {
        LocationEngineProvider.getBestLocationEngine(applicationContext)
    }
    private val plateRecogniserService: PlateRecogniserService =
        UseTogetherApp.plateRecogniserService
    private val taxiPoint = Point.fromLngLat(1.0, 2.0)
    private var needDetectCar = false
    private lateinit var mapboxNavigation: MapboxNavigation
    private lateinit var routeFetcher: RouteFetcher
    private lateinit var lastRouteProgress: RouteProgress
    private lateinit var directionsRoute: DirectionsRoute
    private val arLocationEngine by lazy {
        LocationEngineProvider.getBestLocationEngine(this)
    }
    private val arLocationEngineRequest by lazy {
        LocationEngineRequest.Builder(0)
            .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
            .setFastestInterval(1000)
            .build()
    }
    private val locationCallback by lazy {
        object : LocationEngineCallback<LocationEngineResult> {
            override fun onSuccess(result: LocationEngineResult?) {}

            override fun onFailure(exception: Exception) {}
        }
    }
    private val locationManager: LocationManager by lazy {
        getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }
    private lateinit var origin: Point
    private lateinit var destination: Point
    private lateinit var plate: String

    private val settingsClient: SettingsClient by lazy {
        LocationServices.getSettingsClient(requireContext())
    }

    private val locaitonRequest: LocationRequest by lazy {
        LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 10_000L
            fastestInterval = 2_000L
        }
    }

    private val locationSettingsRequest: LocationSettingsRequest by lazy {
        LocationSettingsRequest.Builder()
            .addLocationRequest(locaitonRequest)
            .build()
    }
    private lateinit var paint: Paint

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        origin = Point.fromLngLat(
            intent.getFloatExtra("originLongitude", 0.0f).toDouble(),
            intent.getFloatExtra("originLatitude", 0.0f).toDouble(),
        )
        destination = Point.fromLngLat(
            requireArguments().getFloat("destinationLongitude").toDouble(),
            requireArguments().getFloat("destinationLatitude").toDouble()
        )
        plate = requireArguments().getString("plate", "")
        preparePaint()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_a_r, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkLocationPermission()
        checkCameraPermission()
    }

    override fun onStop() {
        super.onStop()
        stopVisionManager()
        stopNavigation()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when(requestCode) {
            ACCESS_LOCATION_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults.first() == PackageManager.PERMISSION_GRANTED) {
                    turnOnGps()
                    startTrackLocation()
                } else {
                    showLocationPermissionDeniedDialog()
                }
            }
            CAMERA_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults.first() == PackageManager.PERMISSION_GRANTED) {
                    startVisionManager()
                    startNavigation()
                } else {
                    showCameraPermissionDeniedDialog()
                }
            }
        }
    }

    private fun startVisionManager() {
        VisionManager.create()
        VisionManager.start()
        VisionManager.visionEventsListener = object : VisionEventsListener {

            override fun onFrameDetectionsUpdated(frameDetections: FrameDetections) {
                super.onFrameDetectionsUpdated(frameDetections)
                if(needDetectCar) {
                    val imagePixels = byteArrayOf()
                    frameDetections.frame.image.copyPixels(imagePixels)
                    val encodedImage = Base64.encode(imagePixels, Base64.NO_WRAP)
                    val encodedString = String(encodedImage, Charsets.UTF_8)
                    plateRecogniserService.recognisePlate(encodedString)
                        .enqueue(
                            object : Callback<PlateRecogniserResponse> {
                                override fun onResponse(
                                    call: Call<PlateRecogniserResponse>,
                                    response: Response<PlateRecogniserResponse>
                                ) {
                                    val recognitionResult = response.body()
                                    recognitionResult?.results?.forEach {
                                        if (it.plate == plate) {
                                            val frameBitmap: Bitmap = convertImageToBitmap(
                                                frameDetections.frame.image
                                            )
                                            val canvas = Canvas(frameBitmap)
                                            for (detection in frameDetections.detections) {
                                                if (detection.detectionClass == DetectionClass.Car && detection.confidence > 0.6) {
                                                    drawSingleDetection(canvas, detection)
                                                }
                                            }
                                            requireActivity().runOnUiThread(
                                                Runnable {
                                                    detections_view.setImageBitmap(frameBitmap)
                                                }
                                            )
                                        }
                                    }
                                }

                                override fun onFailure(
                                    call: Call<PlateRecogniserResponse>,
                                    t: Throwable
                                ) {
                                }
                            }
                        )
                }
            }
        }

        VisionArManager.create(VisionManager)
        ar_view.setArManager(VisionArManager)
        ar_view.setFenceVisible(true)
        ar_view.setLaneVisualParams(LaneVisualParams())
    }

    private fun startTrackLocation() {
        val callback = object : LocationEngineCallback<LocationEngineResult> {
            override fun onSuccess(result: LocationEngineResult?) {
                result?.lastLocation?.let { location ->
                    val distanceToTaxi = TurfMeasurement.distance(
                        taxiPoint,
                        Point.fromLngLat(
                            location.longitude,
                            location.latitude
                        ),
                        TurfConstants.UNIT_METERS
                    )
                    needDetectCar = distanceToTaxi < CAR_RECOGNITION_AREA_RADIUS
                }
            }

            override fun onFailure(exception: Exception) {

            }
        }
        locationEngine.requestLocationUpdates(
            locationRequest,
            callback,
            Looper.getMainLooper()
        )
    }

    private fun stopVisionManager() {
        VisionArManager.destroy()
        VisionManager.stop()
        VisionManager.destroy()
    }

    private fun startNavigation() {
        mapboxNavigation = MapboxNavigation(
            requireContext(),
            BuildConfig.MAPBOX_DOWNLOADS_TOKEN,
            MapboxNavigationOptions.builder().build()
        )
        routeFetcher = RouteFetcher(requireContext(), BuildConfig.MAPBOX_DOWNLOADS_TOKEN)
        routeFetcher.addRouteListener(object : RouteListener {
            override fun onResponseReceived(
                response: DirectionsResponse?,
                routeProgress: RouteProgress?
            ) {
                mapboxNavigation.stopNavigation()
                if (response?.routes()?.isEmpty() == true) {
                    Toast.makeText(
                        requireContext(),
                        "Can not calculate the route requested",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    mapboxNavigation.startNavigation(response!!.routes()[0])
                    val route = response.routes()[0]

// Set route progress.
                    VisionArManager.setRoute(
                        Route(
                            route.getRoutePoints(),
                            route.duration()?.toFloat() ?: 0f,
                            "",
                            ""
                        )
                    )
                }
            }

            override fun onErrorReceived(throwable: Throwable?) {
                mapboxNavigation.stopNavigation()
            }

        })
        try {
            arLocationEngine.requestLocationUpdates(
                arLocationEngineRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (se: SecurityException) {
            VisionLogger.e("ARAssistant", se.toString())
        }

        initDirectionsRoute()

        mapboxNavigation.addOffRouteListener(this)
        mapboxNavigation.addProgressChangeListener(this)
    }

    private fun stopNavigation() {
        arLocationEngine.removeLocationUpdates(locationCallback)

        mapboxNavigation.removeProgressChangeListener(this)
        mapboxNavigation.removeOffRouteListener(this)
        mapboxNavigation.stopNavigation()
    }

    private fun initDirectionsRoute() {
        NavigationRoute.builder(requireContext())
            .accessToken(BuildConfig.MAPBOX_DOWNLOADS_TOKEN)
            .origin(origin)
            .destination(destination)
            .build()
            .getRoute(object : Callback<DirectionsResponse> {
                override fun onResponse(
                    call: Call<DirectionsResponse>,
                    response: Response<DirectionsResponse>
                ) {
                    if (response.body() == null || response.body()!!.routes().isEmpty()) {
                        return
                    }

                    directionsRoute = response.body()!!.routes()[0]
                    response.body()!!.waypoints()?.map { directionsWaypoint ->
                        directionsWaypoint.location()
                    }
                    mapboxNavigation.startNavigation(directionsRoute)

// Set route progress.
                    VisionArManager.setRoute(
                        Route(
                            directionsRoute.getRoutePoints(),
                            directionsRoute.duration()?.toFloat() ?: 0f,
                            "",
                            ""
                        )
                    )
                }

                override fun onFailure(call: Call<DirectionsResponse>, t: Throwable) {
                    t.printStackTrace()
                }
            })
    }

    private fun DirectionsRoute.getRoutePoints(): Array<RoutePoint> {
        val routePoints = arrayListOf<RoutePoint>()
        legs()?.forEach { leg ->
            leg.steps()?.forEach { step ->
                val maneuverPoint = RoutePoint(
                    GeoCoordinate(
                        latitude = step.maneuver().location().latitude(),
                        longitude = step.maneuver().location().longitude()
                    ),
                    step.maneuver().type().mapToManeuverType()
                )
                routePoints.add(maneuverPoint)

                step.geometry()
                    ?.buildStepPointsFromGeometry()
                    ?.map { geometryStep ->
                        RoutePoint(
                            GeoCoordinate(
                                latitude = geometryStep.latitude(),
                                longitude = geometryStep.longitude()
                            )
                        )
                    }
                    ?.let { stepPoints ->
                        routePoints.addAll(stepPoints)
                    }
            }
        }

        return routePoints.toTypedArray()
    }

    fun String.buildStepPointsFromGeometry(): List<Point> {
        return PolylineUtils.decode(this, Constants.PRECISION_6)
    }

    fun String?.mapToManeuverType(): ManeuverType = when (this) {
        "turn" -> ManeuverType.Turn
        "depart" -> ManeuverType.Depart
        "arrive" -> ManeuverType.Arrive
        "merge" -> ManeuverType.Merge
        "on ramp" -> ManeuverType.OnRamp
        "off ramp" -> ManeuverType.OffRamp
        "fork" -> ManeuverType.Fork
        "roundabout" -> ManeuverType.Roundabout
        "exit roundabout" -> ManeuverType.RoundaboutExit
        "end of road" -> ManeuverType.EndOfRoad
        "new name" -> ManeuverType.NewName
        "continue" -> ManeuverType.Continue
        "rotary" -> ManeuverType.Rotary
        "roundabout turn" -> ManeuverType.RoundaboutTurn
        "notification" -> ManeuverType.Notification
        "exit rotary" -> ManeuverType.RoundaboutExit
        else -> ManeuverType.None
    }

    override fun onProgressChange(location: Location?, routeProgress: RouteProgress) {
        lastRouteProgress = routeProgress
    }

    override fun userOffRoute(location: Location?) {
        routeFetcher.findRouteFromRouteProgress(location, lastRouteProgress)
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            turnOnGps()
            startTrackLocation()
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                ACCESS_LOCATION_PERMISSION
            )
        }
    }

    private fun checkCameraPermission() {
        if(ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED) {
                    startVisionManager()
                    startNavigation()
        } else {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION)
        }
    }

    private fun drawSingleDetection(canvas: Canvas, detection: Detection) {
// first thing we get coordinates of bounding box
        val relativeBbox = detection.boundingBox
        // we need to transform them from relative (range [0, 1]) to absolute in terms of canvas(frame) size
// we do not care about screen resolution at all - we will use cropCenter mode
        val absoluteBbox = RectF(
            relativeBbox.left * canvas.getWidth(),
            relativeBbox.top * canvas.getHeight(),
            relativeBbox.right * canvas.getWidth(),
            relativeBbox.bottom * canvas.getHeight()
        )
        // we want to draw circle bounds, we need radius and center for that
        val radius = Math.sqrt(
            Math.pow((absoluteBbox.centerX() - absoluteBbox.left).toDouble(), 2.0) +
                    Math.pow((absoluteBbox.centerY() - absoluteBbox.top).toDouble(), 2.0)
        ).toFloat()
        canvas.drawCircle(
            absoluteBbox.centerX(),
            absoluteBbox.centerY(),
            radius,
            paint
        )
    }



    private fun turnOnGps() {
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            settingsClient.checkLocationSettings(locationSettingsRequest)
                .addOnFailureListener { exception ->
                    when ((exception as ApiException).statusCode) {
                        LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> try {
                            (exception as ResolvableApiException).startResolutionForResult(
                                requireActivity(),
                                1
                            )
                        } catch (sie: IntentSender.SendIntentException) {
                            Log.i("map", "PendingIntent unable to execute request.")
                        }
                    }
                }
        }
    }

    private fun convertImageToBitmap(originalImage: Image): Bitmap {

        val bitmap = BitmapFactory.decodeResource(requireContext().getResources(), R.drawable.transparent_drawable)
        val buffer: ByteBuffer = ByteBuffer.allocateDirect(originalImage.sizeInBytes())
        originalImage.copyPixels(buffer)
        buffer.rewind()
        bitmap.copyPixelsFromBuffer(buffer)
        return bitmap
    }

    private fun preparePaint() {
        paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.GREEN
        paint.strokeWidth = 5f
        paint.style = Paint.Style.STROKE
    }

    private fun showLocationPermissionDeniedDialog() {
        AlertDialog.Builder(requireContext())
            .setMessage(resources.getString(R.string.location_permission_denied))
            .setCancelable(true)
            .show()
    }

    private fun showCameraPermissionDeniedDialog() {
        AlertDialog.Builder(requireContext())
            .setMessage(R.string.camera_permission_denied)
            .setCancelable(true)
            .show()
    }

    companion object {
        const val CAR_RECOGNITION_AREA_RADIUS = 30.0
        private const val ACCESS_LOCATION_PERMISSION = 1
        private const val CAMERA_PERMISSION = 2
    }
}