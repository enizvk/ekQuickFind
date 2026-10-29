package com.ekquickfind.app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.ekquickfind.app.settings.ExcludableApp
import com.ekquickfind.app.ui.search.ResultIconView

/** Every app with a switch to keep it out of search. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExcludedAppsScreen(
    apps: List<ExcludableApp>,
    filter: String,
    onFilter: (String) -> Unit,
    onExclude: (ExcludableApp, Boolean) -> Unit,
    onBack: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize().testTag("excluded-apps")) {
        Column(Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
            TopAppBar(
                title = { Text("Excluded apps") },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                    ) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                windowInsets = WindowInsets(0),
            )
            OutlinedTextField(
                value = filter,
                onValueChange = onFilter,
                placeholder = { Text("Find an app") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).testTag("exclude-filter"),
            )
            Text(
                "Excluded apps never appear in search results.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 24.dp, top = 12.dp, bottom = 4.dp),
            )
            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                items(apps, key = { it.id }) { app ->
                    ListItem(
                        headlineContent = { Text(app.label) },
                        supportingContent = app.badge?.let { { Text(it) } },
                        leadingContent = { ResultIconView(app.icon, 36.dp) },
                        trailingContent = {
                            Switch(
                                checked = app.excluded,
                                onCheckedChange = { onExclude(app, it) },
                                modifier = Modifier.testTag("exclude:${app.label}"),
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }
}
