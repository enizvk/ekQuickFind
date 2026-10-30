package com.ekquickfind.app.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.ekquickfind.data.prefs.CustomSite
import com.ekquickfind.data.prefs.SiteState
import com.ekquickfind.data.prefs.UserPreferences
import com.ekquickfind.data.prefs.WeatherUnits
import com.ekquickfind.data.prefs.isValidSiteSeparator
import com.ekquickfind.providers.sites.SiteCatalog
import com.ekquickfind.providers.sites.isValidKeyword
import com.ekquickfind.providers.sites.isValidTemplate

private fun WeatherUnits.label() =
    when (this) {
        WeatherUnits.Auto -> "Auto"
        WeatherUnits.Celsius -> "°C"
        WeatherUnits.Fahrenheit -> "°F"
    }

/** Keywords in use, except [exceptId]'s, so a site can't take another one's keyword. */
internal fun keywordsInUse(
    prefs: UserPreferences,
    exceptId: String? = null,
): Set<String> {
    val builtIn =
        (SiteCatalog.linkSites.map { it.id to it.defaultKeyword } + (SiteCatalog.WEATHER_ID to SiteCatalog.WEATHER_KEYWORD))
            .filter { (id, _) -> id != exceptId && id !in prefs.removedSites }
            .map { (id, default) -> (prefs.sites[id]?.keyword ?: default).lowercase() }
    return (builtIn + prefs.customSites.filter { it.id != exceptId }.map { it.keyword.lowercase() }).toSet()
}

internal fun UserPreferences.withSite(
    id: String,
    change: (SiteState) -> SiteState,
) = copy(sites = sites + (id to change(sites[id] ?: SiteState(enabled = SiteCatalog.onByDefault(id)))))

/** Deletes a ready-made site (it keeps its settings, so restoring brings it back as it was). */
internal fun UserPreferences.withoutSite(id: String) = copy(removedSites = removedSites + id)

internal fun UserPreferences.withDefaultSites() = copy(removedSites = emptySet())

/** Site shortcuts: "yt: cats" searches YouTube straight away. Ready-made sites, your own, and how keywords are typed. */
@Composable
internal fun SearchSitesSection(
    prefs: UserPreferences,
    actions: SettingsActions,
) {
    SettingsGroup(
        "Search sites",
        Icons.Outlined.Bolt,
        more = {
            SeparatorRow(prefs.siteSeparator) { v -> actions.update { it.copy(siteSeparator = v) } }
            if (SiteCatalog.isActive(prefs, SiteCatalog.WEATHER_ID)) {
                Choice("Weather units", WeatherUnits.entries, prefs.weatherUnits, { it.label() }, { v ->
                    actions.update { it.copy(weatherUnits = v) }
                }, "weather-units")
            }
        },
    ) {
        Text(
            "Start with a keyword to search a site straight away, like “yt: cats”. Words like “weather sofia” or " +
                "“translate hello” work too.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        SiteCatalog.linkSites.filter { it.id !in prefs.removedSites }.forEach { site ->
            val state = prefs.sites[site.id]
            SiteRow(
                id = site.id,
                name = site.name,
                keyword = state?.keyword ?: site.defaultKeyword,
                enabled = SiteCatalog.isOn(prefs.sites, site.id),
                taken = keywordsInUse(prefs, exceptId = site.id),
                onKeyword = { kw -> actions.update { it.withSite(site.id) { s -> s.copy(keyword = kw) } } },
                onEnabled = { on -> actions.update { it.withSite(site.id) { s -> s.copy(enabled = on) } } },
                onDelete = { actions.update { it.withoutSite(site.id) } },
            )
        }
        if (SiteCatalog.WEATHER_ID !in prefs.removedSites) WeatherRows(prefs, actions)
        prefs.customSites.forEach { site ->
            SiteRow(
                id = site.id,
                name = site.name,
                keyword = site.keyword,
                enabled = true,
                taken = keywordsInUse(prefs, exceptId = site.id),
                onKeyword = { kw ->
                    actions.update { p -> p.copy(customSites = p.customSites.map { if (it.id == site.id) it.copy(keyword = kw) else it }) }
                },
                onEnabled = null,
                onDelete = { actions.update { p -> p.copy(customSites = p.customSites.filter { it.id != site.id }) } },
            )
        }
        if (prefs.removedSites.isNotEmpty()) {
            TextButton(
                onClick = { actions.update { it.withDefaultSites() } },
                modifier = Modifier.padding(horizontal = 8.dp).testTag("restore-sites"),
            ) { Text("Restore default sites") }
        }
        SettingsDivider()
        AddSiteForm(taken = keywordsInUse(prefs)) { site -> actions.update { it.copy(customSites = it.customSites + site) } }
    }
}

