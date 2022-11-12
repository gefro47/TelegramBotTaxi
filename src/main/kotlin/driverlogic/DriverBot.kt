package driverlogic

import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.*
import dev.inmo.tgbotapi.extensions.utils.privateChatOrNull

suspend fun BehaviourContext.driverBot() {
    println(getMe())
    onCommand("start") {
        val privateChat = it.chat.privateChatOrNull()
        if (privateChat == null) {
            reply(it, "Only using in private chats is allowed!")
            return@onCommand
        }
        reply(it, "Hello, ${privateChat.firstName} ${privateChat.lastName}! " +
                "To start as a taxi driver, please, send me your live location.")
    }

    onStaticLocation {
        reply(it, "Please, use Live Location sharing!")
    }

    onLiveLocation {
        reply(it, "Save: ${it.content.location.longitude}, ${it.content.location.latitude}")
    }

    onEditedLocation {
        reply(it, "Save: ${it.content.location.longitude}, ${it.content.location.latitude}")
//        sleep(5000)
    }

    onContentMessage {
        println(it.content.toString())
        if (it.content.toString() == "/start") {
            return@onContentMessage
        }
        reply(it, "Thank you!")
    }
}
