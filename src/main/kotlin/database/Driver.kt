package database

import driverlogic.DriverState
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.transactions.transaction


object Drivers : IntIdTable() {
    val chatId = long("chat_id")
    val state = enumeration<DriverState>("state")
    val currentLocationLat = double("current_location_lat").nullable()
    val currentLocationLon = double("current_location_lon").nullable()
    val lastLocationUpdate = double("last_location_update").nullable()
}

class Driver(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Driver>(Drivers)

    var chatId by Drivers.chatId
    var state by Drivers.state
    var currentLocationLat by Drivers.currentLocationLat
    var currentLocationLon by Drivers.currentLocationLon
    var lastLocationUpdate by Drivers.lastLocationUpdate

    override fun toString(): String {
        return "chatId = ${chatId}, state = ${state}, currentLocationLat = ${currentLocationLat}, currentLocationLon = ${currentLocationLon}"
    }
}

suspend fun addDriver(chatId_: Long, state_: DriverState): Driver {
    return transaction {
        return@transaction Driver.new {
            chatId = chatId_
            state = state_
        }
    }
}

fun getAvailableDrivers(): List<Driver> {
    return transaction {
        return@transaction Driver.find { Drivers.state eq DriverState.WAIT_FOR_ORDER }.toList()
    }
}

fun getDriver(chatId_: Long): Driver? {
    return transaction {
        return@transaction Driver.find {
            Drivers.chatId eq chatId_
        }.firstOrNull()
    }
}
