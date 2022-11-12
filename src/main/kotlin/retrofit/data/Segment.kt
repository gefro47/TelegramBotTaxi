package retrofit.data

import com.google.gson.annotations.SerializedName

data class Segment(
    @SerializedName("distance")
    val distance: Double,
    @SerializedName("duration")
    val duration: Double,
    @SerializedName("steps")
    val steps: List<Step>
)