/*
 * CardAdvancedAdapter.kt
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

package com.codebutler.farebot.app.feature.card.advanced

import androidx.recyclerview.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.codebutler.farebot.app.R
import com.codebutler.farebot.app.core.kotlin.inflate
import com.codebutler.farebot.app.core.kotlin.bindView
import com.codebutler.farebot.base.ui.FareBotUiTree
import com.codebutler.farebot.base.util.ByteArray
import java.util.Locale

// This is not very efficient.️
class CardAdvancedAdapter(fareBotUiTree: FareBotUiTree) :
    RecyclerView.Adapter<CardAdvancedAdapter.ViewHolder>() {

    private var viewModels: List<ViewModel>
    private var visibleViewModels: List<ViewModel> = listOf()

    init {
        viewModels = flatten(fareBotUiTree.items)
        filterViewModels()
    }

    override fun getItemCount(): Int = visibleViewModels.size

    override fun onCreateViewHolder(parent: ViewGroup, position: Int) =
            ViewHolder(parent.inflate(R.layout.item_card_advanced))

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        viewHolder.bind(visibleViewModels[position])
    }

    private fun flatten(items: List<FareBotUiTree.Item>, parent: ViewModel? = null, depth: Int = 0): List<ViewModel> {
        val viewModels = mutableListOf<ViewModel>()
        for (item in items) {
            val viewModel = ViewModel(
                    title = item.title,
                    value = item.value,
                    parent = parent,
                    canExpand = item.children().isNotEmpty(),
                    depth = depth)
            viewModels.add(viewModel)
            viewModels.addAll(flatten(item.children(), viewModel, depth + 1))
        }
        return viewModels
    }

    private fun filterViewModels() {
        visibleViewModels = viewModels.filter { viewModel -> viewModel.visible }
        notifyDataSetChanged()
    }

    data class ViewModel(
        var title: String,
        var value: Any?,
        var parent: ViewModel?,
        var canExpand: Boolean,
        var expanded: Boolean = false,
        var depth: Int
    ) {

        val visible: Boolean
            get() = parent?.let { it.visible && (if (it.canExpand) it.expanded else true) } ?: true
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val title: TextView by bindView(R.id.title)
        val value: TextView by bindView(R.id.value)

        val padding = itemView.resources.getDimensionPixelSize(R.dimen.grid_unit_2x)

        fun bind(viewModel: ViewModel) {
            itemView.apply {
                title.text = viewModel.title

                val viewModelValue = viewModel.value
                if (viewModelValue != null) {
                    when (viewModelValue) {
                        is ByteArray -> value.text = viewModelValue.hex().toUpperCase(Locale.US)
                        else -> value.text = viewModel.value.toString()
                    }
                    value.visibility = View.VISIBLE
                } else {
                    value.text = null
                    value.visibility = View.GONE
                }

                setPadding(padding * viewModel.depth, paddingTop, paddingRight, paddingBottom)
            }

            itemView.setOnClickListener {
                if (viewModel.canExpand) {
                    itemView.post {
                        viewModel.expanded = !viewModel.expanded
                        filterViewModels()
                    }
                }
            }
        }
    }
}
