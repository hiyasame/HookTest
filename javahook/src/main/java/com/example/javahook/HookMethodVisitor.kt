package com.example.javahook

import com.example.javahook.config.Hook
import com.example.javahook.utils.AsmTransformGlobalContext
import com.example.javahook.utils.Logger
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.commons.LocalVariablesSorter


class HookMethodVisitor(
    private val hooks: MutableList<Hook>,
    nextMethodVisitor: MethodVisitor
) : MethodVisitor(Opcodes.ASM9, nextMethodVisitor) {

    private val allHooks: List<Hook> by lazy {
        ArrayList<Hook>().apply {
            addAll(hooks)
            addAll(AsmTransformGlobalContext.annotationHooks)
        }
    }
    private val localVarSorter: LocalVariablesSorter = LocalVariablesSorter(
        AsmTransformGlobalContext.currentMethodAccess,
        AsmTransformGlobalContext.currentMethodDescriptor,
        nextMethodVisitor)
    private val objectArrayType by lazy {
        Type.getType("[Ljava/lang/Object;")
    }

    override fun visitMethodInsn(
        opcode: Int,
        owner: String,
        name: String,
        descriptor: String,
        isInterface: Boolean
    ) {
        // 处理 Origin#call 调用，内部已经 visit 过了，直接返回
        if (handleOriginCall(opcode, owner, name, descriptor, isInterface)) {
            return
        }
        val match = allHooks.firstOrNull {
            val method = it.method
            method.name == name
                    && method.owner == owner
                    && method.descriptor == descriptor
                    && method.isStatic == (opcode == Opcodes.INVOKESTATIC)
        }
        if (match == null || isInterface) {
            return superVisitMethodInsn(opcode, owner, name, descriptor, isInterface)
        }
        val replaceWith = match.replaceWith
        // 预检
        if ((opcode == Opcodes.INVOKEVIRTUAL
                    && match.replaceWith.descriptor != descriptor.replace("(", "(L${owner};")) ||
            (opcode == Opcodes.INVOKESTATIC && match.replaceWith.descriptor != descriptor)) {
            Logger.error("参数列表不匹配的hook: " +
                    "${if (opcode == Opcodes.INVOKESTATIC) "static" else ""} ${owner}#${name}${descriptor} " +
                    "--> static ${replaceWith.owner}#${replaceWith.name}${replaceWith.descriptor}")
            return superVisitMethodInsn(opcode, owner, name, descriptor, isInterface)
        }
        when (opcode) {
            Opcodes.INVOKESTATIC -> {
                Logger.info("static ${owner}#${name}${descriptor} " +
                        "--> static ${replaceWith.owner}#${replaceWith.name}${replaceWith.descriptor}")
                superVisitMethodInsn(opcode, replaceWith.owner, replaceWith.name, replaceWith.descriptor, false)
                return
            }
            // Foo#foo(String a, Integer b)
            // -> Hooker#fooHook(Foo foo, String a, String b)
            Opcodes.INVOKEVIRTUAL -> {
                Logger.info("${owner}#${name}${descriptor} " +
                        "--> static ${replaceWith.owner}#${replaceWith.name}${replaceWith.descriptor}")
                superVisitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    replaceWith.owner,
                    replaceWith.name,
                    replaceWith.descriptor,
                    false
                )
                return
            }
        }
        superVisitMethodInsn(opcode, owner, name, descriptor, false)
    }

    private fun superVisitMethodInsn(opcode: Int,
                           owner: String,
                           name: String,
                           descriptor: String,
                           isInterface: Boolean) {
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
    }

    private fun handleOriginCall(
        opcode: Int,
        owner: String,
        name: String,
        descriptor: String,
        isInterface: Boolean
    ): Boolean {
        if (owner == "com/example/javahook/api/Origin" && name == "call") {
            val match = allHooks.firstOrNull {
                val replaceWith = it.replaceWith
                replaceWith.name == AsmTransformGlobalContext.currentMethodName
                        && replaceWith.owner == AsmTransformGlobalContext.currentClassName?.replace(".", "/")
                        && replaceWith.descriptor == AsmTransformGlobalContext.currentMethodDescriptor
            }
            val method = match?.method
            // 支持 invoke static 转回 invoke virtual
            if (method != null) {
                // 获取原始方法的参数类型
                val originalMethodParams = Type.getArgumentTypes(method.descriptor)
                val originalMethodReturn = Type.getReturnType(method.descriptor)

                // 将 Object[] 的形式转换成依次压入参数的形式
                // 因为没有办法获取实际传入了多少参数，只能先假设传入的参数是合法的
                // 传少了应该会 out of bound
                // 目前 Object[] 应该在操作数栈顶
                // 虚函数会多一个 receiver 的参数
                val arrayPos = localVarSorter.newLocal(objectArrayType)
                val locals = mutableListOf<Int>()

                // 存储 Object[] 到 arrayPos
                mv.visitVarInsn(Opcodes.ASTORE, arrayPos)

                if (!method.isStatic) {
                    val pos = localVarSorter.newLocal(Type.getType("L${owner};"))
                    // 读 Object[]
                    mv.visitVarInsn(Opcodes.ALOAD, arrayPos)
                    // 读第一个数组元素到栈顶
                    mv.visitIntInsn(Opcodes.BIPUSH, 0)
                    mv.visitInsn(Opcodes.AALOAD)
                    // 写入局部变量表
                    mv.visitVarInsn(Opcodes.ASTORE, pos)
                    locals.add(pos)
                }
                for (i in originalMethodParams.indices) {
                    val index = if (method.isStatic) i else i + 1
                    val pos = localVarSorter.newLocal(Type.getType("L${owner};"))
                    // 读 Object[]
                    mv.visitVarInsn(Opcodes.ALOAD, arrayPos)
                    // 读第一个数组元素到栈顶
                    mv.visitIntInsn(Opcodes.BIPUSH, index)
                    mv.visitInsn(Opcodes.AALOAD)
                    // 写入局部变量表
                    mv.visitVarInsn(Opcodes.ASTORE, pos)
                    locals.add(pos)
                }
                // 依次读出压入操作数栈
                for (pos in locals) {
                    mv.visitVarInsn(Opcodes.ALOAD, pos)
                }

                // 处理参数类型转换
                // Origin.call 的参数都是 Object，需要转换为原始方法的参数类型
                for (i in originalMethodParams.indices.reversed()) {
                    val paramType = originalMethodParams[i]
                    if (paramType.sort == Type.OBJECT || paramType.sort == Type.ARRAY) {
                        // 引用类型直接强制类型转换
                        mv.visitTypeInsn(Opcodes.CHECKCAST, paramType.internalName)
                    } else {
                        // 基本类型需要从包装类型拆箱
                        val wrapper = getWrapperType(paramType)
                        mv.visitTypeInsn(Opcodes.CHECKCAST, wrapper.internalName)
                        unboxPrimitive(paramType)
                    }
                }

                // 如果是虚函数调用需要 check cast receiver
                if (!method.isStatic) {
                    mv.visitTypeInsn(Opcodes.CHECKCAST, method.owner)
                }

                superVisitMethodInsn(
                    if (method.isStatic) Opcodes.INVOKESTATIC else Opcodes.INVOKEVIRTUAL,
                    method.owner,
                    method.name,
                    method.descriptor, false)

                // 处理返回值类型转换
                // 如果原始方法返回基本类型，需要装箱
                if (originalMethodReturn.sort != Type.VOID
                        && originalMethodReturn.sort != Type.OBJECT
                        && originalMethodReturn.sort != Type.ARRAY) {
                    boxPrimitive(originalMethodReturn)
                }
                return true
            }
        }
        return false
    }

    // 获取基本类型对应的包装类型
    private fun getWrapperType(type: Type): Type {
        return when (type.sort) {
            Type.BOOLEAN -> Type.getType(java.lang.Boolean::class.java)
            Type.BYTE -> Type.getType(java.lang.Byte::class.java)
            Type.CHAR -> Type.getType(java.lang.Character::class.java)
            Type.SHORT -> Type.getType(java.lang.Short::class.java)
            Type.INT -> Type.getType(java.lang.Integer::class.java)
            Type.FLOAT -> Type.getType(java.lang.Float::class.java)
            Type.LONG -> Type.getType(java.lang.Long::class.java)
            Type.DOUBLE -> Type.getType(java.lang.Double::class.java)
            else -> throw IllegalArgumentException("Not a primitive type: $type")
        }
    }

    // 基本类型的拆箱操作
    private fun unboxPrimitive(type: Type) {
        val methodName = when (type.sort) {
            Type.BOOLEAN -> "booleanValue"
            Type.BYTE -> "byteValue"
            Type.CHAR -> "charValue"
            Type.SHORT -> "shortValue"
            Type.INT -> "intValue"
            Type.FLOAT -> "floatValue"
            Type.LONG -> "longValue"
            Type.DOUBLE -> "doubleValue"
            else -> throw IllegalArgumentException("Not a primitive type: $type")
        }
        val methodDescriptor = "()" + type.descriptor
        val wrapper = getWrapperType(type)
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, wrapper.internalName, methodName, methodDescriptor, false)
    }

    // 基本类型的装箱操作
    private fun boxPrimitive(type: Type) {
        val wrapper = getWrapperType(type)
        val methodDescriptor = "(" + type.descriptor + ")L" + wrapper.internalName + ";"
        mv.visitMethodInsn(
            Opcodes.INVOKESTATIC,
            wrapper.internalName,
            "valueOf",
            methodDescriptor,
            false
        )
    }

}