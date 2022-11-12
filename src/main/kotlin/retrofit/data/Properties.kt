package retrofit.data

import com.google.gson.annotations.SerializedName

data class Properties(
    @SerializedName("segments")
    val segments: List<Segment>,
    @SerializedName("summary")
    val summary: Summary,
    @SerializedName("way_points")
    val way_points: List<Int>
)