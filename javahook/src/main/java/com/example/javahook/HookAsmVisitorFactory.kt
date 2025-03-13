package com.example.javahook

import com.android.build.api.instrumentation.AsmClassVisitorFactory
import com.android.build.api.instrumentation.ClassContext
import com.android.build.api.instrumentation.ClassData
import org.objectweb.asm.ClassVisitor

abstract class HookAsmVisitorFactory : AsmClassVisitorFactory<HookParams> {

    override fun createClassVisitor(
        classContext: ClassContext,
        nextClassVisitor: ClassVisitor
    ): ClassVisitor {
        return HookClassVisitor(nextClassVisitor, parameters.get().extension, classContext)
    }

    override fun isInstrumentable(classData: ClassData): Boolean {
        return true
    }
}