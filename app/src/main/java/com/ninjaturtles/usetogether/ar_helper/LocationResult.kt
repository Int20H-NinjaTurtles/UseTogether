package com.ninjaturtles.usetogether.ar_helper

import com.mapbox.geojson.Point

sealed class LocationResult {
    class Success(val point: Point): LocationResult()
    object Error: LocationResult()
}