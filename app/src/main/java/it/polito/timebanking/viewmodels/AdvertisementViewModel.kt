package it.polito.timebanking.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import it.polito.timebanking.database.AdvertisementDemo
import it.polito.timebanking.entities.OfferStatus
import it.polito.timebanking.firebase.AdvertisementFirebaseService
import it.polito.timebanking.firebase.UserFirebaseService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.concurrent.thread

class AdvertisementViewModel(a: Application) : AndroidViewModel(a) {


    val auth = Firebase.auth
    private var _userid = auth.currentUser?.uid!!
    private val _adId = MutableLiveData("-1")
    private val _advertisement = MutableLiveData<AdvertisementDemo>()
    var advertisement : LiveData<AdvertisementDemo> = _advertisement
    private val _advertisements = MutableLiveData<List<AdvertisementDemo>>()

    var advertisements: LiveData<List<AdvertisementDemo>> = _advertisements

    init {
        viewModelScope.launch {
            if(_userid != null)
                advertisements = AdvertisementFirebaseService.getAdvertisementsByUserId(_userid).asLiveData(this.coroutineContext, 5000)

            if(_adId.value != null && _adId.value != "-1") {
                advertisement = AdvertisementFirebaseService.getAdvertisementById(_adId.value!!).asLiveData(this.coroutineContext,5000)
            }
       }

    }

    fun loadByID(id: String) {
       _adId.value = id
    }


    @ExperimentalCoroutinesApi
    fun save(aid: String, advertisement: HashMap<String, Any>, success: ()->Unit, failure: (Exception)->Unit) {
        AdvertisementFirebaseService.saveAdvertisement(aid, advertisement, success, failure)
    }

    @ExperimentalCoroutinesApi
    fun delete(aid: String, success: ()->Unit, failure: (Exception)->Unit) {
        AdvertisementFirebaseService.deleteAdvertisement(aid, success, failure)
    }

    fun updateStatus(id: String, status: OfferStatus, assignedUser: String?, success: ()->Unit, failure: (Exception)->Unit){
        viewModelScope.launch {
            val ad = AdvertisementFirebaseService.getAdvertisementById(id).first()
                    .also { it.status = status; it.assignedUser = assignedUser }
            val adMap = ObjectMapper().convertValue(ad, java.util.HashMap::class.java) as java.util.HashMap<String, Any>
            AdvertisementFirebaseService.saveAdvertisement(id, adMap, success, failure)
        }
    }


}