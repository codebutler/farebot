package com.codebutler.farebot.app.core.platform

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import com.codebutler.farebot.shared.platform.PlatformActions
import java.io.File

class AndroidPlatformActions(
    private val context: Context,
) : PlatformActions {
    private var filePickerCallback: ((String?) -> Unit)? = null
    private var filePickerLauncher: ActivityResultLauncher<Intent>? = null
    private var bytesPickerCallback: ((ByteArray?) -> Unit)? = null
    private var bytesPickerLauncher: ActivityResultLauncher<Intent>? = null

    fun registerFilePickerLauncher(activity: ComponentActivity) {
        filePickerLauncher =
            activity.registerForActivityResult(
                ActivityResultContracts.StartActivityForResult(),
            ) { result ->
                val callback = filePickerCallback
                filePickerCallback = null
                if (result.resultCode == Activity.RESULT_OK) {
                    val uri = result.data?.data
                    if (uri != null) {
                        val text =
                            context.contentResolver
                                .openInputStream(uri)
                                ?.bufferedReader()
                                ?.use { it.readText() }
                        callback?.invoke(text)
                    } else {
                        callback?.invoke(null)
                    }
                } else {
                    callback?.invoke(null)
                }
            }
        bytesPickerLauncher =
            activity.registerForActivityResult(
                ActivityResultContracts.StartActivityForResult(),
            ) { result ->
                val callback = bytesPickerCallback
                bytesPickerCallback = null
                if (result.resultCode == Activity.RESULT_OK) {
                    val uri = result.data?.data
                    if (uri != null) {
                        val bytes =
                            context.contentResolver
                                .openInputStream(uri)
                                ?.use { it.readBytes() }
                        callback?.invoke(bytes)
                    } else {
                        callback?.invoke(null)
                    }
                } else {
                    callback?.invoke(null)
                }
            }
    }

    override fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    override val openNfcSettings: (() -> Unit) = {
        val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    override fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("FareBot", text))
    }

    override fun shareFile(
        content: String,
        fileName: String,
        mimeType: String,
    ) {
        val sharedDir = File(context.cacheDir, "shared")
        sharedDir.mkdirs()
        val file = File(sharedDir, fileName)
        file.writeText(content)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent =
            Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        context.startActivity(
            Intent.createChooser(intent, "Share").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    }

    override fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun pickFileForImport(onResult: (String?) -> Unit) {
        val launcher = filePickerLauncher
        if (launcher == null) {
            onResult(null)
            return
        }
        filePickerCallback = onResult
        val intent =
            Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/json"
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/json", "text/plain", "text/*"))
            }
        launcher.launch(intent)
    }

    override fun updateAppTimestamp() {
        // Update last-used timestamp for the app
        val prefs = context.getSharedPreferences("farebot", Context.MODE_PRIVATE)
        prefs.edit().putLong("last_used", System.currentTimeMillis()).apply()
    }

    override fun pickFileForBytes(onResult: (ByteArray?) -> Unit) {
        val launcher = bytesPickerLauncher
        if (launcher == null) {
            onResult(null)
            return
        }
        bytesPickerCallback = onResult
        val intent =
            Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
            }
        launcher.launch(intent)
    }
}
