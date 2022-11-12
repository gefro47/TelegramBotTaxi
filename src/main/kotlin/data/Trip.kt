package data

import java.util.PrimitiveIterator

data class Trip(
    val clientId: Long,
    val startLocation: Location,
    val endLocation: Location,
    val distance: Double = 0.0,
    val price: Double = 0.0
)
