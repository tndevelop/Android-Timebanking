package it.polito.timebanking.database

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue

data class MessageDb(
    var id: String = "",
    var receiver: String = "",
    var sender: String = "",
    var text: String = "",
    var timestamp: FieldValue,
)
