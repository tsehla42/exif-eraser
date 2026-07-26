package com.tsehla.exiferaser.main.data

import android.net.Uri
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class ShareIntentHolder @Inject constructor() {

    private val _uris = MutableStateFlow<List<Uri>?>(null)
    val uris: StateFlow<List<Uri>?> = _uris.asStateFlow()

    fun submit(uris: List<Uri>) {
        _uris.value = uris
    }

    fun consume() {
        _uris.value = null
    }
}
