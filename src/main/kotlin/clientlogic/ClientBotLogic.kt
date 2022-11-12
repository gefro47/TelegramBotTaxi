package clientlogic

import data.Location
import data.TelegramUser
import data.Trip
import database.Driver
import dev.inmo.tgbotapi.extensions.api.answers.answer
import dev.inmo.tgbotapi.extensions.api.edit.edit
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onContentMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onLocation
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onMessageDataCallbackQuery
import dev.inmo.tgbotapi.extensions.utils.*
import dev.inmo.tgbotapi.extensions.utils.types.buttons.InlineKeyboardBuilder
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.MessageContent
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.utils.regular
import dev.inmo.tgbotapi.utils.row
import driverlogic.DriverState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.RequestBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.jetbrains.exposed.sql.transactions.transaction
import retrofit.RestApiService

suspend fun BehaviourContext.clientBot() {

    onCommand("start") {
        val client = getClient(it.chat.id.chatId)
        if (client == null){
            addClient(
                it.chat.id.chatId,
                _dialogState = StateOfClient.StateStart
            )
            send(
                it.chat.id,
                "Hello ${it.chat.privateChatOrNull()?.firstName ?: ""}, this is a taxi service, here you can order a taxi, send your location to get started."
            )
        }else{
            when(client.dialogState){
                StateOfClient.StateStart -> {
                    send(
                        it.chat.id,
                        "Send your location for start."
                    )
                }
                StateOfClient.StateInTrip -> {
                    send(
                        it.chat.id,
                        "Wait when trip is end."
                    )
                }
                else -> {
                    send(
                        chatId = it.chat.id,
                        text = "Reset current process?",
                        replyMarkup = inlineKeyboard {
                            row {
                                dataButton("Decline", "reset_decline")
                                dataButton("Accept", "reset_accept")
                            }
                        }
                    )
                }
            }

        }
    }

//    onCommand("getInfo"){
//        val client = getClient(it.chat.id.chatId)
//        if (client != null){
//            transaction {
//                client.dialogState = StateOfClient.StateWaitCalc
//            }
//        }
//    }

    onMessageDataCallbackQuery {
        val answer = it.data
        val client = getClient(it.message.chat.id.chatId)

        if (client != null){
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
                "reset_accept" -> {
                    edit(
                        it.message.withContent<TextContent>() ?: it.let {
                            answer(it, "Unsupported message type :(")
                            return@onMessageDataCallbackQuery
                        }
                    ) {
                        regular("Send your location for start.")
                    }
                    resetClient(client)
                    println("accept")
                }
                "reset_decline" -> {
                    edit(
                        it.message.withContent<TextContent>() ?: it.let {
                            answer(it, "Unsupported message type :(")
                            return@onMessageDataCallbackQuery
                        }
                    ) {
                        regular("Ok, fine!")
                    }
                    println("decline")
                }
                "order_decline" -> {
                    edit(
                        it.message.withContent<TextContent>() ?: it.let {
                            answer(it, "Unsupported message type :(")
                            return@onMessageDataCallbackQuery
                        }
                    ) {
                        regular("Send your location for start.")
                    }
                    resetClient(client)
                    println("decline")
                }
                "order_accept" -> {
                    edit(
                        it.message.withContent<TextContent>() ?: it.let {
                            answer(it, "Unsupported message type :(")
                            return@onMessageDataCallbackQuery
                        }
                    ) {
                        regular("Wait your driver.")
                    }
                    transaction {
                        client.dialogState = StateOfClient.StateWaitDriver
                    }
                    println("accept")
                }
            }
        }else{

        }

    }

    onLocation {
        val client = getClient(it.chat.id.chatId)
        if (client != null) {
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
                        transaction{
                            client.dialogState = StateOfClient.StateSecondGeo
                            client.endLocationLat = it.content.location.latitude
                            client.endLocationLon = it.content.location.longitude
                        }

                        val clientUpdated = getClient(it.chat.id.chatId)
                        if (clientUpdated != null){
                            if (
                                clientUpdated.startLocationLon != null &&
                                clientUpdated.startLocationLat != null &&
                                clientUpdated.endLocationLat != null &&
                                clientUpdated.endLocationLon != null
                            ){
                                RestApiService().getInfo(
                                    "${clientUpdated.startLocationLon},${clientUpdated.startLocationLat}",
                                    "${clientUpdated.endLocationLon},${clientUpdated.endLocationLat}"
                                ) { response ->
                                    if (response != null) {
                                        if (response.features.isNotEmpty()) {
                                            launch(Dispatchers.IO) {
                                                send(
                                                    it.chat.id,
                                                    """
                                                        ⏱️ Driver :
                                                        💶 Trip amount: ${calculateTrip(response.features.first().properties.summary.distance)} €
                                                    """.trimIndent(),
                                                    replyMarkup = inlineKeyboard {
                                                        row {
                                                            dataButton("Decline", "order_decline")
                                                            dataButton("Accept", "order_accept")
                                                        }
                                                    }
                                                )
                                                transaction {
                                                    client.price = calculateTrip(response.features.first().properties.summary.distance)
                                                    client.distance = response.features.first().properties.summary.distance
                                                }
                                            }
                                        } else {
                                            launch(Dispatchers.IO) {
                                                resetClient(client)
                                                send(
                                                    it.chat.id,
                                                    "Oops, something went wrong! Start again."
                                                )
                                            }
                                        }
                                        println(it.toString())
                                    } else {
                                        launch(Dispatchers.IO) {
                                            resetClient(client)
                                            send(
                                                it.chat.id,
                                                "Oops, something went wrong! Start again."
                                            )
                                        }
                                    }
                                }
                            }else{
                                resetClient(client)
                                send(
                                    it.chat.id,
                                    "Oops, something went wrong! Start again."
                                )
                            }
                        }else{
                            addClient(
                                it.chat.id.chatId,
                                _dialogState = StateOfClient.StateStart
                            )
                            send(
                                it.chat.id,
                                "Hello ${it.chat.privateChatOrNull()?.firstName ?: ""}, this is a taxi service, here you can order a taxi, send your location to get started."
                            )
                        }

//                        send(
//                            it.chat.id,
//                            "Send endpoint location"
//                        )
//                        send(
//                            it.chat.id,
//                            "Wait calculate"
//                        )
                    }
                    StateOfClient.StateStart -> {
                        transaction{
                            client.dialogState = StateOfClient.StateFirstGeo
                            client.startLocationLat = it.content.location.latitude
                            client.startLocationLon = it.content.location.longitude
                        }
                        send(
                            it.chat.id,
                            "Send endpoint location"
                        )

                    }
                    else -> {
                        send(
                            it.chat.id,
                            "Please, over prev step."
                        )
                    }
                }
            }
        }else{
            addClient(
                it.chat.id.chatId,
                _dialogState = StateOfClient.StateStart
            )
            send(
                it.chat.id,
                "Hello ${it.chat.privateChatOrNull()?.firstName ?: ""}, this is a taxi service, here you can order a taxi, send your location to get started."
            )
        }

