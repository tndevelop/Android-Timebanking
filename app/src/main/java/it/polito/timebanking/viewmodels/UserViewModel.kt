package it.polito.timebanking.viewmodels

import android.app.Application
import androidx.lifecycle.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import it.polito.timebanking.database.User
import it.polito.timebanking.firebase.UserFirebaseService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
class UserViewModel(a:Application) : AndroidViewModel(a) {
    var userid = Firebase.auth.currentUser?.uid!!
    private val _user = MutableLiveData<User>()
    var user : LiveData<User> = _user

    init {
        viewModelScope.launch {
            user = UserFirebaseService.getProfileByUserId(userid).asLiveData(this.coroutineContext, 5000)
        }
    }

    @ExperimentalCoroutinesApi
    fun save(uid: String, user: HashMap<String, Any>, success: ()->Unit, failure: (Exception)->Unit) {
        UserFirebaseService.saveProfile(uid, user, success, failure)
    }

    @ExperimentalCoroutinesApi
    fun update(uid: String, user: HashMap<String, Any>, success: ()->Unit, failure: (Exception)->Unit) {
        UserFirebaseService.saveProfile(uid, user, success, failure)
    }
    class UserViewModelFactory(
        val userId: String
    ): ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return modelClass.getConstructor(String::class.java)
                .newInstance(userId)
        }
    }

    fun updateAccepted(userId: String, acceptedTimeSlotId: String, success: ()->Unit, failure: (Exception)->Unit){
        UserFirebaseService.updateAcceptedList(userId, acceptedTimeSlotId, success, failure)
    }

    fun updateAssigned(userId: String, assignedTimeSlotId: String, success: ()->Unit, failure: (Exception)->Unit){
        UserFirebaseService.updateAssignedList(userId, assignedTimeSlotId, success, failure)
    }

}