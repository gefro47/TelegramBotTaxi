package database

import driverlogic.DriverState
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*


object Drivers : IntIdTable() {
    val chatId = long("chat_id")
    val state = enumeration<DriverState>("state")
}

class Driver(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Driver>(Drivers)

    var chatId by Drivers.chatId
    var state by Drivers.state
}
