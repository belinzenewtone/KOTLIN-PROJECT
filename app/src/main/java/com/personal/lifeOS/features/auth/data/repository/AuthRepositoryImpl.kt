package com.personal.lifeOS.features.auth.data.repository

import com.personal.lifeOS.features.auth.data.AuthResult
import com.personal.lifeOS.features.auth.data.SupabaseAuthClient
import com.personal.lifeOS.features.auth.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl
    @Inject
    constructor(
        private val authClient: SupabaseAuthClient,
    ) : AuthRepository {
        override suspend fun signUp(
            email: String,
            password: String,
            username: String,
        ): AuthResult {
            return authClient.signUp(
                email = email,
                password = password,
                username = username,
            )
        }

        override suspend fun signIn(
            email: String,
            password: String,
        ): AuthResult {
            return authClient.signIn(
                email = email,
                password = password,
            )
        }

        override suspend fun getUser(accessToken: String): AuthResult {
            return authClient.getUser(accessToken)
        }

        override suspend fun resendVerification(email: String): Boolean {
            return authClient.resendVerification(email)
        }
    }
