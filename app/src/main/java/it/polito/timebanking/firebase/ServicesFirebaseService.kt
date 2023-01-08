package it.polito.timebanking.firebase

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import it.polito.timebanking.viewmodels.ServicesViewModel


object ServicesFirebaseService {

    suspend fun getAllServices(vm: ServicesViewModel): List<String> {
        val db = FirebaseFirestore.getInstance()
        var docList = mutableListOf<String>()
        db.collection("services")
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    docList.add(document.data.get("name").toString())
                }
                vm.setServices(docList)
            }
            .addOnFailureListener { exception ->
                Log.w("KMB", "Error getting offers: ", exception)
            }

        return docList
    }
}