package it.polito.timebanking.viewmodels

import androidx.lifecycle.*
import it.polito.timebanking.firebase.ServicesFirebaseService
import kotlinx.coroutines.launch

class ServicesViewModel : ViewModel() {

    private val _services = MutableLiveData<List<String>>()
    var services : LiveData<List<String>> = _services

    private var _loaded = MutableLiveData<Boolean>(false)
    var loaded: LiveData<Boolean> = _loaded

    init {
        viewModelScope.launch {
            _services.value = ServicesFirebaseService.getAllServices(this@ServicesViewModel) //.asLiveData(this.coroutineContext, 5000)
        }
    }

    fun setServices(list: List<String>) {
        _services.value = list
        _loaded.value = true
    }
}