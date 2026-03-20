package com.personal.lifeOS.core.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Manages biometric authentication for app lock.
 */
object BiometricAuthManager {
    fun canAuthenticate(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        // Accept biometric (strong or weak) OR device credential (PIN/pattern/password).
        // Using BIOMETRIC_STRONG|WEAK|DEVICE_CREDENTIAL as the combined check ensures
        // the result is true even on devices without enrolled fingerprints as long as
        // a screen lock is configured — which is the correct fallback path.
        val combined =
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        return biometricManager.canAuthenticate(combined) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun authenticate(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val callback =
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence,
                ) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                        errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON
                    ) {
                        onError(errString.toString())
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    // DO NOT call onError here. onAuthenticationFailed fires for every
                    // unsuccessful scan attempt (wet finger, wrong angle, etc.) but the
                    // system BiometricPrompt UI already handles retries with its own
                    // error messaging. Calling onError would incorrectly dismiss the
                    // system dialog and replace it with our manual overlay on every
                    // failed tap. We only surface errors for hard failures via
                    // onAuthenticationError above.
                }
            }

        // Allow both biometric (fingerprint/face) and device credential (PIN/pattern/password)
        // as fallback. DEVICE_CREDENTIAL replaces the negative button, so we must not
        // call setNegativeButtonText when it's included — the system adds its own fallback link.
        val allowedAuthenticators =
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL

        val promptInfo =
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock LifeOS")
                .setSubtitle("Use fingerprint or device PIN to continue")
                .setAllowedAuthenticators(allowedAuthenticators)
                .build()

        BiometricPrompt(activity, executor, callback).authenticate(promptInfo)
    }
}
