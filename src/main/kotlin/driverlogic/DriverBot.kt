package driverlogic

import clientlogic.Client
import clientlogic.ClientBotLogic
import clientlogic.Clients
import clientlogic.getClient
import com.soywiz.klock.DateTime
import database.*
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.edit.edit
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.*
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.text
import dev.inmo.tgbotapi.extensions.utils.privateChatOrNull
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.extensions.utils.withContent
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.chat.PrivateChat
import dev.inmo.tgbotapi.types.location.StaticLocation
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.LocationContent
import dev.inmo.tgbotapi.types.message.content.MessageContent
import dev.inmo.tgbotapi.types.message.content.StaticLocationContent
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.types.queries.callback.MessageDataCallbackQuery
import dev.inmo.tgbotapi.utils.row
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class DriverBot {
    lateinit var context: BehaviourContext
    lateinit var clientBot: ClientBotLogic

    suspend fun driverBot() {
        println(context.getMe())

        context.onCommand("start") {
            val privateChat = checkPrivateChat(this, it as CommonMessage<MessageContent>) ?: return@onCommand
            val driver = getOrCreateDriver(it.chat.id.chatId, this, it as CommonMessage<MessageContent>) ?: return@onCommand

            if (driver.state !in listOf(DriverState.STARTED, DriverState.WAIT_FOR_ORDER)) {
                reply(it, "Unexpected state: ${driver.state}")
                return@onCommand
            }
            transaction {
                driver.state = DriverState.STARTED
                driver.currentLocationLat = null
                driver.currentLocationLon = null
                driver.lastLocationUpdate = null
            }

            reply(it, "Hello, ${privateChat.firstName} ${privateChat.lastName}! " +
                    "To start as a taxi driver, please, send me your live location. " +
                    "If you'd like to stop waiting for passengers, just stop sharing your location.")
        }

        context.onStaticLocation {
            getOrCreateDriver(it.chat.id.chatId, this, it as CommonMessage<MessageContent>) ?: return@onStaticLocation
            reply(it, "Please, use Live Location sharing!")
        }

        context.onLiveLocation {
            val driver = getOrCreateDriver(it.chat.id.chatId, this, it as CommonMessage<MessageContent>) ?: return@onLiveLocation
            if (driver.state !in listOf(DriverState.WAIT_FOR_ORDER, DriverState.GOING_TO_PASSENGER, DriverState.ORDER_IN_PROGRESS)) {
                transaction {
                    driver.state = DriverState.WAIT_FOR_ORDER
                }
            }
            transaction {
                driver.currentLocationLat = it.content.location.latitude
                driver.currentLocationLon = it.content.location.longitude
                driver.lastLocationUpdate = it.date.unixMillis
            }
            reply(it, "Thank you for sharing your location! We're looking for the passengers for you. " +
                    "If you'd like to stop waiting for passengers, just stop sharing your location.")
        }

        context.onEditedLocation {
            val driver = getOrCreateDriver(it.chat.id.chatId, this, it as CommonMessage<MessageContent>) ?: return@onEditedLocation
            val wasStarted = driver.state == DriverState.STARTED
            if (driver.state !in listOf(DriverState.WAIT_FOR_ORDER, DriverState.GOING_TO_PASSENGER, DriverState.ORDER_IN_PROGRESS)) {
                transaction {
                    driver.state = DriverState.WAIT_FOR_ORDER
                }
            }
            transaction {
                driver.currentLocationLat = it.content.location.latitude
                driver.currentLocationLon = it.content.location.longitude
                driver.lastLocationUpdate = it.editDate!!.unixMillis
            }

            if (it.content is StaticLocationContent) {
                // Водитель перестал шарить локацию
                context.driverStoppedShareLocation(driver, it)
                return@onEditedLocation
            }

            if (wasStarted) {
                reply(it, "We're receiving your location again, thank you!")
            }
        }

        context.onMessageDataCallbackQuery {
            val values = it.data.split(" ")
            val orderUuid = UUID.fromString(values[0])
            val order = getOrder(orderUuid)
            if (order == null || order.orderState != OrderState.SEARCHING_DRIVER) {
                edit(
                    message = it.message.withContent<TextContent>()!!,
                    text = it.message.text!! + "\n\nUnfortunately, another driver has accepted this request earlier :( " +
                            "We will find another order for you."
                )
                return@onMessageDataCallbackQuery
            }

            if (values[1] == "accept") {
                driverAcceptsOrder(context, order, it)
            } else {
                driverDeclinesOrder(context, order, it)
            }
        }

//        context.onContentMessage {
//            clientBot.testFun()
//    //        getOrCreateDriver(it.chat.id.chatId, this, it) ?: return@onContentMessage
//    //        println(it.content.toString())
//    //        if (it.content.toString() == "/start") {
//    //            return@onContentMessage
//    //        }
//    //        reply(it, "Thank you!")
//        }
    }

    suspend fun findDriver(clientId: Long) {
        val client = getClient(clientId) ?: return
        if (client.startLocationLon == null || client.startLocationLat == null
                || client.endLocationLon == null || client.endLocationLat == null
                || client.distance == null || client.price == null) {
            println("Something in client $client is null!!!")
            return
        }
        val orderUuid = UUID.randomUUID()

        val drivers = getAvailableDrivers()
        val lastLocationUpdate = DateTime.now().unixMillis - 2 * 60 * 1000  // Две минуты назад.
        val driverChatIds = arrayListOf<Long>()
        for (driver in drivers) {
            if (driver.lastLocationUpdate!! < lastLocationUpdate) {    // локация не обновлялась 2 минуты
                println(driver.lastLocationUpdate!!)
                println(DateTime.now().unixMillis - 2 * 60 * 1000)
                context.driverStoppedShareLocation(driver)
                continue
            }
            if (true) {   // todo: driver near passenger: 10 km
                driverChatIds.add(driver.chatId)
            }
        }
        if (driverChatIds.isEmpty()) {
//            clientBot.driverNotFound()  todo
        }
        addOrder(orderUuid, clientId, driverChatIds.size)
        context.sendOrderToDrivers(driverChatIds, client, orderUuid)
    }

    private suspend fun driverAcceptsOrder(context: BehaviourContext, order: Order, it: MessageDataCallbackQuery) {
        val driverChatId = it.message.chat.id.chatId
        val client = transaction {
            order.orderState = OrderState.IN_PROGRESS
            order.driverChatId = driverChatId

            val driver = Driver.find(Drivers.chatId eq driverChatId).first()
            driver.state = DriverState.GOING_TO_PASSENGER

            val client = Client.find(Clients.clientId eq order.clientChatId).first()

            return@transaction client
        }
        context.edit(
            message = it.message.withContent<TextContent>()!!,
            text = it.message.text!! + "\n\nOrder was accepted."
        )
        context.send(
            chatId = ChatId(driverChatId),
            text = "Please, go to this location:"
        )
        context.send(
            chatId = ChatId(driverChatId),
            location = StaticLocation(longitude = client.startLocationLon!!, latitude = client.startLocationLat!!),
        )
        // todo: driver is found
    }

    private suspend fun driverDeclinesOrder(context: BehaviourContext, order: Order, it: MessageDataCallbackQuery) {
        transaction {
            order.potentialDrivers--
        }
        if (order.potentialDrivers <= 0) {
            // todo: no available drivers
        }

        context.edit(
            message = it.message.withContent<TextContent>()!!,
            text = it.message.text!! + "\n\nYou declined this order."
        )
    }
}

