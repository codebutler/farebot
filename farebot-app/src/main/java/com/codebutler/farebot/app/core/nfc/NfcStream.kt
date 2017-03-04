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
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.MifareClassic
import android.nfc.tech.MifareUltralight
import android.nfc.tech.NfcF
import android.os.Bundle
import com.cantrowitz.rxbroadcast.RxBroadcast
import com.codebutler.farebot.app.core.rx.LastValueRelay
import io.reactivex.Observable

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

        val pendingIntent = PendingIntent.getBroadcast(activity, 0, intent, 0)
        val nfcAdapter = NfcAdapter.getDefaultAdapter(activity)
        nfcAdapter?.enableForegroundDispatch(activity, pendingIntent, null, TECH_LISTS)
    }

    fun onPause() {
        val nfcAdapter = NfcAdapter.getDefaultAdapter(activity)
        nfcAdapter?.disableForegroundDispatch(activity)
    }

    fun observe(): Observable<Tag> {
        val broadcastIntents = RxBroadcast.fromBroadcast(activity, IntentFilter(ACTION))
                .map { it.getParcelableExtra<Tag>(INTENT_EXTRA_TAG) }
        return Observable.merge(relay, broadcastIntents)
    }
}
