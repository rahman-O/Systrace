package com.gis.systrace.core.common

interface ErrorMapper {
    fun map(throwable: Throwable): Failure
}

object DefaultErrorMapper : ErrorMapper {
    override fun map(throwable: Throwable): Failure = when (throwable) {
        is SecurityException -> Failure.PermissionDenied(throwable.message.orEmpty())
        is IllegalArgumentException -> Failure.Validation(throwable.message.orEmpty())
        is java.io.IOException -> Failure.Storage(throwable.message)
        else -> Failure.Unknown
    }
}

