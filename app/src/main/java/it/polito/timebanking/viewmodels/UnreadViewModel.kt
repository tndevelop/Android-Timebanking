package it.polito.timebanking.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import it.polito.timebanking.entities.Chat
import it.polito.timebanking.firebase.ChatsFirebaseService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class UnreadViewModel(a: Application) : AndroidViewModel(a) {
    val auth = Firebase.auth
    private var _userid = auth.currentUser?.uid!!
    private val _chats = MutableLiveData<List<Chat>>()
    var chats: LiveData<List<Chat>> = _chats

    init {
        viewModelScope.launch {
            chats = ChatsFirebaseService.getUserChatsAsReceiver(_userid).asLiveData(this.coroutineContext, 5000)
        }
    }

    fun reload() {
        viewModelScope.launch {
            chats = ChatsFirebaseService.getUserChatsAsReceiver(_userid)
                .asLiveData(this.coroutineContext, 5000)
        }
    }
}