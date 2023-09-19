package com.ncs.nextbus



import android.net.Uri
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ncs.nextbus.repository.RealtimeRepository
import com.ncs.tradezy.ResultState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    private val repo: RealtimeRepository
) : ViewModel(){
    private val _res: MutableState<LocationState> = mutableStateOf(
        LocationState()
    )
    val res: State<LocationState> = _res
    init {
        viewModelScope.launch {
            repo.getLocationData().collect{
                when(it){
                    is ResultState.Success->{
                        _res.value= LocationState(
                            item = it.data
                        )
                    }
                    is ResultState.Failure->{
                        _res.value= LocationState(
                            error = it.msg.toString()
                        )
                    }
                    ResultState.Loading->{
                        _res.value= LocationState(
                            isLoading = true
                        )
                    }
                }
            }
        }
    }


}