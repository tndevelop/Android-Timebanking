package it.polito.timebanking.viewmodels

import android.util.Log
import androidx.lifecycle.*
import it.polito.timebanking.database.AdvertisementDemo
import it.polito.timebanking.database.User
import it.polito.timebanking.entities.Offer
import it.polito.timebanking.entities.OfferStatus
import it.polito.timebanking.firebase.OfferFirebaseService
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

//@ExperimentalCoroutinesApi
class OfferViewModel : ViewModel() {

    var offers : List<AdvertisementDemo> = listOf<AdvertisementDemo>()

    var users : List<User> = listOf<User>()

    lateinit var cor1: Job
    lateinit var cor2: Job

    val sdf: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd hh:mm")

    private var _loaded = MutableLiveData<Int>(0)
    var loaded: LiveData<Int> = _loaded

    init {
        load()
    }

    fun load() {
        cor1 = viewModelScope.launch {
            offers = OfferFirebaseService.getAllOffers(this@OfferViewModel)
        }
        cor1.start()
        cor2 = viewModelScope.launch {
            users = OfferFirebaseService.getAllUsers(this@OfferViewModel)
        }
        cor2.start()
        viewModelScope.launch {
            cor1.join()
            _loaded.value = _loaded.value?.plus(1)
            cor2.join()
            _loaded.value = _loaded.value?.plus(1)
            buildOffers()
        }
    }

    private var _adOffers = MutableLiveData<List<Offer>>()
    val adOffers : LiveData<List<Offer>> = _adOffers

    fun buildOffers() {
        if (offers != emptyList<AdvertisementDemo>() && users != emptyList<User>()) {
            val offersByUserId: Map<String, AdvertisementDemo> = offers.associateBy { it.userId }
            val filterUsers : Map<String, User> = users.filter { offersByUserId.get(it.id) != null }.associateBy { it.id }
            val filteredOffers = offers.filter { filterUsers.get(it.userId) != null }
            val mergedList = filteredOffers.map { offer ->
                filterUsers.get(offer.userId).let { user ->
                    Offer().also { it.id= offer.id; it.title = offer.title; it.dateAndTime = sdf.parse(offer.dateAndTime); it.duration = offer.duration;
                        it.location = offer.location; it.nickname = user!!.nickname; it.rating = user.rating;
                        it.imagePath = user.imagePath; it.service = offer.service; it.description = offer.description;
                        it.userId = offer.userId; it.status = offer.status}
                }
            }
            _adOffers.value = mergedList?: mutableListOf()
            unfilteredAds.value = mergedList?: mutableListOf()
            locations = mergedList?.map { it.location }?: emptyList()
            _loaded.value = _loaded.value?.plus(1)
        } else if (_loaded.value == 2) {
            _loaded.value = _loaded.value?.plus(1)
        }

    }

    fun loadByOfferId(offId : String): Offer? {

        var myOffers= adOffers.value as List<Offer>
        for(off in myOffers){
            if(off.id== offId){
                return off
            }
        }
        return null
    }

    val unfilteredAds : MutableLiveData<List<Offer>> by lazy {
        MutableLiveData<List<Offer>>()
    }

    fun sortDateAndTime() {
        val sortedAds = (adOffers.value?: mutableListOf()) as MutableList
        sortedAds.sortBy { it.dateAndTime }
        _adOffers.value = sortedAds
    }

    fun sortTitle() {
        val sortedAds = adOffers.value as MutableList
        sortedAds.sortBy { it.title }
        _adOffers.value = sortedAds
    }

    fun sortLocation() {
        val sortedAds = adOffers.value as MutableList
        sortedAds.sortBy { it.location }
        _adOffers.value = sortedAds
    }

    fun sortDurationAscending() {
        val sortedAds = adOffers.value as MutableList
        sortedAds.sortBy { it.duration }
        _adOffers.value = sortedAds
    }

    fun sortDurationDescending() {
        val sortedAds = adOffers.value as MutableList
        sortedAds.sortByDescending { it.duration }
        _adOffers.value = sortedAds
    }

    fun sortUserRating() {
        val sortedAds = adOffers.value as MutableList
        sortedAds.sortByDescending { it.rating }
        _adOffers.value = sortedAds
    }

    fun sortUserName() {
        val sortedAds = adOffers.value as MutableList
        sortedAds.sortBy { it.nickname }
        _adOffers.value = sortedAds
    }

    fun filterAccepted(userId : String){
        Log.d("marta", "INSIDE FILTER")

        var filteredAds= mutableListOf<Offer>()
        if (unfilteredAds.value != null) {
            filteredAds = unfilteredAds.value as MutableList
            Log.d("marta", "not null")
        }
        filteredAds.filter{ it.assignedUser== userId && it.status== OfferStatus.ACCEPTED}
        _adOffers.value = filteredAds
        Log.d("marta", "final list= $_adOffers")
    }

    var locations: List<String> = emptyList()

    fun filter(loc: String, dur: Long?, dateFrom: Long?, dateTo: Long?, rating: String, service: String?) {

        var filteredAds = mutableListOf<Offer>()
        if (unfilteredAds.value != null) {
            filteredAds = unfilteredAds.value as MutableList
        }
        if (service != null) {
            Log.d("Service", service.lowercase())
            filteredAds = filteredAds.filter { it.service.lowercase() == service.lowercase() } as MutableList
        }
        if (loc != "Any") {
            filteredAds = filteredAds.filter { it.location == loc } as MutableList
        }
        if (dur != null) {
            filteredAds = filteredAds.filter { it.duration >= dur } as MutableList
        }
        if (dateFrom != null && dateTo != null) {
            filteredAds = filteredAds.filter { it.dateAndTime < Date(dateTo) && it.dateAndTime > Date(dateFrom) } as MutableList
        }
        if (rating != "Any") {
            filteredAds = filteredAds.filter { it.rating >= rating.toFloat() } as MutableList
        }

        _adOffers.value = filteredAds
    }

    fun getOfferById(id: String): Offer? {
        return adOffers.value?.find { it.id == id }
    }

    fun getUserById(id: String): User? {
        return users.find { it.id == id }
    }
}