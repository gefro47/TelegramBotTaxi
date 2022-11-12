package retrofit.data

import com.google.gson.annotations.SerializedName

data class Engine(
    @SerializedName("build_date")
    val build_date: String,
    @SerializedName("graph_date")
    val graph_date: String,
    @SerializedName("version")
    val version: String
)