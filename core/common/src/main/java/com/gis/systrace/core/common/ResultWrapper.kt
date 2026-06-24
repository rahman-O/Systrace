package com.gis.systrace.core.common

sealed interface ResultWrapper<out T> {
    data class Success<T>(val value: T) : ResultWrapper<T>
    data class Error(val failure: Failure) : ResultWrapper<Nothing>
}

