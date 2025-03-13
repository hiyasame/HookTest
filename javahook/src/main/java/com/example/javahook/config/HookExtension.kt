package com.example.javahook.config

import org.gradle.api.Action
import java.io.Serializable

open class HookExtension : Serializable {
    val configData = HookConfigData()

    fun hook(action: Action<Hook>) {
        val hook = Hook()
        action.execute(hook)
        configData.hooks.add(hook)
    }
}