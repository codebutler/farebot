/*
 * AddKeyScreenView.kt
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

package com.codebutler.farebot.app.feature.keys.add

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.widget.Button
import android.widget.SimpleAdapter
import android.widget.Spinner
import android.widget.TextView
import com.codebutler.farebot.R
import com.codebutler.farebot.app.core.kotlin.bindView
import com.codebutler.farebot.base.util.ByteUtils
import com.codebutler.farebot.card.classic.key.ClassicSectorKey
import com.wealthfront.magellan.BaseScreenView

@SuppressLint("ViewConstructor")
class AddKeyScreenView(context: Context, val listener: Listener)
    : BaseScreenView<AddKeyScreen>(context) {

    val cardTypeTextView: TextView by bindView(R.id.card_type)
    val contentView: View by bindView(R.id.content)
    val importFileButton: Button by bindView(R.id.import_file)
    val importClipboardButton: Button by bindView(R.id.import_clipboard)
    val keyDataTextView: TextView by bindView(R.id.key_data)
    val keyTypeSpinner: Spinner by bindView(R.id.key_type)
    val saveButton: Button by bindView(R.id.save)
    val splashView: View by bindView(R.id.splash)
    val tagIdTextView: TextView by bindView(R.id.tag_id)

    init {
        inflate(context, R.layout.screen_keys_add, this)

        importFileButton.setOnClickListener { listener.onImportFile() }
        importClipboardButton.setOnClickListener { listener.onImportClipboard() }
        saveButton.setOnClickListener { listener.onSave() }

        val adapter = SimpleAdapter(
                context,
                listOf(mapOf("name" to ClassicSectorKey.TYPE_KEYA), mapOf("name" to ClassicSectorKey.TYPE_KEYB)),
                android.R.layout.simple_spinner_item, arrayOf("name"),
                intArrayOf(android.R.id.text1))
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        keyTypeSpinner.adapter = adapter
        keyTypeSpinner.setSelection(1)
    }

    fun update(tagInfo: AddKeyScreen.TagInfo) {
        tagIdTextView.text = ByteUtils.getHexString(tagInfo.tagId)
        cardTypeTextView.text = tagInfo.cardType.toString()
        keyDataTextView.text = ByteUtils.getHexString(tagInfo.keyData, null)

        contentView.visibility = if (tagInfo.tagId != null) View.VISIBLE else View.GONE
        splashView.visibility = if (tagInfo.tagId != null) View.GONE else View.VISIBLE
        saveButton.isEnabled = tagInfo.tagId != null && tagInfo.cardType != null && tagInfo.keyData != null
    }

    @Suppress("UNCHECKED_CAST")
    val keyType: String
        get() = (keyTypeSpinner.selectedItem as Map<String, String>)["name"]!!
    interface Listener {
        fun onImportFile()
        fun onImportClipboard()
        fun onSave()
    }
}
