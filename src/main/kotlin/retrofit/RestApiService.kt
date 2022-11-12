package retrofit

import UrlMaps
import okhttp3.RequestBody
import retrofit.data.ResponseMaps
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RestApiService {
    fun getInfo(start: String, end: String, onResult: (ResponseMaps?) -> Unit){
        val retrofit = ServiceBuilder.buildService(RetrofitApi::class.java)
        retrofit.getInfo(apikey = UrlMaps.key, start, end).enqueue(
            object : Callback<ResponseMaps> {
                override fun onFailure(call: Call<ResponseMaps>, t: Throwable) {
                    onResult(null)
                }
                override fun onResponse(call: Call<ResponseMaps>, response: Response<ResponseMaps>) {
                    val addedUser = response.body()
                    onResult(addedUser)
                }
            }
        )
    }
}