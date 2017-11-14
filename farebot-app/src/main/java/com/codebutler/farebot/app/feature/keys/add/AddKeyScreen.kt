/*
 * AddKeyScreen.kt
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

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.nfc.Tag
import android.os.Environment
import android.support.v7.app.AlertDialog
import com.codebutler.farebot.R
import com.codebutler.farebot.app.core.activity.ActivityOperations
import com.codebutler.farebot.app.core.activity.ActivityResult
import com.codebutler.farebot.app.core.inject.ScreenScope
import com.codebutler.farebot.app.core.nfc.NfcStream
import com.codebutler.farebot.app.core.serialize.CardKeysSerializer
import com.codebutler.farebot.app.core.ui.ActionBarOptions
import com.codebutler.farebot.app.core.ui.FareBotScreen
import com.codebutler.farebot.app.feature.main.MainActivity
import com.codebutler.farebot.base.util.ByteUtils
import com.codebutler.farebot.card.CardType
import com.codebutler.farebot.card.classic.key.ClassicCardKeys
import com.codebutler.farebot.persist.CardKeysPersister
import com.codebutler.farebot.persist.db.model.SavedKey
import com.uber.autodispose.ObservableScoper
import dagger.Component
import io.reactivex.android.schedulers.AndroidSchedulers
import javax.inject.Inject

class AddKeyScreen : FareBotScreen<AddKeyScreen.AddKeyComponent, AddKeyScreenView>(), AddKeyScreenView.Listener {

    companion object {
        private val REQUEST_SELECT_FILE = 1
    }

    @Inject lateinit var activityOperations: ActivityOperations
    @Inject lateinit var cardKeysPersister: CardKeysPersister
    @Inject lateinit var cardKeysSerializer: CardKeysSerializer
    @Inject lateinit var nfcStream: NfcStream

    private var tagInfo: TagInfo = TagInfo()

    override fun getTitle(context: Context): String = context.getString(R.string.add_key)

    override fun getActionBarOptions(): ActionBarOptions = ActionBarOptions(
            backgroundColorRes = R.color.accent,
            textColorRes = R.color.white
    )

    override fun onShow(context: Context) {
        super.onShow(context)

        nfcStream.observe()
                .observeOn(AndroidSchedulers.mainThread())
                .to(ObservableScoper<Tag>(this))
                .subscribe { tag -> tag.id
                    val cardType = getCardType(tag)
                    if (cardType == null) {
                        AlertDialog.Builder(activity)
                                .setMessage(R.string.card_keys_not_supported)
                                .setPositiveButton(android.R.string.ok, null)
                                .show()
                        return@subscribe
                    }
                    tagInfo.tagId = tag.id
                    tagInfo.cardType = cardType
                    view.update(tagInfo)
                }

        activityOperations.activityResult
                .to(ObservableScoper<ActivityResult>(this))
                .subscribe { (requestCode, resultCode, dataIntent) ->
                    when (requestCode) {
                        REQUEST_SELECT_FILE -> {
                            if (resultCode == Activity.RESULT_OK && dataIntent != null) {
                                setKey(activity.contentResolver.openInputStream(dataIntent.data).readBytes())
                            }
                        }
                    }
                }
    }

    override fun onCreateView(context: Context): AddKeyScreenView = AddKeyScreenView(context, this)

    override fun onImportFile() {
        val storageUri = Uri.fromFile(Environment.getExternalStorageDirectory())
        val target = Intent(Intent.ACTION_GET_CONTENT)
        target.putExtra(Intent.EXTRA_STREAM, storageUri)
        target.type = "*/*"
        activity.startActivityForResult(
                Intent.createChooser(target, activity.getString(R.string.select_file)),
                REQUEST_SELECT_FILE)
    }

    override fun onSave() {
        val tagId = tagInfo.tagId
        val keyData = tagInfo.keyData
        val cardType = tagInfo.cardType
        if (tagId != null && keyData != null && cardType != null) {
            val serializedKey = cardKeysSerializer.serialize(ClassicCardKeys.fromProxmark3(keyData))
            cardKeysPersister.insert(SavedKey.create(ByteUtils.getHexString(tagId), cardType, serializedKey))
            navigator.goBack()
        }
    }

    override fun createComponent(parentComponent: MainActivity.MainActivityComponent): AddKeyComponent
            = DaggerAddKeyScreen_AddKeyComponent.builder()
            .mainActivityComponent(parentComponent)
            .build()

    override fun inject(component: AddKeyComponent) {
        component.inject(this)
    }

    private fun setKey(keyData: ByteArray) {
        tagInfo.keyData = keyData
        view.update(tagInfo)
    }

    private fun getCardType(tag: Tag): CardType? = when {
        "android.nfc.tech.MifareClassic" in tag.techList -> CardType.MifareClassic
        else -> null
    }

    data class TagInfo(var tagId: ByteArray? = null, var cardType: CardType? = null, var keyData: ByteArray? = null)

    @ScreenScope
    @Component(dependencies = arrayOf(MainActivity.MainActivityComponent::class))
    interface AddKeyComponent {

        fun inject(screen: AddKeyScreen)
    }
}
