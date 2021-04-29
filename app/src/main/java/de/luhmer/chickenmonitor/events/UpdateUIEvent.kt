package de.luhmer.chickenmonitor.events

data class UpdateUIEvent(val raw: String) {

    var avg: String? = null
    var status: String? = null
}