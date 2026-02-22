package com.codebutler.farebot.desktop

import com.codebutler.farebot.shared.platform.PlatformActions
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File
import java.net.URI
import javax.swing.JFileChooser
import javax.swing.JOptionPane
import javax.swing.filechooser.FileNameExtensionFilter

class DesktopPlatformActions : PlatformActions {
    override fun openUrl(url: String) {
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(URI(url))
        }
    }

    // openNfcSettings not overridden — no NFC settings on desktop

    override fun copyToClipboard(text: String) {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(StringSelection(text), null)
    }

    override fun shareFile(
        content: String,
        fileName: String,
        mimeType: String,
    ) {
        val chooser =
            JFileChooser().apply {
                selectedFile = File(fileName)
                dialogTitle = "Export Card"
            }
        val result = chooser.showSaveDialog(null)
        if (result == JFileChooser.APPROVE_OPTION) {
            chooser.selectedFile.writeText(content)
        }
    }

    override fun showToast(message: String) {
        JOptionPane.showMessageDialog(null, message, "FareBot", JOptionPane.INFORMATION_MESSAGE)
    }

    override fun pickFileForImport(onResult: (String?) -> Unit) {
        val chooser =
            JFileChooser().apply {
                fileFilter = FileNameExtensionFilter("JSON files", "json")
                dialogTitle = "Import Card"
            }
        val result = chooser.showOpenDialog(null)
        if (result == JFileChooser.APPROVE_OPTION) {
            onResult(chooser.selectedFile.readText())
        } else {
            onResult(null)
        }
    }

    override fun pickFileForBytes(onResult: (ByteArray?) -> Unit) {
        val chooser =
            JFileChooser().apply {
                dialogTitle = "Select File"
            }
        val result = chooser.showOpenDialog(null)
        if (result == JFileChooser.APPROVE_OPTION) {
            onResult(chooser.selectedFile.readBytes())
        } else {
            onResult(null)
        }
    }
}
