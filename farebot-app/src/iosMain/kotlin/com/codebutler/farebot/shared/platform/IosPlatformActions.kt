/*
 * IosPlatformActions.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2025 Eric Butler <eric@codebutler.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.codebutler.farebot.shared.platform

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSString
import platform.Foundation.NSTimer
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.stringWithContentsOfURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIAlertController
import platform.UIKit.UIAlertControllerStyleAlert
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIPasteboard
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import platform.UniformTypeIdentifiers.UTTypeData
import platform.UniformTypeIdentifiers.UTTypeJSON
import platform.UniformTypeIdentifiers.UTTypePlainText
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

class IosPlatformActions : PlatformActions {

    override fun openUrl(url: String) {
        val nsUrl = NSURL(string = url)
        UIApplication.sharedApplication.openURL(nsUrl, emptyMap<Any?, Any>(), null)
    }

    override fun openNfcSettings() {
        val settingsUrl = NSURL(string = "App-prefs:root")
        UIApplication.sharedApplication.openURL(settingsUrl, emptyMap<Any?, Any>(), null)
    }

    override fun copyToClipboard(text: String) {
        UIPasteboard.generalPasteboard.string = text
    }

    override fun getClipboardText(): String? {
        return UIPasteboard.generalPasteboard.string
    }

    override fun shareText(text: String) {
        val viewController = getTopViewController() ?: run {
            copyToClipboard(text)
            return
        }
        val activityVC = UIActivityViewController(
            activityItems = listOf(text),
            applicationActivities = null,
        )
        viewController.presentViewController(activityVC, animated = true, completion = null)
    }

    override fun showToast(message: String) {
        val viewController = getTopViewController() ?: return
        val alert = UIAlertController.alertControllerWithTitle(
            title = null,
            message = message,
            preferredStyle = UIAlertControllerStyleAlert,
        )
        viewController.presentViewController(alert, animated = true, completion = null)
        // Auto-dismiss after 1.5 seconds
        NSTimer.scheduledTimerWithTimeInterval(
            interval = 1.5,
            repeats = false,
        ) {
            alert.dismissViewControllerAnimated(true, completion = null)
        }
    }

    override fun pickFileForImport(onResult: (String?) -> Unit) {
        // Delay presentation to let Compose finish its recomposition cycle
        // (presenting a view controller mid-recomposition silently fails on iOS).
        NSTimer.scheduledTimerWithTimeInterval(0.1, repeats = false) {
            val viewController = getTopViewController() ?: run {
                onResult(null)
                return@scheduledTimerWithTimeInterval
            }
            val picker = UIDocumentPickerViewController(
                forOpeningContentTypes = listOf(UTTypeJSON, UTTypePlainText, UTTypeData),
            )
            picker.allowsMultipleSelection = false

            val delegate = DocumentPickerDelegate(onResult)
            // Store strong reference to prevent garbage collection
            picker.delegate = delegate
            objc_ref = delegate

            viewController.presentViewController(picker, animated = true, completion = null)
        }
    }

    override fun saveFileForExport(content: String, defaultFileName: String) {
        val viewController = getTopViewController() ?: return
        val activityVC = UIActivityViewController(
            activityItems = listOf(content),
            applicationActivities = null,
        )
        viewController.presentViewController(activityVC, animated = true, completion = null)
    }

    private fun getTopViewController(): UIViewController? {
        // Use scene-based API (UIApplication.windows is deprecated on iOS 15+)
        val keyWindow = UIApplication.sharedApplication.connectedScenes
            .filterIsInstance<UIWindowScene>()
            .flatMap { it.windows.filterIsInstance<UIWindow>() }
            .firstOrNull { it.isKeyWindow() }
        var topVC = keyWindow?.rootViewController
        while (topVC?.presentedViewController != null) {
            topVC = topVC.presentedViewController
        }
        return topVC
    }

    // Strong reference to prevent delegate from being garbage collected
    private var objc_ref: Any? = null

    @OptIn(ExperimentalForeignApi::class)
    private class DocumentPickerDelegate(
        private val onResult: (String?) -> Unit,
    ) : NSObject(), UIDocumentPickerDelegateProtocol {

        override fun documentPicker(
            controller: UIDocumentPickerViewController,
            didPickDocumentsAtURLs: List<*>,
        ) {
            val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL
            if (url != null) {
                val accessed = url.startAccessingSecurityScopedResource()
                val content: String?
                try {
                    content = NSString.stringWithContentsOfURL(
                        url,
                        encoding = NSUTF8StringEncoding,
                        error = null,
                    )
                } finally {
                    if (accessed) {
                        url.stopAccessingSecurityScopedResource()
                    }
                }
                // Dispatch to next run loop to ensure picker dismiss animation completes
                // before the caller tries to present toasts or navigate.
                dispatch_async(dispatch_get_main_queue()) {
                    onResult(content)
                }
            } else {
                dispatch_async(dispatch_get_main_queue()) {
                    onResult(null)
                }
            }
        }

        override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
            onResult(null)
        }
    }
}
