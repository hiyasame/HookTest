package com.example.javahook.config

import org.gradle.api.Action
import java.io.Serializable

data class HookConfigData(
    val hooks: MutableList<Hook> = mutableListOf()
): Serializable

data class Hook(
    var method: MethodConfig = MethodConfig(),
    var replaceWith: MethodConfig = MethodConfig()
): Serializable {
    fun method(action: Action<MethodConfig>) {
        val m = MethodConfig()
        action.execute(m)
        method = m
    }

    fun replaceWith(action: Action<MethodConfig>) {
        val m = MethodConfig()
        action.execute(m)
        replaceWith = m
    }
}

data class MethodConfig(
    var owner: String = "",
    var name: String = "",
    var descriptor: String = "",
    var isStatic: Boolean = false,
) : Serializable