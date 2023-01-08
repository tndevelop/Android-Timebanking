package it.polito.timebanking.firebase

import android.util.Log
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.firebase.firestore.*
import it.polito.timebanking.database.AdvertisementDemo
import it.polito.timebanking.database.User
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import org.json.JSONObject

object AdvertisementFirebaseService {

    @ExperimentalCoroutinesApi
    suspend fun getAdvertisementById(id: String): Flow<AdvertisementDemo> {

        val db = FirebaseFirestore.getInstance()
        return callbackFlow {
            val listenerRegistration =
                db
                    .collection("advertisements")
                    .document(id)
                    .addSnapshotListener {
                        snap: DocumentSnapshot?, e: FirebaseFirestoreException? ->
                            if(e != null) {
                                cancel("Error fetching advertisments", e)
                                return@addSnapshotListener
                            }

                            if(isActive) offer(jacksonObjectMapper().readValue(JSONObject(snap?.data).toString(), AdvertisementDemo::class.java))
                  }
            awaitClose {
                Log.d("timebanking", "Canceling advertisments listener")
                listenerRegistration.remove()
            }
        }
    }

    @ExperimentalCoroutinesApi
    suspend fun getAdvertisementsByUserId(userId: String): Flow<List<AdvertisementDemo>> {

        val db = FirebaseFirestore.getInstance()
        return callbackFlow {
            val listenerRegistration = db.collection("advertisements")
                .whereEqualTo("userId", userId)
                .addSnapshotListener{ snap: QuerySnapshot?, exc: FirebaseFirestoreException? ->
                    if(exc != null) {
                        cancel(message = "Error fetching user", cause = exc)
                        return@addSnapshotListener
                    }

                    var advList = snap?.documents!!.mapNotNull {
                        jacksonObjectMapper().readValue(JSONObject(it.data).toString(), AdvertisementDemo::class.java)
                    }
                    if (isActive) offer(advList!!)
                }
            awaitClose {
                Log.d("timebanking", "Cancelling advertisements listener")
                listenerRegistration.remove()
            }
        }
    }

    @ExperimentalCoroutinesApi
    fun saveAdvertisement(aid: String, advertisement: HashMap<String, Any>, success: ()->Unit, failure: (Exception)->Unit) {

        val db = FirebaseFirestore.getInstance()

        val doc : DocumentReference

        if (aid != "-1")
            doc = db.collection("advertisements")
                .document(aid)
        else
            doc = db.collection("advertisements")
                .document()

        advertisement["id"] = doc.id

        doc.set(advertisement)
            .addOnSuccessListener {
                success()
            }
            .addOnFailureListener {
                failure(it)
            }
    }

    @ExperimentalCoroutinesApi
    fun saveNewAdvertisement(aid: Int, advertisement: HashMap<String, Any>, success: ()->Unit, failure: (Exception)->Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("advertisements")
            .document(aid.toString())
            .set(advertisement)
            .addOnSuccessListener {
                success()
            }
            .addOnFailureListener {
                failure(it)
            }
    }

    @ExperimentalCoroutinesApi
    fun deleteAdvertisement(aid: String, success: ()->Unit, failure: (Exception)->Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("advertisements")
            .document(aid)
            .delete()
            .addOnSuccessListener {
                success()
            }
            .addOnFailureListener {
                failure(it)
            }

        // Also delete associated chats
        var  query = db.collection("messages")
            .whereEqualTo("offerId", aid).get()
        query.addOnCompleteListener {
            val docs = it.result.documents
            for (doc in docs) {
                db.collection("messages").document(doc.id).delete()
            }
        }
    }
}