package database

import driverlogic.DriverState
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable


object Drivers : IntIdTable() {
    val chatId = long("chat_id")
    val state = enumeration<DriverState>("state")
    val currentLocationLat = double("current_location_lat").nullable()
    val currentLocationLon = double("current_location_lon").nullable()
}

class Driver(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Driver>(Drivers)

    var chatId by Drivers.chatId
    var state by Drivers.state
    var currentLocationLat by Drivers.currentLocationLat
    var currentLocationLon by Drivers.currentLocationLon

    override fun toString(): String {
        return "chatId = ${chatId}, state = ${state}, currentLocationLat = ${currentLocationLat}, currentLocationLon = ${currentLocationLon}"
    }
}
