package com.example.javahook

import com.android.build.api.instrumentation.InstrumentationParameters
import com.example.javahook.config.HookConfigData
import org.gradle.api.tasks.Input

interface HookParams : InstrumentationParameters {

    @get:Input
    var extension: HookConfigData
    @get:Input
    var scanOnly: Boolean

}