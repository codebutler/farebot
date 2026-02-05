package com.codebutler.farebot.app.core.platform

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.codebutler.farebot.app.feature.bg.BackgroundTagActivity
import com.codebutler.farebot.shared.platform.PlatformActions

class AndroidPlatformActions(
    private val context: Context,
) : PlatformActions {

    private var filePickerCallback: ((String?) -> Unit)? = null
    private var filePickerLauncher: ActivityResultLauncher<Intent>? = null
    private var bytesPickerCallback: ((ByteArray?) -> Unit)? = null
    private var bytesPickerLauncher: ActivityResultLauncher<Intent>? = null

    fun registerFilePickerLauncher(activity: ComponentActivity) {
        filePickerLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val callback = filePickerCallback
            filePickerCallback = null
            if (result.resultCode == Activity.RESULT_OK) {
                val uri = result.data?.data
                if (uri != null) {
                    val text = context.contentResolver.openInputStream(uri)
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
        bytesPickerLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val callback = bytesPickerCallback
            bytesPickerCallback = null
            if (result.resultCode == Activity.RESULT_OK) {
                val uri = result.data?.data
                if (uri != null) {
                    val bytes = context.contentResolver.openInputStream(uri)
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

    override fun openNfcSettings() {
        val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    override fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("FareBot", text))
    }

    override fun getClipboardText(): String? {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        return clipboard.primaryClip?.getItemAt(0)?.text?.toString()
    }

    override fun shareText(text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Share").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
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
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
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
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        launcher.launch(intent)
    }

    override fun saveFileForExport(content: String, defaultFileName: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_TEXT, content)
            putExtra(Intent.EXTRA_SUBJECT, defaultFileName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Save").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    override fun supportsLaunchFromBackground(): Boolean = true

    override fun isLaunchFromBackgroundEnabled(): Boolean {
        val componentName = ComponentName(context, BackgroundTagActivity::class.java)
        val state = context.packageManager.getComponentEnabledSetting(componentName)
        return state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
    }

    override fun setLaunchFromBackgroundEnabled(enabled: Boolean) {
        val componentName = ComponentName(context, BackgroundTagActivity::class.java)
        val newState = if (enabled) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        context.packageManager.setComponentEnabledSetting(
            componentName,
            newState,
            PackageManager.DONT_KILL_APP,
        )
    }
}
