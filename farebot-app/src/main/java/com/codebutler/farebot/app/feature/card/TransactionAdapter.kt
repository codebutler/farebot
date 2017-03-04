/*
 * TransactionAdapter.kt
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

import android.support.annotation.LayoutRes
import android.support.v7.widget.RecyclerView
import android.text.format.DateFormat
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.codebutler.farebot.R
import com.codebutler.farebot.app.core.inflate
import com.codebutler.farebot.app.core.kotlin.bindView
import com.codebutler.farebot.app.feature.card.TransactionAdapter.TransactionViewHolder.RefillViewHolder
import com.codebutler.farebot.app.feature.card.TransactionAdapter.TransactionViewHolder.SubscriptionViewHolder
import com.codebutler.farebot.app.feature.card.TransactionAdapter.TransactionViewHolder.TripViewHolder
import com.jakewharton.rxrelay2.PublishRelay
import java.util.Calendar
import java.util.Date

class TransactionAdapter(
        val viewModels : List<TransactionViewModel>,
        private val relayClicks: PublishRelay<TransactionViewModel>)
    : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    companion object {
        private const val TYPE_TRIP = 0
        private const val TYPE_REFILL = 1
        private const val TYPE_SUBSCRIPTION = 2
    }

    override fun getItemCount(): Int = viewModels.size

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): TransactionViewHolder = when (viewType) {
        TYPE_TRIP -> TripViewHolder(viewGroup)
        TYPE_REFILL -> RefillViewHolder(viewGroup)
        TYPE_SUBSCRIPTION -> SubscriptionViewHolder(viewGroup)
        else -> throw IllegalArgumentException()
    }

    override fun onBindViewHolder(viewHolder: TransactionViewHolder, position: Int) {
        val viewModel = viewModels.get(position)
        viewHolder.updateHeader(viewModel, isFirstInSection(position))
        when (viewHolder) {
            is TripViewHolder -> viewHolder.update(viewModel as TransactionViewModel.TripViewModel, relayClicks)
            is RefillViewHolder -> viewHolder.update(viewModel as TransactionViewModel.RefillViewModel)
            is SubscriptionViewHolder -> viewHolder.update(viewModel as TransactionViewModel.SubscriptionViewModel)
        }
    }

    override fun getItemViewType(position: Int): Int = when (viewModels.get(position)) {
        is TransactionViewModel.TripViewModel -> TYPE_TRIP
        is TransactionViewModel.RefillViewModel -> TYPE_REFILL
        is TransactionViewModel.SubscriptionViewModel -> TYPE_SUBSCRIPTION
    }

    sealed class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        companion object {
            fun wrapLayout(parent: ViewGroup, @LayoutRes layoutId: Int): View
                    = parent.inflate(R.layout.item_transaction).apply {
                (findViewById(R.id.container) as ViewGroup).inflate(layoutId, true)
            }
        }

        val header: TextView by bindView(R.id.header)

        fun updateHeader(item : TransactionViewModel, isFirstInSection: Boolean) {
            val showHeader = isFirstInSection
            header.visibility = if (showHeader) View.VISIBLE else View.GONE
            if (showHeader) {
                if (item is TransactionViewModel.SubscriptionViewModel) {
                    header.text = header.context.getString(R.string.subscriptions)
                } else {
                    header.text = DateFormat.getLongDateFormat(header.context).format(item.date)
                }
            }
        }

        class TripViewHolder(parent: ViewGroup)
            : TransactionViewHolder(wrapLayout(parent, R.layout.item_transaction_trip)) {

            val item : View by bindView(R.id.item)
            val image : ImageView by bindView(R.id.image)
            val route : TextView by bindView(R.id.route)
            val agency : TextView by bindView(R.id.agency)
            val stations : TextView by bindView(R.id.stations)
            val fare : TextView by bindView(R.id.fare)
            val time : TextView by bindView(R.id.time)

            fun update(viewModel: TransactionViewModel.TripViewModel, relayClicks: PublishRelay<TransactionViewModel>) {
                image.setImageResource(viewModel.imageResId)

                route.text = viewModel.route
                agency.text = viewModel.agency
                stations.text = viewModel.stations
                fare.text = viewModel.fare
                time.text = viewModel.time

                updateTextViewVisibility(route)
                updateTextViewVisibility(agency)
                updateTextViewVisibility(stations)
                updateTextViewVisibility(fare)
                updateTextViewVisibility(time)

                item.setOnClickListener { relayClicks.accept(viewModel) }
            }

            private fun updateTextViewVisibility(textView: TextView) {
                textView.visibility = if (textView.text.isNullOrEmpty()) View.GONE else View.VISIBLE
            }
        }

        class RefillViewHolder(parent: ViewGroup)
            : TransactionViewHolder(wrapLayout(parent, R.layout.item_transaction_refill)) {

            val agency : TextView by bindView(R.id.agency)
            val amount : TextView by bindView(R.id.amount)
            val time : TextView by bindView(R.id.time)

            fun update(viewModel: TransactionViewModel.RefillViewModel) {
                agency.text = viewModel.agency
                amount.text = viewModel.amount
                time.text = viewModel.time
            }
        }

        class SubscriptionViewHolder(parent: ViewGroup)
            : TransactionViewHolder(wrapLayout(parent, R.layout.item_transaction_subscription)) {

            val agency : TextView by bindView(R.id.agency)
            val name : TextView by bindView(R.id.name)
            val valid : TextView by bindView(R.id.valid)
            val used : TextView by bindView(R.id.used)

            fun update(viewModel: TransactionViewModel.SubscriptionViewModel) {
                agency.text = viewModel.agency
                name.text = viewModel.name
                valid.text = viewModel.valid
                used.text = viewModel.used
                used.visibility = if (!viewModel.used.isNullOrEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun isFirstInSection(position: Int): Boolean {
        fun createCalendar(date : Date?) : Calendar? {
            if (date != null) {
                val cal = Calendar.getInstance()
                cal.time = date
                return cal
            }
            return null
        }

        if (position == 0) {
            return true
        }

        val cal1 = createCalendar(viewModels[position].date) ?: return false
        val cal2 = createCalendar(viewModels[position - 1].date) ?: return true

        return cal1.get(Calendar.YEAR) != cal2.get(Calendar.YEAR)
                || cal1.get(Calendar.MONTH) != cal2.get(Calendar.MONTH)
                || cal1.get(Calendar.DAY_OF_MONTH) != cal2.get(Calendar.DAY_OF_MONTH)
    }
}
