package com.ekquickfind.providers.files

import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.Settings
import com.ekquickfind.core.search.Action
import com.ekquickfind.core.search.ActionHost
import com.ekquickfind.core.search.ActionOutcome
import com.ekquickfind.core.search.Glyph
import com.ekquickfind.providers.android

data class OpenFileAction(
    val uri: Uri,
    val mimeType: String?,
    override val label: String = "Open",
) : Action {
    override val glyph = Glyph.File

    override suspend fun perform(host: ActionHost): ActionOutcome {
        val view = Intent(Intent.ACTION_VIEW).setDataAndType(uri, mimeType ?: "*/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val android = host.android()
        // No default app: show the system chooser rather than failing silently.
        if (android.startActivity(view) || android.startActivity(Intent.createChooser(view, "Open with"))) return ActionOutcome.Close
        return ActionOutcome.Stay
    }
}

data class ShareFileAction(
    val uri: Uri,
    val mimeType: String?,
    override val label: String = "Share",
) : Action {
    override val glyph = Glyph.Share

    override suspend fun perform(host: ActionHost): ActionOutcome {
        val send =
            Intent(Intent.ACTION_SEND)
                .setType(mimeType ?: "*/*")
                .putExtra(Intent.EXTRA_STREAM, uri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return if (host.android().startActivity(Intent.createChooser(send, null))) ActionOutcome.Close else ActionOutcome.Stay
    }
}

/** Opens the containing folder in the system file browser, falling back to Samsung My Files, then the browser root. */
data class ShowInFolderAction(
    val relativePath: String?,
    override val label: String = "Show in folder",
) : Action {
    override val glyph = Glyph.Folder

    override suspend fun perform(host: ActionHost): ActionOutcome {
        val folder = relativePath?.trimEnd('/').orEmpty()
        val documentUri = DocumentsContract.buildDocumentUri(EXTERNAL_STORAGE_AUTHORITY, "primary:$folder")
        val candidates =
            listOf(
                Intent(Intent.ACTION_VIEW).setDataAndType(documentUri, DocumentsContract.Document.MIME_TYPE_DIR),
                Intent(SAMSUNG_MY_FILES).putExtra(SAMSUNG_START_PATH, "/storage/emulated/0/$folder"),
                Intent(
                    Intent.ACTION_VIEW,
                ).setDataAndType(
                    DocumentsContract.buildRootUri(EXTERNAL_STORAGE_AUTHORITY, "primary"),
                    DocumentsContract.Root.MIME_TYPE_ITEM,
                ),
            )
        val android = host.android()
        return if (candidates.any {
                android.startActivity(it.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
            }
        ) {
            ActionOutcome.Close
        } else {
            ActionOutcome.Stay
        }
    }

    private companion object {
        const val EXTERNAL_STORAGE_AUTHORITY = "com.android.externalstorage.documents"
        const val SAMSUNG_MY_FILES = "samsung.myfiles.intent.action.LAUNCH_MY_FILES"
        const val SAMSUNG_START_PATH = "samsung.myfiles.intent.extra.START_PATH"
    }
}

/** Opens the system "All files access" page for this app. */
data object AllFilesAccessAction : Action {
    override val label = "Allow"
    override val glyph = Glyph.Lock

    override suspend fun perform(host: ActionHost): ActionOutcome {
        val android = host.android()
        val intent =
            Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.fromParts("package", android.context.packageName, null))
        return if (android.startActivity(intent)) ActionOutcome.Stay else ActionOutcome.Stay
    }
}
