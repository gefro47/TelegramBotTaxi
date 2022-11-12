package driverlogic

import com.soywiz.klock.DateTimeSpan
import database.Driver
import database.Drivers
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onEditedLocation
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onLiveLocation
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onStaticLocation
import dev.inmo.tgbotapi.extensions.utils.liveLocationOrThrow
import dev.inmo.tgbotapi.extensions.utils.privateChatOrNull
import dev.inmo.tgbotapi.types.chat.PrivateChat
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.MessageContent
import dev.inmo.tgbotapi.types.message.content.StaticLocationContent
import org.jetbrains.exposed.sql.transactions.transaction

suspend fun BehaviourContext.driverBot() {
    println(getMe())

    onCommand("start") {
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
        }

        reply(it, "Hello, ${privateChat.firstName} ${privateChat.lastName}! " +
                "To start as a taxi driver, please, send me your live location. " +
                "If you'd like to stop waiting for passengers, just stop sharing your location.")
    }

    onStaticLocation {
        getOrCreateDriver(it.chat.id.chatId, this, it as CommonMessage<MessageContent>) ?: return@onStaticLocation
        reply(it, "Please, use Live Location sharing!")
    }

    onLiveLocation {
        val driver = getOrCreateDriver(it.chat.id.chatId, this, it as CommonMessage<MessageContent>) ?: return@onLiveLocation
        if (driver.state !in listOf(DriverState.WAIT_FOR_ORDER, DriverState.GOING_TO_PASSENGER, DriverState.ORDER_IN_PROGRESS)) {
            transaction {
                driver.state = DriverState.WAIT_FOR_ORDER
            }
        }
        transaction {
            driver.currentLocationLat = it.content.location.latitude
            driver.currentLocationLon = it.content.location.longitude
        }
        reply(it, "Thank you for sharing your location! We're looking for the passengers for you. " +
                "If you'd like to stop waiting for passengers, just stop sharing your location.")
    }

    onEditedLocation {
        val driver = getOrCreateDriver(it.chat.id.chatId, this, it as CommonMessage<MessageContent>) ?: return@onEditedLocation
        if (driver.state !in listOf(DriverState.WAIT_FOR_ORDER, DriverState.GOING_TO_PASSENGER, DriverState.ORDER_IN_PROGRESS)) {
            transaction {
                driver.state = DriverState.WAIT_FOR_ORDER
            }
        }
        transaction {
            driver.currentLocationLat = it.content.location.latitude
            driver.currentLocationLon = it.content.location.longitude
        }

        val livePeriod = DateTimeSpan(seconds = it.content.location.liveLocationOrThrow().livePeriod - 30)
        val endDate = it.date + livePeriod
        if (endDate >= (it.editDate ?: endDate) || it.content is StaticLocationContent) {
            // Время шаринга локации вышло или чувак сам перестал шарить локацию
            transaction {
                driver.state = DriverState.STARTED
                driver.currentLocationLat = null
                driver.currentLocationLon = null
            }

            reply(it, "You stopped sharing your location, we're not looking for passengers for you any more. " +
                    "If you'd like to start as a taxi driver again, please, send me your live location.")
        }
    }

//    onContentMessage {
//        getOrCreateDriver(it.chat.id.chatId, this, it) ?: return@onContentMessage
//        println(it.content.toString())
//        if (it.content.toString() == "/start") {
//            return@onContentMessage
//        }
//        reply(it, "Thank you!")
//    }
}

suspend fun addDriver(chatId_: Long, state_: DriverState): Driver {
    return transaction {
        return@transaction Driver.new {
            chatId = chatId_
            state = state_
        }
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
