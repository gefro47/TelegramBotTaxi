package clientlogic

import data.Location
import data.TelegramUser
import data.Trip
import dev.inmo.tgbotapi.extensions.api.answers.answer
import dev.inmo.tgbotapi.extensions.api.edit.edit
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onLocation
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onMessageDataCallbackQuery
import dev.inmo.tgbotapi.extensions.utils.*
import dev.inmo.tgbotapi.extensions.utils.types.buttons.InlineKeyboardBuilder
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.utils.regular
import dev.inmo.tgbotapi.utils.row
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.RequestBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit.RestApiService

suspend fun BehaviourContext.clientBot() {
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

    onCommand("getInfo"){
        send(
            chatId = it.chat.id,
            text = "LLLLLL",
            replyMarkup = inlineKeyboard {
                row {
                    dataButton("Decline", "decline")
                    dataButton("Accept", "accept")
                }
            }
        )

    }
    onMessageDataCallbackQuery {
        val answer = it.data

        when (answer){
            "decline" -> {
                edit(
                    it.message.withContent<TextContent>() ?: it.let {
                        answer(it, "Unsupported message type :(")
                        return@onMessageDataCallbackQuery
                    }
                ) {
                    regular("Wait please")
                }
                println("decline")
            }
            "accept" -> {
                edit(
                    it.message.withContent<TextContent>() ?: it.let {
                        answer(it, "Unsupported message type :(")
                        return@onMessageDataCallbackQuery
                    }
                ) {
                    regular("Wait please")
                }
                println("accept")
            }
        }

//        edit(
//            it.message.withContent<TextContent>() ?: it.let {
//                answer(it, "Unsupported message type :(")
//                return@onMessageDataCallbackQuery
//            }
//        ) {
//            regular("Wait please")
//        }
    }

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
            when (client.dialogState) {
                StateOfClient.StateFirstGeo -> {
                    send(
                        it.chat.id,
                        "Wait calculate"
                    )
                    RestApiService().getInfo("18.843019,42.285626","18.861267,42.284055") { response ->
//                        RestApiService().getInfo("${client.currentTrip!!.startLocation.len},${client.currentTrip!!.startLocation.lat}","${client.currentTrip!!.endLocation.len},${client.currentTrip!!.endLocation.lat}"){ response ->
                        if (response != null) {
                            if (response.features.isNotEmpty()) {
                                launch(Dispatchers.IO) {
                                    send(
                                        it.chat.id,
                                        response.features.first().properties.summary.distance.toString()
                                    )
                                }
                            } else {
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