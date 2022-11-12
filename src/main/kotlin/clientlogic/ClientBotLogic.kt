package clientlogic

import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand

suspend fun clientBot(
    behaviourContext: BehaviourContext,

){


    behaviourContext.onCommand("start") {
        reply(it, "Hi:)")
    }
}
