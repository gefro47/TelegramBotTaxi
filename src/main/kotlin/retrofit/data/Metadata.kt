package retrofit.data

import com.google.gson.annotations.SerializedName

data class Metadata(
    @SerializedName("attribution")
    val attribution: String,
    @SerializedName("engine")
    val engine: Engine,
    @SerializedName("query")
    val query: Query,
    @SerializedName("service")
    val service: String,
    @SerializedName("timestamp")
    val timestamp: Long
)