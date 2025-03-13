package com.example.javahook.utils

import com.example.javahook.config.Hook

object AsmTransformGlobalContext {
    val annotationHooks: MutableList<Hook> = ArrayList()
    var currentClassName: String? = null
    var currentMethodAccess: Int = Int.MIN_VALUE
    var currentMethodName: String? = null
    var currentMethodDescriptor: String? = null
}