//        trip = Trip(
//            startLocation = Location(
//                lat = it.content.location.latitude,
//                len = it.content.location.longitude
//            ),
//            endLocation = Location(
//                lat = it.content.location.latitude,
//                len = it.content.location.longitude
//            )
//        )
    }
}

fun addClient(
    _clientId: Long,
    _dialogState: String,
    _startLocationLat: Double? = null,
    _startLocationLon: Double? = null,
    _endLocationLat: Double? = null,
    _endLocationLon: Double? = null,
    _distance: Double? = null,
    _price: Double? = null,
    _driverId: Long? = null
){
    transaction {
        Client.new {
            clientId = _clientId
            dialogState = _dialogState
            startLocationLat = _startLocationLat
            startLocationLon = _startLocationLon
            endLocationLat = _endLocationLat
            endLocationLon = _endLocationLon
            distance = _distance
            price = _price
            driverId = _driverId
        }
    }
}

fun getClient(
    chatId: Long
): Client? {
    val clients = transaction {
        return@transaction Client.find {
            Clients.clientId eq chatId
        }.toList()
    }
    if (clients.isEmpty()){
        return null
    }
    return clients.first()
}

fun resetClient(client: Client): Boolean{
    return try {
        transaction {
            client.dialogState = StateOfClient.StateStart
            client.startLocationLat = null
            client.startLocationLon = null
            client.endLocationLat = null
            client.endLocationLon = null
            client.driverId = null
            client.distance = null
            client.price = null
        }
        true
    }catch (ex: Exception){
        false
    }
}

fun calculateTrip(distance: Double): Double{
    val tariff = 0.5
    println(distance)
    return ((distance/1000)*tariff)+1
}