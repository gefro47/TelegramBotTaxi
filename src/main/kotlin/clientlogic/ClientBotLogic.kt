package clientlogic

import data.Location
import data.TelegramUser
import data.Trip
import database.*
import dev.inmo.tgbotapi.extensions.api.answers.answer
import dev.inmo.tgbotapi.extensions.api.edit.edit
import dev.inmo.tgbotapi.extensions.api.edit.location.live.editLiveLocation
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.api.send.sendLiveLocation
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onContentMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onLocation
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onMessageDataCallbackQuery
import dev.inmo.tgbotapi.extensions.utils.*
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.text
import dev.inmo.tgbotapi.extensions.utils.types.buttons.InlineKeyboardBuilder
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.location.LiveLocation
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.LiveLocationContent
import dev.inmo.tgbotapi.types.message.content.MessageContent
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.types.message.textsources.regular
import dev.inmo.tgbotapi.utils.RiskFeature
import dev.inmo.tgbotapi.utils.regular
import dev.inmo.tgbotapi.utils.row
import driverlogic.DriverBot
import driverlogic.DriverState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.RequestBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import retrofit.RestApiService
import java.util.UUID

class ClientBotLogic {
    lateinit var context: BehaviourContext
    lateinit var driverBot: DriverBot

    suspend fun sendDriver(orderUUID: UUID){
        val order = getOrder(orderUUID)
        if (order != null){
            val client = getClient(order.clientChatId)
            val driver = getDriver(order.driverChatId!!)
            if (client != null) {
                context.edit(
                    chatId = ChatId(order.clientChatId),
                    messageId = order.clientMessageId,
                    text = """
                            💶 The cost of travel: ${client.price!!} €
                        """.trimIndent()
                )
                val locationMessageId = context.sendLiveLocation(
                    chatId = ChatId(order.clientChatId),
                    location = LiveLocation(driver!!.currentLocationLat!!, driver.currentLocationLon!!,null, 1200),
                    1200
                ).messageId
                transaction {
                    order.locationMessageId = locationMessageId
                }
            }
        }
    }

    suspend fun editLocationMessage(locationMessageId: Long, clientChatId: Long, lat: Double, lon:Double){
        context.editLiveLocation(
            chatId = ChatId(clientChatId),
            messageId = locationMessageId,
            location = LiveLocation(lat, lon,null, 1200)
        )
    }

    @OptIn(RiskFeature::class)
    suspend fun clientBot() {

        context.onCommand("start") {
            val client = getClient(it.chat.id.chatId)
            if (client == null) {
                addClient(
                    it.chat.id.chatId,
                    _dialogState = StateOfClient.StateStart
                )
                send(
                    it.chat.id,
                    "Hello ${it.chat.privateChatOrNull()?.firstName ?: ""}, this is a taxi service, here you can order a taxi, send your location to get started."
                )
            } else {
                when (client.dialogState) {
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
                                    dataButton("Decline", "decline")
                                    dataButton("Accept", "accept")
                                }
                            }
                        )
                    }
                }

            }
        }

