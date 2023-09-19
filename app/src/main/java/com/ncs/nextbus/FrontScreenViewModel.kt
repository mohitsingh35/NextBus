package com.ncs.nextbus

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ncs.nextbus.repository.RealtimeRepository
import com.ncs.tradezy.ResultState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FrontScreenViewModel @Inject constructor(
    private val repo: RealtimeRepository
) : ViewModel() {
    private val _res: MutableLiveData<LocationState> = MutableLiveData(LocationState())
    val res: LiveData<LocationState> = _res

    init {
        viewModelScope.launch {
            repo.getLocationData().collect { resultState ->
                when (resultState) {
                    is ResultState.Success -> {
                        _res.value = LocationState(item = resultState.data)
                    }
                    is ResultState.Failure -> {
                        _res.value = LocationState(error = resultState.msg.toString())
                    }
                    ResultState.Loading -> {
                        _res.value = LocationState(isLoading = true)
                    }
                }
            }
        }
    }
}
