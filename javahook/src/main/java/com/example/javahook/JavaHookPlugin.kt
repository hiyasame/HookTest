package com.example.javahook

import com.android.build.api.instrumentation.FramesComputationMode
import com.android.build.api.instrumentation.InstrumentationScope
import com.android.build.api.variant.AndroidComponentsExtension
import com.example.javahook.config.HookExtension
import com.example.javahook.utils.AsmTransformGlobalContext
import com.example.javahook.utils.Logger
import org.gradle.api.Plugin
import org.gradle.api.Project

// 直接使用 transform action
class JavaHookPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        Logger.init(target)
        if (!(target.plugins.hasPlugin("com.android.library") || target.plugins.hasPlugin("com.android.application"))) {
            Logger.error("没有 apply android gradle plugin，不能使用 hook 能力")
            return
        }
        target.extensions.add("javahook", HookExtension::class.java)
        val ext = target.extensions.getByType(AndroidComponentsExtension::class.java)

        ext.onVariants {
            it.instrumentation.transformClassesWith(
                AnnotationDetectVisitorFactory::class.java,
                InstrumentationScope.ALL
            ) {

            }
            it.instrumentation.transformClassesWith(
                HookAsmVisitorFactory::class.java,
                InstrumentationScope.ALL
            ) { param ->
                param.scanOnly = false
                param.extension = target.extensions
                    .getByType(HookExtension::class.java)
                    .configData
            }
        }
    }
}