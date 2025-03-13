package com.example.javahook

import com.android.build.api.instrumentation.AsmClassVisitorFactory
import com.android.build.api.instrumentation.ClassContext
import com.android.build.api.instrumentation.ClassData
import com.android.build.api.instrumentation.InstrumentationParameters
import com.example.javahook.config.Hook
import com.example.javahook.config.HookConfigData
import com.example.javahook.utils.AsmTransformGlobalContext
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

abstract class AnnotationDetectVisitorFactory : AsmClassVisitorFactory<InstrumentationParameters> {

    override fun createClassVisitor(
        classContext: ClassContext,
        nextClassVisitor: ClassVisitor
    ): ClassVisitor {
        return AnnotationDetectClassVisitor(nextClassVisitor, classContext)
    }

    override fun isInstrumentable(classData: ClassData): Boolean {
        return true
    }
}

class AnnotationDetectClassVisitor(
    nextClassVisitor: ClassVisitor,
    private val classContext: ClassContext
) : ClassVisitor(Opcodes.ASM9, nextClassVisitor) {

    override fun visitMethod(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        AsmTransformGlobalContext.currentClassName = classContext.currentClassData.className
        AsmTransformGlobalContext.currentMethodName = name
        AsmTransformGlobalContext.currentMethodDescriptor = descriptor
        return AnnotationDetectMethodVisitor(super.visitMethod(access, name, descriptor, signature, exceptions))
    }
}

class AnnotationDetectMethodVisitor(nextMethodVisitor: MethodVisitor) : MethodVisitor(Opcodes.ASM9, nextMethodVisitor) {

    // 打了 @HookMethod 注解的方法手动加入 hooks 列表
    inner class HookAnnotationVisitor(nextAnnotationVisitor: AnnotationVisitor) : AnnotationVisitor(Opcodes.ASM9, nextAnnotationVisitor) {

        private val hook = Hook()

        override fun visit(name: String?, value: Any?) {
            when (name) {
                "targetOwner" -> {
                    hook.method.owner = value.toString()
                }
                "targetName" -> {
                    hook.method.name = value.toString()
                }
                "targetDescriptor" -> {
                    hook.method.descriptor = value.toString()
                }
                "targetIsStatic" -> {
                    hook.method.isStatic = value as Boolean
                }
            }
            super.visit(name, value)
        }

        override fun visitEnd() {
            hook.replaceWith.apply {
                owner = AsmTransformGlobalContext.currentClassName?.replace(".", "/").toString()
                name = AsmTransformGlobalContext.currentMethodName.toString()
                descriptor = AsmTransformGlobalContext.currentMethodDescriptor.toString()
            }
            AsmTransformGlobalContext.annotationHooks.add(hook)
            super.visitEnd()
        }
    }

    override fun visitAnnotation(descriptor: String?, visible: Boolean): AnnotationVisitor {
        if (descriptor == "Lcom/example/javahook/api/HookMethod;") {
            return HookAnnotationVisitor(super.visitAnnotation(descriptor, visible))
        }
        return super.visitAnnotation(descriptor, visible)
    }
}