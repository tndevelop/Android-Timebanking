package it.polito.timebanking.firebase

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import it.polito.timebanking.database.AdvertisementDemo
import it.polito.timebanking.database.User
import it.polito.timebanking.viewmodels.OfferViewModel


object OfferFirebaseService {

    suspend fun getAllOffers(vm: OfferViewModel): List<AdvertisementDemo> {
        val db = FirebaseFirestore.getInstance()

            var docList = mutableListOf<AdvertisementDemo>()
            db.collection("advertisements")
            .get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                            docList.add(document.toObject<AdvertisementDemo>())
                    }
                    vm.buildOffers()
                }
                .addOnFailureListener { exception ->
                    Log.w("Firebase", "Error getting offers: ", exception)
                }

        return docList
    }

    suspend fun getAllUsers(vm: OfferViewModel): List<User> {
        var docList = mutableListOf<User>()
        val db = FirebaseFirestore.getInstance()
            db.collection("users")
                .get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        docList.add(document.toObject<User>())
                    }
                    vm.buildOffers()
                }
                .addOnFailureListener { exception ->
                    Log.w("Firebase", "Error getting users: ", exception)
                }

        return docList
    }
}