package clientlogic

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

//data class ClientUser(
//    val clientId: Long,
//    val dialogState: String,
//    val startLocationLat: Double = 0.0,
//    val startLocationLon: Double = 0.0,
//    val endLocationLat: Double = 0.0,
//    val endLocationLon: Double = 0.0,
//    val distance: Double = 0.0,
//    val price: Double = 0.0,
//    val driverId: Long = 0L
//)

object Clients: IntIdTable(){
    val clientId = long("client_id")
    val dialogState = varchar("dialog_state", length = 100)
    val startLocationLat = double("start_location_lat").nullable()
    val startLocationLon = double("start_location_lon").nullable()
    val endLocationLat = double("end_location_lat").nullable()
    val endLocationLon = double("end_location_lon").nullable()
    val distance = double("distance").nullable()
    val price = double("price").nullable()
    val driverId = long("driver_id").nullable()
}

class Client(id: EntityID<Int>): IntEntity(id){
    companion object: IntEntityClass<Client>(Clients)

    var clientId by Clients.clientId
    var dialogState by Clients.dialogState
    var startLocationLat by Clients.startLocationLat
    var startLocationLon by Clients.startLocationLon
    var endLocationLat by Clients.endLocationLat
    var endLocationLon by Clients.endLocationLon
    var distance by Clients.distance
    var price by Clients.price
    var driverId by Clients.driverId

    override fun toString(): String {
        return "clientId=$clientId, startLocationLat=$startLocationLat, startLocationLon=$startLocationLon, " +
                "endLocationLat=$endLocationLat, endLocationLon=$endLocationLon"
    }
}
