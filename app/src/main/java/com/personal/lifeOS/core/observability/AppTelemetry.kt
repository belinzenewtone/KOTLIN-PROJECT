package com.personal.lifeOS.core.observability

import android.app.Application
import android.util.Log
import com.personal.lifeOS.BuildConfig
import io.sentry.Breadcrumb
import io.sentry.Sentry
import io.sentry.SentryLevel
import io.sentry.android.core.SentryAndroid
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Lightweight app-wide telemetry bridge.
 *
 * We keep one small surface for product events so we can:
 * 1) ship observability quickly;
 * 2) switch providers later without touching feature code.
 */
object AppTelemetry {
    private const val TAG = "AppTelemetry"
    private val initialized = AtomicBoolean(false)

    fun init(application: Application) {
        if (!initialized.compareAndSet(false, true)) return
        if (BuildConfig.SENTRY_DSN.isBlank()) {
            Log.i(TAG, "Sentry DSN not configured; telemetry will stay local (Logcat only).")
            return
        }

        SentryAndroid.init(application) { options ->
            options.dsn = BuildConfig.SENTRY_DSN
            options.environment = BuildConfig.SENTRY_ENVIRONMENT.ifBlank { "production" }
            options.tracesSampleRate = BuildConfig.SENTRY_TRACES_SAMPLE_RATE.coerceIn(0.0, 1.0)
            options.release =
                "${BuildConfig.APPLICATION_ID}@${BuildConfig.VERSION_NAME}+${BuildConfig.VERSION_CODE}"
            options.isEnableAutoSessionTracking = true
            options.setDiagnosticLevel(SentryLevel.ERROR)
        }
        Log.i(TAG, "Sentry telemetry initialized.")
    }

    fun trackEvent(
        name: String,
        attributes: Map<String, String> = emptyMap(),
        captureAsMessage: Boolean = false,
    ) {
        val safeName = name.trim().lowercase(Locale.US)
        if (safeName.isEmpty()) return
        val safeAttributes =
            attributes
                .filterKeys { it.isNotBlank() }
                .mapValues { (_, value) -> value.take(120) }

        Log.i(TAG, "event=$safeName attrs=$safeAttributes")

        val crumb =
            Breadcrumb().apply {
                category = "product_event"
                level = SentryLevel.INFO
                message = safeName
                safeAttributes.forEach { (key, value) -> data[key] = value }
            }
        Sentry.addBreadcrumb(crumb)

        if (captureAsMessage) {
            val attrsText =
                if (safeAttributes.isEmpty()) {
                    ""
                } else {
                    safeAttributes.entries.joinToString(
                        prefix = " ",
                        separator = ",",
                    ) { "${it.key}=${it.value}" }
                }
            Sentry.captureMessage("event:$safeName$attrsText", SentryLevel.INFO)
        }
    }

    fun captureError(
        throwable: Throwable,
        context: Map<String, String> = emptyMap(),
    ) {
        val safeContext =
            context
                .filterKeys { it.isNotBlank() }
                .mapValues { (_, value) -> value.take(120) }
        safeContext.forEach { (key, value) -> Sentry.setTag(key, value) }
        Log.e(TAG, "captureError context=$safeContext", throwable)
        Sentry.captureException(throwable)
    }
}
