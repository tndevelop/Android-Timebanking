package it.polito.timebanking.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import it.polito.timebanking.entities.Chat
import it.polito.timebanking.firebase.ChatsFirebaseService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class ChatsViewModel(a: Application) : AndroidViewModel(a) {
    val auth = Firebase.auth
    private var _userid = auth.currentUser?.uid!!
    private val _chatties = MutableLiveData<List<Chat>>()
    var chatties: LiveData<List<Chat>> = _chatties

    init {
        viewModelScope.launch {
            if(_userid != null) {
                chatties= flowOf(ChatsFirebaseService.getUserChatsAsSender(_userid), ChatsFirebaseService.getUserChatsAsReceiver(_userid))
                    .flattenMerge().asLiveData(this.coroutineContext, 5000)
            }
        }
    }

}