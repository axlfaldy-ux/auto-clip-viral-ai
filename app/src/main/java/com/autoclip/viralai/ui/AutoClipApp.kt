package com.autoclip.viralai.ui

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.autoclip.viralai.ui.pages.ClipResultsPage
import com.autoclip.viralai.ui.pages.HomePage
import com.autoclip.viralai.ui.pages.ImportVideoPage
import com.autoclip.viralai.ui.pages.SettingsPage
import com.autoclip.viralai.ui.pages.metadata.MetadataStudioPage
import com.autoclip.viralai.ui.pages.SubtitleStudioPage

private enum class Destination(val label: String, val symbol: String) {
    Home("Home", "⌂"), Results("Klip", "▣"), Studio("Subtitle", "Aa"), Metadata("Meta", "✎"), Settings("Setelan", "⚙"), Import("Impor", "＋")
}

@Composable
fun AutoClipApp() {
    val context = LocalContext.current
    val projectPreferences = remember { context.getSharedPreferences("clip_project", 0) }
    var destinationName by rememberSaveable { mutableStateOf(Destination.Home.name) }
    var selectedTemplate by rememberSaveable {
        mutableStateOf(projectPreferences.getString("subtitle_template", "Karaoke") ?: "Karaoke")
    }
    var importedUri by rememberSaveable { mutableStateOf(projectPreferences.getString("import_uri", null)) }
    var importedName by rememberSaveable { mutableStateOf(projectPreferences.getString("import_name", null)) }
    val destination = Destination.valueOf(destinationName)

    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            importedUri = uri.toString()
            projectPreferences.edit().putString("import_uri", importedUri).apply()
            importedName = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (column >= 0 && cursor.moveToFirst()) cursor.getString(column) else null
            } ?: uri.lastPathSegment
            projectPreferences.edit().putString("import_name", importedName).apply()
            destinationName = Destination.Import.name
        }
    }
    val chooseVideo = { videoPicker.launch(arrayOf("video/*")) }

    Scaffold(bottomBar = {
        if (destination != Destination.Import) {
            NavigationBar {
                listOf(Destination.Home, Destination.Results, Destination.Studio, Destination.Metadata, Destination.Settings).forEach { item ->
                    NavigationBarItem(
                        selected = destination == item,
                        onClick = { destinationName = item.name },
                        icon = { Text(item.symbol) },
                        label = { Text(item.label) },
                    )
                }
            }
        }
    }) { insets ->
        when (destination) {
            Destination.Home -> HomePage(importedName, chooseVideo, { destinationName = Destination.Results.name }, Modifier.padding(insets))
            Destination.Import -> ImportVideoPage(
                importedName, importedUri,
                onBack = { destinationName = Destination.Home.name },
                onPickVideo = chooseVideo,
                onOpenResults = { destinationName = Destination.Results.name },
                modifier = Modifier.padding(insets),
            )
            Destination.Results -> ClipResultsPage(importedName, chooseVideo, Modifier.padding(insets))
            Destination.Studio -> SubtitleStudioPage(
                selectedTemplate, {
                    selectedTemplate = it
                    projectPreferences.edit().putString("subtitle_template", it).apply()
                }, Modifier.padding(insets),
            )
            Destination.Metadata -> MetadataStudioPage(metadataScope(importedUri), importedName, Modifier.padding(insets))
            Destination.Settings -> SettingsPage(Modifier.padding(insets))
        }
    }
}


private fun metadataScope(sourceUri: String?): String {
    if (sourceUri == null) return "draft-transcript"
    val digest = java.security.MessageDigest.getInstance("SHA-256")
        .digest(sourceUri.toByteArray())
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }
    return "source-$digest"
}
