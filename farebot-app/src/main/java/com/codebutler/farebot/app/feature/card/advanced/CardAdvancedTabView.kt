/*
 * CardAdvancedTabView.kt
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

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.widget.FrameLayout
import com.codebutler.farebot.R
import com.codebutler.farebot.app.core.kotlin.bindView
import com.codebutler.farebot.base.ui.FareBotUiTree

class CardAdvancedTabView : FrameLayout {

    private val recyclerView: RecyclerView by bindView(R.id.recycler)

    constructor(context: Context?)
            : super(context)

    constructor(context: Context?, attrs: AttributeSet?)
            : super(context, attrs)

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int)
            : super(context, attrs, defStyleAttr)

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int)
            : super(context, attrs, defStyleAttr, defStyleRes)

    fun setAdvancedUi(fareBotUiTree: FareBotUiTree) {
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = CardAdvancedAdapter(fareBotUiTree)
        recyclerView.addItemDecoration(object : RecyclerView.ItemDecoration() {
            val divider: Drawable

            init {
                val attrs = intArrayOf(android.R.attr.listDivider)
                val ta = context.applicationContext.obtainStyledAttributes(attrs)
                divider = ta.getDrawable(0)
                ta.recycle()
            }

            override fun onDraw(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
                for (i in 0..parent.childCount - 1) {
                    val child = parent.getChildAt(i)

                    if (parent.getChildAdapterPosition(child) == parent.adapter.itemCount - 1) {
                        continue
                    }

                    val params = child.layoutParams as RecyclerView.LayoutParams
                    val childLeft = left + child.paddingLeft
                    val childTop = child.bottom + params.bottomMargin
                    val childBottom = childTop + divider.intrinsicHeight

                    divider.setBounds(childLeft, childTop, right, childBottom)
                    divider.draw(canvas)
                }
            }
        })
    }
}
