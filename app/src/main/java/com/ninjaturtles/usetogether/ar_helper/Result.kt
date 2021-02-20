package com.ninjaturtles.usetogether.ar_helper

data class Result(
    val box: Box,
    val candidates: List<Candidate>,
    val color: List<Color>,
    val dscore: Double,
    val model_make: List<ModelMake>,
    val orientation: List<Orientation>,
    val plate: String,
    val region: Region,
    val score: Double,
    val vehicle: Vehicle
)