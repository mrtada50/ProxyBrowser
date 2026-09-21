package com.proxybrowser.app.state

import kotlinx.coroutines.flow.MutableStateFlow

/** أمر معلّق جاي من اختصار تطبيق أو أيقونة موقع مثبّتة، تلتقطه شاشة المتصفح وتنفّذه ثم تصفّره. */
sealed class ShortcutAction {
    object NewTab : ShortcutAction()
    object Homepage : ShortcutAction()
    data class OpenUrl(val url: String) : ShortcutAction()
}

object ShortcutIntentState {
    val pendingAction = MutableStateFlow<ShortcutAction?>(null)
}
