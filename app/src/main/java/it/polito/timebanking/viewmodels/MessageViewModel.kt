package it.polito.timebanking.viewmodels

import android.util.Log
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistryOwner
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import it.polito.timebanking.Message
import it.polito.timebanking.entities.Chat
import it.polito.timebanking.firebase.ChatsFirebaseService
import it.polito.timebanking.firebase.MessageFirebaseService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch


class SomeViewModelFactory(
    private val owner: SavedStateRegistryOwner,
    private val otherUser: String,
    private val offer: String) : AbstractSavedStateViewModelFactory(owner, null) {
    override fun <T : ViewModel?> create(key: String, modelClass: Class<T>, state: SavedStateHandle) =
        MessageViewModel(state, otherUser, offer) as T
}

@ExperimentalCoroutinesApi
class MessageViewModel(private val state: SavedStateHandle, private val otherUser: String, private val offerId: String) : ViewModel()  {

    val feedPosition = state.get<Int>(0.toString()).let { position ->
        if (position == null) 0 else position
    }

    fun saveFeedPosition(position: Int) {
        state.set(0.toString(), position)
    }

    private val _messages = MutableLiveData<List<Message>>()
    var messages : LiveData<List<Message>> = _messages

    private var _loaded = MutableLiveData<Boolean>(false)
    var loaded: LiveData<Boolean> = _loaded
    val thisUser = Firebase.auth.currentUser?.uid.toString()
    init {
        load()
    }

    @OptIn(FlowPreview::class)
    fun load() {
        viewModelScope.launch {
            messages = flowOf(MessageFirebaseService.getMessages(otherUser, thisUser, offerId), MessageFirebaseService.getMessages(thisUser, otherUser, offerId))
                .flattenMerge().asLiveData(this.coroutineContext, 5000)
            }
    }


    @ExperimentalCoroutinesApi
    fun save(message: HashMap<String, Any>, success: ()->Unit, failure: (Exception)->Unit) {
        MessageFirebaseService.saveMessage(message, success, failure)
        load()
    }

}