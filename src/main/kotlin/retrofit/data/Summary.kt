package retrofit.data

import com.google.gson.annotations.SerializedName

data class Summary(
    @SerializedName("distance")
    val distance: Double,
    @SerializedName("duration")
    val duration: Double
)