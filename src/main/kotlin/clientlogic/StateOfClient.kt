package clientlogic

class StateOfClient (){
    companion object{
        const val StateStart = "100"
        const val StateFirstGeo = "200"
        const val StateSecondGeo = "300"
        const val StateWaitCalc = "400"
        const val StateFindDriver = "500"
        const val StateWaitDriver = "600"
        const val StateInTrip = "700"
        const val StateFinishedTrip = "800"
    }
}