//    context.onCommand("getInfo"){
//        val client = getClient(it.chat.id.chatId)
//        if (client != null){
//            transaction {
//                client.dialogState = StateOfClient.StateWaitCalc
//            }
//        }
//    }

        context.onMessageDataCallbackQuery {
            val answer = it.data
            val client = getClient(it.message.chat.id.chatId)

            if (client != null) {
                when (answer) {
                    "decline" -> {
                        when (client.dialogState) {
                            StateOfClient.StateSecondGeo -> {
                                println(it.message.text)
                                edit(
                                    message = it.message.withContent<TextContent>()!!,
                                    text = "Declined."
                                )
//                                edit(
//                                    it.message.withContent<TextContent>() ?: it.let {
//                                        answer(it, "Unsupported message type :(")
//                                        return@onMessageDataCallbackQuery
//                                    }
//                                ) {
//                                    regular("Declined.")
//                                }
                                send(
                                    it.message.chat,
                                    "Send your location for start."
                                )
                                resetClient(client)
                            }
                            else -> {
                                edit(
                                    message = it.message.withContent<TextContent>()!!,
                                    text = "Ok, fine!"
                                )
//                                edit(
//                                    it.message.withContent<TextContent>() ?: it.let {
//                                        answer(it, "Unsupported message type :(")
//                                        return@onMessageDataCallbackQuery
//                                    }
//                                ) {
//                                    regular("Ok, fine!")
//                                }

                            }
                        }
                        println("decline")
                    }
                    "accept" -> {
                        when (client.dialogState) {
                            StateOfClient.StateSecondGeo -> {
                                edit(
                                    message = it.message.withContent<TextContent>()!!,
                                    text = "Accepted"
                                )
//                                edit(
//                                    it.message.withContent<TextContent>() ?: it.let {
//                                        answer(it, "Unsupported message type :(")
//                                        return@onMessageDataCallbackQuery
//                                    }
//                                ) {
//                                    regular(
//                                        "Accepted"
//                                    )
//                                }
                                val messageId: Long = send(
                                    it.message.chat,
                                    it.message.text +
                                            "\nSearching your driver...",
                                    replyMarkup = inlineKeyboard {
                                        row {
                                            dataButton("Cancel", "cancel")
                                        }
                                    }
                                ).messageId
                                driverBot.findDriver(client.clientId, messageId)
                                transaction {
                                    client.dialogState = StateOfClient.StateWaitDriver
                                }
                            }
                            else -> {
                                edit(
                                    message = it.message.withContent<TextContent>()!!,
                                    text = "Send your location for start."
                                )
//                                edit(
//                                    it.message.withContent<TextContent>() ?: it.let {
//                                        answer(it, "Unsupported message type :(")
//                                        return@onMessageDataCallbackQuery
//                                    }
//                                ) {
//                                    regular("Send your location for start.")
//                                }
                                resetClient(client)
                            }
                        }
                        println("accept")
                    }
                    "cancel" -> {
                        val chatId = it.message.chat
                        val order = getOrderByMessageId(it.message.messageId)
                        if (order != null){
                            resetClient(client)
                            transaction {
                                order.orderState = OrderState.CANCELED
                            }
                            edit(
                                message = it.message.withContent<TextContent>()!!,
                                text = "Your order canceled."
                            )
//                            edit(
//                                it.message.withContent<TextContent>() ?: it.let {
//                                    answer(it, "Unsupported message type :(")
//                                    return@onMessageDataCallbackQuery
//                                }
//                            ) {
//                                regular("Your order canceled.")
//                            }
                            send(
                                chatId,
                                "Send your location for start."
                            )
                        }else{
                            edit(
                                message = it.message.withContent<TextContent>()!!,
                                text = "Oops, something went wrong! Start again."
                            )
                            resetClient(client)
//                            send(
//                                chatId,
//                                "Oops, something went wrong! Start again."
//                            )
                        }
                    }
                }
            } else {

            }

        }

        context.onLocation {
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
                            transaction {
                                client.dialogState = StateOfClient.StateSecondGeo
                                client.endLocationLat = it.content.location.latitude
                                client.endLocationLon = it.content.location.longitude
                            }

                            val clientUpdated = getClient(it.chat.id.chatId)
                            if (clientUpdated != null) {
                                if (
                                    clientUpdated.startLocationLon != null &&
                                    clientUpdated.startLocationLat != null &&
                                    clientUpdated.endLocationLat != null &&
                                    clientUpdated.endLocationLon != null
                                ) {
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
                                                        💶 The cost of travel: ${calculateTrip(response.features.first().properties.summary.distance)} €
                                                    """.trimIndent(),
                                                        replyMarkup = inlineKeyboard {
                                                            row {
                                                                dataButton("Decline", "decline")
                                                                dataButton("Accept", "accept")
                                                            }
                                                        }
                                                    )
                                                    transaction {
                                                        client.price =
                                                            calculateTrip(response.features.first().properties.summary.distance)
                                                        client.distance =
                                                            response.features.first().properties.summary.distance
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
                                } else {
                                    resetClient(client)
                                    send(
                                        it.chat.id,
                                        "Oops, something went wrong! Start again."
                                    )
                                }
                            } else {
                                addClient(
                                    it.chat.id.chatId,
                                    _dialogState = StateOfClient.StateStart
                                )
                                send(
                                    it.chat.id,
                                    "Hello ${it.chat.privateChatOrNull()?.firstName ?: ""}, this is a taxi service, here you can order a taxi, send your location to get started."
                                )
                            }
                        }
                        StateOfClient.StateStart -> {
                            transaction {
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
            } else {
                addClient(
                    it.chat.id.chatId,
                    _dialogState = StateOfClient.StateStart
                )
                send(
                    it.chat.id,
                    "Hello ${it.chat.privateChatOrNull()?.firstName ?: ""}, this is a taxi service, here you can order a taxi, send your location to get started."
                )
            }
        }
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
) {
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
    if (clients.isEmpty()) {
        return null
    }
    return clients.first()
}

fun resetClient(client: Client): Boolean {
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
    } catch (ex: Exception) {
        false
    }
}

fun calculateTrip(distance: Double): Double {
    val tariff = 0.5
    println(distance)
    return ((distance / 1000) * tariff) + 1
}

fun getOrderByMessageId(messageId: Long): Order?{
    return transaction {
        return@transaction Order.find(Orders.clientMessageId eq messageId).firstOrNull()
    }
}