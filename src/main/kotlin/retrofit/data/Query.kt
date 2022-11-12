package retrofit.data

import com.google.gson.annotations.SerializedName

data class Query(
    @SerializedName("coordinates")
    val coordinates: List<List<Double>>,
    @SerializedName("format")
    val format: String,
    @SerializedName("profile")
    val profile: String
)