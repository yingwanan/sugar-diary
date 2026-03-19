package com.localdiary.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

inline fun <reified T : ViewModel> singleFactory(crossinline builder: () -> T): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        override fun <VM : ViewModel> create(modelClass: Class<VM>): VM {
            if (modelClass.isAssignableFrom(T::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return builder() as VM
            }
            error("Unsupported ViewModel class: ${modelClass.name}")
        }
    }
