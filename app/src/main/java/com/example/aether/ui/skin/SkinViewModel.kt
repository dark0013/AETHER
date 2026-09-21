package com.example.aether.ui.skin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SkinViewModel(private val repository: SkinRepository) : ViewModel() {

    val selectedSkin: StateFlow<Skin> = repository.selectedSkinId
        .map { SkinEngine.resolve(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, BuiltinSkins.Default)

    fun selectSkin(id: SkinId) {
        viewModelScope.launch { repository.setSkin(id) }
    }

    companion object {
        fun factory(repository: SkinRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SkinViewModel(repository) as T
                }
            }
    }
}
