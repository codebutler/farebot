/*
 * FareBotCrossfadeTransition.kt
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

package com.codebutler.farebot.app.core.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.view.View
import com.wealthfront.magellan.Direction
import com.wealthfront.magellan.NavigationType
import com.wealthfront.magellan.transitions.Transition

class FareBotCrossfadeTransition(context: Context) : Transition {

    private val shortAnimationDuration = context.resources.getInteger(android.R.integer.config_shortAnimTime).toLong()

    override fun animate(
            viewFrom: View,
            viewTo: View,
            navType: NavigationType,
            direction: Direction,
            callback: Transition.Callback) {

        viewTo.alpha = 0f
        viewTo.visibility = View.VISIBLE

        viewTo.animate()
                .alpha(1f)
                .setDuration(shortAnimationDuration)
                .setListener(null)

        viewFrom.animate()
                .alpha(0f)
                .setDuration(shortAnimationDuration)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator?) {
                        viewFrom.visibility = View.GONE
                        callback.onAnimationEnd()
                    }
                })
    }
}
