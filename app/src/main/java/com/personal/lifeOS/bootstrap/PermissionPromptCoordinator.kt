package com.personal.lifeOS.bootstrap

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionPromptCoordinator
    @Inject
    constructor() {
        suspend fun prepareStartupPermissionRouting() {
            // Intentionally lightweight; actual prompt timing belongs to feature/UI flows.
        }
    }
