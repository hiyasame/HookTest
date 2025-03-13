package com.example.javahook

import com.android.build.api.instrumentation.ClassContext
import com.example.javahook.config.Hook
import com.example.javahook.config.HookConfigData
import com.example.javahook.utils.AsmTransformGlobalContext
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes


class HookClassVisitor(
    nextClassVisitor: ClassVisitor,
    private val params: HookConfigData,
    private val classContext: ClassContext
) : ClassVisitor(Opcodes.ASM9, nextClassVisitor) {

    override fun visitMethod(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        AsmTransformGlobalContext.apply {
            currentClassName = classContext.currentClassData.className
            currentMethodAccess = access
            currentMethodName = name
            currentMethodDescriptor = descriptor
        }
        return HookMethodVisitor(params.hooks, super.visitMethod(access, name, descriptor, signature, exceptions))
    }
}

