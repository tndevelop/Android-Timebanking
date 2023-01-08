package it.polito.timebanking.entities

data class Rating(
    var rate: Float = 0f,
    var ratingUserId: String = "0",
    var ratedUserId: String = "0",
    var offerId: String = "0",
    var comment: String = ""
) {
    var id: String = ""
}