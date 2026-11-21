package com.autoclip.viralai.ui

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
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
import com.autoclip.viralai.domain.subtitles.SubtitleStyle
import com.autoclip.viralai.domain.subtitles.TalkingHeadAnimation

private enum class Destination(val label: String, val symbol: String) {
    Home("Home", "⌂"), Results("Klip", "▣"), Studio("Subtitle", "Aa"), Metadata("Meta", "✎"), Settings("Setelan", "⚙"), Import("Impor", "＋")
}

@Composable
fun AutoClipApp() {
    val context = LocalContext.current
    val projectPreferences = remember { context.getSharedPreferences("clip_project", 0) }
    var destinationName by rememberSaveable { mutableStateOf(Destination.Home.name) }
    var selectedTemplate by rememberSaveable {
        mutableStateOf(projectPreferences.getString("subtitle_template", "Classic") ?: "Classic")
    }
    var cameraAnimationName by rememberSaveable {
        mutableStateOf(projectPreferences.getString("camera_animation", TalkingHeadAnimation.SUBTLE_CAMERA.name) ?: TalkingHeadAnimation.SUBTLE_CAMERA.name)
    }
    val cameraAnimation = TalkingHeadAnimation.entries.firstOrNull { it.name == cameraAnimationName } ?: TalkingHeadAnimation.SUBTLE_CAMERA
    var subtitleSize by rememberSaveable { mutableStateOf(projectPreferences.getFloat("subtitle_size", 0.046f)) }
    var subtitleTextColor by rememberSaveable { mutableStateOf(projectPreferences.getLong("subtitle_text_color", 0xFFFFFFFFL)) }
    var subtitleOutlineColor by rememberSaveable { mutableStateOf(projectPreferences.getLong("subtitle_outline_color", 0xFF000000)) }
    var subtitleOutlineWidth by rememberSaveable { mutableStateOf(projectPreferences.getFloat("subtitle_outline_width", 0.0025f)) }
    var subtitleShadow by rememberSaveable { mutableStateOf(projectPreferences.getFloat("subtitle_shadow", 0.004f)) }
    var subtitlePosition by rememberSaveable { mutableStateOf(projectPreferences.getFloat("subtitle_position", 0.84f)) }
    val subtitleStyle = SubtitleStyle(subtitleSize, subtitleTextColor, subtitleOutlineColor, subtitleOutlineWidth, subtitleShadow, subtitlePosition)
    var importedUri by rememberSaveable { mutableStateOf(projectPreferences.getString("import_uri", null)) }
    var importedName by rememberSaveable { mutableStateOf(projectPreferences.getString("import_name", null)) }
    var apiEndpoint by rememberSaveable { mutableStateOf(projectPreferences.getString("api_endpoint", "") ?: "") }
    var importReturnDestination by rememberSaveable { mutableStateOf(Destination.Home.name) }
    val destination = Destination.valueOf(destinationName)
    BackHandler(enabled = destination == Destination.Import) { destinationName = importReturnDestination }

    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            val persistGranted = runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }.isSuccess
            importedUri = uri.toString()
            projectPreferences.edit().putString("import_uri", importedUri).apply()
            importedName = runCatching {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (column >= 0 && cursor.moveToFirst()) cursor.getString(column) else null
                }
            }.getOrNull() ?: uri.lastPathSegment
            projectPreferences.edit().putString("import_name", importedName).apply()
            if (!persistGranted) projectPreferences.edit().remove("import_uri").apply()
            destinationName = Destination.Import.name
        }
    }
    val chooseVideo = {
        if (destination != Destination.Import) importReturnDestination = destinationName
        videoPicker.launch(arrayOf("video/*"))
    }

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
                onBack = { destinationName = importReturnDestination },
                onPickVideo = chooseVideo,
                onOpenResults = { destinationName = Destination.Results.name },
                modifier = Modifier.padding(insets),
            )
            Destination.Results -> ClipResultsPage(importedName, importedUri, apiEndpoint, selectedTemplate, subtitleStyle, cameraAnimation, chooseVideo, Modifier.padding(insets))
            Destination.Studio -> SubtitleStudioPage(
                selectedTemplate,
                cameraAnimation,
                subtitleStyle,
                { style ->
                    subtitleSize = style.sizeFraction
                    subtitleTextColor = style.textArgb
                    subtitleOutlineColor = style.outlineArgb
                    subtitleOutlineWidth = style.outlineFraction
                    subtitleShadow = style.shadowFraction
                    subtitlePosition = style.positionFraction
                    projectPreferences.edit()
                        .putFloat("subtitle_size", style.sizeFraction)
                        .putLong("subtitle_text_color", style.textArgb)
                        .putLong("subtitle_outline_color", style.outlineArgb)
                        .putFloat("subtitle_outline_width", style.outlineFraction)
                        .putFloat("subtitle_shadow", style.shadowFraction)
                        .putFloat("subtitle_position", style.positionFraction)
                        .apply()
                },
                { animation ->
                    cameraAnimationName = animation.name
                    projectPreferences.edit().putString("camera_animation", animation.name).apply()
                },
                {
                    selectedTemplate = it
                    projectPreferences.edit().putString("subtitle_template", it).apply()
                }, Modifier.padding(insets),
            )
            Destination.Metadata -> MetadataStudioPage(metadataScope(importedUri), importedName, Modifier.padding(insets))
            Destination.Settings -> SettingsPage(
                apiEndpoint,
                { apiEndpoint = it; projectPreferences.edit().putString("api_endpoint", it.trim()).apply() },
                Modifier.padding(insets),
            )
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
