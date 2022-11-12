package driverlogic

import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand

suspend fun driverBot(context: BehaviourContext) {
    println(context.getMe())
    context.onCommand("start") {
        reply(it, "Hi:)")
    }
}