/** Optional symbols after the keyword; blank means just a space. */
@Composable
private fun SeparatorRow(
    separator: String,
    onChange: (String) -> Unit,
) {
    var text by remember(separator) { mutableStateOf(separator) }
    val valid = text.isEmpty() || isValidSiteSeparator(text)
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Keyword separator", style = MaterialTheme.typography.bodyLarge)
                InfoTip(
                    "Keyword separator",
                    "Symbols you like to put after a keyword, such as “:” for “yt: cats”. They're optional, and blank means just a space: “yt cats”.",
                )
            }
            Text(
                if (valid) "Optional, like : or -. Blank means a space: “yt cats”" else "Up to 3 symbols, no letters, digits or spaces",
                style = MaterialTheme.typography.bodySmall,
                color = if (valid) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
            )
        }
        CompactField(
            value = text,
            onValueChange = { typed ->
                text = typed.trim()
                if (text.isEmpty() || isValidSiteSeparator(text)) onChange(text)
            },
            placeholder = "space",
            isError = !valid,
            modifier = Modifier.width(64.dp).testTag("site-separator"),
        )
    }
}

@Composable
private fun WeatherRows(
    prefs: UserPreferences,
    actions: SettingsActions,
) {
    val state = prefs.sites[SiteCatalog.WEATHER_ID]
    val enabled = SiteCatalog.isOn(prefs.sites, SiteCatalog.WEATHER_ID)
    SiteRow(
        id = SiteCatalog.WEATHER_ID,
        name = "Weather",
        keyword = state?.keyword ?: SiteCatalog.WEATHER_KEYWORD,
        enabled = enabled,
        taken = keywordsInUse(prefs, exceptId = SiteCatalog.WEATHER_ID),
        onKeyword = { kw -> actions.update { it.withSite(SiteCatalog.WEATHER_ID) { s -> s.copy(keyword = kw) } } },
        onEnabled = { on -> actions.update { it.withSite(SiteCatalog.WEATHER_ID) { s -> s.copy(enabled = on) } } },
        onDelete = { actions.update { it.withoutSite(SiteCatalog.WEATHER_ID) } },
        detail = "A live card from Open-Meteo; needs the internet",
    )
    if (!enabled) return
    val context = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    val located =
        remember(tick) { context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED }
    val requester = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { tick++ }
    if (!located) {
        TextButton(
            onClick = { requester.launch(Manifest.permission.ACCESS_COARSE_LOCATION) },
            modifier = Modifier.padding(horizontal = 8.dp).testTag("weather-location"),
        ) { Text("Allow approximate location for “weather” with no place") }
    }
}

@Composable
private fun SiteRow(
    id: String,
    name: String,
    keyword: String,
    enabled: Boolean,
    taken: Set<String>,
    onKeyword: (String) -> Unit,
    onEnabled: ((Boolean) -> Unit)?,
    onDelete: (() -> Unit)? = null,
    detail: String? = null,
) {
    var text by remember(keyword) { mutableStateOf(keyword) }
    val problem = keywordProblem(text, taken)
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).testTag("site:$id"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodyLarge)
            Text(
                problem ?: detail ?: "“$keyword …”",
                style = MaterialTheme.typography.bodySmall,
                color = if (problem != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        CompactField(
            value = text,
            onValueChange = { typed ->
                text = typed.trim()
                if (keywordProblem(text, taken) == null) onKeyword(text)
            },
            isError = problem != null,
            modifier = Modifier.width(64.dp).testTag("site-keyword:$id"),
        )
        onEnabled?.let { Checkbox(checked = enabled, onCheckedChange = it, modifier = Modifier.testTag("site-switch:$id")) }
        onDelete?.let {
            IconButton(onClick = it, modifier = Modifier.testTag("site-delete:$id")) {
                Icon(Icons.Outlined.Delete, contentDescription = "Delete $name")
            }
        }
    }
}

/** Why a keyword can't be used, or null when it's fine. */
internal fun keywordProblem(
    keyword: String,
    taken: Set<String>,
): String? =
    when {
        !isValidKeyword(keyword) -> "Use 1 to 12 letters or digits"
        keyword.lowercase() in taken -> "Another site uses “$keyword”"
        else -> null
    }

@Composable
private fun AddSiteForm(
    taken: Set<String>,
    onAdd: (CustomSite) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var keyword by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    val keywordIssue = keyword.takeIf { it.isNotEmpty() }?.let { keywordProblem(it, taken) }
    val urlIssue = url.takeIf { it.isNotEmpty() && !isValidTemplate(it) }?.let { "An https:// address with %s where the search goes" }
    val ready = name.isNotBlank() && keyword.isNotEmpty() && keywordIssue == null && url.isNotEmpty() && urlIssue == null
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Add your own site", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CompactField(name, { name = it }, Modifier.weight(1f).testTag("add-site-name"), placeholder = "Name")
            CompactField(
                keyword,
                { keyword = it.trim() },
                Modifier.width(88.dp).testTag("add-site-keyword"),
                "Keyword",
                keywordIssue != null,
            )
        }
        CompactField(
            url,
            { url = it.trim() },
            Modifier.fillMaxWidth().testTag("add-site-url"),
            placeholder = "https://example.com/search?q=%s",
            isError = urlIssue != null,
        )
        (keywordIssue ?: urlIssue)?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
        FilledTonalButton(
            onClick = {
                onAdd(CustomSite("custom-${System.currentTimeMillis()}", name.trim(), keyword, url))
                name = ""
                keyword = ""
                url = ""
            },
            enabled = ready,
            modifier = Modifier.testTag("add-site"),
        ) { Text("Add site") }
    }
}
