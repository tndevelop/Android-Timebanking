package it.polito.timebanking.firebase

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.QuerySnapshot
import it.polito.timebanking.database.AdvertisementDemo
import it.polito.timebanking.database.User
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import org.json.JSONObject

object UserFirebaseService {

    @ExperimentalCoroutinesApi
    suspend fun getProfileByUserId(userId: String): Flow<User> {
        val db = FirebaseFirestore.getInstance()
        return callbackFlow {
            val listenerRegistration = db.collection("users")
                .document(userId)
                .addSnapshotListener{ snap: DocumentSnapshot?, exc: FirebaseFirestoreException? ->
                    if(exc != null){
                        cancel(message = "Error fetching user",
                            cause = exc)
                        return@addSnapshotListener
                    }

                    if (isActive && snap?.data != null) offer(jacksonObjectMapper().readValue(JSONObject(snap?.data).toString(), User::class.java))
                }
            awaitClose {
                Log.d("timebanking", "Cancelling user listener")
                listenerRegistration.remove()
            }
        }
    }

    @ExperimentalCoroutinesApi
    fun saveProfile(uid: String, user: HashMap<String, Any>, success: ()->Unit, failure: (Exception)->Unit) {
        val db = FirebaseFirestore.getInstance()

        user["id"] = uid

        db.collection("users")
            .document(uid)
            .set(user)
            .addOnSuccessListener {
                success()
            }
            .addOnFailureListener {
                failure(it)
            }
    }

    @ExperimentalCoroutinesApi
    fun updateUserRating(uid: String, rating: Float, success: ()->Unit, failure: (Exception)->Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("users")
            .document(uid)
            .update("rating", rating)
            .addOnSuccessListener {
                success()
            }
            .addOnFailureListener {
                failure(it)
            }
    }

    // Atomic transaction to transfer credit
    fun creditTransfer(senderId: String, receiverId: String, creditToTransfer: Int, success: ()->Unit, failure: (Exception)->Unit) {
        val db = FirebaseFirestore.getInstance()
        val sender = db.collection("users").document(senderId)
        val receiver = db.collection("users").document(receiverId)
        db.runTransaction { transaction ->
            val senderCredit = transaction.get(sender).getDouble("credit")
            if (senderCredit == null || senderCredit - creditToTransfer < 0) {
                // Cancel transaction and signal insufficient credit
                throw FirebaseFirestoreException("Insufficient credit",
                    FirebaseFirestoreException.Code.ABORTED)
            } else {
                val receiverCredit = transaction.get(receiver).getDouble("credit")!!
                transaction.update(sender, "credit", senderCredit - creditToTransfer)
                transaction.update(receiver, "credit", receiverCredit + creditToTransfer)
            }
        }.addOnSuccessListener {
            success()
        }.addOnFailureListener {
            failure(it)
        }
    }

    //Selective update for acceptedTimeSlots
    fun updateAcceptedList(userId: String, acceptedId: String, success: ()->Unit, failure: (Exception)->Unit) {
        val db = FirebaseFirestore.getInstance()
        val user = db.collection("users").document(userId)

        db.runTransaction { transaction ->
            var userAccepted = transaction.get(user).getString("acceptedTimeSlots")
            if (userAccepted == null ) {
                // Cancel transaction and signal insufficient credit
                throw FirebaseFirestoreException("Problems when loading user's accepted list",
                    FirebaseFirestoreException.Code.ABORTED)
            } else {
                userAccepted= "$userAccepted$acceptedId,"
                transaction.update(user, "acceptedTimeSlots", userAccepted)
            }
        }.addOnSuccessListener {
            success()
        }.addOnFailureListener {
            failure(it)
        }
    }

    //Selective update for assignedTimeSlots
    fun updateAssignedList(userId: String, assignedId: String, success: ()->Unit, failure: (Exception)->Unit) {
        val db = FirebaseFirestore.getInstance()
        val user = db.collection("users").document(userId)

        db.runTransaction { transaction ->
            var userAssigned= transaction.get(user).getString("assignedTimeSlots")
            if (userAssigned==null) {
                // Cancel transaction and signal insufficient credit
                throw FirebaseFirestoreException("Problems when loading user's lists",
                    FirebaseFirestoreException.Code.ABORTED)
            } else {
                userAssigned= "$userAssigned$assignedId,"
                transaction.update(user, "assignedTimeSlots", userAssigned)
            }
        }.addOnSuccessListener {
            success()
        }.addOnFailureListener {
            failure(it)
        }
    }



}