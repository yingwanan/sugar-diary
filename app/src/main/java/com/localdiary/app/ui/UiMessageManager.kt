package com.localdiary.app.ui

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class UiMessageEvent(
    val message: String,
)

class UiMessageManager {
    private val _messages = MutableSharedFlow<UiMessageEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    val messages: SharedFlow<UiMessageEvent> = _messages.asSharedFlow()

    fun show(message: String) {
        if (message.isBlank()) return
        _messages.tryEmit(UiMessageEvent(message))
    }
}
