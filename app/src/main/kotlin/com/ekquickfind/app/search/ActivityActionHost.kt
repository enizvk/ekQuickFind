package com.ekquickfind.app.search

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import com.ekquickfind.core.search.ResultId
import com.ekquickfind.core.search.SearchResult
import com.ekquickfind.providers.AndroidActionHost
import kotlinx.coroutines.CompletableDeferred

/** A pending "pick one" question shown as a dialog by the activity. */
class ChoiceRequest(
    val title: String,
    val options: List<String>,
    val answer: CompletableDeferred<Int?>,
)

/** Bridges provider actions to the activity. Must be created before the activity is started (it registers a result launcher). */
class ActivityActionHost(
    private val activity: ComponentActivity,
    private val onHide: suspend (ResultId) -> Unit,
    private val onPin: suspend (SearchResult) -> Unit = {},
    private val onUnpin: suspend (SearchResult) -> Unit = {},
) : AndroidActionHost {
    override val context: Context get() = activity

    /** Observed by the activity to show the chooser dialog. */
    val choice = mutableStateOf<ChoiceRequest?>(null)

    private val askedBefore = activity.getSharedPreferences("permissions", Context.MODE_PRIVATE)

    private var pendingPermissions: CompletableDeferred<Boolean>? = null
    private val permissionLauncher =
        activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
            pendingPermissions?.complete(grants.values.all { it })
            pendingPermissions = null
        }

    private var pendingResult: CompletableDeferred<Int>? = null
    private val resultLauncher =
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            pendingResult?.complete(result.resultCode)
            pendingResult = null
        }

    override suspend fun startForResult(intent: Intent): Int? {
        val deferred = CompletableDeferred<Int>()
        pendingResult = deferred
        return try {
            resultLauncher.launch(intent)
            deferred.await()
        } catch (_: ActivityNotFoundException) {
            null
        } catch (_: SecurityException) {
            null
        }
    }

    override fun startActivity(intent: Intent): Boolean =
        try {
            activity.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        }

    override suspend fun requestPermissions(permissions: List<String>): Boolean {
        // Asked before and no rationale means "don't ask again": the dialog won't show, so open app settings.
        val permanentlyDenied = permissions.all { askedBefore.getBoolean(it, false) && !shouldShowRationale(it) }
        if (permanentlyDenied) {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", activity.packageName, null)))
            return false
        }
        askedBefore.edit().apply { permissions.forEach { putBoolean(it, true) } }.apply()
        val deferred = CompletableDeferred<Boolean>()
        pendingPermissions = deferred
        permissionLauncher.launch(permissions.toTypedArray())
        return deferred.await()
    }

    override fun shouldShowRationale(permission: String) = activity.shouldShowRequestPermissionRationale(permission)

    override suspend fun copyToClipboard(
        label: String,
        text: String,
    ) {
        // Android 13+ shows its own "copied" confirmation.
        activity.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText(label, text))
    }

    override suspend fun pin(result: SearchResult) = onPin(result)

    override suspend fun unpin(result: SearchResult) = onUnpin(result)

    override suspend fun hide(result: ResultId) = onHide(result)

    override suspend fun choose(
        title: String,
        options: List<String>,
    ): Int? {
        val request = ChoiceRequest(title, options, CompletableDeferred())
        choice.value = request
        return try {
            request.answer.await()
        } finally {
            choice.value = null
        }
    }
}
