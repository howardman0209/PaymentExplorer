package com.payment.explorer.inspector

import com.payment.explorer.util.DebugPanelManager

abstract class BasicInspector {
    open fun log(message: String) {
        DebugPanelManager.log(message)
    }
}