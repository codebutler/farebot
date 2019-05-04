/*
 * CardScreenView.kt
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

package com.codebutler.farebot.app.feature.card

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.codebutler.farebot.R
import com.codebutler.farebot.app.core.kotlin.bindView
import com.codebutler.farebot.transit.TransitInfo
import com.jakewharton.rxrelay2.PublishRelay
import com.wealthfront.magellan.BaseScreenView
import com.xwray.groupie.GroupAdapter
import io.reactivex.Observable

class CardScreenView(context: Context) : BaseScreenView<CardScreen>(context) {

    private val clicksRelay = PublishRelay.create<TransactionViewModel>()

    private val balanceLayout: LinearLayout by bindView(R.id.balance_layout)
    private val balanceTextView: TextView by bindView(R.id.balance)
    private val errorTextView: TextView by bindView(R.id.error)
    private val recycler: RecyclerView by bindView(R.id.recycler)

    init {
        inflate(context, R.layout.screen_card, this)
        recycler.layoutManager = LinearLayoutManager(context)
    }

    internal fun observeItemClicks(): Observable<TransactionViewModel> = clicksRelay.hide()

    fun setTransitInfo(transitInfo: TransitInfo, viewModels: List<TransactionViewModel>) {
        val balance = transitInfo.getBalanceString(resources)
        if (balance.isEmpty()) {
            setError(resources.getString(R.string.no_information))
        } else {
            balanceTextView.text = balance
            if (viewModels.isNotEmpty()) {
                recycler.adapter = GroupAdapter<TransactionAdapter.TransactionViewHolder>()
                recycler.adapter = TransactionAdapter(viewModels, clicksRelay)
            } else {
                recycler.visibility = View.GONE
                balanceLayout.layoutParams = LinearLayout.LayoutParams(
                        LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            }
        }
    }

    fun setError(error: String) {
        recycler.visibility = View.GONE
        balanceLayout.visibility = View.GONE
        errorTextView.visibility = View.VISIBLE
        errorTextView.text = error
    }
}
