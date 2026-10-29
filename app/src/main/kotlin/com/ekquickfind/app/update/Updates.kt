package com.ekquickfind.app.update

import android.content.Intent
import android.content.pm.PackageInstaller
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed interface UpdateState {
    data object NotConfigured : UpdateState

    data object Idle : UpdateState

    data object Checking : UpdateState

    data object UpToDate : UpdateState

    data class Available(
        val release: Release,
    ) : UpdateState

    data class Downloading(
        val release: Release,
        val progress: Float,
    ) : UpdateState

    data class Installing(
        val release: Release,
    ) : UpdateState

    /** Android wants the owner to confirm; [confirm] opens its dialog. */
    data class NeedsConfirmation(
        val release: Release?,
        val confirm: Intent,
    ) : UpdateState

    data class Failed(
        val reason: String,
        val release: Release?,
    ) : UpdateState
}

/** Check, download and install, with one shared state for the settings panel, the worker and the receiver. */
class Updates(
    private val checker: UpdateChecker,
    private val downloader: UpdateDownloader,
    private val installer: UpdateInstaller,
) {
    private val _state = MutableStateFlow(if (checker.isConfigured) UpdateState.Idle else UpdateState.NotConfigured)
    val state: StateFlow<UpdateState> = _state.asStateFlow()
    private val busy = Mutex()

    /** Set while a search or settings screen is on screen, so the install confirmation can open right away. */
    @Volatile var uiVisible = false

    val isConfigured: Boolean get() = checker.isConfigured

    fun canInstall(): Boolean = installer.canInstall()

    suspend fun check(): UpdateCheck =
        busy.withLock {
            _state.value = UpdateState.Checking
            checker.check().also { result ->
                _state.value =
                    when (result) {
                        UpdateCheck.NotConfigured -> UpdateState.NotConfigured
                        UpdateCheck.UpToDate -> UpdateState.UpToDate
                        is UpdateCheck.Available -> UpdateState.Available(result.release)
                        is UpdateCheck.Failed -> UpdateState.Failed(result.reason, null)
                    }
            }
        }

    suspend fun install(release: Release) =
        busy.withLock {
            if (!installer.canInstall()) {
                _state.value = UpdateState.Failed("Allow ekQuickFind to install updates first", release)
                return@withLock
            }
            try {
                _state.value = UpdateState.Downloading(release, 0f)
                val apk = downloader.download(release) { _state.value = UpdateState.Downloading(release, it) }
                _state.value = UpdateState.Installing(release)
                installer.install(apk)
            } catch (e: UpdateException) {
                _state.value = UpdateState.Failed(e.message.orEmpty(), release)
            }
        }

    /** Called by [UpdateStatusReceiver] with the installer's answer. */
    fun onInstallStatus(
        status: Int,
        message: String?,
        confirm: Intent?,
    ) {
        val release = (_state.value as? UpdateState.Installing)?.release
        _state.value =
            when {
                status == PackageInstaller.STATUS_PENDING_USER_ACTION && confirm != null -> UpdateState.NeedsConfirmation(release, confirm)
                // On success Android restarts the app as the new version; nothing to show.
                status == PackageInstaller.STATUS_SUCCESS -> UpdateState.Idle
                status == PackageInstaller.STATUS_FAILURE_ABORTED -> UpdateState.Failed("Install was cancelled", release)
                else -> UpdateState.Failed(message ?: "Install failed", release)
            }
    }
}
