package retrofit.data

import com.google.gson.annotations.SerializedName

data class ResponseMaps(
    @SerializedName("bbox")
    val bbox: List<Double>,
    @SerializedName("features")
    val features: List<Feature>,
    @SerializedName("metadata")
    val metadata: Metadata,
    @SerializedName("type")
    val type: String
)