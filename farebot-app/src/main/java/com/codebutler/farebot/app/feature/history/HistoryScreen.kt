/*
 * HistoryScreen.kt
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

@file:Suppress("UNNECESSARY_NOT_NULL_ASSERTION")

package com.codebutler.farebot.app.feature.history

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.codebutler.farebot.R
import com.codebutler.farebot.app.core.activity.ActivityOperations
import com.codebutler.farebot.app.core.activity.ActivityResult
import com.codebutler.farebot.app.core.activity.RequestPermissionsResult
import com.codebutler.farebot.app.core.inject.ScreenScope
import com.codebutler.farebot.app.core.kotlin.Optional
import com.codebutler.farebot.app.core.kotlin.filterAndGetOptional
import com.codebutler.farebot.app.core.transit.TransitFactoryRegistry
import com.codebutler.farebot.app.core.ui.ActionBarOptions
import com.codebutler.farebot.app.core.ui.FareBotScreen
import com.codebutler.farebot.app.core.util.ErrorUtils
import com.codebutler.farebot.app.core.util.ExportHelper
import com.codebutler.farebot.app.feature.card.CardScreen
import com.codebutler.farebot.app.feature.main.MainActivity
import com.codebutler.farebot.card.serialize.CardSerializer
import com.codebutler.farebot.persist.CardPersister
import com.codebutler.farebot.persist.db.model.SavedCard
import com.codebutler.farebot.transit.TransitIdentity
import com.uber.autodispose.ObservableScoper
import com.uber.autodispose.SingleScoper
import dagger.Component
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.io.File
import javax.inject.Inject

class HistoryScreen : FareBotScreen<HistoryScreen.HistoryComponent, HistoryScreenView>(), HistoryScreenView.Listener {

    companion object {
        private val REQUEST_SELECT_FILE = 1
        private val REQUEST_PERMISSION_STORAGE = 2
        private val FILENAME = "farebot-export.json"
    }

    @Inject lateinit var activityOperations: ActivityOperations
    @Inject lateinit var cardPersister: CardPersister
    @Inject lateinit var cardSerializer: CardSerializer
    @Inject lateinit var exportHelper: ExportHelper
    @Inject lateinit var transitFactoryRegistry: TransitFactoryRegistry

    override fun getTitle(context: Context): String = context.getString(R.string.history)

    override fun getActionBarOptions(): ActionBarOptions = ActionBarOptions(
            backgroundColorRes = R.color.accent,
            textColorRes = R.color.white
    )

    override fun onCreateView(context: Context): HistoryScreenView
            = HistoryScreenView(context, activityOperations, this)

    override fun onUpdateMenu(menu: Menu) {
        activity.menuInflater.inflate(R.menu.screen_history, menu)
    }

    override fun onShow(context: Context) {
        super.onShow(context)

        activityOperations.menuItemClick
                .to(ObservableScoper<MenuItem>(this))
                .subscribe { menuItem ->
                    val clipboardManager = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    when (menuItem.itemId) {
                        R.id.import_file -> {
                            val storageUri = Uri.fromFile(Environment.getExternalStorageDirectory())
                            val target = Intent(Intent.ACTION_GET_CONTENT)
                            target.putExtra(Intent.EXTRA_STREAM, storageUri)
                            target.type = "*/*"
                            activity.startActivityForResult(
                                    Intent.createChooser(target, activity.getString(R.string.select_file)),
                                    REQUEST_SELECT_FILE)
                        }
                        R.id.import_clipboard -> {
                            val importClip = clipboardManager.primaryClip
                            if (importClip != null && importClip.itemCount > 0) {
                                val text = importClip.getItemAt(0).coerceToText(activity).toString()
                                onCardsImported(exportHelper.importCards(text))
                            }
                        }
                        R.id.copy -> {
                            val exportClip = ClipData.newPlainText(null, exportHelper.exportCards())
                            clipboardManager.primaryClip = exportClip
                            Toast.makeText(activity, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
                        }
                        R.id.share -> {
                            val intent = Intent(Intent.ACTION_SEND)
                            intent.type = "text/plain"
                            intent.putExtra(Intent.EXTRA_TEXT, exportHelper.exportCards())
                            activity.startActivity(intent)
                        }
                        R.id.save -> exportToFile()
                    }
                }

        activityOperations.permissionResult
                .to(ObservableScoper<RequestPermissionsResult>(this))
                .subscribe { result ->
                    when (result.requestCode) {
                        REQUEST_PERMISSION_STORAGE -> {
                            if (result.grantResults.getOrNull(0) == PackageManager.PERMISSION_GRANTED) {
                                exportToFileWithPermission()
                            }
                        }
                    }
                }

        activityOperations.activityResult
                .to(ObservableScoper<ActivityResult>(this))
                .subscribe { result ->
                    when (result.requestCode) {
                        REQUEST_SELECT_FILE -> {
                            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                                val uri = result.data.data
                                val json = activity.contentResolver.openInputStream(uri)
                                        .bufferedReader()
                                        .use { it.readText() }
                                onCardsImported(exportHelper.importCards(json))
                            }
                        }
                    }
                }

        loadCards()

        view.observeItemClicks()
                .to(ObservableScoper<HistoryViewModel>(this))
                .subscribe { viewModel -> navigator.goTo(CardScreen(viewModel.rawCard)) }
    }

    override fun onDeleteSelectedItems(items: List<HistoryViewModel>) {
        for (item in items) {
            cardPersister.deleteCard(item.savedCard)
        }
        loadCards()
    }

    override fun createComponent(parentComponent: MainActivity.MainActivityComponent): HistoryComponent
            = DaggerHistoryScreen_HistoryComponent.builder()
            .mainActivityComponent(parentComponent)
            .build()

    override fun inject(component: HistoryComponent) {
        component.inject(this)
    }

    private fun loadCards() {
        observeCards()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .to(SingleScoper<List<HistoryViewModel>>(this))
                .subscribe(
                        { viewModels -> view.setViewModels(viewModels) },
                        { e -> ErrorUtils.showErrorToast(activity, e) })
    }

    private fun observeCards(): Single<List<HistoryViewModel>> {
        return Single.create<List<SavedCard>> { e ->
            try {
                e.onSuccess(cardPersister.cards)
            } catch (error: Throwable) {
                e.onError(error)
            }
        }.map { savedCards ->
            savedCards.map { savedCard ->
                val rawCard = cardSerializer.deserialize(savedCard.data())
                var transitIdentity: TransitIdentity? = null
                var parseException: Exception? = null
                try {
                    transitIdentity = transitFactoryRegistry.parseTransitIdentity(rawCard.parse())
                } catch (ex: Exception) {
                    parseException = ex
                }
                HistoryViewModel(savedCard, rawCard, transitIdentity, parseException)
            }
        }
    }

    private fun onCardsImported(cardIds: List<Long>) {
        loadCards()

        val text = activity.resources.getQuantityString(R.plurals.cards_imported, cardIds.size, cardIds.size)
        Toast.makeText(activity, text, Toast.LENGTH_SHORT).show()

        if (cardIds.size == 1) {
            Single.create<Optional<SavedCard>> { e -> e.onSuccess(Optional(cardPersister.getCard(cardIds[0]))) }
                    .filterAndGetOptional()
                    .map { savedCard -> cardSerializer.deserialize(savedCard.data()) }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { rawCard -> navigator.goTo(CardScreen(rawCard)) }
        }
    }

    private fun exportToFile() {
        val permission = ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE)
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    REQUEST_PERMISSION_STORAGE)
        } else {
            exportToFileWithPermission()
        }
    }

    private fun exportToFileWithPermission() {
        try {
            val file = File(Environment.getExternalStorageDirectory(), FILENAME)
            file.writeText(exportHelper.exportCards())
            Toast.makeText(activity, activity.getString(R.string.saved_to_x, FILENAME), Toast.LENGTH_SHORT).show()
        } catch (ex: Exception) {
            ErrorUtils.showErrorAlert(activity, ex)
        }
    }

    @ScreenScope
    @Component(dependencies = arrayOf(MainActivity.MainActivityComponent::class))
    interface HistoryComponent {
        fun inject(historyScreen: HistoryScreen)
    }
}

