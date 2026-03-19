package com.personal.lifeOS.bootstrap

import com.personal.lifeOS.core.security.AuthSessionStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionHydrationUseCase
    @Inject
    constructor(
        private val authSessionStore: AuthSessionStore,
    ) {
        suspend operator fun invoke(): Boolean {
            return authSessionStore.getAccessToken().isNotBlank() && authSessionStore.getUserId().isNotBlank()
        }
    }
