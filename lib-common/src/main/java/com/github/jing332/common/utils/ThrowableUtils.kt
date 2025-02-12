package com.github.jing332.common.utils

import cn.hutool.core.exceptions.ExceptionUtil
import cn.hutool.core.exceptions.ExceptionUtil.getThrowableList


object ThrowableUtils {
}

val Throwable.rootCause: Throwable
    get() = ExceptionUtil.getRootCause(this)

val Throwable.readableString: String
    get() = "${rootCause}\n⬇ More:\n${stackTraceToString()}"
