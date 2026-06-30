package com.gis.systrace.core.domain.sync

/** Schedules a debounced backend upload after local snapshot collection. */
fun interface BackendUploadScheduler {
    fun scheduleUploadAfterSnapshot()
}