suspend fun getOrCreateDriver(chatId_: Long, context: BehaviourContext, message: CommonMessage<MessageContent>): Driver? {
    checkPrivateChat(context, message) ?: return null
    val drivers = transaction {
        return@transaction Driver.find {
            Drivers.chatId eq chatId_
        }.toList()
    }
    if (drivers.isEmpty()) {
        return addDriver(message.chat.id.chatId, DriverState.STARTED)
    }
    if (drivers.size > 1) {
        context.reply(message, "Too many drivers! Bot is broken, please, contact the developers :,((")
        return null
    }
    return drivers.first()
}

suspend fun checkPrivateChat(context: BehaviourContext, message: CommonMessage<MessageContent>): PrivateChat? {
    val privateChat = message.chat.privateChatOrNull()
    if (privateChat == null) {
        context.reply(message, "Only using in private chats is allowed!")
    }
    return privateChat
}

suspend fun BehaviourContext.sendOrderToDrivers(drivers: List<Long>, client: Client, orderUuid: UUID) {
    for (driverChatId in drivers) {
        send(
            chatId = ChatId(driverChatId),
            text = "New order! Start point:"
        )
        send(
            chatId = ChatId(driverChatId),
            location = StaticLocation(longitude = client.startLocationLon!!, latitude = client.startLocationLat!!),
        )
        send(
            chatId = ChatId(driverChatId),
            text = "Destination point:"
        )
        send(
            chatId = ChatId(driverChatId),
            location = StaticLocation(longitude = client.endLocationLon!!, latitude = client.endLocationLat!!),
        )
        send(
            chatId = ChatId(driverChatId),
            text = "Order distance is ${client.distance!!}. Order price is ${client.price!! * 0.95}. " +
                    "Do you want to take this order?",
            replyMarkup = inlineKeyboard {
                row {
                    dataButton("Accept", "$orderUuid accept")
                    dataButton("Decline", "$orderUuid decline")
                }
            }
        )
    }
}

suspend fun BehaviourContext.driverStoppedShareLocation(driver: Driver, it: CommonMessage<LocationContent>? = null) {

    transaction {
        driver.state = DriverState.STARTED
        driver.currentLocationLat = null
        driver.currentLocationLon = null
        driver.lastLocationUpdate = null
    }

    val text = "You stopped sharing your location, we're not looking for passengers for you any more. " +
            "If you'd like to start as a taxi driver again, please, send me your live location."
    if (it != null) {
        reply(it, text)
    } else {
        send(ChatId(driver.chatId), text)
    }
}
