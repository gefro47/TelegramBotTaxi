package driverlogic

enum class DriverState {
    STARTED,    // location is not shared
    WAIT_FOR_ORDER,         // location is shared
    GOING_TO_PASSENGER,     // location is shared
    ORDER_IN_PROGRESS,      // location is shared
}
