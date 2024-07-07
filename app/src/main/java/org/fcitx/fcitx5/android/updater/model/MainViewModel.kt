package org.fcitx.fcitx5.android.updater.model

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow

class MainViewModel : ViewModel() {

    val loaded = MutableStateFlow(false)

    val versions = MutableStateFlow(sortedMapOf<String, VersionViewModel>())

}
