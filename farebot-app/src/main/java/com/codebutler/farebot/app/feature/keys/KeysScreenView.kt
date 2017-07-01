/*
 * KeysScreenView.kt
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

package com.codebutler.farebot.app.feature.keys

import android.content.Context
import android.support.v7.view.ActionMode
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.Menu
import android.view.MenuItem
import com.codebutler.farebot.R
import com.codebutler.farebot.app.core.activity.ActivityOperations
import com.codebutler.farebot.app.core.kotlin.bindView
import com.jakewharton.rxrelay2.PublishRelay
import com.uber.autodispose.ObservableScoper
import com.uber.autodispose.android.ViewScopeProvider
import com.wealthfront.magellan.BaseScreenView

class KeysScreenView(
        context: Context,
        val activityOperations: ActivityOperations,
        val listener: KeysScreenView.Listener)
    : BaseScreenView<KeysScreen>(context) {

    private val selectionRelay = PublishRelay.create<List<KeyViewModel>>()

    private val recyclerView: RecyclerView by bindView(R.id.recycler)

    private var actionMode : ActionMode? = null

    init {
        inflate(context, R.layout.screen_keys, this)
        recyclerView.layoutManager = LinearLayoutManager(context)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        selectionRelay
                .to(ObservableScoper(ViewScopeProvider.from(this)))
                .subscribe { items ->
                    if (items.isNotEmpty()) {
                        if (actionMode == null) {
                            actionMode = activityOperations.startActionMode(object : ActionMode.Callback {
                                override fun onCreateActionMode(actionMode: ActionMode, menu: Menu): Boolean {
                                    actionMode.menuInflater.inflate(R.menu.action_keys, menu)
                                    return true
                                }

                                override fun onActionItemClicked(actionMode: ActionMode, menuItem: MenuItem): Boolean {
                                    @Suppress("UNCHECKED_CAST")
                                    when (menuItem.itemId) {
                                        R.id.delete -> {
                                            listener.onDeleteSelectedItems(actionMode.tag as List<KeyViewModel>)
                                        }
                                    }
                                    actionMode.finish()
                                    return false
                                }

                                override fun onPrepareActionMode(p0: ActionMode?, p1: Menu?): Boolean = false

                                override fun onDestroyActionMode(actionMode: ActionMode?) {
                                    this@KeysScreenView.actionMode = null
                                    (recyclerView.adapter as? KeysAdapter)?.clearSelectedItems()
                                }
                            })
                        }
                        actionMode?.title = items.size.toString()
                        actionMode?.tag = items
                    } else {
                        actionMode?.finish()
                    }
                }
    }

    fun setViewModels(viewModels: List<KeyViewModel>) {
        recyclerView.adapter = KeysAdapter(viewModels, selectionRelay)
    }

    interface Listener {
        fun onDeleteSelectedItems(items: List<KeyViewModel>)
    }
}

