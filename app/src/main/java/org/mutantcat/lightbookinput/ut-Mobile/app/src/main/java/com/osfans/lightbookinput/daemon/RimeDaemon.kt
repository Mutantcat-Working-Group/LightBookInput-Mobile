// SPDX-FileCopyrightText: 2015 - 2026 Rime community
//
// SPDX-License-Identifier: GPL-3.0-or-later

package org.mutantcat.lightbookinput.daemon

import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import org.mutantcat.lightbookinput.R
import org.mutantcat.lightbookinput.LightBookInputApplication
import org.mutantcat.lightbookinput.core.Rime
import org.mutantcat.lightbookinput.core.RimeApi
import org.mutantcat.lightbookinput.core.RimeLifecycle
import org.mutantcat.lightbookinput.core.RimeMessage
import org.mutantcat.lightbookinput.core.lifecycleScope
import org.mutantcat.lightbookinput.core.whenReady
import org.mutantcat.lightbookinput.data.sync.RimeDataSync
import org.mutantcat.lightbookinput.ui.main.LogActivity
import org.mutantcat.lightbookinput.util.DeployNotification
import org.mutantcat.lightbookinput.util.appContext
import org.mutantcat.lightbookinput.util.readText
import org.mutantcat.lightbookinput.util.subprocess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import splitties.systemservices.notificationManager
import timber.log.Timber
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Manage the singleton instance of [Rime]
 *
 * To use rime, client should call [createSession] to obtain a [RimeSession],
 * and call [destroySession] on client destroyed. Client should not leak the instance of [RimeApi],
 * and must use [RimeSession] to access rime functionalities.
 *
 * The instance of [Rime] always exists,but whether the dispatcher runs and callback works depend on clients, i.e.
 * if no clients are connected, [Rime.finalize] will be called.
 *
 * Functions are thread-safe in this class.
 *
 * Adapted from [fcitx5-android/FcitxDaemon.kt](https://github.com/fcitx5-android/fcitx5-android/blob/364afb44dcf0d9e3db3d43a21a32601b2190cbdf/app/src/main/java/org/fcitx/fcitx5/android/daemon/FcitxDaemon.kt)
 */
object RimeDaemon {
    private const val STARTUP_RETRY_DELAY_MS = 1_000L
    private const val STARTUP_RETRY_MAX_ATTEMPTS = 30

    private val realRime by lazy { Rime() }

    private val rimeImpl by lazy { object : RimeApi by realRime {} }

    private val sessions = mutableMapOf<String, RimeSession>()

    private val lock = ReentrantLock()

    @Volatile
    private var startupRetryJob: Job? = null

    private fun establish(name: String) = object : RimeSession {
        private inline fun <T> ensureEstablished(block: () -> T) = if (name in sessions) {
            block()
        } else {
            throw IllegalStateException("Session $name is not established")
        }

        override fun <T> run(block: suspend RimeApi.() -> T): T = ensureEstablished {
            runBlocking { block(rimeImpl) }
        }

        override suspend fun <T> runOnReady(block: suspend RimeApi.() -> T): T = ensureEstablished {
            realRime.lifecycle.whenReady { block(rimeImpl) }
        }

        override fun runIfReady(block: suspend RimeApi.() -> Unit) {
            ensureEstablished {
                if (realRime.isReady) {
                    realRime.lifecycleScope.launch {
                        block(rimeImpl)
                    }
                }
            }
        }

        override val lifecycleScope: CoroutineScope
            get() = realRime.lifecycle.lifecycleScope
    }

    private fun tryStarLightBookInputLocked(): Boolean {
        if (realRime.lifecycle.currentState != RimeLifecycle.State.STOPPED) {
            return true
        }
        return realRime.startup()
    }

    private fun scheduleStartupRetry() {
        if (startupRetryJob?.isActive == true) return
        Timber.i("Scheduling Rime startup retry until storage is available")
        startupRetryJob =
            LightBookInputApplication.getInstance().coroutineScope.launch {
                repeat(STARTUP_RETRY_MAX_ATTEMPTS) { attempt ->
                    delay(STARTUP_RETRY_DELAY_MS)
                    if (sessions.isEmpty()) return@launch
                    if (realRime.lifecycle.currentState != RimeLifecycle.State.STOPPED) return@launch
                    if (!RimeDataSync.isStorageAvailable()) {
                        Timber.d("Rime startup retry ${attempt + 1}: storage still unavailable")
                        return@repeat
                    }
                    val started =
                        lock.withLock {
                            if (sessions.isEmpty()) return@launch
                            tryStarLightBookInputLocked()
                        }
                    if (started) {
                        Timber.i("Rime started after storage became available")
                        return@launch
                    }
                }
                Timber.w("Rime startup retry exhausted while sessions remain connected")
            }
    }

    fun createSession(name: String): RimeSession = lock.withLock {
        if (name in sessions) {
            return@withLock sessions.getValue(name)
        }
        if (!tryStarLightBookInputLocked()) {
            scheduleStartupRetry()
        }
        val session = establish(name)
        sessions[name] = session
        return@withLock session
    }

    fun destroySession(name: String): Unit = lock.withLock {
        if (name !in sessions) {
            return
        }
        sessions -= name
        if (sessions.isEmpty()) {
            startupRetryJob?.cancel()
            startupRetryJob = null
            realRime.finalize()
        }
    }

    /**
     * Reuse a session for remote service
     */
    fun getFirstSessionOrNull() = sessions.firstNotNullOfOrNull { it.value }

    private var restartId = 0

    init {
        DeployNotification.ensureChannel()
        LightBookInputApplication.getInstance().coroutineScope.launch {
            realRime.messageFlow.collect {
                handleRimeMessage(it)
            }
        }
    }

    private inline fun sendNotification(
        id: Int,
        buildAction: NotificationCompat.Builder.() -> Unit,
    ) {
        val builder =
            NotificationCompat
                .Builder(appContext, DeployNotification.CHANNEL_ID)
                .setContentTitle(appContext.getString(R.string.rime_daemon))
        builder.buildAction()
        builder.build().let { notificationManager.notify(id, it) }
    }

    /**
     * Restart Rime instance to deploy while keep the session
     */
    fun restarLightBookInput(fullCheck: Boolean = false) = lock.withLock {
        val id = restartId++
        if (!fullCheck) {
            sendNotification(id) {
                setSmallIcon(R.drawable.ic_baseline_sync_24)
                setContentTitle(appContext.getString(R.string.rime_daemon))
                setContentText(appContext.getString(R.string.restarting_rime))
                setOngoing(true)
                setProgress(100, 0, true)
                setPriority(NotificationCompat.PRIORITY_HIGH)
            }
        }
        realRime.finalize()
        if (!tryStarLightBookInputLocked()) {
            scheduleStartupRetry()
        }
        LightBookInputApplication.getInstance().coroutineScope.launch {
            realRime.lifecycle.whenReady {
                notificationManager.cancel(id)
            }
        }
    }

    private suspend fun handleRimeMessage(it: RimeMessage<*>) {
        if (it is RimeMessage.DeployMessage) {
            when (it.data) {
                RimeMessage.DeployMessage.State.Start -> {
                    DeployNotification.showProgress()
                    withContext(Dispatchers.IO) { subprocess("logcat", "--clear") }
                }

                RimeMessage.DeployMessage.State.Success -> {
                    DeployNotification.showSuccess()
                }

                RimeMessage.DeployMessage.State.Failure -> {
                    val intent =
                        Intent(appContext, LogActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            val log =
                                subprocess("logcat", "-v", "brief", "-s", "rime.LightBookInput:W", "-d")
                                    .readText()
                            putExtra(LogActivity.FROM_DEPLOY, true)
                            putExtra(LogActivity.DEPLOY_FAILURE_TRACE, log)
                        }
                    val pendingIntent =
                        PendingIntent.getActivity(
                            appContext,
                            0,
                            intent,
                            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE,
                        )
                    DeployNotification.showFailure(pendingIntent)
                }
            }
        }
    }
}
