package com.ekquickfind.app.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.ekquickfind.data.prefs.Translator
import com.ekquickfind.data.prefs.UserPreferences
import com.ekquickfind.providers.sites.SiteCatalog
import com.ekquickfind.providers.translate.TranslateLanguages
import com.ekquickfind.providers.translate.languageName
import java.util.Locale

internal data class TranslateLanguageOption(
    val code: String,
    val name: String,
)

/** "" is the phone's language. */
internal fun translateLanguageName(
    code: String,
    locale: Locale,
) = if (code.isEmpty()) "Phone language (${languageName(locale.language, locale)})" else languageName(code, locale)

/** The phone's language first, then the rest by name. */
internal fun translateLanguageOptions(
    codes: List<String>,
    locale: Locale,
): List<TranslateLanguageOption> =
    listOf(TranslateLanguageOption("", translateLanguageName("", locale))) +
        codes.map { TranslateLanguageOption(it, languageName(it, locale)) }.sortedBy { it.name }

/** Its own panel: show translations in the results, into which language, and online or on the phone. */
@Composable
internal fun TranslationSection(
    prefs: UserPreferences,
    actions: SettingsActions,
) {
    SettingsGroup("Translation", Icons.Outlined.Translate) {
        if (!SiteCatalog.isActive(prefs, SiteCatalog.Translate.id)) {
            // The card uses Google Translate's keyword, so it needs that site.
            Text(
                "Uses the Google Translate keyword, like “tr hello”. Turn on (or restore) Google Translate in Search sites first.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            return@SettingsGroup
        }
        SwitchRow(
            "Show translation in results",
            "“tr hello” shows the translation in its own group",
            prefs.translateInline,
            { v -> actions.update { it.copy(translateInline = v) } },
            "translate-inline",
        )
        if (!prefs.translateInline) return@SettingsGroup
        LanguagePicker(prefs.translateTo) { code -> actions.update { it.copy(translateTo = code) } }
        Choice(
            "Translator",
            Translator.entries,
            prefs.translator,
            { if (it == Translator.Google) "Google (online)" else "On this phone" },
            { v -> actions.update { it.copy(translator = v) } },
            "translator",
            info =
                "Google sends the text after “tr” to Google's web translator. On this phone works offline after a one-time " +
                    "download of about 30 MB per language.",
        )
    }
}

@Composable
private fun LanguagePicker(
    selected: String,
    onSelect: (String) -> Unit,
) {
    val locale = Locale.getDefault()
    val options = remember(locale) { translateLanguageOptions(TranslateLanguages.codes, locale) }
    var open by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Translate to",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Box {
            TextButton(onClick = { open = true }, modifier = Modifier.testTag("translate-to")) {
                Text(translateLanguageName(selected, locale))
                Icon(Icons.Outlined.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.name) },
                        onClick = {
                            open = false
                            onSelect(option.code)
                        },
                        modifier = Modifier.testTag("translate-to:${option.code}"),
                    )
                }
            }
        }
    }
}
