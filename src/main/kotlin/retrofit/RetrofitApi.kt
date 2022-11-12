package retrofit

import okhttp3.RequestBody
import retrofit.data.ResponseMaps
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path
import retrofit2.http.Query

interface RetrofitApi {
    @Headers("Content-Type: application/json")
    @GET("v2/directions/driving-car")
    fun getInfo(@Query("api_key") apikey: String, @Query("start") start: String, @Query("end") end: String,): Call<ResponseMaps>
}