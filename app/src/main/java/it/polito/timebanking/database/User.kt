

package it.polito.timebanking.database

data class User(
    var id: String = "0",
    var nickname: String = "",
    var fullName: String = "",
    var description: String = "",
    var email: String = "",
    var location: String = "",
    var rating: Float = 0f,
    var skillsArray: String = "",
    var imagePath: String = "",
    var favourites: String = "", //It contains the ids of the favourite offers
    var acceptedTimeSlots : String = "", //It contains the ids of the offers the user had accepted
    var assignedTimeSlots : String = "", //It contains the ids of the offers the user is providing to a requesting user
    var credit: Int = 180 // Credit in minutes, every user starts with 3 hours of credit
    )
{

}