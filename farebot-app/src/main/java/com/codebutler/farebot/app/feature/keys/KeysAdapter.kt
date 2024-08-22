/*
 * KeysAdapter.kt
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

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.codebutler.farebot.app.R
import com.codebutler.farebot.app.core.kotlin.bindView
import com.codebutler.farebot.app.core.kotlin.inflate
import com.jakewharton.rxrelay2.PublishRelay

class KeysAdapter(
    private val viewModels: List<KeyViewModel>,
    private val selectionRelay: PublishRelay<List<KeyViewModel>>
) :
    RecyclerView.Adapter<KeysAdapter.KeyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, position: Int): KeyViewHolder =
            KeyViewHolder(parent.inflate(R.layout.item_key))

    override fun onBindViewHolder(holder: KeyViewHolder, position: Int) {
        val viewModel = viewModels[position]
        holder.update(viewModel)
        holder.itemView.setOnClickListener {
            if (hasSelectedItems()) {
                viewModel.isSelected = !viewModel.isSelected
                notifySelectionChanged()
            }
        }
        holder.itemView.setOnLongClickListener {
            if (!hasSelectedItems()) {
                viewModel.isSelected = true
                notifySelectionChanged()
                true
            } else {
                false
            }
        }
    }

    override fun getItemCount(): Int = viewModels.size

    private fun hasSelectedItems(): Boolean = viewModels.any { it.isSelected }

    private fun notifySelectionChanged() {
        notifyDataSetChanged()
        selectionRelay.accept(viewModels.filter { it.isSelected })
    }

    fun clearSelectedItems() {
        for (viewModel in viewModels) {
            viewModel.isSelected = false
        }
        notifySelectionChanged()
    }

    class KeyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView1: TextView by bindView(android.R.id.text1)
        private val textView2: TextView by bindView(android.R.id.text2)

        internal fun update(viewModel: KeyViewModel) {
            textView1.text = viewModel.savedKey.cardId
            textView2.text = viewModel.savedKey.cardType.toString()
            itemView.isSelected = viewModel.isSelected
        }
    }
}
