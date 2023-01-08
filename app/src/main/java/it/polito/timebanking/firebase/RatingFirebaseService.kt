package it.polito.timebanking.firebase

import android.util.Log
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.firebase.Timestamp
import com.google.firebase.firestore.*
import com.google.firebase.firestore.ktx.toObject
import it.polito.timebanking.Message
import it.polito.timebanking.database.AdvertisementDemo
import it.polito.timebanking.database.User
import it.polito.timebanking.entities.Rating
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import org.json.JSONObject

object RatingFirebaseService {

    fun getRating(offerId:String, myCallback: (Rating?) -> Unit) {

        var rating: Rating? = null
        val db = FirebaseFirestore.getInstance()

        db.collection("ratings")
            .whereEqualTo("offerId", offerId)
            .get()
            .addOnSuccessListener { snap ->
                if(snap != null && snap.documents != null && snap!!.documents.size > 0) {
                    val document: QueryDocumentSnapshot =
                        snap!!.documents.get(0) as QueryDocumentSnapshot
                    rating = document.toObject<Rating>()
                }
                else rating = null

                myCallback(rating)
            }
            .addOnFailureListener { exception ->
                Log.w("Firebase", "Error getting offers: ", exception)
            }
    }

    @ExperimentalCoroutinesApi
    fun saveRating(rating: HashMap<String, Any>, success: ()->Unit, failure: (Exception)->Unit) {

        val db = FirebaseFirestore.getInstance()
        val doc : DocumentReference = db.collection("ratings").document()

        rating["id"] = doc.id
        doc.set(rating)
            .addOnSuccessListener {
                val ratedUserId = jacksonObjectMapper().readValue(JSONObject(rating.toMap()).toString(), Rating::class.java).ratedUserId
                updateUsersRating(ratedUserId)
                success()
            }
            .addOnFailureListener {
                failure(it)
            }
    }

    @ExperimentalCoroutinesApi
    fun updateRating(oldRatingId: String, rating: HashMap<String, Any>, success: ()->Unit, failure: (Exception)->Unit) {

        val db = FirebaseFirestore.getInstance()
        db.collection("ratings").document(oldRatingId).update(rating)
            .addOnSuccessListener {
                val ratedUserId = jacksonObjectMapper().readValue(JSONObject(rating.toMap()).toString(), Rating::class.java).ratedUserId
                updateUsersRating(ratedUserId)
                success()
            }
            .addOnFailureListener {
                failure(it)
            }
    }

    fun updateUsersRating(ratedUserId: String) {
        getUsersRating(ratedUserId) {
            val ratingsArray = it.map { it.rate }
            val userRating = ratingsArray.sum() / ratingsArray.size
            UserFirebaseService.updateUserRating(ratedUserId, userRating, {
                Log.d("Rating", "Rating of $ratedUserId updated to: $userRating")
            }, {
                Log.d("Rating", "An error occured: $it")
            })
        }
    }

    @ExperimentalCoroutinesApi
    fun getUsersRating(ratedUserId: String,  myCallback: (ArrayList<Rating>) -> Unit) {

        var ratings: ArrayList<Rating> = ArrayList()
        val db = FirebaseFirestore.getInstance()
        db.collection("ratings")
            .whereEqualTo("ratedUserId", ratedUserId)
            .get()
            .addOnSuccessListener { snap ->
                if(snap != null && snap.documents != null && snap!!.documents.size > 0) {
                    for (document in snap.documents) {
                        document.toObject<Rating>()?.let { ratings.add(it) }
                    }
                }

                myCallback(ratings)
            }
            .addOnFailureListener { exception ->
                Log.w("Firebase", "Error getting offers: ", exception)
            }
    }
}