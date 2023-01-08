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
class PublicUserViewModel(a:Application) : AndroidViewModel(a) {
    lateinit var userid: String
    private val _user = MutableLiveData<User>()
    var user : LiveData<User> = _user


    fun loadUser(id: String) {
        userid = id
        viewModelScope.launch {
            user = UserFirebaseService.getProfileByUserId(userid).asLiveData(this.coroutineContext, 5000)
        }
    }
}