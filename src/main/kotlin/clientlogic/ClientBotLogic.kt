package clientlogic

import data.Location
import data.TelegramUser
import data.Trip
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onLocation
import dev.inmo.tgbotapi.extensions.utils.*

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
            dialogState = StateOfClient.StateStart
        )

        send(
            it.chat.id,
            "Hello ${client.telegramData.first_name}, this is a taxi service, here you can order a taxi, send your location to get started."
        )

        println(client.toString())
    }

    onLocation {
        client = ClientUser(
            telegramData = TelegramUser(
                id = it.chat.id.chatId,
                username = it.chat.usernameChatOrNull()?.username?.username ?: "",
                first_name = it.chat.privateChatOrNull()?.firstName ?: "",
                last_name = it.chat.privateChatOrNull()?.lastName ?: ""
            ),
            dialogState = StateOfClient.StateStart
        )

        it.content.location.ifLiveLocation { location ->
            send(
                it.chat.id,
                "Nope, only static location!"
            )
        }

        it.content.location.ifStaticLocation { location ->
            when (client.dialogState){
                StateOfClient.StateFirstGeo -> {
                    send(
                        it.chat.id,
                        "Wait calculate"
                    )
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
