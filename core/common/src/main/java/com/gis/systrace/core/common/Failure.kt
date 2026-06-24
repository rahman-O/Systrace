package com.gis.systrace.core.common

sealed interface Failure {
    data object Unknown : Failure
    data class PermissionDenied(val permission: String) : Failure
    data class Validation(val reason: String) : Failure
    data class Network(val code: Int? = null, val message: String? = null) : Failure
    data class Storage(val message: String? = null) : Failure
    data class Platform(val message: String? = null) : Failure
}

