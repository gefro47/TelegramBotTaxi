package driverlogic

import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.utils.privateChatOrNull

suspend fun driverBot(context: BehaviourContext) {
    println(context.getMe())
    context.onCommand("start") {
        val privateChat = it.chat.privateChatOrNull()
        if (privateChat == null) {
            reply(it, "Only using in private chats is allowed!")
            return@onCommand
        }
        reply(it, "Hello, ${privateChat.firstName} ${privateChat.lastName}! To start as a taxi driver, please, send me your location.")
    }
}
