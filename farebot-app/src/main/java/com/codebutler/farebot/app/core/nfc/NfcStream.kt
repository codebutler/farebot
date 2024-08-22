/*
 * NfcStream.kt
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

package com.codebutler.farebot.app.core.nfc

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.MifareClassic
import android.nfc.tech.MifareUltralight
import android.nfc.tech.NfcF
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import com.codebutler.farebot.app.core.rx.LastValueRelay
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class NfcStream(private val activity: Activity) {

    companion object {
        private val ACTION = "com.codebutler.farebot.ACTION_TAG"
        private val INTENT_EXTRA_TAG = "android.nfc.extra.TAG"

        private val TECH_LISTS = arrayOf(
                arrayOf(IsoDep::class.java.name),
                arrayOf(MifareClassic::class.java.name),
                arrayOf(MifareUltralight::class.java.name),
                arrayOf(NfcF::class.java.name))
    }

    private val relay = LastValueRelay.create<Tag>()

    fun onCreate(activity: Activity, savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            activity.intent.getParcelableExtra<Tag>(INTENT_EXTRA_TAG)?.let {
                relay.accept(it)
            }
        }
    }

    fun onResume() {
        val intent = Intent(ACTION)
        intent.`package` = activity.packageName

        val pendingIntent = PendingIntent.getBroadcast(activity, 0, intent, PendingIntent.FLAG_MUTABLE)
        val nfcAdapter = NfcAdapter.getDefaultAdapter(activity)
        nfcAdapter?.enableForegroundDispatch(activity, pendingIntent, null, TECH_LISTS)
    }

    fun onPause() {
        val nfcAdapter = NfcAdapter.getDefaultAdapter(activity)
        nfcAdapter?.disableForegroundDispatch(activity)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun observe(): Observable<Tag> {
        // Create a PublishSubject to emit broadcast events
        val broadcastSubject = PublishSubject.create<Tag>()

        // Register the BroadcastReceiver manually
        val broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val tag = intent.getParcelableExtra<Tag>(INTENT_EXTRA_TAG)
                if (tag != null) {
                    broadcastSubject.onNext(tag)
                }
            }
        }

        // Register the receiver with the IntentFilter
        activity.registerReceiver(broadcastReceiver, IntentFilter(ACTION), Context.RECEIVER_NOT_EXPORTED)

        // Return an observable that merges the relay and the broadcastSubject
        return Observable.merge(relay, broadcastSubject)
            .doOnDispose {
                // Unregister the receiver when the observable is disposed to avoid leaks
                activity.unregisterReceiver(broadcastReceiver)
            }
    }
}
