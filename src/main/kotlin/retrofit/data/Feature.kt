package retrofit.data

import com.google.gson.annotations.SerializedName

data class Feature(
    @SerializedName("bbox")
    val bbox: List<Double>,
    @SerializedName("geometry")
    val geometry: Geometry,
    @SerializedName("properties")
    val properties: Properties,
    @SerializedName("type")
    val type: String
)