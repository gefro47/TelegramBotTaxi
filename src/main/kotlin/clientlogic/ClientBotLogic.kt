package clientlogic

import data.Location
import data.TelegramUser
import data.Trip
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onLocation
import dev.inmo.tgbotapi.extensions.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.RequestBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit.RestApiService

suspend fun BehaviourContext.clientBot(){
    var client: ClientUser

    var trip: Trip

    onCommand("start") {
        client = ClientUser(
            telegramData = TelegramUser(
                id = it.chat.id.chatId,
                username = it.chat.usernameChatOrNull()?.username?.username ?: "",
                first_name = it.chat.privateChatOrNull()?.firstName ?: "",
                last_name = it.chat.privateChatOrNull()?.lastName ?: ""
            ),
            dialogState = StateOfClient.StateStart,
            null
        )

        send(
            it.chat.id,
            "Hello ${client.telegramData.first_name}, this is a taxi service, here you can order a taxi, send your location to get started."
        )

        println(client.toString())
    }

//    onCommand("getInfo"){
//        val api = RestApiService()
//        api.getInfo("8.681495,49.41461","8.687872,49.420318"){
//            if (it != null) {
//                println(it.toString())
//            } else {
//                println("Error registering new user")
//            }
//        }
//    }

    onLocation {
        client = ClientUser(
            telegramData = TelegramUser(
                id = it.chat.id.chatId,
                username = it.chat.usernameChatOrNull()?.username?.username ?: "",
                first_name = it.chat.privateChatOrNull()?.firstName ?: "",
                last_name = it.chat.privateChatOrNull()?.lastName ?: ""
            ),
            dialogState = StateOfClient.StateFirstGeo,
            currentTrip = Trip(
                startLocation = Location(
                    42.284067, 18.861253
                ),
                endLocation = Location(
                    42.285673, 18.842989
                )
            )
        )

        it.content.location.ifLiveLocation { location ->
            send(
                it.chat.id,
                "Nope, only static location!"
            )
        }

        it.content.location.ifStaticLocation { location ->

            println(location.latitude)
            when (client.dialogState){
                StateOfClient.StateFirstGeo -> {
                    send(
                        it.chat.id,
                        "Wait calculate"
                    )
                    RestApiService().getInfo("${client.currentTrip!!.startLocation.len},${client.currentTrip!!.startLocation.lat}","${client.currentTrip!!.endLocation.len},${client.currentTrip!!.endLocation.lat}"){ response ->
                        if (response != null) {
                            if (response.features.isNotEmpty()){
                                launch(Dispatchers.IO) {
                                    send(
                                        it.chat.id,
                                        response.features.first().properties.summary.distance.toString()
                                    )
                                }
                            }else{
                                launch(Dispatchers.IO) {
                                    send(
                                        it.chat.id,
                                        "Error"
                                    )
                                }
                            }
                            println(it.toString())
                        } else {
                            launch(Dispatchers.IO) {
                                send(
                                    it.chat.id,
                                    "Error"
                                )
                            }
                        }
                    }
                }
                StateOfClient.StateStart -> {
                    send(
                        it.chat.id,
                        "Send endpoint location"
                    )
                }
                else -> {

                }
            }
        }

        trip = Trip(
            startLocation = Location(
                lat = it.content.location.latitude,
                len = it.content.location.longitude
            ),
            endLocation = Location(
                lat = it.content.location.latitude,
                len = it.content.location.longitude
            )
        )
    }
}
