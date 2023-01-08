package it.polito.timebanking

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue

class Message (var message: String, var isReceived: Boolean, var timestamp : Timestamp)