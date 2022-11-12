package driverlogic

import database.Driver
import database.Drivers
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.*
import dev.inmo.tgbotapi.extensions.utils.privateChatOrNull
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.MessageContent
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.transactions.transaction

suspend fun BehaviourContext.driverBot() {
    println(getMe())
    onCommand("start") {
        val privateChat = it.chat.privateChatOrNull()
        if (privateChat == null) {
            reply(it, "Only using in private chats is allowed!")
            return@onCommand
        }
        addDriver(it.chat.id.chatId, DriverState.START)
        reply(it, "Hello, ${privateChat.firstName} ${privateChat.lastName}! " +
                "To start as a taxi driver, please, send me your live location.")
    }

    onStaticLocation {
        checkDriver(it.chat.id.chatId, this, it as CommonMessage<MessageContent>)?.let { return@onStaticLocation }
        reply(it, "Please, use Live Location sharing!")
    }

    onLiveLocation {
        checkDriver(it.chat.id.chatId, this, it as CommonMessage<MessageContent>)?.let { return@onLiveLocation }
        reply(it, "Save: ${it.content.location.longitude}, ${it.content.location.latitude}")
    }

    onEditedLocation {
        checkDriver(it.chat.id.chatId, this, it as CommonMessage<MessageContent>)?.let { return@onEditedLocation }
        reply(it, "Save: ${it.content.location.longitude}, ${it.content.location.latitude}")
//        sleep(5000)
    }

    onContentMessage {
        checkDriver(it.chat.id.chatId, this, it as CommonMessage<MessageContent>)?.let { return@onContentMessage }
        println(it.content.toString())
        if (it.content.toString() == "/start") {
            return@onContentMessage
        }
        reply(it, "Thank you!")
    }
}

suspend fun addDriver(chatId_: Long, state_: DriverState) {
    transaction {
        Driver.new {
            chatId = chatId_
            state = state_
        }
    }
}

suspend fun checkDriver(chatId_: Long, context: BehaviourContext, message: CommonMessage<MessageContent>): Driver? {
    val drivers = transaction {
        return@transaction Driver.find{
            Drivers.chatId eq chatId_
        }.toList()
    }
    if (drivers.isEmpty()) {
        context.reply(message, "Unknown driver, please use /start command")
        return null
    }
    return drivers.first()
}
