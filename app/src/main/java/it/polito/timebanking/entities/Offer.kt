package it.polito.timebanking.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

data class Offer (

    var id: String = "0",

    var title: String = "",

    var service: String = "",

    var description: String = "",

    var dateAndTime: Date = Date(),

    var duration: Long = 0L,

    var location: String = "",

    var restrictions: String = "",

    var userId: String = "1",

    var nickname: String = "",

    var rating: Float = 0f,

    var imagePath: String = "",

    var status: OfferStatus = OfferStatus.OPEN,

) {
    var assignedUser: String? = null
}