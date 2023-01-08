package it.polito.timebanking.firebase

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.QuerySnapshot
import it.polito.timebanking.entities.Chat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive

object ChatsFirebaseService {

    @ExperimentalCoroutinesApi
    suspend fun getUserChatsAsSender( userId: String): Flow<List<Chat>> {

        var chats = mutableSetOf<Chat>()
        val db = FirebaseFirestore.getInstance()

        return callbackFlow {
            val listenerS = db.collection("messages")
                .whereEqualTo("sender", userId)
                .addSnapshotListener { snap: QuerySnapshot?, exc: FirebaseFirestoreException? ->
                    if (exc != null) {
                        cancel(
                            message = "Error fetching user",
                            cause = exc
                        )
                        return@addSnapshotListener
                    }
                    for (document in snap?.documents!!) {
                        chats.add(
                            Chat(
                                listOf(
                                    document.data?.get("sender").toString(),
                                    document.data?.get("receiver").toString()
                                ),
                                document.data?.get("offerId").toString(),
                            ).also { it.unreadByUser1 = document.data?.get("unreadByUser1") as Boolean?;
                                it.unreadByUser2 = document.data?.get("unreadByUser2") as Boolean?;
                                Log.d("test", "unreaduser1 ${document.data?.get("unreadByUser1")}; " +
                                        "unreaduser2 ${document.data?.get("unreadByUser2")};" +
                                        document.data?.get("offerId").toString()
                                )}
                        )
                    }

                    if (isActive) offer(chats.toList())
                }
            awaitClose {
                Log.d("timebanking", "Cancelling user listener")
                listenerS.remove()
            }
        }
    }

    @ExperimentalCoroutinesApi
    suspend fun getUserChatsAsReceiver( userId: String): Flow<List<Chat>> {

        var chats = mutableSetOf<Chat>()
        val db = FirebaseFirestore.getInstance()

        return callbackFlow {
            val listenerR = db.collection("messages")
                .whereEqualTo("receiver", userId)
                .addSnapshotListener { snap: QuerySnapshot?, exc: FirebaseFirestoreException? ->
                    if (exc != null) {
                        cancel(
                            message = "Error fetching user",
                            cause = exc
                        )
                        return@addSnapshotListener
                    }
                    for (document in snap?.documents!!) {
                        chats.add(
                            Chat(
                                listOf(
                                    document.data?.get("sender").toString(),
                                    document.data?.get("receiver").toString()
                                ),
                                document.data?.get("offerId").toString()
                            ).also { it.unreadByUser1 = document.data?.get("unreadByUser1") as Boolean?;
                                it.unreadByUser2 = document.data?.get("unreadByUser2") as Boolean?;
                                Log.d("test", "unreaduser1 ${document.data?.get("unreadByUser1")}; " +
                                        "unreaduser2 ${document.data?.get("unreadByUser2")}"  +
                                        document.data?.get("offerId").toString())}
                        )
                    }
                    if (isActive) offer(chats.toList())
                }
            awaitClose {
                Log.d("timebanking", "Cancelling user listener")
                listenerR.remove()
            }

        }

    }
}