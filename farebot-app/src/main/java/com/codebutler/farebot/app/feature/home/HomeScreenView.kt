/*
 * HomeScreenView.kt
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

package com.codebutler.farebot.app.feature.home

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.ViewPropertyAnimator
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.codebutler.farebot.BuildConfig
import com.codebutler.farebot.R
import com.codebutler.farebot.app.core.kotlin.bindView
import com.wealthfront.magellan.BaseScreenView

@SuppressLint("ViewConstructor")
class HomeScreenView internal constructor(ctx: Context, private val listener: Listener) :
    BaseScreenView<HomeScreen>(ctx) {

    private val splashImageView: ImageView by bindView(R.id.splash)
    private val progressBar: ProgressBar by bindView(R.id.progress)
    private val errorViewGroup: ViewGroup by bindView(R.id.nfc_error_viewgroup)
    private val errorTextView: TextView by bindView(R.id.nfc_error_text)
    private val errorButton: TextView by bindView(R.id.nfc_error_button)

    private val shortAnimationDuration = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()

    private var fadeInAnim: ViewPropertyAnimator? = null
    private var fadeOutAnim: ViewPropertyAnimator? = null

    init {
        inflate(context, R.layout.screen_home, this)
        errorButton.setOnClickListener { listener.onNfcErrorButtonClicked() }

        if (BuildConfig.DEBUG) {
            splashImageView.setOnLongClickListener { listener.onSampleButtonClicked(); true }
        }
    }

    fun showLoading(show: Boolean) {
        fadeInAnim?.cancel()
        fadeOutAnim?.cancel()

        val viewFadeIn = if (show) progressBar else splashImageView
        val viewFadeOut = if (show) splashImageView else progressBar

        viewFadeIn.alpha = 0f
        viewFadeIn.visibility = View.VISIBLE

        fadeInAnim = viewFadeIn.animate()
                .alpha(1f)
                .setDuration(shortAnimationDuration)
                .setListener(null)

        fadeOutAnim = viewFadeOut.animate()
                .alpha(0f)
                .setDuration(shortAnimationDuration)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator?) {
                        viewFadeOut.visibility = View.GONE
                    }
                })
    }

    internal fun showNfcError(error: NfcError) {
        if (error == NfcError.NONE) {
            errorViewGroup.visibility = View.GONE
            return
        }
        when (error) {
            HomeScreenView.NfcError.DISABLED -> {
                errorTextView.setText(R.string.nfc_off_error)
                errorButton.visibility = View.VISIBLE
            }
            HomeScreenView.NfcError.UNAVAILABLE -> {
                errorTextView.setText(R.string.nfc_unavailable)
                errorButton.visibility = View.GONE
            }
            HomeScreenView.NfcError.NONE -> { /* Unreachable */ }
        }
        errorViewGroup.visibility = View.VISIBLE
    }

    internal enum class NfcError {
        NONE,
        DISABLED,
        UNAVAILABLE
    }

    internal interface Listener {
        fun onNfcErrorButtonClicked()
        fun onSampleButtonClicked()
    }
}
