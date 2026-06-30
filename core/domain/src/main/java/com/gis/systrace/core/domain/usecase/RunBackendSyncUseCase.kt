package com.gis.systrace.core.domain.usecase

import com.gis.systrace.core.domain.repository.BackendSyncRepository
import com.gis.systrace.core.domain.sync.SyncBatchResult

class RunBackendSyncUseCase(
    private val backendSyncRepository: BackendSyncRepository,
) {
    suspend operator fun invoke(): SyncBatchResult = backendSyncRepository.runFullSync()
}
