/*
 * MainActivity.kt
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2017 Eric Butler <eric@codebutler.com>
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

package com.codebutler.farebot.app.feature.main

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.MifareClassic
import android.nfc.tech.MifareUltralight
import android.nfc.tech.NfcF
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import com.codebutler.farebot.app.core.app.FareBotApplication
import com.codebutler.farebot.app.core.platform.AndroidPlatformActions
import com.codebutler.farebot.app.feature.home.AndroidCardScanner
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.BuildConfig
import com.codebutler.farebot.shared.FareBotApp
import com.codebutler.farebot.shared.di.LocalAppGraph
import com.codebutler.farebot.shared.platform.initDeviceRegion

class MainActivity : ComponentActivity() {

    companion object {
        private const val ACTION_TAG = "com.codebutler.farebot.ACTION_TAG"
        private const val INTENT_EXTRA_TAG = "android.nfc.extra.TAG"

        private val TECH_LISTS = arrayOf(
                arrayOf(IsoDep::class.java.name),
                arrayOf(MifareClassic::class.java.name),
                arrayOf(MifareUltralight::class.java.name),
                arrayOf(NfcF::class.java.name))

    }

    private val graph by lazy { (application as FareBotApplication).graph }

    private var nfcReceiver: BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Start observing NFC tags
        (graph.cardScanner as? AndroidCardScanner)?.startObservingTags()

        // Handle initial tag from launch intent
        if (savedInstanceState == null) {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra<Tag>(INTENT_EXTRA_TAG)?.let { tag ->
                graph.nfcStream.emitTag(tag)
            }
            handleFileIntent(intent)
        }

        // Register broadcast receiver for NFC tags
        nfcReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                @Suppress("DEPRECATION")
                val tag = intent.getParcelableExtra<Tag>(INTENT_EXTRA_TAG)
                if (tag != null) {
                    graph.nfcStream.emitTag(tag)
                }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(nfcReceiver, IntentFilter(ACTION_TAG), RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(nfcReceiver, IntentFilter(ACTION_TAG))
        }

        initDeviceRegion(this)

        val supportedCardTypes = CardType.entries.toSet().let { all ->
            if (packageManager.hasSystemFeature("com.nxp.mifare")) all
            else all - setOf(CardType.MifareClassic)
        }
        val platformActions = AndroidPlatformActions(this)
        platformActions.registerFilePickerLauncher(this)

        setContent {
            CompositionLocalProvider(LocalAppGraph provides graph) {
                FareBotApp(
                    platformActions = platformActions,
                    supportedCardTypes = supportedCardTypes,
                    isDebug = BuildConfig.DEBUG,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val intent = Intent(ACTION_TAG)
        intent.`package` = packageName
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE
        } else {
            0
        }
        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, flags)
        val nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, null, TECH_LISTS)
    }

    override fun onPause() {
        super.onPause()
        val nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        nfcReceiver?.let { unregisterReceiver(it) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleFileIntent(intent)
    }

    @Suppress("DEPRECATION")
    private fun handleFileIntent(intent: Intent) {
        val uri = intent.data
            ?: intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            ?: return
        val text = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        if (text != null) {
            graph.cardImporter.submitImport(text)
        }
    }
}
