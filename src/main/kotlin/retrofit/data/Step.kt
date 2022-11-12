package retrofit.data

import com.google.gson.annotations.SerializedName

data class Step(
    @SerializedName("distance")
    val distance: Double,
    @SerializedName("duration")
    val duration: Double,
    @SerializedName("instruction")
    val instruction: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("type")
    val type: Int,
    @SerializedName("way_points")
    val way_points: List<Int>
)