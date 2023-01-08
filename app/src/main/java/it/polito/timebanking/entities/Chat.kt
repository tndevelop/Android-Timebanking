package it.polito.timebanking.entities

class Chat(
    val userIds : List<String>,
    val offerId : String
) {
    var unreadByUser1 : Boolean? = false
    var unreadByUser2 : Boolean? = false
}