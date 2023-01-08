package it.polito.timebanking.firebase

import android.util.Log
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.*
import com.google.firebase.ktx.Firebase
import it.polito.timebanking.Message
import it.polito.timebanking.database.User
import it.polito.timebanking.entities.Chat
import it.polito.timebanking.viewmodels.MessageViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import org.json.JSONObject

object MessageFirebaseService {

    @ExperimentalCoroutinesApi
    suspend fun getMessages( senderUserId: String, receiverUserId: String, offerId: String): Flow<List<Message>> {

        var messages = mutableSetOf<Message>()
        val db = FirebaseFirestore.getInstance()
        val thisUser = Firebase.auth.currentUser?.uid!!

        return callbackFlow {
            val listenerR = db.collection("messages")
                .whereEqualTo("sender", senderUserId)
                .whereEqualTo("receiver", receiverUserId)
                .whereEqualTo("offerId", offerId)
                .addSnapshotListener { snap: QuerySnapshot?, exc: FirebaseFirestoreException? ->
                    if (exc != null) {
                        cancel(
                            message = "Error fetching user",
                            cause = exc
                        )
                        return@addSnapshotListener
                    }
                    for (document in snap?.documents!!) {
                        messages.add(
                            Message(
                                document.data?.get("text") as String,
                                document.data?.get("receiver") == thisUser,
                                document.data?.get("timestamp") as Timestamp)
                        )
                        Log.d("KMB", "Firebase Messages: ${messages.map { it.isReceived }}")
                    }

                    if (isActive) offer(messages.toList())
                }
            awaitClose {
                Log.d("timebanking", "Cancelling user listener")
                listenerR.remove()
            }
        }
    }


    @ExperimentalCoroutinesApi
    fun saveMessage(message: HashMap<String, Any>, success: ()->Unit, failure: (Exception)->Unit) {
        val db = FirebaseFirestore.getInstance()

        val doc = db.collection("messages")
            .document()

        message["id"] = doc.id


        doc.set(message)
            .addOnSuccessListener {
                success()
            }
            .addOnFailureListener {
                failure(it)
            }
    }

    @ExperimentalCoroutinesApi
    fun updateReadMessages(senderUserId: String, receiverUserId: String, offerId: String) {
        val db = FirebaseFirestore.getInstance()

        db.collection("messages")
        .whereEqualTo("sender", senderUserId)
        .whereEqualTo("receiver", receiverUserId)
        .whereEqualTo("offerId", offerId).addSnapshotListener { snap: QuerySnapshot?, exc: FirebaseFirestoreException? ->
            for (doc in snap?.documents!!) {
                val message = hashMapOf("text" to doc.data?.get("text"), "sender" to doc.data?.get("sender"),
                    "receiver" to doc.data?.get("receiver"),
                    "timestamp" to doc.data?.get("timestamp"), "offerId" to doc.data?.get("offerId"),
                    "unreadByUser1" to doc.data?.get("unreadByUser1"), "unreadByUser2" to false)
                db.collection("messages").document(doc.id).update(message)
            }

        }

    }
}

