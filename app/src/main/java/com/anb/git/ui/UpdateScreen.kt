package com.anb.git.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

private data class ReleaseInfo(
    val versionName: String,
    val versionCode: Int,
    val apkUrl: String,
    val sha256: String
)

@Composable
internal fun InAppUpdateSection() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("Check GitHub for the latest official release.") }
    var release by remember { mutableStateOf<ReleaseInfo?>(null) }
    var downloaded by remember { mutableStateOf<File?>(null) }
    var busy by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var error by remember { mutableStateOf<String?>(null) }

    fun checkForUpdate() {
        if (busy) return
        busy = true
        error = null
        status = "Checking official GitHub release…"
        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) { fetchLatestRelease() }
                release = result
                downloaded = null
                if (result.versionCode <= com.anb.git.BuildConfig.VERSION_CODE) {
                    status = "You are using the latest version (${com.anb.git.BuildConfig.VERSION_NAME})."
                } else {
                    status = "Update available: ${result.versionName} (version code ${result.versionCode})."
                }
            } catch (e: Exception) {
                error = e.message ?: "Could not check for updates."
                status = "Update check failed."
            } finally {
                busy = false
            }
        }
    }

    fun downloadUpdate(info: ReleaseInfo) {
        if (busy) return
        busy = true
        error = null
        progress = 0f
        status = "Downloading update…"
        scope.launch {
            try {
                val file = withContext(Dispatchers.IO) {
                    downloadAndVerify(context.cacheDir, info) { value ->
                        scope.launch(Dispatchers.Main) { progress = value }
                    }
                }
                downloaded = file
                status = "Download complete. APK checksum verified."
            } catch (e: Exception) {
                error = e.message ?: "Download failed."
                status = "Update download failed."
            } finally {
                busy = false
            }
        }
    }

    fun installUpdate(file: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            val settingsIntent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(settingsIntent)
            status = "Allow installs from ANB Git, return here, then tap Install again."
            return
        }
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            status = "Android package installer opened. Confirm installation there."
        } catch (e: Exception) {
            error = e.message ?: "Could not open Android installer."
        }
    }

    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("App Updates", style = MaterialTheme.typography.titleLarge)
            Text("Current version: ${com.anb.git.BuildConfig.VERSION_NAME} (${com.anb.git.BuildConfig.VERSION_CODE})")
            Text(status, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (busy && status.startsWith("Downloading")) {
                LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth())
                Text("${(progress * 100).toInt()}%")
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    enabled = !busy,
                    onClick = { checkForUpdate() }
                ) { Text("Check for Updates") }
                val info = release
                if (info != null && info.versionCode > com.anb.git.BuildConfig.VERSION_CODE && downloaded == null) {
                    Button(enabled = !busy, onClick = { downloadUpdate(info) }) {
                        Text("Download")
                    }
                }
                downloaded?.let { file ->
                    Button(enabled = !busy, onClick = { installUpdate(file) }) {
                        Text("Install")
                    }
                }
            }
            Text(
                "Updates are downloaded from the official ANB-Git GitHub Releases. Android will ask you to confirm installation.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun fetchLatestRelease(): ReleaseInfo {
    val connection = (URL("https://api.github.com/repos/anbsolution/ANB-Git/releases/latest")
        .openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 15000
        readTimeout = 15000
        setRequestProperty("Accept", "application/vnd.github+json")
        setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
        setRequestProperty("User-Agent", "ANB-Git-Android-Updater")
    }
    try {
        val code = connection.responseCode
        if (code != HttpURLConnection.HTTP_OK) {
            throw IllegalStateException(
                if (code == 404) "No official GitHub Release has been published yet."
                else "GitHub returned HTTP $code."
            )
        }
        val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        val body = json.optString("body", "")
        val versionCode = Regex("(?m)^versionCode=(\\d+)\\s*$")
            .find(body)?.groupValues?.get(1)?.toIntOrNull()
            ?: throw IllegalStateException("Release metadata is missing versionCode.")
        val versionName = Regex("(?m)^versionName=(\\S+)\\s*$")
            .find(body)?.groupValues?.get(1)
            ?: json.optString("tag_name").removePrefix("v")
        val checksum = Regex("(?m)^sha256=([a-fA-F0-9]{64})\\s*$")
            .find(body)?.groupValues?.get(1)?.lowercase()
            ?: throw IllegalStateException("Release metadata is missing its SHA-256 checksum.")
        val assets = json.getJSONArray("assets")
        var apkUrl: String? = null
        for (i in 0 until assets.length()) {
            val asset = assets.getJSONObject(i)
            if (asset.optString("name").endsWith(".apk", ignoreCase = true)) {
                apkUrl = asset.optString("browser_download_url")
                break
            }
        }
        val verifiedUrl = apkUrl ?: throw IllegalStateException("Release does not contain an APK asset.")
        val parsedUrl = URL(verifiedUrl)
        if (parsedUrl.protocol != "https" || parsedUrl.host != "github.com") {
            throw SecurityException("Release APK URL is not an official HTTPS GitHub URL.")
        }
        return ReleaseInfo(
            versionName = versionName,
            versionCode = versionCode,
            apkUrl = verifiedUrl,
            sha256 = checksum
        )
    } finally {
        connection.disconnect()
    }
}

private fun downloadAndVerify(
    cacheDir: File,
    info: ReleaseInfo,
    onProgress: (Float) -> Unit
): File {
    val connection = (URL(info.apkUrl).openConnection() as HttpURLConnection).apply {
        connectTimeout = 20000
        readTimeout = 30000
        instanceFollowRedirects = true
        setRequestProperty("User-Agent", "ANB-Git-Android-Updater")
    }
    val updateDir = File(cacheDir, "anb-git-updates")
    if (!updateDir.exists() && !updateDir.mkdirs()) {
        throw IllegalStateException("Could not create private update cache.")
    }
    val target = File(updateDir, "anb-git-update.apk")
    val temporary = File(updateDir, "anb-git-update.apk.part")
    var completed = false
    try {
        val code = connection.responseCode
        if (code !in 200..299) throw IllegalStateException("APK download returned HTTP $code.")
        val total = connection.contentLengthLong
        connection.inputStream.use { input ->
            temporary.outputStream().buffered().use { output ->
                val buffer = ByteArray(64 * 1024)
                var received = 0L
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    output.write(buffer, 0, count)
                    received += count
                    if (total > 0) onProgress((received.toFloat() / total).coerceIn(0f, 1f))
                }
            }
        }
        val digest = MessageDigest.getInstance("SHA-256")
        temporary.inputStream().buffered().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        val actual = digest.digest().joinToString("") { "%02x".format(it) }
        if (!actual.equals(info.sha256, ignoreCase = true)) {
            temporary.delete()
            throw SecurityException("APK SHA-256 verification failed. The file was rejected.")
        }
        if (target.exists()) target.delete()
        if (!temporary.renameTo(target)) throw IllegalStateException("Could not finalize downloaded APK.")
        onProgress(1f)
        completed = true
        return target
    } finally {
        if (!completed) temporary.delete()
        connection.disconnect()
    }
}
