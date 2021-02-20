package com.ninjaturtles.usetogether.ar_helper

data class PlateRecogniserResponse(
    val camera_id: Any,
    val filename: String,
    val processing_time: Double,
    val results: List<Result>,
    val timestamp: String,
    val version: Int
)