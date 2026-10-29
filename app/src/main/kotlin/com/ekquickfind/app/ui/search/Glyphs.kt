package com.ekquickfind.app.ui.search

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.ToggleOn
import androidx.compose.material.icons.outlined.TravelExplore
import androidx.compose.material.icons.outlined.VideoFile
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.ui.graphics.vector.ImageVector
import com.ekquickfind.core.search.Glyph

fun Glyph.vector(): ImageVector =
    when (this) {
        Glyph.Calculator -> Icons.Outlined.Calculate
        Glyph.Convert -> Icons.Outlined.SwapHoriz
        Glyph.Settings -> Icons.Outlined.Settings
        Glyph.Toggle -> Icons.Outlined.ToggleOn
        Glyph.Web -> Icons.Outlined.TravelExplore
        Glyph.Search -> Icons.Outlined.Search
        Glyph.Suggestion -> Icons.Outlined.Search
        Glyph.Contact -> Icons.Outlined.Person
        Glyph.Call -> Icons.Outlined.Call
        Glyph.Message -> Icons.AutoMirrored.Outlined.Message
        Glyph.File -> Icons.AutoMirrored.Outlined.InsertDriveFile
        Glyph.Image -> Icons.Outlined.Image
        Glyph.Video -> Icons.Outlined.VideoFile
        Glyph.Audio -> Icons.Outlined.AudioFile
        Glyph.Document -> Icons.Outlined.Description
        Glyph.Folder -> Icons.Outlined.Folder
        Glyph.Share -> Icons.Outlined.Share
        Glyph.Info -> Icons.Outlined.Info
        Glyph.Uninstall -> Icons.Outlined.Delete
        Glyph.Hide -> Icons.Outlined.VisibilityOff
        Glyph.Pin -> Icons.Outlined.PushPin
        Glyph.Unpin -> Icons.Outlined.PushPin
        Glyph.Shortcut -> Icons.AutoMirrored.Outlined.OpenInNew
        Glyph.Lock -> Icons.Outlined.Lock
        Glyph.Flashlight -> Icons.Outlined.FlashlightOn
        Glyph.History -> Icons.Outlined.History
        Glyph.Chat -> Icons.Outlined.AutoAwesome
    }
