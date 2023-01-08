package it.polito.timebanking.database

import it.polito.timebanking.entities.OfferStatus
import java.util.*

data class AdvertisementDemo(
    var id: String = "-1",
    var title: String = "",
    var service: String = "",
    var description: String = "",
    var dateAndTime: String = "",
    var duration: Long = 0L,
    var location: String = "",
    var restrictions: String = "",
    var userId: String = "0",
    var status: OfferStatus = OfferStatus.OPEN)
{
    var assignedUser: String? = null
}