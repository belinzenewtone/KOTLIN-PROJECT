package com.personal.lifeOS.features.auth.domain.repository

import com.personal.lifeOS.features.auth.data.AuthResult

interface AuthRepository {
    suspend fun signUp(
        email: String,
        password: String,
        username: String,
    ): AuthResult

    suspend fun signIn(
        email: String,
        password: String,
    ): AuthResult

    suspend fun getUser(accessToken: String): AuthResult

    suspend fun resendVerification(email: String): Boolean

    suspend fun sendPasswordReset(email: String): Boolean
}
